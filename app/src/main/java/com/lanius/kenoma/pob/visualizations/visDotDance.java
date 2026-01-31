package com.lanius.kenoma.pob.visualizations;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.TreeMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class visDotDance extends ExRender
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
                    "uniform vec2 iResolution;" +
                    "uniform vec4 vColor;" +
                    "uniform float Progress;" +
                    "uniform float Frame;" +
                    "vec2 rot(vec2 uv,float a)" +
                    "{" +
                    "return vec2(uv.x*cos(a)-uv.y*sin(a),uv.y*cos(a)+uv.x*sin(a));" +
                    "}" +
                    "" +
                    "void main(void){" +
                    "" +
                    "const int maxIterations=7;\n" +
                    "    float SIDES =3.14159265358979323846264*Progress;\n" +
                    "    float circleSize= (1.7-0.5*Progress) / (3.0*pow(2.0,float(maxIterations)));\n" +
                    "    vec2 uv=iResolution.xy;\n" +
                    "    uv=-1.0*(uv-2.0*gl_FragCoord.xy)/uv.x;\n" +
                    "    uv=rot(uv,SIDES);\n" +
                    "    uv*=sin(SIDES)*0.7+1.9;\n" +
                    "    float s= 1.0-0.95*Progress;\n" +
                    "    for(int i=0;i<maxIterations;i++)\n" +
                    "    {\n" +
                    "    \tuv=abs(uv)-s;\n" +
                    "    \tuv=rot(uv,SIDES);\n" +
                    "    s=s/(2.0);" +
                    "    }\n" +
                    "    float c=length(uv)>circleSize?0.0:1.0;\n" +
                    "    gl_FragColor = vColor+vec4(c,c,c,1.0);" +
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

    public visDotDance(TreeMap<Integer, float[]> phaseColors)
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
        int phase = p_type[current_phase];
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

