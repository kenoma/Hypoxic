package com.lanius.kenoma.pob.visualizations;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.R;

import java.util.TreeMap;

public class ExerciseDraw extends GLSurfaceView
{
    private ExRender render;

    public ExerciseDraw(Context context)
    {
        super(context);
        initRender();
    }

    public ExerciseDraw(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        if (!this.isInEditMode())
            initRender();
    }

    private void initRender()
    {

        //if(supportsEs2)
        //    super.setEGLContextClientVersion(2);
        //else
        super.setEGLContextClientVersion(2);
        //
        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        TreeMap<Integer, float[]> map = new TreeMap<Integer, float[]>();
        Resources res = getContext().getResources();
        map.put(Constants.EXERCISE_PHASE_PREPARATION_1, colorToArr(res.getColor(R.color.blank)));
        map.put(Constants.EXERCISE_PHASE_PREPARATION_2, colorToArr(res.getColor(R.color.blank)));
        map.put(Constants.EXERCISE_PHASE_PREPARATION_3, colorToArr(res.getColor(R.color.blank)));
        map.put(Constants.EXERCISE_PHASE_PREPARATION_4, colorToArr(res.getColor(R.color.blank)));
        map.put(Constants.EXERCISE_PHASE_FINISH, colorToArr(res.getColor(R.color.blank)));

        map.put(Constants.EXERCISE_PHASE_INHALE, colorToArr(res.getColor(R.color.inhale)));
        map.put(Constants.EXERCISE_PHASE_EXHALE, colorToArr(res.getColor(R.color.exhale)));
        map.put(Constants.EXERCISE_PHASE_PAUSE, colorToArr(res.getColor(R.color.pause)));
        map.put(Constants.EXERCISE_PHASE_HOLD, colorToArr(res.getColor(R.color.hold)));

        Context cont = getContext();

        if (cont != null)
        {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(cont);
            String effect = sharedPrefs.getString("prefVisual", "Circles");
            switch (effect)
            {
                case "Circles":
                    render = new visCircles(map);
                    break;
                case "Serpinsky":
                    render = new visDotDance(map);
                    break;
                case "Flower":
                    render = new visFlower(map);
                    break;

                default:
                    render = new visBlank(map);
            }
            //this.setEGLConfigChooser(true);
            this.setRenderer(render);
        }else
            this.setRenderer(new visBlank(map));
    }

    public void UpdateState(int t, int[] phaseTypes, int[] phaseDurations)
    {
        render.setState( t, phaseTypes, phaseDurations);
    }

    @Override
    public void onResume()
    {
        // super.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private float[] colorToArr(int col)
    {
        float[] res = new float[]
                {
                        (0xFF & (col >> 16)) / 255.0f,
                        (0xFF & (col >> 8)) / 255.0f,
                        (0xFF & (col)) / 255.0f,
                        (0xFF & (col >> 24)) / 255.0f,
                };

        return res;
    }
}

