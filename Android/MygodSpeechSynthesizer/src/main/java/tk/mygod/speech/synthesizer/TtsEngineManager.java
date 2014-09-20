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

    static final int IDLE = 0, SPEAKING = 1, SYNTHESIZING = 2;

    static AvailableTtsEngines engines;
    private static OnTerminatedListener onTerminatedListener;
    static SharedPreferences pref;
    static SharedPreferences.Editor editor;
    static int status;

    static void setOnTerminatedListener(OnTerminatedListener listener) {
        onTerminatedListener = listener;
    }
    static void init(Context context) {
        String engineID = (pref = context.getSharedPreferences("settings", Context.MODE_PRIVATE))
                .getString("engine", "");
        engines = new AvailableTtsEngines(context);
        editor = pref.edit();
        selectEngine(engineID, context);
    }
    static void configureEngine() {
        engines.selectedEngine.setPitch(Float.parseFloat(pref.getString("tweaks.pitch", "1")));
        engines.selectedEngine.setSpeechRate(Float.parseFloat(pref.getString("tweaks.speechRate", "1")));
        engines.selectedEngine.setPan(Float.parseFloat(pref.getString("tweaks.pan", "0")));
        engines.selectedEngine.setIgnoreSingleLineBreaks(pref.getBoolean("text.ignoreSingleLineBreak", false));
    }
    static void terminate() {
        if (engines.selectedEngine != null) engines.selectedEngine.stop();
        if (onTerminatedListener != null) onTerminatedListener.onTerminated();
    }

    static void selectEngine(String id, Context context) {
        terminate();
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
    }

    static void selectLanguage(String lang) {
        selectLanguage(engines.selectedEngine, lang);
    }
    static void selectLanguage(TtsEngine engine, String lang) {
        engine.setLanguage(LocaleUtils.parseLocale(lang));
        editor.putString("engine." + engine.getID() + ".lang", lang);
        editor.apply();
    }

    public static interface OnTerminatedListener {
        void onTerminated();
    }
}
