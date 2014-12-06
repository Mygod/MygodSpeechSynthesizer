package tk.mygod.speech.synthesizer;

import android.content.Context;
import android.content.SharedPreferences;
import tk.mygod.speech.tts.AvailableTtsEngines;
import tk.mygod.speech.tts.TtsEngine;

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
        String engineVoice = "engine." + id + ".voice", sourceVoice = pref.getString(engineVoice, "");
        if (sourceVoice == null || "".equals(sourceVoice)) sourceVoice = pref.getString("engine.voice", "");
        engines.selectedEngine.setVoice(sourceVoice);
    }

    static void selectVoice(String name) {
        selectVoice(engines.selectedEngine, name);
    }
    static void selectVoice(TtsEngine engine, String name) {
        engine.setVoice(name);
        editor.putString("engine." + engine.getID() + ".voice", name);
        editor.apply();
    }

    public static interface OnSelectedEngineChangingListener {
        void onSelectedEngineChanging();
    }
}
