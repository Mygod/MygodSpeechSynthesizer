package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Pair;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.util.IOUtils;
import tk.mygod.util.LocaleUtils;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Project:  Mygod Speech Synthesizer
 * @author   Mygod
 */
public class GoogleTranslateTtsEngine extends TtsEngine {
    private static Set<Locale> supportedLanguages;
    private String language = "en";
    private CharSequence currentText;
    private int startOffset;

    static {
        supportedLanguages = new TreeSet<Locale>(new LocaleUtils.DisplayNameComparator());
        for (String code : new String[] {
                "af", "sq", "ar", "hy", "bs", "ca", "zh-CN", "zh-TW", "hr", "cs", "da", "nl", "en", "eo", "fi", "fr",
                "de", "el", "ht", "hi", "hu", "is", "id", "it", "ja", "la", "lv", "mk", "no", "pl", "pt", "ro", "ru",
                "sr", "sk", "es", "sw", "sv", "ta", "th", "tr", "vi", "cy" })
            supportedLanguages.add(LocaleUtils.parseLocale(code));
    }

    @Override
    public Set<Locale> getLanguages() {
        return supportedLanguages;
    }
    @Override
    public Locale getLanguage() {
        return LocaleUtils.parseLocale(language);
    }
    @Override
    public boolean setLanguage(Locale loc) {
        String lang = loc.toString().replace('_', '-');
        if (supportedLanguages.contains(loc)) {
            language = lang;
            return true;
        }
        return false;
    }

    @Override
    public String getName(Context context) {
        return context.getResources().getString(R.string.google_translate_tts_engine_name);
    }
    @Override
    protected Drawable getIconInternal(Context context) {
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
        return "https://translate.google.com/translate_tts?ie=UTF-8&tl=" + language + "&q=" +
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
        private final ArrayBlockingQueue<Object> playbackQueue = new ArrayBlockingQueue<Object>(29);
        private final HashMap<MediaPlayer, Pair<Integer, Integer>>
                rangeMap = new HashMap<MediaPlayer, Pair<Integer, Integer>>(32);
        private PlayerThread playThread;

        public void stop() {
            cancel(false);
            if (playThread == null || playThread.player == null) return;
            playThread.player.stop();   // playback should be stopped instantly or somebody might get angry
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
                        Pair<Integer, Integer> pair = rangeMap.get(player);
                        try {   // not done yet
                            if (!isCancelled()) {
                                if (listener != null) listener.onTtsSynthesisCallback(pair.first, pair.second);
                                player.setOnCompletionListener(this);
                                player.setOnErrorListener(this);
                                playLock.acquireUninterruptibly();
                                player.start();
                                playLock.acquireUninterruptibly(); // wait for release
                                playLock.release();
                                if (listener != null) listener.onTtsSynthesisCallback(pair.second, pair.second);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (listener != null) listener.onTtsSynthesisError(pair.first, pair.second);
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
                    if (!isCancelled() && listener != null)
                        listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                    speakTask = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCompletion(MediaPlayer mp) {
                Pair<Integer, Integer> pair = rangeMap.get(mp);
                if (listener != null) listener.onTtsSynthesisCallback(pair.second, pair.second);
                playLock.release();
            }

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Pair<Integer, Integer> pair = rangeMap.get(mp);
                if (listener != null) listener.onTtsSynthesisError(pair.first, pair.second);
                playLock.release();
                return false;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Disabling aggressive mode to reduce MediaPlayers
                ArrayList<Pair<Integer, Integer>> ranges = splitSpeech(currentText, startOffset, false);
                if (isCancelled()) return null;
                (playThread = new PlayerThread()).start();
                for (Pair<Integer, Integer> range : ranges) {
                    if (isCancelled()) return null;
                    MediaPlayer player = null;
                    try {
                        while (true) try {
                            player = new MediaPlayer();
                            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            player.setDataSource(getUrl(currentText.subSequence(range.first, range.second).toString()));
                            player.prepare();
                            break;
                        } catch (IOException e) {
                            if (!"Prepare failed.: status=0x1".equals(e.getMessage())) throw e;
                            player.release();   // useless copy now
                            Thread.sleep(1000);
                        }
                        if (isCancelled()) return null;
                        rangeMap.put(player, range);
                        playbackQueue.put(player);
                        if (listener != null) listener.onTtsSynthesisPrepared(range.second);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (player != null) player.release();
                        if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
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
                for (Pair<Integer, Integer> range : splitSpeech(currentText, startOffset, false)) {
                    if (isCancelled()) return null;
                    InputStream input = null;
                    try {
                        if (listener != null) listener.onTtsSynthesisCallback(range.first, range.second);
                        IOUtils.copy(input = new URL(getUrl(currentText.subSequence(range.first, range.second)
                                .toString())).openStream(), output);
                        if (listener != null) listener.onTtsSynthesisCallback(range.second, range.second);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
                    } finally {
                        if (input != null) try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (listener != null) listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
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
