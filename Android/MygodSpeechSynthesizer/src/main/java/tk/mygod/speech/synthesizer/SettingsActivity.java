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
import android.view.MenuItem;
import tk.mygod.preference.IconListPreference;
import tk.mygod.speech.tts.TtsEngine;

import java.util.Locale;
import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new TtsSettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TtsSettingsFragment extends PreferenceFragment {
        private IconListPreference engine;
        private ListPreference lang;
        private Preference features, pitch, speechRate, pan;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("settings");
            addPreferencesFromResource(R.xml.settings);
            boolean forceOld = Build.VERSION.SDK_INT < 19;
            CheckBoxPreference oldTimeySaveDialog = (CheckBoxPreference)findPreference("appearance.oldTimeySaveDialog");
            oldTimeySaveDialog.setChecked(TtsEngineManager.pref.getBoolean("appearance.oldTimeySaveDialog", forceOld));
            if (forceOld) oldTimeySaveDialog.setEnabled(false);
            (pitch = findPreference("tweaks.pitch"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            pitch.setSummary((String) newValue);
                            return true;
                        }
                    });
            pitch.setSummary(TtsEngineManager.pref.getString("tweaks.pitch", "1"));
            (speechRate = findPreference("tweaks.speechRate"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            speechRate.setSummary((String) newValue);
                            return true;
                        }
                    });
            speechRate.setSummary(TtsEngineManager.pref.getString("tweaks.speechRate", "1"));
            (pan = findPreference("tweaks.pan"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            pan.setSummary((String) newValue);
                            return true;
                        }
                    });
            pan.setSummary(TtsEngineManager.pref.getString("tweaks.pan", "1"));
            (engine = (IconListPreference) findPreference("engine"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectEngine((String) newValue, getActivity());
                            engine.setSummary(TtsEngineManager.engines.selectedEngine.getName(getActivity()));
                            engine.setIcon(TtsEngineManager.engines.selectedEngine.getIcon(getActivity()));
                            engine.setValue((String) newValue); // temporary hack
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
                        ? getString(R.string.settings_features_network)
                        : TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS.equals(feature)
                            ? getString(R.string.settings_features_embedded) : feature);
            }
            features.setSummary(builder.toString());
        }
    }
}
