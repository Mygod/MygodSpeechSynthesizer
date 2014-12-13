package tk.mygod.speech.synthesizer;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.speech.tts.TextToSpeech;
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

import java.util.Locale;
import java.util.Set;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        new ToolbarConfigurer(this, (Toolbar) findViewById(R.id.toolbar), true);
    }

    public static class TtsSettingsFragment extends PreferenceFragment {
        private IconListPreference engine, voice;
        private ListPreference lang, start;

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
                            engine.setSummary(TtsEngineManager.engines.selectedEngine.getName(getActivity()));
                            engine.setIcon(TtsEngineManager.engines.selectedEngine.getIcon(getActivity()));
                            updateLanguages();
                            return true;
                        }
                    });
            (lang = (ListPreference) findPreference("engine.lang"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectLanguage(newValue.toString());
                            lang.setSummary(TtsEngineManager.engines.selectedEngine.getLanguage().getDisplayName());
                            updateVoices();
                            return true;
                        }
                    });
            (voice = (IconListPreference) findPreference("engine.voice"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectVoice(newValue.toString());
                            voice.setSummary(TtsEngineManager.engines.selectedEngine.getVoice()
                                    .getDisplayName(TtsSettingsFragment.this.getActivity()));
                            return false;
                        }
                    });
            int count = TtsEngineManager.engines.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            Drawable[] icons = new Drawable[count];
            for (int i = 0; i < count; ++i) {
                TtsEngine te = TtsEngineManager.engines.get(i);
                names[i] = te.getName(getActivity());
                ids[i] = te.getID();
                icons[i] = te.getIcon(getActivity());
            }
            engine.setEntries(names);
            engine.setEntryValues(ids);
            engine.setEntryIcons(icons);
            engine.setValue(TtsEngineManager.engines.selectedEngine.getID());
            engine.setSummary(TtsEngineManager.engines.selectedEngine.getName(getActivity()));
            engine.setIcon(TtsEngineManager.engines.selectedEngine.getIcon(getActivity()));
            updateLanguages();
            (start = (ListPreference)findPreference("text.start")).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            start.setValue(newValue.toString());
                            start.setSummary(start.getEntry());
                            return true;
                        }
                    }
            );
            start.setSummary(start.getEntry());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference("appearance.oldTimeySaveUI");
            if (Build.VERSION.SDK_INT < 19) pref.setEnabled(false);
            else pref.setChecked(TtsEngineManager.getOldTimeySaveUI());
            pref = (CheckBoxPreference) findPreference("text.enableSsmlDroid");
            if (Build.VERSION.SDK_INT < 21) pref.setEnabled(false);
            else pref.setChecked(TtsEngineManager.getEnableSsmlDroid());
        }

        private void updateLanguages() {
            Set<Locale> languages = TtsEngineManager.engines.selectedEngine.getLanguages();
            int count = languages.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            int i = 0;
            for (Locale locale : languages) {
                names[i] = locale.getDisplayName();
                ids[i++] = locale.toString();
            }
            lang.setEntries(names);
            lang.setEntryValues(ids);
            Locale locale = TtsEngineManager.engines.selectedEngine.getLanguage();
            lang.setValue(locale.toString());
            lang.setSummary(locale.getDisplayName());
            updateVoices();
        }

        @SuppressWarnings("deprecation")
        private void updateVoices() {
            Set<TtsVoice> voices = TtsEngineManager.engines.selectedEngine.getVoices();
            int count = voices.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            int i = 0;
            for (TtsVoice voice : voices) { // TODO: Localizations
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(voice.getDisplayName(getActivity()));
                int start = builder.length();
                Set<String> features = voice.getFeatures();
                if (!(voice instanceof LocaleWrapper)) builder.append(String.format("\nName: %s\nQuality: %d (higher = better)\nLatency: %d (lower = better)", voice.getName(), voice.getQuality(), voice.getLatency()));
                boolean first = true, notInstalled = false;
                for (String feature : features)
                    if (ConstantsWrapper.KEY_FEATURE_NOT_INSTALLED.equals(feature)) notInstalled = true;
                    else if (!TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS.equals(feature) &&
                            !TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS.equals(feature) &&
                            !ConstantsWrapper.KEY_FEATURE_NETWORK_RETRIES_COUNT.equals(feature) &&
                            !ConstantsWrapper.KEY_FEATURE_NETWORK_TIMEOUT_MS.equals(feature)) {
                        if (first) {
                            first = false;
                            builder.append("\nUnsupported features: ");
                        } else builder.append(", ");
                        builder.append(feature);
                    }
                if (notInstalled) builder.append("\nAdditional data needed downloading");
                if (voice.isNetworkConnectionRequired()) builder.append("\nNetwork connection required");
                if (builder.length() != start) builder.setSpan(new TextAppearanceSpan(getActivity(),
                                android.R.style.TextAppearance_Small), start + 1, builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                names[i] = builder;
                ids[i++] = voice.getName();
            }
            voice.setEntries(names);
            voice.setEntryValues(ids);
            TtsVoice v = TtsEngineManager.engines.selectedEngine.getVoice();
            voice.setValue(v.getName());
            voice.setSummary(v.getDisplayName(getActivity()));
        }
    }
}
