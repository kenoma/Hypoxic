package com.lanius.kenoma.pob.hr_monitor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;

/**
 * Created by Kenoma on 30.10.2014.
 */
public class HRBodySensors extends HRMonitor implements SensorEventListener
{
    static Sensor mHeartRateSensor;
    static SensorManager mSensorManager;
    static private int beatsNum = 0;
    static long started = -1;
    static private int[] beatsArray = new int[15];

    @Override
    public boolean isAlive()
    {
       return true;
    }

    @Override
    public  void TurnOff()
    {

    }

    @Override
    public  void FindHRMonitor()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            if (owner != null)
            {
                mSensorManager = ((SensorManager) owner.getApplicationContext().getSystemService(Context.SENSOR_SERVICE));
                mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                if(mHeartRateSensor!=null)
                    mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public  void Stop()
    {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        //Update your data. This check is very raw. You should improve it when the sensor is unable to calculate the heart rate
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE)
        {
            if (started == -1)
                started = event.timestamp;
            beatsNum++;
            if (Constants.D)
                Log.d("PSensor", "BEAT #" + beatsNum + " RR: " + event.timestamp);


            System.arraycopy(beatsArray, 0, beatsArray, 1, 14);
            beatsArray[0] = (int) (event.timestamp - started);
            if (hrmonitorListener != null)
                hrmonitorListener.onData(new HxMData(beatsArray, beatsNum));

            //if ((int)event.values[0]>0)
            //{

            //mCircledImageView.setCircleColor(getResources().getColor(R.color.green));
            //mTextView.setText("" + (int) event.values[0]);
            //}
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
