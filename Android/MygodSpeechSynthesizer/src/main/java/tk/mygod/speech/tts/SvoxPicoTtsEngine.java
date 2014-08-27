package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Pair;
import tk.mygod.util.LocaleUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class SvoxPicoTtsEngine extends TtsEngine implements TextToSpeech.OnInitListener {
    protected TextToSpeech tts;
    private final Semaphore initLock = new Semaphore(1);
    public int initStatus;
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
        for (Locale locale : Locale.getAvailableLocales()) {
            int test = tts.isLanguageAvailable(locale);
            if (test == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE ||
                    TextUtils.isEmpty(locale.getVariant()) && (test == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                            TextUtils.isEmpty(locale.getCountry()) && test == TextToSpeech.LANG_AVAILABLE))
                supportedLanguages.add(locale);
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

    @Override
    public void speak(String text) throws IOException {
        // TODO: long text splitting
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0," + text.length());
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }
    @Override
    public void synthesizeToFile(String text, String filename) {
        // TODO: long text splitting
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "0," + text.length());
        tts.synthesizeToFile(text, params, filename);
    }
    @Override
    public void stop() {
        tts.stop();
    }

    private Pair<Integer, Integer> getRange(String id) {
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
                if (listener != null) listener.onTtsSynthesisCallback(pair.second, pair.second);
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
        tts.stop();
        tts.shutdown();
    }
}
