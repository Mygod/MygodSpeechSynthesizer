package tk.mygod.speech.synthesizer;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.speech.tts.TextToSpeech;
import tk.mygod.preference.IconListPreference;
import tk.mygod.speech.tts.TtsEngine;

import java.util.Locale;
import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * Author:  Mygod (mygod.tk)
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new TtsSettingsFragment()).commit();
    }

    public static class TtsSettingsFragment extends PreferenceFragment {
        private IconListPreference engine;
        private ListPreference lang;
        private Preference features;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("settings");
            addPreferencesFromResource(R.xml.settings);
            (engine = (IconListPreference) findPreference("engine"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectEngine((String) newValue, getActivity().getApplicationContext());
                            engine.setSummary(TtsEngineManager.engines.selectedEngine
                                    .getName(getActivity().getApplicationContext()));
                            engine.setIcon(TtsEngineManager.engines.selectedEngine
                                    .getIcon(getActivity().getApplicationContext()));
                            updateLanguages();
                            return true;
                        }
                    });
            (lang = (ListPreference) findPreference("engine.lang"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectLanguage((String) newValue);
                            Locale locale = TtsEngineManager.engines.selectedEngine.getLanguage();
                            lang.setSummary(locale.getDisplayName());
                            updateFeatures(locale);
                            return true;
                        }
                    });
            features = findPreference("engine.features");
            int count = TtsEngineManager.engines.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            Drawable[] icons = new Drawable[count];
            for (int i = 0; i < count; ++i) {
                TtsEngine te = TtsEngineManager.engines.get(i);
                names[i] = te.getName(getActivity().getApplicationContext());
                ids[i] = te.getID();
                icons[i] = te.getIcon(getActivity().getApplicationContext());
            }
            engine.setEntries(names);
            engine.setEntryValues(ids);
            engine.setEntryIcons(icons);
            engine.setValue(TtsEngineManager.engines.selectedEngine.getID());
            engine.setSummary(TtsEngineManager.engines.selectedEngine.getName(getActivity().getApplicationContext()));
            engine.setIcon(TtsEngineManager.engines.selectedEngine.getIcon(getActivity().getApplicationContext()));
            updateLanguages();
        }

        private void updateLanguages() {
            Set<Locale> languages = TtsEngineManager.engines.selectedEngine.getSupportedLanguages();
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
            updateFeatures(locale);
        }

        private void updateFeatures(Locale locale) {
            StringBuilder builder = new StringBuilder();
            for (String feature : TtsEngineManager.engines.selectedEngine.getFeatures(locale)) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS.equals(feature)
                        ? getText(R.string.settings_features_network)
                        : TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS.equals(feature)
                            ? getText(R.string.settings_features_embedded) : feature);
            }
            features.setSummary(builder.toString());
        }
    }
}
