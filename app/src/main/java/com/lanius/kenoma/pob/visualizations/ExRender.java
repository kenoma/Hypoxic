package com.lanius.kenoma.pob.visualizations;


import android.graphics.Color;
import android.opengl.GLSurfaceView;

import java.util.TreeMap;

public abstract class ExRender implements GLSurfaceView.Renderer
{
    public ExRender(TreeMap<Integer,float[]> phaseColors)
    {

    }

    public void setState( int CurrentPhase, int[] phaseTypes, int[] phaseDurations)
    {
    }
}
