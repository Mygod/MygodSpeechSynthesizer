package tk.mygod.speech.tts;

import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public abstract class TtsVoice implements Comparable<TtsVoice> {
    public abstract Set<String> getFeatures();
    public abstract int getLatency();
    public abstract Locale getLocale();
    public abstract String getName();
    public abstract int getQuality();
    public abstract boolean isNetworkConnectionRequired();
    public abstract String getDisplayName();

    @Override
    public int compareTo(@NonNull TtsVoice another) {
        return getLocale().getDisplayName().compareTo(another.getLocale().getDisplayName());
    }
}
