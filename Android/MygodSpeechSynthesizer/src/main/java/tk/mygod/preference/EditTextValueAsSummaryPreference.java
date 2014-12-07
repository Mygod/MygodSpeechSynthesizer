package tk.mygod.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Project: MygodSpeechSynthesizer
 *
 * @author Mygod
 */
public class EditTextValueAsSummaryPreference extends EditTextPreference
        implements Preference.OnPreferenceChangeListener {
    public EditTextValueAsSummaryPreference(Context context) {
        super(context);
        super.setOnPreferenceChangeListener(this);
    }
    public EditTextValueAsSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnPreferenceChangeListener(this);
    }
    public EditTextValueAsSummaryPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        setSummary(getText());
    }

    private OnPreferenceChangeListener listener;
    @Override
    public OnPreferenceChangeListener getOnPreferenceChangeListener() {
        return listener;
    }
    @Override
    public void setOnPreferenceChangeListener(OnPreferenceChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (listener != null && !listener.onPreferenceChange(preference, newValue)) return false;
        setSummary(newValue.toString());
        return true;
    }
}
