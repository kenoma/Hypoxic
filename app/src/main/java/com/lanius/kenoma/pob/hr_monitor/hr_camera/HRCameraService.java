package com.lanius.kenoma.pob.hr_monitor.hr_camera;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.HRWatcher;
import com.lanius.kenoma.pob.PretrainActivity;
import com.lanius.kenoma.pob.R;
import com.lanius.kenoma.pob.hr_monitor.HRCamera;
import com.lanius.kenoma.pob.hr_monitor.HRMonitor;

import java.io.IOException;
import java.util.List;


public class HRCameraService extends Service implements SurfaceHolder.Callback
{
    static  private WindowManager windowManager;
    static private SurfaceView surfaceView;
    static private Camera camera = null;

    @Override
    public void onCreate()
    {
        // Start foreground service to avoid unexpected kill
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Camera HR Detector is on")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
        startForeground(1234, notification);

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                32, 32,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        if (camera == null)
        {
            if(Constants.D)
                Log.d("PCam", "surfaceCreated");
            try
            {
                camera = Camera.open();

            } catch (Exception e)
            {
                if(Constants.D)
                    Log.e("PCam", "failed to open Camera");
                e.printStackTrace();
            }

            if (camera != null)
            {
                Camera.Parameters parameters = camera.getParameters();
                if(Constants.D)
                    Log.d("PCam","Image format: "+parameters.getPreviewFormat()+" recomended "+ImageFormat.NV21);

                parameters.setPreviewFormat(ImageFormat.NV21);
                List<Camera.Size> localSizes = parameters.getSupportedPreviewSizes();
                Camera.Size bestSize = localSizes.get(0);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                boolean switchOnFlash = settings.getBoolean("switchOnFlash", false);
                if(Constants.D)
                    Log.d("PCam", "allowFLASH is "+switchOnFlash);
                if(switchOnFlash&&getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                for (int i = 1; i < localSizes.size(); i++)
                    if (localSizes.get(i).width * localSizes.get(i).height < bestSize.width * bestSize.height)
                        bestSize = localSizes.get(i);

                //float defaultCameraRatio = (float) parameters.getPictureSize().width / (float) parameters.getPictureSize().height;

                localSizes = camera.getParameters().getSupportedPictureSizes();
                Camera.Size bestPicSize = localSizes.get(0);

                for (Camera.Size s : localSizes)
                    if (s.width * s.height < bestPicSize.width * bestPicSize.height)
                        bestPicSize = s;

                parameters.setPreviewSize(bestSize.width, bestSize.height);

                int yStride = (int) Math.ceil(bestSize.width / 16.0) * 16;
                int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
                int ySize = yStride * bestSize.height;
                int uvSize = uvStride * bestSize.height / 2;
                int bufferSize = ySize + uvSize * 2;

                parameters.setPictureSize(bestPicSize.width, bestPicSize.height);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                {
                    if (parameters.isAutoExposureLockSupported())
                        parameters.setAutoExposureLock(true);
                }

                List<int[]> fps = parameters.getSupportedPreviewFpsRange();
                int[] bestfps=new int[]{0,0};
                for (int[] s : fps)
                    if (s[0]>bestfps[0])
                        bestfps = s;

                parameters.setPreviewFpsRange(bestfps[0],bestfps[1]);

                camera.setParameters(parameters);
                if(Constants.D)
                    Log.d("PCam", "set preview size: Size = " + bestSize.width + " : " + bestSize.height);
                try
                {
                    if (camera != null)
                    {
                        HRMonitor mon = ((HRWatcher) getApplicationContext()).monitor;
                        if (mon != null && mon instanceof HRCamera)
                        {
                            camera.setPreviewDisplay(surfaceHolder);
                            HRCamera.size = bestSize;
                            HRCamera.buffer = new byte[bufferSize];
                            camera.addCallbackBuffer(HRCamera.buffer);
                            //camera.setPreviewCallback(HRCamera.previewCallback);
                            camera.setPreviewCallbackWithBuffer(HRCamera.previewCallback);
                            if(Constants.D)
                                Log.d("PCam", "ready to use");
                        }
                    }

                } catch (IOException exception)
                {
                    if(Constants.D)
                        Log.e("PCam", exception.getMessage());
                    camera.release();
                    camera = null;
                }
            }else
            {
                Intent intent = new Intent("no_camera");
                sendBroadcast(intent);

            }
        }

    }


    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy()
    {
        if(Constants.D)
            Log.d("PCam", "onDestroy");
        if (camera != null)
        {
            if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            {
                Log.d("PCam", "switching off flash");
                Camera.Parameters parameters = camera.getParameters();
                if (!parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF))
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);
            }
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        windowManager.removeView(surfaceView);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
        if(Constants.D)
            Log.d("PCam", "surfaceChanged");
        if (camera != null)
            camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        if(Constants.D)
            Log.d("PCam", "surfaceDestroyed");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

}