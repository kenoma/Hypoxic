package com.lanius.kenoma.pob.Classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

public class DatePreference extends DialogPreference
{
    private DatePicker picker = null;

    public DatePreference(Context ctxt, AttributeSet attrs)
    {
        super(ctxt, attrs);

        setPositiveButtonText("OK");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView()
    {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
        picker = new DatePicker(getContext());
        picker.setCalendarViewShown(false);
        picker.updateDate(
                p.getInt("BYear", 1980),
                p.getInt("BMonth", 1)-1,
                p.getInt("BDay", 1));

        return (picker);
    }

    @Override
    protected void onBindDialogView(View v)
    {
        super.onBindDialogView(v);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        if (positiveResult)
        {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor ed = p.edit();
            ed.putInt("BYear", picker.getYear());
            ed.putInt("BMonth", picker.getMonth()+1);
            ed.putInt("BDay", picker.getDayOfMonth());
            ed.putString("prefUserBirthdate", String.format("%02d.%02d.%04d",
                    picker.getDayOfMonth(),
                    picker.getMonth()+1,
                    picker.getYear()));
            ed.commit();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {

    }
}
