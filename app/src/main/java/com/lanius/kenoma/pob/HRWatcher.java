package com.lanius.kenoma.pob;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lanius.kenoma.pob.hr_monitor.HRMonitor;

public class HRWatcher extends Application
{
    public boolean isBluetoothWasSitchedOn = true;
    public HRMonitor monitor;

    @Override
    public void onCreate()
    {
        super.onCreate();

    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        if (monitor != null)
            monitor.Stop();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("prefAutoSwitch", false) && !isBluetoothWasSitchedOn)
            monitor.TurnOff();
    }
}
