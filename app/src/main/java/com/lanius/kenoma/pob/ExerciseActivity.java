package com.lanius.kenoma.pob;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.ProgressBar;
import com.lanius.kenoma.pob.Control.HeartRateController;
import com.lanius.kenoma.pob.visualizations.ExerciseDraw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class ExerciseActivity extends Activity implements Animation.AnimationListener
{
    TextView TVCount;
    TextView TVPhase;
    ExerciseDraw drawer;
    ProgressBar progress_bar;
    HeartRateController hrControl;
    private PowerManager.WakeLock wakeLock;
    private GestureDetector gestureDetector;
    private boolean isInterrupted = false;
    private boolean isSoundEnable = true;
    private Timer autoUpdate;
    private Timer metroUpdate;
    private int[] p_duration;
    private int[] p_pattern;
    private int[] p_type;
    private int[] p_control_parameters;
    private ArrayList<Integer> p_control_value = new ArrayList<>();
    private String mode;
    private int CurrentPhase = 0;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                // Get extra data included in the Intent
                String message = intent.getStringExtra("aRR");
                if (message != null)
                {
                    Log.d("receiver", "Got message: " + message);
                    setHRValue(message);
                }

                int[] hr_message = intent.getIntArrayExtra("RR");
                if (hr_message != null)
                {
                    if (mode.equals("control"))
                    {
                        hrControl.update(p_type, p_duration, p_pattern, hr_message, CurrentPhase);
                        while (p_control_value.size() != hr_message.length)
                            p_control_value.add((int) hrControl.getControlValue());
                    }
                }
            } catch (Exception ex)
            {
                Log.d("Broadcast", "Error bleat");
            }
        }
    };
    private long CurrentDuration = 0;
    private long prevTime = 0;
    private long phaseTime = System.currentTimeMillis();
    private long corrected_time = 0;

    private MediaPlayer[] mPlayer = new MediaPlayer[4];

    private boolean isMetronomeEnabled= false;
    private int currentMetronome = 0;
    private MediaPlayer[] metronome = new MediaPlayer[2];
    private MetronomeTask mtask = new MetronomeTask();

    private int State = Constants.EXERCISE_PHASE_PREPARATION_1;
    private Animation animFadein;
    private int lCycle = 0;

    class MetronomeTask extends TimerTask {
        public void run()
        {
            if (CurrentPhase < p_type.length && p_duration[CurrentPhase] < 5000)
                return;

            if (isMetronomeEnabled && isSoundEnable && metronome[++currentMetronome % 2] != null && metronome[(currentMetronome + 1) % 2] != null)
            {
                try
                {
                    if (metronome[(currentMetronome + 1) % 2].isPlaying())
                        metronome[(currentMetronome + 1) % 2].pause();
                    metronome[(currentMetronome + 1) % 2].seekTo(0);
                    metronome[currentMetronome % 2].start();
                } catch (Exception ex)
                {
                    Log.d("metronome", "");

                }

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_exercise);

        TVCount = (TextView) findViewById(R.id.cycles_remains);
        TVPhase = (TextView) findViewById(R.id.current_phase);
        drawer = (ExerciseDraw) findViewById(R.id.draw_exercise);

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        animFadein = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        // set animation listener
        animFadein.setAnimationListener(this);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        isSoundEnable = sharedPrefs.getBoolean("prefSoundOnOff", true);
        //ImageButton btn = (ImageButton) findViewById(R.id.sound_on_off);
        /*if (isSoundEnable)
            btn.setColorFilter(R.color.volume_button_active);
        else
            btn.setColorFilter(R.color.volume_button_inactive);

        if (btn != null)
        {
            if (isSoundEnable)
                btn.setImageResource(R.drawable.volumeon);
            else
                btn.setImageResource(R.drawable.volumeoff);
        }*/

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.BROADCAST_HR));

        hrControl = new HeartRateController();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                if (CurrentPhase < p_duration.length && p_duration[CurrentPhase] > 6000)
                {
                    CurrentDuration = p_duration[CurrentPhase];
                    Toast.makeText(ExerciseActivity.this, R.string.exercize_phase_interrupted, Toast.LENGTH_SHORT).show();
                    isInterrupted = true;
                }
                return true;
            }
        });

        if (sharedPrefs.getBoolean("isPreventScreenLock", true))
        {
            PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "LocLockLock");
            wakeLock.acquire();
        } else
            wakeLock = null;

        PrepareSoundPool();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (gestureDetector.onTouchEvent(event))
            return true;
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (isMetronomeEnabled && metroUpdate == null)
        {
            metroUpdate = new Timer();
        }

        if (autoUpdate == null)
        {

            drawer.onResume();
            drawer.setRenderMode(1);

            prevTime = System.currentTimeMillis();
            phaseTime = System.currentTimeMillis();

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
                            try
                            {
                                updateEx();
                            } catch (Exception e)
                            {
                                if (e != null && e.getMessage() != null)
                                    Log.d("Exercise thread", e.getMessage());
                            }
                        }
                    });
                }
            }, 0, 10);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //drawer.onPause();
    }

    @Override
    protected void onDestroy()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        for (int i = 0; i < metronome.length; i++)
            if (metronome[i] != null)
            {
                metronome[i].stop();
                metronome[i].release();
            }

        for (int i = 0; i < 4; i++)
            if (mPlayer[i] != null)
            {
                mPlayer[i].stop();
                mPlayer[i].release();

            }


        if (autoUpdate != null)
            autoUpdate.cancel();
        if (wakeLock != null)
            wakeLock.release();
        if(metroUpdate!=null)
            metroUpdate.cancel();
        super.onDestroy();
    }

    public void setHRValue(String HR)
    {
        ///TODO placa here colored control values
        TextView tv = (TextView) findViewById(R.id.heart_rate);
        //ImageButton ib = (ImageButton) findViewById(R.id.heart_icon);

        if (tv != null)
        {
            if (HR != null && HR.equals("♥ NA"))
            {
                //      ib.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);
            } else if (HR != null && !HR.equals("0"))
            {
                double CurrentInterbeatValue = Double.parseDouble(HR);
                tv.setText(String.format("♥ %.0f bpm", 60000 / CurrentInterbeatValue));
                //    ib.setVisibility(View.VISIBLE);
                tv.setVisibility(View.VISIBLE);
            } else
            {
                tv.setText(getString(R.string.hxm_monitor_lost));
                //  ib.setVisibility(View.VISIBLE);
                tv.setVisibility(View.VISIBLE);
            }
        }
    }

    public void SetSoundOnOFF(View v)
    {
        //isSoundEnable = !isSoundEnable;
        /*ImageButton btn = (ImageButton) findViewById(R.id.sound_on_off);
        if (btn != null)
        {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = sharedPrefs.edit();

            if (isSoundEnable)
            {
                btn.setImageResource(R.drawable.volumeon);
                btn.setColorFilter(R.color.volume_button_active);

                edit.putBoolean("prefSoundOnOff", true);
            } else
            {
                btn.setImageResource(R.drawable.volumeoff);
                btn.setColorFilter(R.color.volume_button_inactive);
                edit.putBoolean("prefSoundOnOff", false);
            }

            edit.commit();
        }*/
    }

    private void PrepareSoundPool()
    {
        String[] prefSound = new String[]{"prefInhaleSound", "prefPauseSound", "prefExhaleSound", "prefHoldSound"};
        String[] defVals = new String[]{"clean_e1", "drum_1", "clean_re", "drum_2"};
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String packageName = getPackageName();

        for (int i = 0; i < 4; i++)
        {
            String sound = sharedPrefs.getString(prefSound[i], defVals[i]);
            if (sound != "")
            {
                int resID = getResources().getIdentifier(sound, "raw", packageName);
                //AssetFileDescriptor afd = getResources().openRawResourceFd(resID);
                if (resID != 0)
                {
                    Log.d("load sound", "playSong :: " + sound);
                    mPlayer[i] = MediaPlayer.create(this, resID);

                    //int soundID = mSoundPool.load(this, resID, 1);
                    //mSoundID[i] = soundID;
                } else
                {
                    mPlayer[i] = new MediaPlayer();
                    try
                    {
                        mPlayer[i].setDataSource(sound);
                        mPlayer[i].prepare();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        Toast.makeText(this, "No audio file " + sound, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        String metrsound = "tick";
        isMetronomeEnabled = sharedPrefs.getBoolean("prefMetronomeOnOff", false);

        int resID = getResources().getIdentifier(metrsound, "raw", packageName);
        if (resID != 0)
        {
            Log.d("load sound", "metronome :: " + metrsound);
            metronome[0] = MediaPlayer.create(this, resID);
        }
        metrsound = "tack";
        resID = getResources().getIdentifier(metrsound, "raw", packageName);
        if (resID != 0)
        {
            Log.d("load sound", "metronome :: " + metrsound);
            metronome[1] = MediaPlayer.create(this, resID);
        }

    }

    private void updateEx()
    {

        if (State == Constants.EXERCISE_PHASE_FINISH)
        {
            if(metroUpdate!=null)
                metroUpdate.cancel();

            TVPhase.setText(getString(R.string.exercize_done));
            TVCount.setText(getString(R.string.exercize_press_back));
            Intent resultIntent = new Intent();
            resultIntent.putExtra("duration", p_duration);
            resultIntent.putExtra("isInterrupted", isInterrupted);

            int[] cnt = new int[p_control_value.size()];
            for (int i = 0; i < p_control_value.size(); i++)
                cnt[i] = p_control_value.get(i);

            resultIntent.putExtra("control", cnt);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            return;
        } else if (State == Constants.EXERCISE_PHASE_PREPARATION_1)
        {

            if (p_duration == null)
                p_duration = getIntent().getIntArrayExtra("duration");
            if (p_pattern == null)
                p_pattern = getIntent().getIntArrayExtra("pattern");

            if (p_type == null)
            {
                p_type = getIntent().getIntArrayExtra("type");
                lCycle = getIntent().getIntExtra("lcycle", 4);
                if (p_duration != null)
                {
                    ImageView img = (ImageView) findViewById(R.id.image_progress_bar);
                    TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
                    Resources res = getResources();

                    map.put(Constants.EXERCISE_PHASE_PREPARATION_1, res.getColor(R.color.blank));
                    map.put(Constants.EXERCISE_PHASE_PREPARATION_2, res.getColor(R.color.blank));
                    map.put(Constants.EXERCISE_PHASE_PREPARATION_3, res.getColor(R.color.blank));
                    map.put(Constants.EXERCISE_PHASE_PREPARATION_4, res.getColor(R.color.blank));
                    map.put(Constants.EXERCISE_PHASE_FINISH, res.getColor(R.color.blank));

                    map.put(Constants.EXERCISE_PHASE_INHALE, res.getColor(R.color.inhale));
                    map.put(Constants.EXERCISE_PHASE_EXHALE, res.getColor(R.color.exhale));
                    map.put(Constants.EXERCISE_PHASE_PAUSE, res.getColor(R.color.pause));
                    map.put(Constants.EXERCISE_PHASE_HOLD, res.getColor(R.color.hold));

                    Display display = getWindowManager().getDefaultDisplay();
                    int width = display.getWidth();

                    progress_bar = new ProgressBar(img, width, 20, map, p_duration, p_type);
                }
            }


            if (p_control_parameters == null)
            {
                p_control_parameters = getIntent().getIntArrayExtra("control");
            }

            mode = getIntent().getStringExtra("mode");

            TVCount.setText("");
            TVPhase.setText(getString(R.string.exercize_preparation_1));
        }

        if (CurrentPhase >= p_duration.length)
        {
            State = Constants.EXERCISE_PHASE_FINISH;
            return;
        }


        if (p_duration[CurrentPhase] <= CurrentDuration)
        {
            TVPhase.startAnimation(animFadein);
            switch (State)
            {
                case Constants.EXERCISE_PHASE_PREPARATION_1:
                    State = Constants.EXERCISE_PHASE_PREPARATION_2;
                    TVCount.setText("");
                    TVPhase.setText(getString(R.string.exercize_preparation_2));
                    break;

                case Constants.EXERCISE_PHASE_PREPARATION_2:
                    State = Constants.EXERCISE_PHASE_PREPARATION_3;
                    TVCount.setText("");
                    TVPhase.setText(getString(R.string.exercize_preparation_3));
                    break;
                case Constants.EXERCISE_PHASE_PREPARATION_3:
                    State = Constants.EXERCISE_PHASE_PREPARATION_4;
                    TVCount.setText("");
                    TVPhase.setText(getString(R.string.exercize_preparation_4));
                    break;
                case Constants.EXERCISE_PHASE_PREPARATION_4:
                    State = Constants.EXERCISE_PHASE_INHALE;
                    TVCount.startAnimation(animFadein);
                    TVCount.setText(String.format(getString(R.string.exercize_remains), (p_duration.length - CurrentPhase) / lCycle));
                    if (isSoundEnable && mPlayer[0] != null)
                        mPlayer[0].start();

                    if(isMetronomeEnabled && metroUpdate!=null)
                        metroUpdate.schedule(mtask, 0, 500);

                    TVPhase.setText(getString(R.string.exercize_inhale));
                    break;
                default:
                    updateExercisePhase();
                    break;
            }

            long pnow = System.currentTimeMillis();
            long pelapsed = (pnow - phaseTime);

            corrected_time += p_duration[CurrentPhase];
            if (p_duration[CurrentPhase] != 0)
            {
                p_duration[CurrentPhase] = (int) pelapsed;
                phaseTime = pnow;
            }

            CurrentPhase++;
            CurrentDuration = 0;

            if (CurrentPhase < p_duration.length && p_duration[CurrentPhase] != 0)
                drawer.UpdateState(CurrentPhase, p_type, p_duration);
        }

        long now = System.currentTimeMillis();
        long elapsedTime = (now - prevTime);
        prevTime = now;
        CurrentDuration += elapsedTime;

        if (progress_bar != null)
        {
            progress_bar.drawProgress(corrected_time + CurrentDuration);
            Log.d("ProgressBar", "Update bar: " + (corrected_time + CurrentDuration));
        }

        if (CurrentPhase < p_duration.length && p_duration[CurrentPhase] == 0)
            updateEx();

    }

    private void updateExercisePhase()
    {
        if (isSoundEnable)
        {
            for (int i = 0; i < 4; i++)
                if (mPlayer[i] != null)
                {
                    if (mPlayer[i].isPlaying())
                        mPlayer[i].pause();
                    mPlayer[i].seekTo(0);
                }
        }

        if (CurrentPhase + 1 < p_type.length)
            State = p_type[CurrentPhase + 1];

        if ((CurrentPhase - 3) % lCycle == 0)
        {
            TVCount.startAnimation(animFadein);
            TVCount.setText(String.format(getString(R.string.exercize_remains), (p_duration.length - CurrentPhase) / lCycle));
            if (mode.equals("control"))
                ControlStage();
        }

        if (State == Constants.EXERCISE_PHASE_INHALE)
        {
            TVPhase.setText(getString(R.string.exercize_inhale));
            if (isSoundEnable && mPlayer[0] != null)
                mPlayer[0].start();

        } else if (State == Constants.EXERCISE_PHASE_EXHALE)
        {
            TVPhase.setText(getString(R.string.exercize_exhale));
            if (isSoundEnable && mPlayer[2] != null)
                mPlayer[2].start();

        } else if (State == Constants.EXERCISE_PHASE_HOLD && CurrentPhase + 1 < p_duration.length && p_duration[CurrentPhase + 1] != 0)
        {
            TVPhase.setText(getString(R.string.exercize_hold));
            if (isSoundEnable && mPlayer[3] != null)
                mPlayer[3].start();

        } else if (State == Constants.EXERCISE_PHASE_PAUSE && CurrentPhase + 1 < p_duration.length && p_duration[CurrentPhase + 1] != 0)
        {
            TVPhase.setText(getString(R.string.exercize_pause));
            if (isSoundEnable && mPlayer[1] != null)
                mPlayer[1].start();

        }
    }

    private void ControlStage()
    {
        return;
//        if (CurrentPhase + 1 >= p_control_parameters.length)
//            return;
//
//        int[] prevCycle = new int[lCycle];
//
//        for (int i = 0; i < lCycle; i++)
//            prevCycle[i] = p_duration[CurrentPhase - lCycle + i + 1];
//
//        int[] res = hrControl.constructPhase(p_control_parameters[CurrentPhase + 1], prevCycle);
//
//        for (int i = 0; i < lCycle; i++)
//            p_duration[CurrentPhase + 1 + i] = res[i];
    }

    @Override
    public void onAnimationEnd(Animation animation)
    {

    }

    @Override
    public void onAnimationRepeat(Animation animation)
    {
        // Animation is repeating
    }

    @Override
    public void onAnimationStart(Animation animation)
    {
        // Animation started
    }

}
