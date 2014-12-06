package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Pair;
import tk.mygod.util.FileUtils;
import tk.mygod.util.IOUtils;
import tk.mygod.util.LocaleUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class SvoxPicoTtsEngine extends TtsEngine implements TextToSpeech.OnInitListener {
    private final Semaphore initLock = new Semaphore(1);
    protected TextToSpeech tts;
    private Pair<Integer, Integer> last;
    public TextToSpeech.EngineInfo engineInfo;
    private String currentText;
    private int startOffset;

    public SvoxPicoTtsEngine(final Context context) {
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(context, this);
        setListener();
    }
    public SvoxPicoTtsEngine(Context context, TextToSpeech.EngineInfo info) {
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(context, this, (engineInfo = info).name);
        setListener();
    }
    private Set<Locale> supportedLanguages;

    @Override
    public void onInit(int status) {
        supportedLanguages = new TreeSet<Locale>(new LocaleUtils.DisplayNameComparator());
        for (Locale locale : Locale.getAvailableLocales()) try {
            int test = tts.isLanguageAvailable(locale);
            if (test != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE && test != TextToSpeech.LANG_COUNTRY_AVAILABLE &&
                test != TextToSpeech.LANG_AVAILABLE) continue;
            tts.setLanguage(locale);
            supportedLanguages.add(tts.getLanguage());
        } catch (Exception e) { // god damn Samsung TTS
            e.printStackTrace();
        }
        initLock.release();
    }
    @Override
    public Set<Locale> getSupportedLanguages() {
        initLock.acquireUninterruptibly();
        initLock.release();
        return supportedLanguages;
    }
    @Override
    public Locale getLanguage() {
        return tts.getLanguage();
    }
    @Override
    public boolean setLanguage(Locale loc) {
        try {
            tts.setLanguage(loc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public Set<String> getFeatures(Locale locale) {
        return tts.getFeatures(locale);
    }

    @Override
    public String getID() {
        return super.getID() + ':' + engineInfo.name;
    }
    @Override
    public String getName(Context context) {
        return engineInfo.label;
    }
    @Override
    protected Drawable getIconInternal(Context context) {
        return context.getPackageManager().getDrawable(engineInfo.name, engineInfo.icon, null);
    }

    @Override
    public String getMimeType() {
        return "audio/x-wav";
    }

    private Float pan;
    @Override
    public void setPitch(float value) {
        tts.setPitch(value);
    }
    @Override
    public void setSpeechRate(float value) {
        tts.setSpeechRate(value);
    }
    @Override
    public void setPan(float value) {
        pan = value;
    }

    private HashMap<String, String> getParams(int start, int end) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, start + "," + end);
        if (pan != null) params.put(TextToSpeech.Engine.KEY_PARAM_PAN, pan.toString());
        return params;
    }
    @Override
    public void speak(String text, int startOffset) {
        currentText = text;
        this.startOffset = startOffset;
        synthesizeToStreamTask = null;
        (speakTask = new SpeakTask()).execute();
    }
    @Override
    public void synthesizeToStream(String text, int startOffset, FileOutputStream output, File cacheDir) {
        currentText = text;
        this.startOffset = startOffset;
        speakTask = null;
        (synthesizeToStreamTask = new SynthesizeToStreamTask()).execute(output, cacheDir);
    }
    @Override
    public void stop() {
        if (speakTask != null) speakTask.cancel(false);
        if (synthesizeToStreamTask != null) synthesizeToStreamTask.cancel(false);
        tts.stop();
    }

    private void setListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (speakTask == null) return;
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (listener != null) listener.onTtsSynthesisCallback(pair.first, pair.second);
            }

            @Override
            public void onDone(String utteranceId) {
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (synthesizeToStreamTask != null) {
                    synthesizeToStreamTask.mergeQueue.add(utteranceId);
                    synthesizeToStreamTask.synthesizeLock.release();
                    if (listener != null) listener.onTtsSynthesisPrepared(pair.second);
                }
                else if (speakTask != null && listener != null)
                    if (pair.first.equals(last.first) && pair.second.equals(last.second)) {
                        listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                        speakTask = null;
                    }
                    else listener.onTtsSynthesisCallback(pair.second, pair.second);
            }

            @Override
            public void onError(String utteranceId) {
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (listener != null) listener.onTtsSynthesisError(pair.first, pair.second);
                if (synthesizeToStreamTask != null) synthesizeToStreamTask.synthesizeLock.release();
            }
        });
    }

    @Override
    public void onDestroy() {
        stop();
        tts.shutdown();
    }

    @Override
    protected int getMaxLength() {
        return Build.VERSION.SDK_INT >= 18 ? TextToSpeech.getMaxSpeechInputLength() : 4000;  // fallback to default
    }

    private SpeakTask speakTask;
    private SynthesizeToStreamTask synthesizeToStreamTask;

    private class SpeakTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                ArrayList<Pair<Integer, Integer>> ranges = splitSpeech(currentText, startOffset, true);
                last = null;
                for (Pair<Integer, Integer> range : ranges) try {
                    if (isCancelled()) {
                        tts.stop();
                        return null;
                    }
                    tts.speak(processText(currentText.substring(range.first, range.second)), TextToSpeech.QUEUE_ADD,
                              getParams(range.first, range.second));
                    last = range;   // assuming preparer is faster than speaker, which is often the case
                    if (listener != null) listener.onTtsSynthesisPrepared(range.second);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
                }
                if (isCancelled()) tts.stop();
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            }
            if (last == null && listener != null)   // nothing speakable, end now
                listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
            return null;
        }
    }

    private class SynthesizeToStreamTask extends AsyncTask<Object, Void, Void> {
        private FileOutputStream output;
        private final LinkedBlockingDeque<String> mergeQueue = new LinkedBlockingDeque<String>();
        private final HashMap<String, File> pathMap = new HashMap<String, File>();
        public final Semaphore synthesizeLock = new Semaphore(1);

        @Override
        protected Void doInBackground(Object... params) {
            Thread merger = null;
            try {
                if (params.length != 2 || !(params[0] instanceof FileOutputStream) || !(params[1] instanceof File))
                    throw new InvalidParameterException("Params incorrect.");
                output = (FileOutputStream) params[0];
                File cacheDir = (File) params[1];
                ArrayList<Pair<Integer, Integer>> ranges = splitSpeech(currentText, startOffset, false);
                if (isCancelled()) return null;
                (merger = new Thread() {
                    @Override
                    public void run() {
                        try {
                            String id;
                            byte[] header = null;
                            long length = 0;
                            while (!(id = mergeQueue.take()).isEmpty()) {
                                Pair<Integer, Integer> pair = getRange(id);
                                File cache = pathMap.get(id);
                                FileInputStream input = null;
                                try {
                                    if (!isCancelled()) {
                                        if (listener != null) listener.onTtsSynthesisCallback(pair.first, pair.second);
                                        input = new FileInputStream(cache);
                                        if (header == null) {
                                            header = new byte[44];
                                            if (input.read(header, 0, 44) != 44)
                                                throw new IOException("File malformed.");
                                            output.write(header, 0, 44);
                                        } else if (input.skip(44) != 44) throw new IOException("File malformed.");
                                        length += IOUtils.copy(input, output);
                                        if (listener != null) listener.onTtsSynthesisCallback(pair.second, pair.second);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    if (listener != null) listener.onTtsSynthesisError(pair.first, pair.second);
                                } finally {
                                    if (input != null) try {
                                        input.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    pathMap.remove(id);
                                    if (!cache.delete()) cache.deleteOnExit();
                                }
                            }
                            if (header != null) {                   // update the header even if the operation is
                                header[40] = (byte) length;         // cancelled so that saved part can be read
                                header[41] = (byte) (length >> 8);
                                header[42] = (byte) (length >> 16);
                                header[43] = (byte) (length >> 24);
                                length += 36;
                                header[4] = (byte) length;
                                header[5] = (byte) (length >> 8);
                                header[6] = (byte) (length >> 16);
                                header[7] = (byte) (length >> 24);
                                output.getChannel().position(0);
                                output.write(header, 0, 44);
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
                        if (!isCancelled() && listener != null)
                            listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                        synthesizeToStreamTask = null;
                    }
                }).start();
                for (Pair<Integer, Integer> range : ranges) {
                    if (isCancelled()) return null;
                    try {
                        File cache = new File(cacheDir, FileUtils.getTempFileName() + range.first);
                        pathMap.put(range.first + "," + range.second, cache);
                        synthesizeLock.acquireUninterruptibly();
                        tts.synthesizeToFile(processText(currentText.substring(range.first, range.second)),
                                             getParams(range.first, range.second), cache.getAbsolutePath());
                        synthesizeLock.acquireUninterruptibly();    // wait for synthesis
                        synthesizeLock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            } finally {
                if (merger != null) mergeQueue.add(""); // end sign
            }
            return null;
        }
    }
}
