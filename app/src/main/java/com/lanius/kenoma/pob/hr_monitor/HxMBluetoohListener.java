package com.lanius.kenoma.pob.hr_monitor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;

import java.io.IOException;
import java.io.InputStream;

class HxMBluetoothListener extends Thread
{
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final int STX = 0x02;
    private final int MSGID = 0x26;
    private final int DLC = 55;
    private final int ETX = 0x03;
    private HRMonitor parent;
    private HxMData last_recieved;


    public HxMBluetoothListener(BluetoothDevice device, HRMonitor Parent)
    {
        parent = Parent;
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try
        {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(Constants.HXM_UUID);
        } catch (IOException e)
        {
            if(Constants.D)
                Log.v("BT", "Opening socket " + e.getMessage());
        }
        mmSocket = tmp;
    }

    public void run()
    {
        try
        {
            mmSocket.connect();
        } catch (IOException connectException)
        {
            // Unable to connect; close the socket and get out
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                if(Constants.D)
                    Log.v("BT", "Opening socket " + e.getMessage());
            }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        manageConnectedSocket();
    }

    private void manageConnectedSocket()
    {
        InputStream mmInStream = null;
        try
        {
            mmInStream = mmSocket.getInputStream();
        } catch (IOException e)
        {
            if(Constants.D)
                Log.e("BT", "manageConnectedSocket(): sockets not created", e);
        }
        if ( mmInStream != null)
        {
            if(Constants.D)
                Log.d("BT", "manageConnectedSocket");
            byte[] buffer = new byte[1024];
            int b = 0;
            int bufferIndex = 0;
            int payloadBytesRemaining;

            // Keep listening to the InputStream while connected
            while (!Thread.interrupted() && mmInStream != null)
            {
                try
                {
                    bufferIndex = 0;
                    // Read bytes from the stream until we encounter the the
                    // start of message character

                    while ((b = mmInStream.read()) != STX) ;

                    buffer[bufferIndex++] = (byte) b;

                    // The next byte must be the message ID, see the basic
                    // message format in the document
                    if ((b = mmInStream.read()) != MSGID)
                        continue;

                    buffer[bufferIndex++] = (byte) b;

                    // The next byte must be the expected data length code,
                    // we don't handle variable length messages, see the doc
                    if ((b = mmInStream.read()) != DLC)
                        continue;

                    buffer[bufferIndex++] = (byte) b;

                    payloadBytesRemaining = b;

                    while ((payloadBytesRemaining--) > 0)
                    {
                        buffer[bufferIndex++] = (byte) (b = mmInStream.read());
                    }

                    // The next byte should be a CRC
                    buffer[bufferIndex++] = (byte) (b = mmInStream.read());

                    // The next byte must be the end of text indicator, or
                    // there was sadness, see the basic message format in
                    // the document
                    if ((b = mmInStream.read()) != ETX)
                        continue;

                    buffer[bufferIndex++] = (byte) b;

                    HxMData data = new HxMData(buffer);

                    if (last_recieved == null || last_recieved.HeartBeatNum != data.HeartBeatNum)
                    {
                        last_recieved = data;
                        if (parent.hrmonitorListener != null)
                            parent.hrmonitorListener.onData(data);
                    }

                    if(Constants.D)
                        Log.d("HXMDataRead", "manageConnectedSocket: read " + Integer.toString(bufferIndex) + " bytes, HR: " + data.HeartRate + " , Index: " + data.HeartBeatNum + " , Battary: " + data.BatteryCharge);

                    // Send the obtained bytes to the UI Activity
                    // mHandler.obtainMessage(R.string.HXM_SERVICE_MSG_READ,
                    // bufferIndex, 0, buffer)
                    // .sendToTarget();

                } catch (IOException e)
                {
                    if(Constants.D)
                        Log.e("BT", "disconnected", e);
                    connectionLost();
                    break;
                }
            }
            try
            {
                mmInStream.close();
                mmSocket.close();
            } catch (IOException e)
            {
                if(Constants.D)
                    Log.e("BT", "disconnected", e);
            }
            if(Constants.D)
                Log.d("BT", "ConnectedThread.run(): finished");
        }
    }

    private void connectionLost()
    {
        try
        {
            if(mmSocket!=null)
                mmSocket.close();
        } catch (IOException e)
        {
            if(Constants.D)
                Log.e("BT", "disconnected", e);
        }
        this.cancel();
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public void cancel()
    {
        try
        {
            if(mmSocket!=null)
                mmSocket.close();
            //this.cancel();
        } catch (IOException e)
        {
            if(Constants.D)
                Log.e("BT", "disconnected", e);
        }
    }
}
