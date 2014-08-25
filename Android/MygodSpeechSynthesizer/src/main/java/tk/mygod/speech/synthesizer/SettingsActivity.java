package tk.mygod.speech.synthesizer;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import tk.mygod.preference.IconListPreference;
import tk.mygod.speech.tts.TtsEngine;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * Author:  Mygod (mygod.tk)
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (TtsEngineManager.engines == null) finish(); // wrong entrance?
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings, target);
    }

    @Override
    protected boolean isValidFragment (String fragmentName) {
        return true;
    }

    public static class TtsSettingsFragment extends PreferenceFragment {
        private IconListPreference engine;
        private ListPreference lang;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("tts");
            addPreferencesFromResource(R.xml.settings_tts);
            (engine = (IconListPreference) findPreference("tts.engine"))
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
            (lang = (ListPreference) findPreference("tts.lang"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectLanguage((String) newValue);
                            lang.setSummary(TtsEngineManager.engines.selectedEngine.getLanguage().getDisplayName());
                            return true;
                        }
                    });
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
            lang.setValue(TtsEngineManager.engines.selectedEngine.getLanguage().toString());
            lang.setSummary(TtsEngineManager.engines.selectedEngine.getLanguage().getDisplayName());
        }
    }
}
