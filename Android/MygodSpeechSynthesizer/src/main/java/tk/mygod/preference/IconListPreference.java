package tk.mygod.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import tk.mygod.speech.synthesizer.R;

/**
 * @author   Mygod
 * Based on: https://github.com/atanarro/IconListPreference/blob/master/src/com/tanarro/iconlistpreference/IconListPreference.java
 */
public class IconListPreference extends ListPreference implements DialogInterface.OnClickListener {
    private Drawable[] mEntryIcons = null;
    private int selectedEntry = -1;

    public IconListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference, defStyle, 0);
        int entryIconsResId = a.getResourceId(R.styleable.IconPreference_entryIcons, -1);
        if (entryIconsResId != -1) setEntryIcons(entryIconsResId);
        a.recycle();
    }

    public Drawable[] getEntryIcons() {
        return mEntryIcons;
    }

    public void setEntryIcons(Drawable[] entryIcons) {
        mEntryIcons = entryIcons;
    }

    public void setEntryIcons(int entryIconsResId) {
        TypedArray icons_array = getContext().getResources().obtainTypedArray(entryIconsResId);
        Drawable[] icon_ids_array = new Drawable[icons_array.length()];
        for (int i = 0; i < icons_array.length(); i++) icon_ids_array[i] = icons_array.getDrawable(i);
        setEntryIcons(icon_ids_array);
        icons_array.recycle();
    }

    @Override
    protected void onPrepareDialogBuilder(@NonNull Builder builder) {
        CharSequence[] entries = getEntries(), entryValues = getEntryValues();
        if (entries.length != entryValues.length) throw new IllegalStateException
                ("ListPreference requires an entries array and an entryValues array which are both the same length");
        if (mEntryIcons != null && entries.length != mEntryIcons.length) throw new IllegalStateException
                ("IconListPreference requires the icons entries array be the same length than entries or null");
        CheckedListAdapter adapter = new CheckedListAdapter();
        String selectedValue = getValue();
        for (selectedEntry = 0; selectedEntry < entryValues.length; selectedEntry++)
            if (selectedValue.compareTo((String) entryValues[selectedEntry]) == 0) break;
        builder.setAdapter(adapter, this);
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!positiveResult || selectedEntry < 0) return;
        String value = getEntryValues()[selectedEntry].toString();
        if (callChangeListener(value)) setValue(value);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        selectedEntry = which;
        super.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
        dialog.dismiss();
    }

    private class CheckedListAdapter extends BaseAdapter {
        public int getCount() {
            return getEntries().length;
        }

        public Object getItem(int position) {
            return getEntries()[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(parent.getContext())
                    .inflate(Resources.getSystem().getIdentifier(Build.VERSION.SDK_INT >= 21
                            ? "select_dialog_singlechoice_material" : "select_dialog_singlechoice_holo", "layout",
                            "android"), parent, false);
            CheckedTextView text = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            text.setText(getEntries()[position]);
            text.setChecked(selectedEntry == position);
            if (mEntryIcons != null)
                text.setCompoundDrawablesWithIntrinsicBounds(mEntryIcons[position], null, null, null);
            return convertView;
        }
    }
}