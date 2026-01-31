package com.lanius.kenoma.pob.Classes;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.lanius.kenoma.pob.R;

import java.io.IOException;

public class SoundListPreference extends ListPreference
{

    CharSequence[] mEntries;
    CharSequence[] mEntryValues;
    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private int mClickedDialogEntryIndex;
    private String mValue;

    public SoundListPreference(Context context)
    {
        super(context);
        mContext = context;
    }

    public SoundListPreference(Context ctxt, AttributeSet attrs)
    {
        super(ctxt, attrs);

        mContext = ctxt;

        //setNegativeButtonText("afasfasda");
    }

    /**
     * Returns the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     *
     * @return The value of the key.
     */
    public String getValue()
    {
        return mValue;
    }

    /**
     * Sets the value of the key. This should be one of the entries in
     * {@link #getEntryValues()}.
     *
     * @param value The value to set for the key.
     */
    public void setValue(String value)
    {
        mValue = value;

        persistString(value);
    }

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or null.
     */
    public CharSequence getEntry()
    {
        int index = getValueIndex();
        return index >= 0 && mEntries != null ? mEntries[index] : null;
    }

    public int findIndexOfValue(String value)
    {
        if (value != null && mEntryValues != null)
        {
            for (int i = mEntryValues.length - 1; i >= 0; i--)
            {
                if (mEntryValues[i].equals(value))
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getValueIndex()
    {

        return findIndexOfValue(mValue);
    }

    /**
     * Sets the value to the given index from the entry values.
     *
     * @param index The index of the value to set.
     */
    public void setValueIndex(int index)
    {
        if (mEntryValues != null)
        {
            setValue(mEntryValues[index].toString());
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        super.onPrepareDialogBuilder(builder);

        mMediaPlayer = new MediaPlayer();
        mEntries = getEntries();
        mEntryValues = getEntryValues();

        if (mEntries == null || mEntryValues == null)
        {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mClickedDialogEntryIndex = getValueIndex();
        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener()
                {

                    public void onClick(DialogInterface dialog, int which)
                    {
                        mClickedDialogEntryIndex = which;

                        String value = mEntryValues[which].toString();

                        try
                        {
                            playSong(value);
                        } catch (IllegalStateException | IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
        );

        builder.setPositiveButton("OK", this);
        builder.setNegativeButton(mContext.getString(R.string.sound_pref_cancel), this);
        builder.setNeutralButton(mContext.getString(R.string.sound_pref_chooose_file), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try
                {
                    String key = getKey();
                    int code = 0;
                    if (key.equals("prefInhaleSound"))
                        code = Constants.REQUEST_SOUND_FILE_INHALE;
                    else if (key.equals("prefPauseSound"))
                        code = Constants.REQUEST_SOUND_FILE_PAUSE;
                    else if (key.equals("prefExhaleSound"))
                        code = Constants.REQUEST_SOUND_FILE_EXHALE;
                    else if (key.equals("prefHoldSound"))
                        code = Constants.REQUEST_SOUND_FILE_HOLD;

                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    ((Activity) mContext).startActivityForResult(i, code);
                } catch (android.content.ActivityNotFoundException ex)
                {
                    // Potentially direct the user to the Market with a Dialog
                    Toast.makeText(getContext(), "Please install a File Manager.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void playSong(String path) throws IllegalArgumentException,
            IllegalStateException, IOException
    {
        String packageName = getContext().getPackageName();
        int resID = getContext().getResources().getIdentifier(path, "raw", packageName);
        if (resID != 0)
        {
            AssetFileDescriptor afd = getContext().getResources().openRawResourceFd(resID);
            Log.d("ringtone", "playSong :: " + path);

            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            afd.close();
        }
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state == null || !state.getClass().equals(SavedState.class))
        {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntryValues != null)
        {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            if (callChangeListener(value))
            {
                setValue(value);
            }
        }

        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        setValue(restoreValue ? getPersistedString(mValue) : (String) defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent())
        {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    public static class SavedState extends BaseSavedState
    {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>()
                {
                    public SavedState createFromParcel(Parcel in)
                    {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size)
                    {
                        return new SavedState[size];
                    }
                };
        String value;

        public SavedState(Parcel source)
        {
            super(source);
            value = source.readString();
        }

        public SavedState(Parcelable superState)
        {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            super.writeToParcel(dest, flags);
            dest.writeString(value);
        }
    }
}