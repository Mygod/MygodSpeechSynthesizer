package tk.mygod.speech.synthesizer;

import android.content.Context;
import android.content.SharedPreferences;
import tk.mygod.speech.tts.AvailableTtsEngines;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.util.LocaleUtils;

import java.util.Locale;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
final class TtsEngineManager {
    private TtsEngineManager() {
        throw new AssertionError();
    }

    static AvailableTtsEngines engines;
    private static OnSelectedEngineChangedListener onSelectedEngineChangedListener;
    static SharedPreferences pref;
    static SharedPreferences.Editor editor;

    static void init(Context context, OnSelectedEngineChangedListener listener) {
        String engineID = (pref = context.getSharedPreferences("settings", Context.MODE_PRIVATE))
                .getString("engine", "");
        engines = new AvailableTtsEngines(context);
        editor = pref.edit();
        selectEngine(engineID, context);
        onSelectedEngineChangedListener = listener; // well I don't want it fired right away
    }

    static void selectEngine(String id, Context context) {
        if (!engines.selectEngine(id)) selectEngine(engines.get(0).getID(), context);
        editor.putString("engine", id);
        editor.apply();
        String engineLang = "engine." + id + ".lang";
        Locale sourceLang = LocaleUtils.parseLocale(pref.getString(engineLang, ""));
        if (sourceLang == null) {
            sourceLang = LocaleUtils.parseLocale(pref.getString("engine.lang", ""));
            if (sourceLang == null) sourceLang = context.getResources().getConfiguration().locale;
        }
        if (!engines.selectedEngine.setLanguage(sourceLang))
            engines.selectedEngine.setLanguage(context.getResources().getConfiguration().locale);
        Locale targetLang = engines.selectedEngine.getLanguage();
        if (targetLang != null && sourceLang != targetLang) selectLanguage(targetLang.toString());
        if (onSelectedEngineChangedListener != null) onSelectedEngineChangedListener.onSelectedEngineChanged();
    }

    static void selectLanguage(String lang) {
        selectLanguage(engines.selectedEngine, lang);
    }
    static void selectLanguage(TtsEngine engine, String lang) {
        engine.setLanguage(LocaleUtils.parseLocale(lang));
        editor.putString("engine." + engine.getID() + ".lang", lang);
        editor.apply();
    }

    public static interface OnSelectedEngineChangedListener {
        void onSelectedEngineChanged();
    }
}
