package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Pair;
import tk.mygod.util.LocaleUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class SvoxPicoTtsEngine extends TtsEngine implements TextToSpeech.OnInitListener {
    private final Semaphore initLock = new Semaphore(1);
    protected TextToSpeech tts;
    public int initStatus;
    private Pair<Integer, Integer> last;
    public TextToSpeech.EngineInfo engineInfo;
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
    private String currentText;
    /**
     * Called to signal the completion of the TextToSpeech engine initialization.
     *
     * @param status {@link android.speech.tts.TextToSpeech#SUCCESS}
     * or {@link android.speech.tts.TextToSpeech#ERROR}.
     */
    @Override
    public void onInit(int status) {
        initStatus = status;
        supportedLanguages = new TreeSet<Locale>(new LocaleUtils.DisplayNameComparator());
        for (Locale locale : Locale.getAvailableLocales()) try {
            int test = tts.isLanguageAvailable(locale);
            if (test == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE ||
                    TextUtils.isEmpty(locale.getVariant()) && (test == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                            TextUtils.isEmpty(locale.getCountry()) && test == TextToSpeech.LANG_AVAILABLE))
                supportedLanguages.add(locale);
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
        int test = tts.isLanguageAvailable(loc);
        if (test == TextToSpeech.LANG_AVAILABLE) loc = new Locale(loc.getLanguage());
        else if (test == TextToSpeech.LANG_COUNTRY_AVAILABLE) loc = new Locale(loc.getLanguage(), loc.getCountry());
        else if (test != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) return false;
        tts.setLanguage(loc);
        return true;
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
    public CharSequence getName(Context context) {
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
    public void speak(String text) throws IOException {
        currentText = text;
        new SpeakTask().execute();
    }
    @Override
    public void synthesizeToFile(String text, String filename) {
        // TODO: long text splitting
        tts.synthesizeToFile(text, getParams(0, text.length()), filename);
    }
    @Override
    public void stop() {
        if (speakTask != null) speakTask.cancel(false);
        tts.stop();
    }

    private static Pair<Integer, Integer> getRange(String id) {
        String[] parts = id.split(",");
        return new Pair<Integer, Integer>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
    private void setListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (listener != null) listener.onTtsSynthesisCallback(pair.first, pair.second);
            }

            @Override
            public void onDone(String utteranceId) {
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (listener != null)
                    if (pair.first.equals(last.first) && pair.second.equals(last.second))
                        listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                    else listener.onTtsSynthesisCallback(pair.second, pair.second);
            }

            @Override
            public void onError(String utteranceId) {
                Pair<Integer, Integer> pair = getRange(utteranceId);
                if (listener != null) listener.onTtsSynthesisError(pair.first, pair.second);
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
        return Build.VERSION.SDK_INT >= 18 ? tts.getMaxSpeechInputLength() : 4000;  // fallback to default
    }

    private SpeakTask speakTask;
    private class SpeakTask extends AsyncTask<Void, Integer, Exception> {
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                ArrayList<Pair<Integer, Integer>> ranges = splitSpeech(currentText);
                last = ranges.get(ranges.size() - 1);
                for (Pair<Integer, Integer> range : ranges) try {
                    if (isCancelled()) {
                        tts.stop();
                        return null;
                    }
                    tts.speak(currentText.substring(range.first, range.second), TextToSpeech.QUEUE_ADD,
                              getParams(range.first, range.second));
                    if (listener != null) listener.onTtsSynthesisPrepared(range.second);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
                }
                if (isCancelled()) tts.stop();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
        }

        protected void onPostExecute(Exception e) {
            if (listener != null && e != null) listener.onTtsSynthesisError(0, currentText.length());
            speakTask = null;
        }
    }

    /*private class SynthesizeToFileTask extends AsyncTask<String, Integer, Exception> {
        @Override
        protected Exception doInBackground(String... params) {
            try {
                if (params.length != 1) throw new InvalidParameterException("There must be and only be 1 param.");
                for (Pair<Integer, Integer> range : splitSpeech(currentText)) try {
                    tts.synthesizeToFile(currentText.substring(range.first, range.second), TextToSpeech.QUEUE_ADD,
                            getParams(range.first, range.second));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(range.first, range.second);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
        }

        protected void onPostExecute(Exception e) {
            if (listener == null) return;   // nobody listens?! well YKW fuck it
            if (e != null) listener.onTtsSynthesisError(0, currentText.length());
            listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
        }
    }*/
}
