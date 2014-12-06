package tk.mygod.speech.synthesizer;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import tk.mygod.preference.IconListPreference;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.speech.tts.TtsVoice;

import java.util.Set;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public class SettingsActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        private ListPreference voice, start;
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
                            TtsEngineManager.selectEngine(newValue.toString());
                            engine.setSummary(TtsEngineManager.engines.selectedEngine.getName(getActivity()));
                            engine.setIcon(TtsEngineManager.engines.selectedEngine.getIcon(getActivity()));
                            engine.setValue((String) newValue); // temporary hack
                            updateVoices();
                            return true;
                        }
                    });
            (voice = (ListPreference) findPreference("engine.voice"))
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            TtsEngineManager.selectVoice(newValue.toString());
                            TtsVoice v = TtsEngineManager.engines.selectedEngine.getVoice();
                            voice.setSummary(v.getDisplayName());
                            updateFeatures(v);
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
            updateVoices();
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
            boolean forceOld = Build.VERSION.SDK_INT < 19;
            CheckBoxPreference oldTimeySaveDialog = (CheckBoxPreference)findPreference("appearance.oldTimeySaveUI");
            oldTimeySaveDialog.setChecked(TtsEngineManager.pref.getBoolean("appearance.oldTimeySaveUI", forceOld));
            if (forceOld) oldTimeySaveDialog.setEnabled(false);
        }

        private void updateVoices() {
            Set<TtsVoice> voices = TtsEngineManager.engines.selectedEngine.getVoices();
            int count = voices.size();
            CharSequence[] names = new CharSequence[count], ids = new CharSequence[count];
            int i = 0;
            for (TtsVoice v : voices) {
                names[i] = v.getDisplayName();
                ids[i++] = v.toString();
            }
            voice.setEntries(names);
            voice.setEntryValues(ids);
            TtsVoice v = TtsEngineManager.engines.selectedEngine.getVoice();
            if (v == null) return;
            voice.setValue(v.toString());
            voice.setSummary(v.getDisplayName());
            updateFeatures(v);
        }

        private void updateFeatures(TtsVoice v) {   // todo: show other features!
            StringBuilder builder = new StringBuilder();
            for (String feature : v.getFeatures()) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS.equals(feature)
                        ? getString(R.string.settings_features_network)
                        : TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS.equals(feature)
                            ? getString(R.string.settings_features_embedded) : feature);
            }
            if (builder.length() <= 0) builder.append(getString(R.string.settings_features_none));
            features.setSummary(builder.toString());
        }
    }
}
