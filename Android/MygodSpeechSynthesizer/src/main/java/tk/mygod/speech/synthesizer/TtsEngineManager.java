package tk.mygod.speech.synthesizer;

import android.content.Context;
import android.content.SharedPreferences;
import tk.mygod.speech.tts.AvailableTtsEngines;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.util.LocaleUtil;

import java.util.Locale;

/**
 * Project: MygodSpeechSynthesizer
 * Author:  Mygod (mygod.tk)
 */
final class TtsEngineManager {
    private TtsEngineManager() {
        throw new AssertionError();
    }

    static AvailableTtsEngines engines;
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    static void init(Context context) {
        String engineID = (pref = context.getSharedPreferences("tts", Context.MODE_PRIVATE))
                .getString("tts.engine", "");
        engines = new AvailableTtsEngines(context);
        editor = pref.edit();
        selectEngine(engineID, context);
    }

    static void selectEngine(String id, Context context) {
        if (!engines.selectEngine(id)) selectEngine(engines.get(0).getID(), context);
        editor.putString("tts.engine", id);
        editor.apply();
        String engineLang = "tts." + id + ".lang";
        Locale sourceLang = LocaleUtil.parseLocale(pref.getString(engineLang, ""));
        if (sourceLang == null) {
            sourceLang = LocaleUtil.parseLocale(pref.getString("tts.lang", ""));
            if (sourceLang == null) sourceLang = context.getResources().getConfiguration().locale;
        }
        if (!engines.selectedEngine.setLanguage(sourceLang))
            engines.selectedEngine.setLanguage(context.getResources().getConfiguration().locale);
        Locale targetLang = engines.selectedEngine.getLanguage();
        if (targetLang != null && sourceLang != targetLang) selectLanguage(targetLang.toString());
    }

    static void selectLanguage(String lang) {
        selectLanguage(engines.selectedEngine, lang);
    }
    static void selectLanguage(TtsEngine engine, String lang) {
        engine.setLanguage(LocaleUtil.parseLocale(lang));
        editor.putString("tts." + engine.getID() + ".lang", lang);
        editor.apply();
    }
}
