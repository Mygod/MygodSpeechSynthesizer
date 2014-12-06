package tk.mygod.speech.tts;

import java.util.Locale;
import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public abstract class TtsVoice {
    public abstract Set<String> getFeatures();
    public abstract int getLatency();
    public abstract Locale getLocale();
    public abstract String getName();
    public abstract int getQuality();
    public abstract boolean isNetworkConnectionRequired();
    public abstract String getDisplayName();
}
