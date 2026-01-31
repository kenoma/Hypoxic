package com.lanius.kenoma.pob.hr_monitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.R;

import java.util.Set;

public abstract class HRMonitor
{
    public static Activity owner;
    public static IHRMonitorData hrmonitorListener = null;

    public abstract boolean isAlive();


    public abstract void TurnOff();
    public abstract void FindHRMonitor();
    public abstract void Stop();

    public boolean Init(Activity Owner)
    {
        owner = Owner;
        return true;
    }

    public void setListener(IHRMonitorData listener)
    {
        hrmonitorListener = listener;
    }
}
