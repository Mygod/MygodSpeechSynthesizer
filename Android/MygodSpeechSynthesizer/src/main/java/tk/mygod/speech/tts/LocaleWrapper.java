package tk.mygod.speech.tts;

import android.content.Context;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.util.LocaleUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

class LocaleWrapper extends TtsVoice {
    protected Locale locale;

    LocaleWrapper(Locale loc) {
        locale = loc;
    }
    LocaleWrapper(String code) {
        locale = LocaleUtils.parseLocale(code);
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
        return "Default";
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
    public String getDisplayName(Context context) {
        return context.getString(R.string.settings_voice_default);
    }
}