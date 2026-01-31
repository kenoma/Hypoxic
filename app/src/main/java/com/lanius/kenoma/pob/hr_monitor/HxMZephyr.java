package com.lanius.kenoma.pob.hr_monitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.R;

import java.util.Set;

public class HxMZephyr extends HRMonitor
{
    BluetoothAdapter mBluetoothAdapter;
    HxMBluetoothListener listener;

    public boolean isAlive()
    {
        return listener != null && listener.isAlive();
    }

    @Override
    public boolean Init(Activity Owner)
    {
        super.Init(Owner);
        Stop();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(owner, R.string.no_bluetoothsupport, Toast.LENGTH_SHORT).show();
            return false;
        }
        else
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                owner.startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
            }
            else
                FindHRMonitor();
            return true;
        }
    }

    public void TurnOff()
    {
        if (mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.disable();
        }
    }

    public void FindHRMonitor()
    {
        if (mBluetoothAdapter != null)
        {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0)
            {
                for (BluetoothDevice device : pairedDevices)
                {
                    if (device.getName().contains("HXM"))
                    {
                        Toast.makeText(owner, R.string.hxm_monitor_connected, Toast.LENGTH_SHORT).show();
                        listener = new HxMBluetoothListener(device, this);
                        listener.start();
                        break;
                    }
                }
            }

            mBluetoothAdapter.cancelDiscovery();
        }
        else
            Toast.makeText(owner, R.string.no_bluetoothsupport, Toast.LENGTH_SHORT).show();
    }

    public void Stop()
    {
        if (listener != null)
        {
            listener.interrupt();
            listener = null;
        }
    }


}
