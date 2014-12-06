package tk.mygod.speech.tts;

import tk.mygod.util.LocaleUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class LocaleWrapper extends TtsVoice {
    protected Locale locale;
    private String creator;

    LocaleWrapper(Locale loc) {
        locale = loc;
    }
    LocaleWrapper(String code) {
        locale = LocaleUtils.parseLocale(creator = code);
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<String>();
    }
    @Override
    public int getLatency() {
        return 300; // Voice.LATENCY_NORMAL
    }
    @Override
    public Locale getLocale() {
        return locale;
    }
    @Override
    public String getName() {
        return locale == null ? "" : locale.getDisplayName();
    }
    @Override
    public int getQuality() {
        return 300; // Voice.QUALITY_NORMAL
    }
    @Override
    public boolean isNetworkConnectionRequired() {
        return true;
    }
    @Override
    public String getDisplayName() {
        return locale.getDisplayName();
    }
    @Override
    public String toString() {
        return (creator == null ? locale.toString() : creator);
    }
}