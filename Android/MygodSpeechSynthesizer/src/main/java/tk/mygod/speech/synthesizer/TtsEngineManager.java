package tk.mygod.speech.synthesizer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import tk.mygod.speech.tts.AvailableTtsEngines;
import tk.mygod.speech.tts.TtsEngine;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
@SuppressLint("CommitPrefEdits")
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

    static void destroy() {
        onSelectedEngineChangingListener = null;
        engines.onDestroy();
    }

    static void selectEngine(String id) {
        if (onSelectedEngineChangingListener != null) onSelectedEngineChangingListener.onSelectedEngineChanging();
        if (!engines.selectEngine(id)) selectEngine(engines.get(0).getID());
        editor.putString("engine", id);
        editor.apply();
        engines.selectedEngine.setVoice(pref.getString("engine." + id, ""));
    }

    static void selectVoice(String voice) {
        selectVoice(engines.selectedEngine, voice);
    }
    static void selectVoice(TtsEngine engine, String voice) {
        engine.setVoice(voice);
        editor.putString("engine." + engine.getID(), engine.getVoice().getName());
        editor.apply();
    }

    public interface OnSelectedEngineChangingListener {
        void onSelectedEngineChanging();
    }

    static boolean getEnableSsmlDroid() {
        return pref.getBoolean("text.enableSsmlDroid", false);
    }
    static boolean getIgnoreSingleLineBreak() {
        return pref.getBoolean("text.ignoreSingleLineBreak", false);
    }
    static boolean getOldTimeySaveUI() {
        boolean old = Build.VERSION.SDK_INT < 19;
        return old || pref.getBoolean("appearance.oldTimeySaveUI", false);
    }
    static String getLastSaveDir() {
        return pref.getString("fileSystem.lastSaveDir", null);
    }
}
