package tk.mygod.speech.synthesizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import tk.mygod.preference.IconListPreference;
import tk.mygod.speech.tts.ConstantsWrapper;
import tk.mygod.speech.tts.LocaleWrapper;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.speech.tts.TtsVoice;
import tk.mygod.support.v7.util.ToolbarConfigurer;

import java.util.Set;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public final class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        new ToolbarConfigurer(this, (Toolbar) findViewById(R.id.toolbar), true);
    }

    public static class TtsSettingsFragment extends PreferenceFragment {
        private IconListPreference engine, voice;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("settings");
            addPreferencesFromResource(R.xml.settings);
            (engine = (IconListPreference) findPreference("engine"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectEngine(newValue.toString());
                            updateVoices();
                            return true;
                        }
                    });
            (voice = (IconListPreference) findPreference("engine.voice"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectVoice(newValue.toString());
                            voice.setSummary(TtsEngineManager.engines.selectedEngine.getVoice().getDisplayName());
                            return false;
                        }
                    });
            int count = TtsEngineManager.engines.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            Drawable[] icons = new Drawable[count];
            for (int i = 0; i < count; ++i) {
                TtsEngine te = TtsEngineManager.engines.get(i);
                names[i] = te.getName();
                ids[i] = te.getID();
                icons[i] = te.getIcon();
            }
            engine.setEntries(names);
            engine.setEntryValues(ids);
            engine.setEntryIcons(icons);
            engine.setValue(TtsEngineManager.engines.selectedEngine.getID());
            engine.init();
            updateVoices();
            findPreference("ssmlDroid.userGuidelines")
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getActivity()
                                    .getString(R.string.url_ssmldroid_user_guidelines))));
                            return false;
                        }
                    });
        }

        @SuppressWarnings("deprecation")
        private void updateVoices() {
            Set<TtsVoice> voices = TtsEngineManager.engines.selectedEngine.getVoices();
            int count = voices.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            int i = 0;
            for (TtsVoice voice : voices) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(voice.getDisplayName());
                int start = builder.length();
                Set<String> features = voice.getFeatures();
                if (!(voice instanceof LocaleWrapper)) builder.append(String.format(
                        getString(R.string.settings_voice_information), voice.getLocale().getDisplayName(),
                        qualityFormat(voice.getQuality()), latencyFormat(voice.getLatency())));
                boolean first = true, notInstalled = false;
                for (String feature : features)
                    if (ConstantsWrapper.KEY_FEATURE_NOT_INSTALLED.equals(feature)) notInstalled = true;
                    else if (!TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS.equals(feature) &&
                            !TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS.equals(feature) &&
                            !ConstantsWrapper.KEY_FEATURE_NETWORK_RETRIES_COUNT.equals(feature) &&
                            !ConstantsWrapper.KEY_FEATURE_NETWORK_TIMEOUT_MS.equals(feature)) {
                        if (first) {
                            first = false;
                            builder.append(getText(R.string.settings_voice_information_unsupported_features));
                        } else builder.append(", ");
                        builder.append(feature);
                    }
                if (notInstalled) builder.append(getText(R.string.settings_voice_information_not_installed));
                if (voice.isNetworkConnectionRequired())
                    builder.append(getText(R.string.settings_voice_information_network_connection_required));
                if (builder.length() != start) builder.setSpan(new TextAppearanceSpan(getActivity(),
                                android.R.style.TextAppearance_Small), start + 1, builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                names[i] = builder;
                ids[i++] = voice.getName();
            }
            voice.setEntries(names);
            voice.setEntryValues(ids);
            TtsVoice v = TtsEngineManager.engines.selectedEngine.getVoice();
            if (v == null) {
                voice.setValue(null);
                voice.setSummary(null);
            } else {
                voice.setValue(v.getName());
                voice.setSummary(v.getDisplayName());
            }
            voice.init();
        }

        private CharSequence latencyFormat(int latency) {
            switch (latency) {
                case Voice.LATENCY_VERY_LOW: return getText(R.string.settings_latency_very_low);
                case Voice.LATENCY_LOW: return getText(R.string.settings_latency_low);
                case Voice.LATENCY_NORMAL: return getText(R.string.settings_latency_normal);
                case Voice.LATENCY_HIGH: return getText(R.string.settings_latency_high);
                case Voice.LATENCY_VERY_HIGH: return getText(R.string.settings_latency_very_high);
                default: return String.format(getString(R.string.settings_latency), latency);
            }
        }
        private CharSequence qualityFormat(int quality) {
            switch (quality) {
                case Voice.QUALITY_VERY_LOW: return getText(R.string.settings_quality_very_low);
                case Voice.QUALITY_LOW: return getText(R.string.settings_quality_low);
                case Voice.QUALITY_NORMAL: return getText(R.string.settings_quality_normal);
                case Voice.QUALITY_HIGH: return getText(R.string.settings_quality_high);
                case Voice.QUALITY_VERY_HIGH: return getText(R.string.settings_quality_very_high);
                default: return String.format(getString(R.string.settings_quality), quality);
            }
        }
    }
}
