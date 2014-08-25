package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import tk.mygod.util.LocaleUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

/**
 * Project: Mygod Speech Synthesizer
 * Author:  Mygod (mygod.tk)
 */
public class SvoxPicoTtsEngine extends TtsEngine implements TextToSpeech.OnInitListener {
    protected TextToSpeech tts;
    private final Semaphore initLock = new Semaphore(1);
    public int initStatus;
    public TextToSpeech.EngineInfo engineInfo;
    public SvoxPicoTtsEngine(final Context context) {
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(context, this);
    }
    public SvoxPicoTtsEngine(Context context, TextToSpeech.EngineInfo info) {
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(context, this, (engineInfo = info).name);
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
        supportedLanguages = new TreeSet<Locale>(new LocaleUtil.DisplayNameComparator());
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
    public void speak(String text, OnTtsSynthesisCallbackListener listener) throws IOException {
        // TODO: long text splitting
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void synthesizeToFile(String text, String filename, OnTtsSynthesisCallbackListener listener) {
        // TODO: long text splitting
        tts.synthesizeToFile(text, null, filename);
    }

    @Override
    public void stop() {
        tts.stop();
    }

    @Override
    public void onDestroy() {
        tts.stop();
        tts.shutdown();
    }
}
