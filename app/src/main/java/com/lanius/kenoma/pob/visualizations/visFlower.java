package com.lanius.kenoma.pob.visualizations;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;

import java.util.TreeMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class visFlower extends ExRender
{
    private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "uniform float Frame;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    " gl_Position = vPosition;" +
                    "}";


    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform float Frame;" +
                    "uniform vec2 iResolution;" +
                    "uniform vec4 vColor;" +
                    "uniform float Progress;" +
                    "void main(void)" +
                    "{" +
                    "" +
                    "    float SIDES = (0.9*Progress - 0.07 - sin(Frame)/10.0 )* 3.14159265358979323846264 * 1.8;\n" +
                    "    vec2 para = (2.0*gl_FragCoord.xy-iResolution.xy)/min(0.9*iResolution.y,0.9*iResolution.x);\n" +
                    "    float a = atan(para.x,para.y);\n" +
                    "    float r = length(para)*(1.2+0.2*sin(0.3*SIDES));\n" +
                    "    float w = cos(0.5*SIDES+-r*2.0);\n" +
                    "    float radi = 0.5 + 0.5*cos((7.0)*a-w*1.0+r*(4.0*SIDES+1.0)+ 0.9*SIDES);\n" +
                    "    float diam = (0.02 +SIDES *0.08) +  0.75*pow(radi,(1.0-SIDES*0.2)*r)*(0.8+0.2*w);\n" +
                    "    float f = sqrt(1.0-r/diam)*r*2.5;\n" +
                    "    f *= 1.124235+0.15*cos(((11.0+SIDES)*a-w*7.0+r*8.0)/2.0);\n" +
                    "    f *= 1.0 - 0.55*(0.5+0.5*sin(r*30.0))*(0.5+0.5*cos(16.0*a-w*(3.0+6.0*SIDES)+r*8.0));\n" +
                    "    vec3 col = vec3( f,\n" +
                    "    f-radi*0.1+r*0.1 + 0.15*radi*(0.5-r),\n" +
                    "    f-radi*r + 0.1*radi );\n" +
                    "    col = clamp( col, 0.0, 1.0 );\n" +
                    "    vec3 bcol = vec3(vColor.x*0.2, vColor.y*0.2, vColor.z*0.2);\n" +
                    "    col = mix(vec3( vColor) + col, bcol, smoothstep(0.1,0.3,r-diam) );\n" +
                    "    gl_FragColor = vec4( col, 1.0 );" +
                    "" +
                    "}";

    ShaderDrawer shDrawer;
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private long prevTime;
    private TreeMap<Integer, float[]> colors;
    private float[] resolution = new float[2];
    private float FrameCounter;

    private int[] p_type;
    private int[] p_duration;
    private int current_phase;
    private long phase_duration;

    public visFlower(TreeMap<Integer, float[]> phaseColors)
    {
        super(phaseColors);
        colors = phaseColors;
    }


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        shDrawer = new ShaderDrawer(fragmentShaderCode, vertexShaderCode);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h)
    {
        resolution[0] = w;
        resolution[1] = h;
        GLES20.glViewport(0, 0, w, h);
        float ratio = (float) w / h;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        if (p_type != null && p_duration != null)
        {
            float progress = getProgress();
            if(p_duration[current_phase]!=0)
            {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                long now = System.currentTimeMillis();
                FrameCounter += (float) (now - prevTime) / 1000.0;
                shDrawer.draw(mMVPMatrix, getColor(), resolution, progress, FrameCounter);
            }
            if(Constants.D)
                Log.d("visDotDance", "Phase:" + p_type[current_phase] + " Progress:" + progress);
        }else
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void setState(int CurrentPhase, int[] phaseTypes, int[] phaseDurations)
    {
        current_phase = CurrentPhase;
        p_type = phaseTypes;
        p_duration = phaseDurations;
        phase_duration = 0;
    }

    private float getProgress()
    {
        long now = System.currentTimeMillis();
        long elapsedTime = (now - prevTime);
        phase_duration += elapsedTime;
        prevTime = now;

        int phase = p_type[current_phase];

        if (p_duration[current_phase] != 0)
        {
            if (phase == Constants.EXERCISE_PHASE_INHALE)
                return Math.min(1.0f, (float) phase_duration / (float) p_duration[current_phase]);
            else if (phase == Constants.EXERCISE_PHASE_EXHALE)
                return 1.0f - Math.min(1.0f, (float) phase_duration / (float) p_duration[current_phase]);
            else if (phase == Constants.EXERCISE_PHASE_PAUSE)
                return 1.0f;
            else if (phase == Constants.EXERCISE_PHASE_HOLD)
                return 0.0f;
        } else
            return 0.0f;
        return 0.0f;
    }

    private float[] getColor()
    {
        float p = 1.0f;
        //int phase = p_type[current_phase];
        //if (phase == Constants.EXERCISE_PHASE_PAUSE || phase == Constants.EXERCISE_PHASE_HOLD)
        p = Math.min(1.0f, (float) phase_duration / (float) p_duration[current_phase]);

        float[] c1 = colors.get(p_type[current_phase]);
        float[] retval = c1.clone();

        for (int i = current_phase + 1; i < p_type.length; i++)
            if (p_duration[i] != 0)
            {
                c1 = colors.get(p_type[i]);
                for (int j = 0; j < 4; j++)
                    retval[j] = p * retval[j] + (1.0f - p) * c1[j];

                break;
            }

        return retval;
    }
}