package maigosoft.mcpdict;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class CustomListPreference extends DialogPreference {

    // Two differences from android.preference.ListPreference:
    // (1) Displays the currently selected entry as the summary
    //     Reference: http://stackoverflow.com/a/8004498
    // (2) Stores the index of the selected entry (an integer) instead of a String into the SharedPreferences
    //     Reference: http://stackoverflow.com/a/20295410

    private final static String ANDROID_NS = "http://schemas.android.com/apk/res/android";
        // This is specified by the xmlns:android attribute of the PreferenceScreen tag
        //   in res/xml/preferences.xml

    private CharSequence[] mEntries;
    private int mValue;
    private transient int mTempValue;

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();

        int entriesResId = attrs.getAttributeResourceValue(ANDROID_NS, "entries", 0);
        if (entriesResId != 0) {
            mEntries = res.getTextArray(entriesResId);
        }

        int defaultValueResId = attrs.getAttributeResourceValue(ANDROID_NS, "defaultValue", 0);
        if (defaultValueResId != 0) {
            mValue = res.getInteger(defaultValueResId);
        }

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object value) {
                pref.setSummary(getEntry());
                return true;
            }
        });
    }

    public CustomListPreference(Context context) {
        this(context, null);
    }

    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    public void setEntries(int entriesResId) {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public void setValue(int value) {
        mValue = value;
        persistInt(value);
    }

    public int getValue() {
        return mValue;
    }

    public CharSequence getEntry() {
        return (mEntries != null && mValue >= 0 && mValue < mEntries.length) ? mEntries[mValue] : null;
    }

    @Override
    public CharSequence getSummary() {
        return getEntry();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (mEntries == null) {
            throw new IllegalStateException("CustomListPreference requires an entries array.");
        }

        builder.setSingleChoiceItems(mEntries, mValue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mTempValue = which;
                /*
                 * Clicking on an item simulates the positive button
                 * click, and dismisses the dialog.
                 */
                CustomListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
        });

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if (callChangeListener(mTempValue)) {
                setValue(mTempValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, -1);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
