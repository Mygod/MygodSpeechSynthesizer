package tk.mygod.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import tk.mygod.speech.synthesizer.R;

/**
 * @author Mygod
 */
public class EditTextPlusPreference extends EditTextPreference {
    private String mSummary;

    public EditTextPlusPreference(Context context) {
        this(context, null);
    }
    public EditTextPlusPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditTextPlusPreference);
        mSummary = a.getString(R.styleable.EditTextPlusPreference_summary);
        a.recycle();
    }

    /**
     * Returns the summary of this ListPreference. If the summary
     * has a {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
    @Override
    public CharSequence getSummary() {
        String text = getText();
        if (mSummary == null || text == null) {
            return super.getSummary();
        } else {
            return String.format(mSummary, text);
        }
    }

    /**
     * Sets the summary for this Preference with a CharSequence.
     * If the summary has a
     * {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place when it's retrieved.
     *
     * @param summary The summary for the preference.
     */
    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null) {
            mSummary = null;
        } else if (summary != null && !summary.equals(mSummary)) {
            mSummary = summary.toString();
        }
    }
}
