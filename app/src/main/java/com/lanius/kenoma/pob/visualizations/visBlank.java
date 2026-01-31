package com.lanius.kenoma.pob.visualizations;

import android.opengl.GLES10;
import android.opengl.GLES20;

import java.util.TreeMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class visBlank extends ExRender
{

//    private int phase_duration,current_phase;
//    private int[] p_type,p_duration;
//    private TreeMap<Integer, float[]> colors;
    public visBlank(TreeMap<Integer, float[]> phaseColors)
    {
        super(phaseColors);
//        colors = phaseColors;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h)
    {

    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES10.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        if(p_duration!=null && p_type!=null)
//        {
//            float[] col = getColor();
//            GLES20.glClearColor(col[0], col[1], col[2], 1.0f);
//            GLES10.glClearColor(col[0], col[1], col[2], 1.0f);
//        }
    }

    @Override
    public void setState(int CurrentPhase, int[] phaseTypes, int[] phaseDurations)
    {
//        current_phase = CurrentPhase;
//        p_type = phaseTypes;
//        p_duration = phaseDurations;
//        phase_duration = 0;
    }
//
//    private float[] getColor()
//    {
//        float p = 1.0f;
//        p = Math.min(1.0f, (float) phase_duration / (float) p_duration[current_phase]);
//
//        float[] c1 = colors.get(p_type[current_phase]);
//        float[] retval = c1.clone();
//
//        for (int i = current_phase + 1; i < p_type.length; i++)
//            if (p_duration[i] != 0)
//            {
//                c1 = colors.get(p_type[i]);
//                for (int j = 0; j < 4; j++)
//                    retval[j] = p * retval[j] + (1.0f - p) * c1[j];
//
//                break;
//            }
//
//        return retval;
//    }
}