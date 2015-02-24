package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.util.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Project:  Mygod Speech Synthesizer
 * @author   Mygod
 */
public final class GoogleTranslateTtsEngine extends TtsEngine {
    private static Set<TtsVoice> voices;
    private LocaleWrapper voice;
    private CharSequence currentText;
    private int startOffset;

    static {
        voices = new TreeSet<>();
        for (String code : new String[] {
                "af", "sq", "ar", "hy", "bs", "ca", "zh-CN", "zh-TW", "hr", "cs", "da", "nl", "en", "eo", "fi", "fr",
                "de", "el", "ht", "hi", "hu", "is", "id", "it", "ja", "la", "lv", "mk", "no", "pl", "pt", "ro", "ru",
                "sr", "sk", "es", "sw", "sv", "ta", "th", "tr", "vi", "cy" }) voices.add(new LocaleWrapper(code));
    }

    protected GoogleTranslateTtsEngine(Context context) {
        super(context);
        setVoice("en");
    }

    @Override
    public Set<TtsVoice> getVoices() {
        return voices;
    }
    @Override
    public TtsVoice getVoice() {
        return voice;
    }
    @Override
    public boolean setVoice(TtsVoice voice) {
        boolean result = voices.contains(voice);
        if (result) this.voice = (LocaleWrapper) voice;
        return result;
    }
    @Override
    public boolean setVoice(String voice) {
        if (voice == null || voice.isEmpty()) return false;
        for (TtsVoice v : voices) if (voice.equals(v.getName())) {
            this.voice = (LocaleWrapper) v;
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return context.getResources().getString(R.string.google_translate_tts_engine_name);
    }
    @Override
    protected Drawable getIconInternal() {
        try {
            return context.getPackageManager().getApplicationIcon("com.google.android.apps.translate");
        } catch (Exception e) { // fallback if you don't have the official app installed :S
            return context.getResources().getDrawable(R.drawable.ic_google_translate);
        }
    }

    @Override
    public String getMimeType() {
        return "audio/mpeg";
    }

    private String getUrl(String text) throws UnsupportedEncodingException {
        return "https://translate.google.com/translate_tts?ie=UTF-8&tl=" + voice.code + "&q=" +
                URLEncoder.encode(text, "UTF-8");
    }
    @Override
    public void speak(CharSequence text, int startOffset) {
        currentText = text;
        this.startOffset = startOffset;
        synthesizeToStreamTask = null;
        (speakTask = new SpeakTask()).execute();
    }
    @Override
    public void synthesizeToStream(CharSequence text, int startOffset, FileOutputStream output, File cacheDir) {
        currentText = text;
        this.startOffset = startOffset;
        speakTask = null;
        new SynthesizeToStreamTask().execute(output);
    }
    @Override
    public void stop() {
        if (speakTask != null) speakTask.stop();
        if (synthesizeToStreamTask != null) synthesizeToStreamTask.cancel(false);
    }

    @Override
    public void onDestroy() {
        stop();
    }

    @Override
    protected int getMaxLength() {
        return 100;
    }

    private SpeakTask speakTask;
    private SynthesizeToStreamTask synthesizeToStreamTask;

    private final class SpeakTask extends AsyncTask<Void, Void, Void> {
        // it seems creating 32+ unreleased MediaPlayer will fail miserably with:
        // java.io.IOException: Prepare failed.: status=0x1
        private final ArrayBlockingQueue<Object> playbackQueue = new ArrayBlockingQueue<>(29);
        private final HashMap<MediaPlayer, SpeechPart> partMap = new HashMap<>(32);
        private PlayerThread playThread;

        public void stop() {
            if (isCancelled()) return;
            cancel(false);
            if (playThread == null || playThread.player == null) return;
            // playback should be stopped instantly or somebody might get angry >:(
            if (playThread.player.isPlaying()) playThread.player.stop();
        }

        private class PlayerThread extends Thread
                implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
            private MediaPlayer player;
            private final Semaphore playLock = new Semaphore(1);

            @Override
            public void run() {
                try {
                    Object obj = playbackQueue.take();
                    while (obj instanceof MediaPlayer) {
                        player = (MediaPlayer) obj;
                        SpeechPart part = partMap.get(player);
                        try {   // not done yet
                            if (!isCancelled()) {
                                if (listener != null) listener.onTtsSynthesisCallback(part.Start, part.End);
                                player.setOnCompletionListener(this);
                                player.setOnErrorListener(this);
                                playLock.acquireUninterruptibly();
                                player.start();
                                playLock.acquireUninterruptibly(); // wait for release
                                playLock.release();
                                if (listener != null) listener.onTtsSynthesisCallback(part.End, part.End);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                        } finally {
                            try {
                                player.stop();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            player.release();
                            obj = playbackQueue.take();
                        }
                    }
                    speakTask = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                SpeechPart part = partMap.get(mp);
                if (listener != null) listener.onTtsSynthesisCallback(part.End, part.End);
                playLock.release();
            }

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                SpeechPart part = partMap.get(mp);
                if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                playLock.release();
                return false;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Disabling aggressive mode to reduce MediaPlayers
                ArrayList<SpeechPart> parts = splitSpeech(currentText, startOffset, false);
                if (isCancelled()) return null;
                (playThread = new PlayerThread()).start();
                for (SpeechPart part : parts) {
                    if (isCancelled()) return null;
                    MediaPlayer player = null;
                    try {
                        while (true) try {
                            player = new MediaPlayer();
                            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            String str = currentText.subSequence(part.Start, part.End).toString();
                            player.setDataSource(part.IsEarcon ? str : getUrl(str));
                            player.prepare();
                            break;
                        } catch (IOException e) {
                            if (!"Prepare failed.: status=0x1".equals(e.getMessage())) throw e;
                            player.release();   // useless copy now
                            Thread.sleep(1000);
                        }
                        if (isCancelled()) return null;
                        partMap.put(player, part);
                        playbackQueue.put(player);
                        if (listener != null) listener.onTtsSynthesisPrepared(part.End);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (player != null) player.release();
                        if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            } finally {
                if (playThread != null) playbackQueue.add(new Object());    // put end sign no matter what
            }
            return null;
        }
    }

    private class SynthesizeToStreamTask extends AsyncTask<FileOutputStream, Void, Void> {
        @Override
        protected Void doInBackground(FileOutputStream... params) {
            if (params.length != 1) throw new InvalidParameterException("Params incorrect.");
            FileOutputStream output = params[0];
            try {
                for (SpeechPart part : splitSpeech(currentText, startOffset, false)) {
                    if (isCancelled()) return null;
                    InputStream input = null;
                    try {
                        if (listener != null) listener.onTtsSynthesisCallback(part.Start, part.End);
                        String str = currentText.subSequence(part.Start, part.End).toString();
                        IOUtils.copy(input = part.IsEarcon  // dummy mp3 merger
                                ? context.getContentResolver().openInputStream(Uri.parse(str))
                                : new URL(getUrl(str)).openStream(), output);
                        if (listener != null) listener.onTtsSynthesisCallback(part.End, part.End);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                    } finally {
                        if (input != null) try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            } finally {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(Void arg) {
            synthesizeToStreamTask = null;
        }
    }
}
