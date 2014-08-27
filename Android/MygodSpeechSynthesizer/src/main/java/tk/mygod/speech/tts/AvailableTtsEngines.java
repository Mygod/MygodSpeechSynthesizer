package tk.mygod.speech.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public class AvailableTtsEngines extends ArrayList<TtsEngine> {
    public AvailableTtsEngines(Context context) {
        SvoxPicoTtsEngine defaultEngine = new SvoxPicoTtsEngine(context);
        add(defaultEngine);
        String defaultEngineName = defaultEngine.tts.getDefaultEngine();
        for (TextToSpeech.EngineInfo info : defaultEngine.tts.getEngines())
            if (info.name.equals(defaultEngineName)) defaultEngine.engineInfo = info;
            else add(new SvoxPicoTtsEngine(context, info));
        add(new GoogleTranslateTtsEngine());
    }

    public TtsEngine selectedEngine;
    public boolean selectEngine(String id) {
        for (TtsEngine engine : this) if (engine.getID().equals(id)) {
            selectedEngine = engine;
            return true;
        }
        return false;
    }

    public void onDestroy() {
        for (TtsEngine engine : this) engine.onDestroy();
    }
}
