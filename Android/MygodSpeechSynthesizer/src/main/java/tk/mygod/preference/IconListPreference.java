package tk.mygod.preference;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
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
public class IconListPreference extends ListPreference {

    private Context mContext;
    private LayoutInflater mInflater;
    private CharSequence[] entries;
    private CharSequence[] entryValues;
    private Drawable[] mEntryIcons = null;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String mKey;
    private int selectedEntry = -1;

    public IconListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mContext = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconPreference, defStyle, 0);

        int entryIconsResId = a.getResourceId(R.styleable.IconPreference_entryIcons, -1);
        if (entryIconsResId != -1) {
            setEntryIcons(entryIconsResId);
        }
        mInflater = LayoutInflater.from(context);
        mKey = getKey();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();

        a.recycle();

    }

    @Override
    public CharSequence getEntry() {
        if (selectedEntry != -1)
            return entries[selectedEntry];
        return super.getEntry().toString();
    }

    @Override
    public String getValue() {
        if (selectedEntry != -1)
            return entryValues[selectedEntry].toString();
        return super.getValue();
    }

    public void setEntryIcons(Drawable[] entryIcons) {
        mEntryIcons = entryIcons;
    }

    public void setEntryIcons(int entryIconsResId) {
        TypedArray icons_array = mContext.getResources().obtainTypedArray(entryIconsResId);
        Drawable[] icon_ids_array = new Drawable[icons_array.length()];
        for (int i = 0; i < icons_array.length(); i++) {
            icon_ids_array[i] = icons_array.getDrawable(i);
        }
        setEntryIcons(icon_ids_array);
        icons_array.recycle();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        entries = getEntries();
        entryValues = getEntryValues();

        if (entries.length != entryValues.length) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        if (mEntryIcons != null && entries.length != mEntryIcons.length) {
            throw new IllegalStateException("IconListPreference requires the icons entries array be the same length than entries or null");
        }

        IconListPreferenceScreenAdapter iconListPreferenceAdapter = new IconListPreferenceScreenAdapter();

        if (mEntryIcons != null) {
            String selectedValue = prefs.getString(mKey, "");
            for (int i = 0; i < entryValues.length; i++) {
                if (selectedValue.compareTo((String) entryValues[i]) == 0) {
                    selectedEntry = i;
                    break;
                }
            }
            builder.setAdapter(iconListPreferenceAdapter, null);

        }
    }

    private class IconListPreferenceScreenAdapter extends BaseAdapter {
        public int getCount() {
            return entries.length;
        }

        class CustomHolder {
            private CheckedTextView text = null;

            CustomHolder(View row, int position) {
                text = (CheckedTextView) row.findViewById(android.R.id.text1);
                text.setText(entries[position]);
                text.setId(position);
                text.setClickable(false);
                text.setChecked(selectedEntry == position);

                if (mEntryIcons != null) {
                    text.setText(" " + text.getText());
                    text.setCompoundDrawablesWithIntrinsicBounds(mEntryIcons[position], null, null, null);
                }
            }
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(com.android.internal.R.layout.select_dialog_singlechoice_holo,
                                                parent, false);
            CustomHolder holder;
            final int p = position;
            holder = new CustomHolder(convertView, position);

            convertView.setTag(holder);

            // row.setClickable(true);
            // row.setFocusable(true);
            // row.setFocusableInTouchMode(true);
            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    v.requestFocus();

                    Dialog mDialog = getDialog();
                    mDialog.dismiss();

                    IconListPreference.this.callChangeListener(entryValues[p]);
                    editor.putString(mKey, entryValues[p].toString());
                    selectedEntry = p;
                    editor.commit();

                }
            });

            return convertView;
        }

    }

}