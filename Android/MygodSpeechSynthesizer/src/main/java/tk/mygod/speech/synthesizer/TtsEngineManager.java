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
    private static OnSelectedEngineChangingListener onSelectedEngineChangingListener;
    static SharedPreferences pref;
    static SharedPreferences.Editor editor;
    static MainActivity mainActivity;

    static void init(MainActivity context, OnSelectedEngineChangingListener listener) {
        String engineID = (pref = context.getSharedPreferences("settings", Context.MODE_PRIVATE))
                .getString("engine", "");
        engines = new AvailableTtsEngines(mainActivity = context);
        editor = pref.edit();
        selectEngine(engineID);
        onSelectedEngineChangingListener = listener;    // well I don't want it fired right away
    }

    static void selectEngine(String id) {
        if (onSelectedEngineChangingListener != null) onSelectedEngineChangingListener.onSelectedEngineChanging();
        if (!engines.selectEngine(id)) selectEngine(engines.get(0).getID());
        editor.putString("engine", id);
        editor.apply();
        Locale sourceLang = LocaleUtils.parseLocale(pref.getString("engine." + id, ""));
        if (sourceLang != null || (sourceLang = LocaleUtils.parseLocale(pref.getString("engine.lang", ""))) != null)
            engines.selectedEngine.setLanguage(sourceLang);
    }

    static void selectLanguage(String lang) {
        selectLanguage(engines.selectedEngine, lang);
    }
    static void selectLanguage(TtsEngine engine, String lang) {
        engine.setLanguage(LocaleUtils.parseLocale(lang));
        editor.putString("engine." + engine.getID(), engine.getLanguage().toString());
        editor.apply();
    }

    static void selectVoice(String voice) {
        selectVoice(engines.selectedEngine, voice);
    }
    static void selectVoice(TtsEngine engine, String voice) {
        engine.setVoice(voice);
        editor.putString("engine." + engine.getID() + '.' + engine.getLanguage().toString(),
                         engine.getVoice().getName());
        editor.apply();
    }

    public static interface OnSelectedEngineChangingListener {
        void onSelectedEngineChanging();
    }
}
