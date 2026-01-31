package com.lanius.kenoma.pob.hr_monitor;

import android.util.Log;

import com.albertcbraun.cms50fwlib.BluetoothNotAvailableException;
import com.albertcbraun.cms50fwlib.BluetoothNotEnabledException;
import com.albertcbraun.cms50fwlib.CMS50FWBluetoothConnectionManager;
import com.albertcbraun.cms50fwlib.CMS50FWConnectionListener;
import com.albertcbraun.cms50fwlib.DataFrame;
import com.lanius.kenoma.pob.Classes.Constants;


public class HRCMS50 extends HRMonitor implements CMS50FWConnectionListener {
    private static final String CMS50FW_BLUETOOTH_DEVICE_NAME = "SpO202";

    private CMS50FWBluetoothConnectionManager cms50FWBluetoothConnectionManager = null;
    private static final int ONE_HUNDRED = 100;
    private boolean _state = false;
    static private int beatsNum = 0;
    static private int[] beatsArray = new int[15];

    @Override
    public boolean isAlive() {
        return cms50FWBluetoothConnectionManager != null && _state;
    }

    @Override
    public void TurnOff() {
        _state = false;
        if (cms50FWBluetoothConnectionManager != null) {
            Stop();
            cms50FWBluetoothConnectionManager.dispose(owner);
        }
        cms50FWBluetoothConnectionManager = null;
    }

    @Override
    public void FindHRMonitor() {
        try {
            TurnOff();
            cms50FWBluetoothConnectionManager = new CMS50FWBluetoothConnectionManager(CMS50FW_BLUETOOTH_DEVICE_NAME);
            cms50FWBluetoothConnectionManager.setCMS50FWConnectionListener(this);
            cms50FWBluetoothConnectionManager.connect(owner);
        } catch (BluetoothNotAvailableException | BluetoothNotEnabledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Stop() {
        _state = false;
        if (cms50FWBluetoothConnectionManager != null) {
            cms50FWBluetoothConnectionManager.stopData();
        }
    }

/////CMS50FWConnectionListener
    @Override
    public void onConnectionAttemptInProgress() {

    }

    @Override
    public void onConnectionEstablished() {
        _state = true;
        cms50FWBluetoothConnectionManager.startData();
    }

    @Override
    public void onDataReadAttemptInProgress() {

    }

    long lastFrameDate =0;
    @Override
    public void onDataFrameArrived(DataFrame dataFrame) {
        if (dataFrame != null &&
                dataFrame.spo2Percentage <= ONE_HUNDRED &&
                !dataFrame.isFingerOutOfSleeve) { // valid data frame

            int currentRR = 60000 / dataFrame.pulseRate;
            if (currentRR > 300 && currentRR < 3000 &&
                    lastFrameDate + currentRR < dataFrame.time) {
                beatsNum++;
                if (Constants.D)
                    Log.d("PCam", "BEAT #" + beatsNum + " RR: " + dataFrame.pulseRate);

                System.arraycopy(beatsArray, 0, beatsArray, 1, 14);
                beatsArray[0] = (int) (lastFrameDate + currentRR);
                if (hrmonitorListener != null)
                    hrmonitorListener.onData(new HxMData(beatsArray, beatsNum));

                lastFrameDate += currentRR;
            }
        }
    }


    @Override
    public void onDataReadStopped() {

    }

    @Override
    public void onBrokenConnection() {
        _state=false;
    }

    @Override
    public void onConnectionReset() {

    }

    @Override
    public void onLogEvent(long timeMs, String message) {

    }
}
