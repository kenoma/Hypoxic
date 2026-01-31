package com.lanius.kenoma.pob;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.AlaramReceiver;
import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.Exercise;
import com.lanius.kenoma.pob.Classes.ExerciseLog;
import com.lanius.kenoma.pob.Classes.HRV;
import com.lanius.kenoma.pob.dialogs.ExInfoDialog;
import com.lanius.kenoma.pob.hr_monitor.HRBodySensors;
import com.lanius.kenoma.pob.hr_monitor.HRCamera;
import com.lanius.kenoma.pob.hr_monitor.HRCMS50;
import com.lanius.kenoma.pob.hr_monitor.HRMonitor;
import com.lanius.kenoma.pob.hr_monitor.HxMData;
import com.lanius.kenoma.pob.hr_monitor.HxMZephyr;
import com.lanius.kenoma.pob.hr_monitor.IHRMonitorData;
import com.lanius.kenoma.pob.sql.StoredStatsDbHelper;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PretrainActivity extends ActionBarActivity implements ExInfoDialog.DialogListener
{
    final private String debugTag = "PretrainActivity";
    HRWatcher watcher;
    private ExerciseAdapter exAdapter;
    private ExerciseLog log = new ExerciseLog();
    private Timer autoUpdate = null;
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private ArrayList<Exercise> exercises = new ArrayList<Exercise>();
    private Integer currentExercise = 0;
    private ProgressDialog progress;

    static int[] toIntArray(List<Integer> integerList)
    {
        int[] intArray = new int[integerList.size()];
        for (int i = 0; i < integerList.size(); i++)
            intArray[i] = integerList.get(i);

        return intArray;
    }

    public static ArrayList<Exercise> loadAdditionalExercises(Context mContext)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String addfile = sharedPrefs.getString("prefPathToAdditionalEx", "");
        if (!addfile.equals(""))
        {
            XmlPullParserFactory factory = null;
            try
            {
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new FileReader(addfile + "/hypoxic_add.xml"));
                return Exercise.LoadExList(mContext.getSharedPreferences(Constants.PROG_PREF + "_EX", 0), xpp);

            } catch (XmlPullParserException e)
            {
                //e.printStackTrace();
            } catch (FileNotFoundException e)
            {
                //e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        int currentOrientation = getResources().getConfiguration().orientation;
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String mode = sharedPrefs.getString("prefHRMode", "none");
        SharedPreferences.Editor ed = sharedPrefs.edit();
        if (mode.equals("none") || mode.equals(""))
        {
            ed.putBoolean("prefAutoSwitch", false);
            ed.apply();
        } else
        {
            ed.putBoolean("prefAutoSwitch", true);
            ed.apply();
        }

        if (!sharedPrefs.getBoolean("prefAutoSwitch", false) && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setContentView(R.layout.activity_pretrain_simple);
        } else
            setContentView(R.layout.activity_pretrain);

        currentExercise = sharedPrefs.getInt("lastSelectedExercise", 0);

        raterAndAlert(sharedPrefs);

        setAlarm();

        watcher = ((HRWatcher) getApplicationContext());



    }

//    private void setScreenOffListener()
//    {
//        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
//        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
//        registerReceiver(new BroadcastReceiver()
//        {
//            @Override
//            public void onReceive(Context context, Intent intent)
//            {
//                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
//                {
//                    totallyStopApp = false;
//                    Log.d("PretrainActivity", Intent.ACTION_SCREEN_OFF);
//                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
//                {
//                    Log.d("PretrainActivity", Intent.ACTION_SCREEN_ON);
//                }
//            }
//        }, intentFilter);
//    }

    private void raterAndAlert(final SharedPreferences sharedPrefs)
    {
        AppRater.app_launched(this);

        if (sharedPrefs.getBoolean("showWelcomeAlert", true))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).setTitle(R.string.welcome_alert_title)
                    .setMessage(getString(R.string.welcome_alert_content))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // continue with delete
                        }
                    })
                    .setNegativeButton(R.string.welcome_alert_ok_do_not_show_again, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            SharedPreferences.Editor ed = sharedPrefs.edit();
                            ed.putBoolean("showWelcomeAlert", false);
                            ed.commit();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }
    }

    private void WIP(final SharedPreferences sharedPrefs)
    {

        if (sharedPrefs.getBoolean("showCameraAlertA", true))
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).setTitle(R.string.camera_alert_title)
                    .setMessage(getString(R.string.camera_alert_content))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // continue with delete
                        }
                    })
                    .setNegativeButton(R.string.camera_alert_ok_do_not_show_again, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            SharedPreferences.Editor ed = sharedPrefs.edit();
                            ed.putBoolean("showCameraAlertA", false);
                            ed.apply();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void setAlarm()
    {
        try
        {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            Calendar calendar = Calendar.getInstance();
            int hour = Math.max(0, calendar.get(Calendar.HOUR_OF_DAY) - 1);
            int min = Math.max(0, calendar.get(Calendar.MINUTE));

            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, min);
            calendar.set(Calendar.SECOND, 0);
            Intent intent1 = new Intent(PretrainActivity.this, AlaramReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(PretrainActivity.this, Constants.ALARM_ID, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager am = (AlarmManager) PretrainActivity.this.getSystemService(Context.ALARM_SERVICE);
            if (sharedPrefs.getBoolean("allowDailyReminder", false))
            {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            } else
                am.cancel(pendingIntent);
        } catch (Exception e)
        {
            Log.d("ERROR", e.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (progress != null && progress.isShowing())
                progress.dismiss();
            else
            {
                if (watcher != null && watcher.monitor != null)
                {
                    watcher.monitor.TurnOff();
                    watcher.monitor.Stop();
                    watcher.monitor = null;
                    if (autoUpdate != null)
                        autoUpdate.cancel();
                    autoUpdate = null;
                }
                finish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("prefAutoSwitch", false))
        {
            ConnectDevice();
        } else
        {
            sendMessage("aRR", "NA");
            LinearLayout chart = (LinearLayout) findViewById(R.id.chart);
            chart.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {


        if (requestCode == Constants.REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                watcher.isBluetoothWasSitchedOn = false;
                if (Constants.D)
                    Log.d(debugTag, "onActivityResult: Constants.REQUEST_ENABLE_BT");
                if (watcher != null && watcher.monitor != null)
                    watcher.monitor.FindHRMonitor();
            } else
            {
                if (progress != null)
                {
                    progress.dismiss();
                    progress = null;
                }
            }
        }
        if (requestCode == Constants.REQUEST_TAGS_CHOOSEN)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                String retval = data.getStringExtra("tags");
                if (Constants.D)
                    Log.d(debugTag, "onActivityResult: Constants.REQUEST_TAGS_CHOOSEN: " + retval);
                if (log != null)
                    log.Tags += retval;

                GoToExercise();
            }
        }
        if (requestCode == Constants.REQUEST_EXERCISE)
        {
            if (Constants.D)
                Log.d(debugTag, "onActivityResult: Constants.REQUEST_TAGS_EXERCISE");
            if (resultCode == Activity.RESULT_OK)
            {

                log.Finished = new Date();
                if (data != null)
                {
                    log.phaseDurations = data.getIntArrayExtra("duration");
                    log.controlValue = data.getIntArrayExtra("control");
                    //if (data.getBooleanExtra("isInterrupted", false))
                    //    log.Tags += getString(R.string.exercize_phase_interrupted) + ";";
                }
                String filename = SaveData(log);

                SharedPreferences settings = getSharedPreferences(Constants.PROG_PREF, 0);
                boolean isShowStats = settings.getBoolean("isAfterExStat", true);
                if (isShowStats)
                {
                    Intent resultAct = new Intent(PretrainActivity.this, ResultActivity.class);
                    resultAct.putExtra("RR", ExerciseLog.convertIntegers(log.RR));
                    resultAct.putExtra("d", log.phaseDurations);
                    resultAct.putExtra("t", log.phaseTypes);
                    resultAct.putExtra("control", log.controlValue);
                    resultAct.putExtra("Tags", log.Tags);
                    resultAct.putExtra("filename", filename);
                    resultAct.putExtra("exName", log.exercisName);

                    startActivityForResult(resultAct, Constants.REQUEST_RESULTS);
                } else
                {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }



            } else
            {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }
        if (requestCode == Constants.REQUEST_RESULTS)
        {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

        if (requestCode == Constants.REQUEST_SETTINGS)
        {
            if (watcher != null && watcher.monitor != null)
            {
                watcher.monitor.TurnOff();
                watcher.monitor.Stop();
            }
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

    }


    @Override
    public void onResume()
    {
        super.onResume();

        if (autoUpdate == null)
        {
            ConnectDevice();
            autoUpdate = new Timer();
            autoUpdate.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            updateHRInfo();
                        }
                    });
                }
            }, 0, 1500);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (!sharedPrefs.getBoolean("prefAutoSwitch", false))
                autoUpdate.cancel();

            LinearLayout chart = (LinearLayout) findViewById(R.id.chart);
            if (chart != null)
            {
                if (mChart == null)
                {
                    initChart();
                    mRenderer.setPanEnabled(false);
                    mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.1f);
                    chart.addView(mChart);

                } else
                    mChart.repaint();
            }

            if (exercises != null)
                exercises.clear();

            exercises = Exercise.LoadExList(getSharedPreferences(Constants.PROG_PREF + "_EX", 0), getResources().getXml(R.xml.exercises));
            ArrayList<Exercise> add_ex = PretrainActivity.loadAdditionalExercises(this);
            if (add_ex != null)
                exercises.addAll(add_ex);


            exAdapter = new ExerciseAdapter(this, R.layout.exercise_info, exercises);
            Spinner sp = (Spinner) findViewById(R.id.exerc_name);
            sp.setAdapter(exAdapter);
            if (currentExercise >= sp.getCount())
                currentExercise = 0;

            sp.setSelection(currentExercise);

            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
                {
                    if (arg1 != null)
                    {
                        Log.d(debugTag, "LoadExList: Selected: " + arg2);
                        currentExercise = arg2;

                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(arg1.getContext());
                        SharedPreferences.Editor ed = sharedPrefs.edit();
                        ed.putInt("lastSelectedExercise", currentExercise);
                        ed.commit();

                        updateInterface();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0)
                {
                }
            });
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();


    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_pretrain, menu);
        MenuItem item = menu.findItem(R.id.view_reconnect_hr_monitor);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("prefAutoSwitch", false))
            item.setVisible(true);
        else
            item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.view_db:
                GoToDBScreen();
                return true;
            case R.id.view_settings:
                GoToSettings();
                return true;
            case R.id.view_statistics:
                GoToStat();
                return true;
            case R.id.view_exercise_details:
                showDetailedInfo();
                return true;
            case R.id.view_reconnect_hr_monitor:

                if (watcher != null && watcher.monitor != null)
                    watcher.monitor.Stop();
                watcher.monitor = null;
                ConnectDevice();
                return true;
            case R.id.view_schedule:
                GoToScheduler();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void GoToScheduler()
    {
        try
        {
            if (android.os.Build.VERSION.SDK_INT >= 19)
            {
                Intent i = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                //i.putExtra(AlarmClock.EXTRA_MESSAGE, "Hypoxic - training session");
                //i.putExtra(AlarmClock.EXTRA_HOUR, 10);
                //i.putExtra(AlarmClock.EXTRA_MINUTES, 20);
                startActivity(i);
            } else
            {
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, "Hypoxic - training session");
                i.putExtra(AlarmClock.EXTRA_HOUR, 10);
                i.putExtra(AlarmClock.EXTRA_MINUTES, 20);
                startActivity(i);
            }
        } catch (Exception ex)
        {
            Toast.makeText(this, getString(R.string.alarmclock_no_support), Toast.LENGTH_SHORT).show();
        }
    }

    public void ConnectDevice()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String mode = sharedPrefs.getString("prefHRMode", "none");

        if (mode.equals("none") || mode.equals(""))
            return;

        if (watcher != null && watcher.monitor == null)
        {
            progress = ProgressDialog.show(this, getString(R.string.progress_connect_hr_monitor_title),
                    getString(R.string.progress_connect_hr_monitor_text), true);
            progress.setCancelable(true);

            if(mode.equals("CMS50"))
                watcher.monitor = new HRCMS50();
            else if (mode.equals("zephyr"))
                watcher.monitor = new HxMZephyr();
            else if (mode.equals("camera"))
            {
                WIP(sharedPrefs);
                watcher.monitor = new HRCamera();
                BroadcastReceiver receiver = new BroadcastReceiver()
                {
                    @Override
                    public void onReceive(Context context, Intent intent)
                    {
                        if (intent.getAction().equals("no_camera"))
                        {
                            if (progress != null && progress.isShowing())
                                progress.dismiss();

                            AlertDialog alertDialog = new AlertDialog.Builder(PretrainActivity.this).setTitle(R.string.camera_alert_title)
                                    .setMessage(getString(R.string.camera_service_no_camera))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            // continue with delete
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    }
                };

                IntentFilter filter = new IntentFilter("no_camera");
                registerReceiver(receiver, filter);

            } else if (mode.equals("sensor"))
            {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    watcher.monitor = new HRBodySensors();
                else
                    Toast.makeText(this, getString(R.string.sensor_no_support), Toast.LENGTH_SHORT).show();
            }

            if (watcher.monitor != null)
            {
                watcher.monitor.setListener(new IHRMonitorData()
                {
                    @Override
                    public void onData(HxMData data)
                    {
                        if (progress != null && progress.isShowing())
                            progress.dismiss();

                        log.Update(data);
                        if (Constants.D)
                            Log.d(debugTag, "Broadcasting message");
                        Intent intent = new Intent(Constants.BROADCAST_HR);
                        intent.putExtra("aRR", String.format("%s", log.getARR()));
                        intent.putExtra("RR", toIntArray(log.FilteredRR));
                        sendMessage(intent);
                    }
                });

                if (!watcher.monitor.Init(this))
                    if (progress != null && progress.isShowing())
                        progress.dismiss();
            } else
            {
                if (progress != null && progress.isShowing())
                    progress.dismiss();
            }
        } else
        {
            HRMonitor.owner = this;
            if (watcher != null)
                watcher.monitor.setListener(new IHRMonitorData()
                {
                    @Override
                    public void onData(HxMData data)
                    {
                        log.Update(data);
                        if (Constants.D)
                            Log.d(debugTag, "Broadcasting message");
                        Intent intent = new Intent(Constants.BROADCAST_HR);
                        intent.putExtra("aRR", String.format("%s", log.getARR()));
                        intent.putExtra("RR", toIntArray(log.FilteredRR));
                        sendMessage(intent);
                    }
                });
        }
    }

    private void sendMessage(Intent intent)
    {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendMessage(String type, String message)
    {
        if (Constants.D)
            Log.d(debugTag, "Broadcasting message :" + message);
        Intent intent = new Intent(Constants.BROADCAST_HR);
        // You can also include some extra data.
        intent.putExtra(type, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void StartExercize(View v)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isTags = settings.getBoolean("isTagsAllowed", false);
        if (isTags)
        {
            Intent tagSelector = new Intent(PretrainActivity.this, TagSelector.class);
            startActivityForResult(tagSelector, Constants.REQUEST_TAGS_CHOOSEN);
        } else
            GoToExercise();
    }

    private void GoToExercise()
    {
        LinearLayout chart = (LinearLayout) findViewById(R.id.chart);
        if (chart != null)
            chart.removeAllViews();
        if (mCurrentSeries != null)
        {
            mCurrentSeries.clear();
            mCurrentSeries = null;
        }

        Exercise ex = exercises.get(currentExercise);
        if (ex.mode.equals("hypoxic"))
        {
            log.Tags += "Hold " + ex.events.get(-1)[3] / 1000 + " sec;";
        }
        log.exerciseID = ex.id;
        log.exercisName = ex.name;

        Intent exercise = new Intent(PretrainActivity.this, ExerciseActivity.class);
        int[] phase_duration = ex.getDurationScheme();
        int[] phase_type = ex.getTypeScheme();
        int[] control_rr = ex.getControlScheme();

        exercise.putExtra("duration", phase_duration);
        exercise.putExtra("pattern", ex.pattern);
        exercise.putExtra("type", phase_type);
        exercise.putExtra("control", control_rr);
        exercise.putExtra("mode", ex.mode);
        exercise.putExtra("lcycle", ex.pattern.length);
        startActivityForResult(exercise, Constants.REQUEST_EXERCISE);
        if (log != null)
        {
            log.Started = new Date();
            log.phaseDurations = phase_duration.clone();
            log.phaseTypes = phase_type.clone();
            //log.FilteredRR.clear();
            //log.RR.clear();
        }
    }

    private String SaveData(ExerciseLog log)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH);
        String filename = String.format("%s_%s_%s_%s.ex",
                log.exercisName,
                log.RR.size(),
                sdf.format(log.Started),
                sdf.format(log.Finished));
        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(this.getApplicationContext());
        HashMap<String, Double> dic = HRV.analyse(toIntArray(log.FilteredRR), log.controlValue, log.phaseDurations, log.phaseTypes);
        if (dic != null)
            dbHelper.storeData(filename, log, dic);

        File f = new File(filename);
        f.mkdirs();

        FileOutputStream outputStream;
        try
        {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(log.Serialize().getBytes("UTF-8"));
            outputStream.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return filename;
    }


    private void updateHRInfo()
    {
        if (Constants.D)
            Log.d(debugTag, "updateHRInfo");
        //if (mRenderer != null)
        //    mRenderer.setChartTitle(getString(R.string.hr_chart_title));

        if (watcher != null && watcher.monitor != null && watcher.monitor.isAlive())
        {
            if (log != null && mCurrentSeries != null)
            {
                int i = mCurrentSeries.getItemCount();
                if (i < log.FilteredRR.size())
                {
                    if (progress != null && progress.isShowing())
                        progress.dismiss();

                    String status = String.format(getString(R.string.chart_heartrate_status), log.getCurrentHR(), log.getBCharge());
                    //TextView tw = (TextView) findViewById(R.id.heart_rate_monior_status);
                    if (mRenderer != null)
                        mRenderer.setChartTitle(status);
                    //tw.setText(status);

                    while (i < log.FilteredRR.size())
                        if (log.FilteredRR.get(i) != 0)
                            mCurrentSeries.add(i, 60000.0 / log.FilteredRR.get(i++));
                    if (mCurrentSeries.getItemCount() > 40)
                        mCurrentSeries.remove(0);

                    mChart.repaint();
                }
            }
        }

    }

    public void GoToSettings()
    {
        Intent i = new Intent(PretrainActivity.this, SettingsActivity.class);
        startActivityForResult(i, Constants.REQUEST_SETTINGS);
    }

    public void GoToDBScreen()
    {
        Intent activity = new Intent(PretrainActivity.this, ViewDatabaseActivity.class);
        startActivity(activity);
    }

    public void GoToStat()
    {
        StoredStatsDbHelper dbHelper = new StoredStatsDbHelper(getApplicationContext());
        if (dbHelper.isAnyRecords())
        {
            Intent i = new Intent(PretrainActivity.this, StatisticActivity.class);
            startActivity(i);
        } else
            Toast.makeText(this, R.string.statistics_no_data, Toast.LENGTH_SHORT).show();
    }

    private void initChart()
    {
        mCurrentSeries = new XYSeries(getString(R.string.hr_chart_title));
        mDataset.addSeries(mCurrentSeries);
        XYSeriesRenderer mCurrentRenderer = new XYSeriesRenderer();
        mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setShowTickMarks(false);
        mRenderer.setChartTitle(getString(R.string.hr_chart_title));
        mRenderer.setChartTitleTextSize(getResources().getDimension(R.dimen.pretrain_heart_rate_chart));

        mRenderer.setShowLegend(false);
        mRenderer.setMargins(new int[]{0, 0, 0, 0});
        mRenderer.setYLabelsAngle(270.0f);
        mRenderer.setYLabelsPadding(10.0f);
        mRenderer.setMarginsColor(Color.BLACK);
        //mRenderer.setMargins(new int[4]);
        mRenderer.setShowGridX(true);
        mRenderer.setShowGridY(true);
        mRenderer.setXLabels(0);

        //mRenderer.setYLabels(0);

    }


    private void updateInterface()
    {
        Exercise ex = exercises.get(currentExercise);

        long overall_duration = 0;
        for (int e : ex.getDurationScheme()) overall_duration += e;

        long hours = overall_duration / (60 * 60 * 1000);
        overall_duration = overall_duration - hours * (60 * 60 * 1000);
        long minutes = overall_duration / (60 * 1000);
        overall_duration = overall_duration - minutes * (60 * 1000);
        long seconds = overall_duration / (1000);
        NumberFormat formatter = formatter = new DecimalFormat("00");
        TextView tv = (TextView) findViewById(R.id.overall_time);
        tv.setText(formatter.format(minutes) + ":" + formatter.format(seconds));
    }

    public void showDetailedInfo()
    {
        if (currentExercise < exercises.size())
        {
            FragmentManager fm = getSupportFragmentManager();
            ExInfoDialog infoDialog = new ExInfoDialog();
            Bundle i = new Bundle();
            i.putInt("id", exercises.get(currentExercise).id);
            i.putInt("cycles", exercises.get(currentExercise).Cycles);

            infoDialog.setArguments(i);
            infoDialog.show(fm, "nonono");

            //startActivityForResult(i, Constants.REQUEST_DESCRIPTION);
        }
    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog)
    {
        Bundle data = dialog.getArguments();
        if (data != null)
        {
            int id = data.getInt("id", 0);
            Spinner sp = (Spinner) findViewById(R.id.exerc_name);

            if (sp != null && exercises != null)
            {
                for (int i = 0; i < exAdapter.getCount(); i++)
                    if (exAdapter.getItem(i).id == id)
                    {
                        currentExercise = i;
                        sp.setSelection(currentExercise);

                        Exercise ex = exAdapter.getItem(i);
                        cookExercise(data, ex);
                        break;
                    }
                updateInterface();
            }

        }
    }

    private void cookExercise(Bundle data, Exercise ex)
    {
        int[] first = data.getIntArray("duration");
        int Cycles = data.getInt("cycles", ex.Cycles);

        if (first != null)
        {
            ex.events.put(-1, first);
            ex.events.put(0, first);
        }
        ex.Cycles = Cycles;

    }


    private class ExerciseAdapter extends BaseAdapter
    {
        LayoutInflater inflator;
        private ArrayList<Exercise> exList;

        public ExerciseAdapter(Context context, int textViewResourceId,
                               ArrayList<Exercise> exercises)
        {
            this.exList = exercises;
            inflator = LayoutInflater.from(context);
        }

        @Override
        public int getCount()
        {
            return exList.size();
        }

        @Override
        public Exercise getItem(int position)
        {
            return exList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return exList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            convertView = inflator.inflate(R.layout.exercise_info, null);
            Exercise ex = exList.get(position);
            TextView tv = (TextView) convertView.findViewById(R.id.ex_name);
            tv.setText(ex.name);
            tv = (TextView) convertView.findViewById(R.id.ex_shortDescription);
            tv.setText(ex.shortDescription);
            /*tv = (TextView) convertView.findViewById(R.id.ex_scheme);

            String scheme = "";

            for (int i = 0; i < ex.pattern.length; i++)
            {
                String next = "";
                if (ex.pattern[i] == Constants.EXERCISE_PHASE_INHALE)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.inhale), "i");
                else if (ex.pattern[i] == Constants.EXERCISE_PHASE_EXHALE)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.exhale), "e");
                else if (ex.pattern[i] == Constants.EXERCISE_PHASE_PAUSE)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.pause), "p");
                else if (ex.pattern[i] == Constants.EXERCISE_PHASE_HOLD)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.hold), "h");

                scheme += next;
            }
            tv.setText(Html.fromHtml("[" + scheme));

            tv = (TextView) convertView.findViewById(R.id.ex_mask);
            scheme = "";

            for (int i = 0; i < ex.mask.length(); i++)
            {
                String next = "";
                if (i == 0)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.inhale), ex.mask.charAt(i));
                else if (i == 1)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.pause), ex.mask.charAt(i));
                else if (i == 2)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.exhale), ex.mask.charAt(i));
                else if (i == 3)
                    next = String.format("<font color='%s'>%s</font>", getResources().getColor(R.color.hold), ex.mask.charAt(i));

                scheme += next;
            }
            tv.setText(Html.fromHtml(scheme + "]"));*/


            return convertView;
        }


    }
}
