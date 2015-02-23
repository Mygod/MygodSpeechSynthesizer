package tk.mygod.speech.tts;

import tk.mygod.util.LocaleUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class LocaleWrapper extends TtsVoice {
    protected final Locale locale;
    public final String code;

    LocaleWrapper(Locale loc) {
        code = (locale = loc).toString();
    }
    LocaleWrapper(String code) {
        locale = LocaleUtils.parseLocale(this.code = code);
    }

    @Override
    public Set<String> getFeatures() {
        return Collections.EMPTY_SET;
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
        return code;
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
}