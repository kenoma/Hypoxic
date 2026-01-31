package com.lanius.kenoma.pob.hr_monitor;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.HRWatcher;
import com.lanius.kenoma.pob.hr_monitor.hr_camera.HRCameraService;


public class HRCamera extends HRMonitor
{
    static final int NZEROS = 4;
    static double[] xv = new double[NZEROS + 1];
    static final int NPOLES = 4;
    static double[] yv = new double[NPOLES + 1];
    static final double GAIN = 8.655595672e+01;
    final static int time_step = 10;
    final static int window_size = 250;
    static double[] signal = new double[window_size * 2];
    static public byte[] buffer;
    static public Camera.Size size;
    static int width, height;
    static int ared;
    static int two_step_aside_amp;
    static long two_step_aside_timestamp = -1;
    static long step_aside_timestamp = -1;
    static int step_aside_amp;
    static long pp_index_1 = -1, pp_index_2 = -1, pp_index_3 = -1;
    static private HRWatcher myApp = null;
    static private int[] beatsArray = new int[15];
    static private int beatsNum = 0;
    static private long executed = System.currentTimeMillis();
    static private boolean processing = false;
    static public Camera.PreviewCallback previewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam)
        {
            if (!processing && data != null && size != null)
                processImage(buffer, size);

            cam.addCallbackBuffer(buffer);
        }
    };
    static private int[] decoder_y = new int[4];

    private static double butterworth(double input)
    {
        xv[0] = xv[1];
        xv[1] = xv[2];
        xv[2] = xv[3];
        xv[3] = xv[4];
        xv[4] = input / GAIN;
        yv[0] = yv[1];
        yv[1] = yv[2];
        yv[2] = yv[3];
        yv[3] = yv[4];
        yv[4] = (xv[0] + xv[4]) - 2 * xv[2]
                + (-0.7219367214 * yv[0]) + (3.1120694922 * yv[1])
                + (-5.0568844402 * yv[2]) + (3.6667279877 * yv[3]);

        return yv[4];
    }

    private static int decodeRedSum(byte[] data, int width, int height)
    {
        if (data == null)
            return 0;
        final int size = width * height;
        int v, r, sum = 0;

        for (int i = 0, k = 0; i < size; i += 2, k += 2)
        {
            decoder_y[0] = data[i] & 0xff;
            decoder_y[1] = data[i + 1] & 0xff;
            decoder_y[2] = data[width + i] & 0xff;
            decoder_y[3] = data[width + i + 1] & 0xff;

            v = (data[size + k] & 0xff) - 128;

            for (int y : decoder_y)
            {
                r = (y + (1772 * v)) / 1000;
                sum += r > 255 ? 255 : r < 0 ? 0 : r;
            }

            if ((i + 2) % width == 0)
                i += width;
        }
        return sum;
    }

    static protected void processImage(byte[] data, Camera.Size size)
    {
        processing = true;

        width = size.width;
        height = size.height;
        ared = decodeRedSum(data, width, height);
        if (ared < 100)
            ared = 0;

        long timestamp = (System.currentTimeMillis() - executed);


        if (Constants.D)
            Log.d("CameraRED", "Level = " + ared);

        long frame = isPeak(timestamp, ared);
        if (frame != -1)
        {
            if (pp_index_3 == -1)
            {
                pp_index_3 = pp_index_2;
                pp_index_2 = pp_index_1;
                pp_index_1 = frame;
            } else
            {
                long rr_prevs = pp_index_2 - pp_index_3;
                long rr_center = pp_index_1 - pp_index_2;
                long rr_future = frame - pp_index_1;

                if (rr_prevs > 300 && rr_prevs < 3000 &&
                        rr_future > 300 && rr_future < 3000)
                {
                    if (rr_center + 200 > rr_prevs + rr_future)
                    {
                        long index = rr_center / (rr_prevs + rr_future);
                        if (index < 2)
                        {
                            sendPulse((int) (pp_index_2 + (rr_future + rr_prevs) / 2));
                            sendPulse((int) pp_index_1);
                            pp_index_2 += (rr_future + rr_prevs) / 2;
                        }
                    } else if (rr_center > 300 && rr_center < 3000)
                        sendPulse((int) pp_index_1);

                } else if (rr_center > 300 && rr_center < 3000)
                    sendPulse((int) pp_index_1);

                pp_index_3 = pp_index_2;
                pp_index_2 = pp_index_1;
                pp_index_1 = frame;
            }

        }


        processing = false;
    }

  /*  static public void sendCameraData()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, "bghati@gmail.com");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Data from me "+System.currentTimeMillis());


        StringBuilder sb = new StringBuilder();
        sb.append("int[] raw_vals = new int[] {");
        for(int val:data_amp)
            sb.append(val).append(", ");
        sb.append("};\r\n");
        sb.append("long[] T = new long[] {");
        for(int val:data_time)
            sb.append(val).append(", ");
        sb.append("};");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        owner.startActivity(Intent.createChooser(intent, "Send Email"));
    }*/

    private static void sendPulse(int timestamp)
    {
        beatsNum++;
        if (Constants.D)
            Log.d("PCam", "BEAT #" + beatsNum + " RR: " + timestamp);

        System.arraycopy(beatsArray, 0, beatsArray, 1, 14);
        beatsArray[0] = timestamp;
        if (hrmonitorListener != null)
            hrmonitorListener.onData(new HxMData(beatsArray, beatsNum));
    }

    private static long isPeak(long timestamp, int amp)
    {
        if (step_aside_timestamp == -1)
        {
            step_aside_amp = amp;
            step_aside_timestamp = timestamp;
            return -1;
        }

        if (two_step_aside_timestamp == -1)
        {
            two_step_aside_amp = step_aside_amp;
            two_step_aside_timestamp = step_aside_timestamp;
            step_aside_amp = amp;
            step_aside_timestamp = timestamp;
            return -1;
        }

        int ticks = (int) (timestamp - step_aside_timestamp) / time_step;
        int arrlen = 2 * window_size;
        if (ticks >= arrlen)
            ticks = arrlen - 1;
        if (ticks < 0)
            ticks = 0;

        System.arraycopy(signal, ticks, signal, 0, arrlen - ticks);

        int pos = arrlen - ticks - 1;
        double px = 0;
        for (long x = step_aside_timestamp; x < timestamp; x += time_step)
        {
            if (x <= timestamp)
                px = step_aside_amp + (amp - step_aside_amp) * (x - step_aside_timestamp) / (timestamp - step_aside_timestamp);
            signal[pos++] = butterworth(px);
        }

        long retval = -1;

        if (signal[arrlen - ticks - 1] <= 0 && signal[pos - 1] > 0)
        {
            int peaks = 0;
            boolean isGoDown = false;
            double minval = 0.0;
            double aver_peak_amp = 0.0;
            int min_index = 0;
            int godown_index = 0;
            long last_peak = 0;
            for (int x = 1; x < arrlen - 1; x++)
            {
                aver_peak_amp = Math.min(aver_peak_amp, signal[x]);

                if (signal[x - 1] >= 0 && signal[x] < 0)
                {
                    isGoDown = true;
                    godown_index = x;
                    minval = 0.0;
                }
                if (isGoDown)
                {
                    if (minval > signal[x])
                    {
                        minval = signal[x];
                        min_index = x;
                    }
                    if (signal[x] < 0 && signal[x + 1] >= 0)
                    {
                        isGoDown = false;
                        if (min_index - godown_index < 0.8 * (x - min_index) && x - godown_index >= 14)
                        {
                            peaks++;

                            last_peak = min_index;
                        }
                    }
                }
            }

            if (peaks >= 2)
            {
                aver_peak_amp = signal[min_index] / aver_peak_amp;
                if (aver_peak_amp >= 0.3 && aver_peak_amp <= 1.7)
                    retval = step_aside_timestamp + (last_peak - arrlen + ticks) * time_step;
            }
        }

        two_step_aside_amp = step_aside_amp;
        two_step_aside_timestamp = step_aside_timestamp;
        step_aside_timestamp = timestamp;
        step_aside_amp = amp;

        return retval;
    }

    @Override
    public boolean isAlive()
    {
        for (int aBeatsArray : beatsArray)
            if (aBeatsArray != 0)
                return true;
        return false;
    }

    @Override
    public boolean Init(Activity Owner)
    {
        super.Init(Owner);
        myApp = (HRWatcher) owner.getApplicationContext();
        myApp.startService(new Intent(owner.getApplicationContext(), HRCameraService.class));

        return true;
    }

    @Override
    public void TurnOff()
    {
        if (Constants.D)
            Log.d("PCam", "turnoff");
        myApp.stopService(new Intent(owner.getApplicationContext(), HRCameraService.class));
    }

    @Override
    public void FindHRMonitor()
    {

    }

    @Override
    public void Stop()
    {


    }

}
