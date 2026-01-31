package com.lanius.kenoma.pob.visualizations;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;

import java.util.TreeMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class visCircles extends ExRender
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
                    "\tfloat PI = 3.14159;\n" +
                    "    float lines = 2.0 + Progress*0.1;\n" +
                    "    float gradLength = (1.0 / lines);\n" +
                    "    \n" +
                    "    vec2 uv = gl_FragCoord.xy;\n" +
                    "    vec2 center = iResolution.xy * 0.5;\n" +
                    "    vec2 delta = uv - center;\n" +
                    "    delta.x = abs(delta.x);\n" +
                    "    float len = length(delta);\n" +
                    "    float gradStep = floor(len * 0.006 * lines) / lines;\n" +
                    "    float gradSmooth = len * 0.005;\n" +
                    "    float gradCenter = gradStep + (gradLength * 0.55);\n" +
                    "    float percentFromCenter = abs(gradSmooth - gradCenter) / (gradLength * 0.5);\n" +
                    "    float interpLength = 0.02 * lines;\n" +
                    "    \n" +
                    "    float s = 1.0 - smoothstep(0.5 - interpLength, 0.5 + interpLength, percentFromCenter);\n" +
                    "    float index = gradStep / gradLength;\n" +
                    "    vec4 color = vec4(\n" +
                    "        sin(index*0.4), \n" +
                    "        sin(index*0.4), \n" +
                    "        sin(index*0.4), 1)*0.5;\n" +
                    "    \n" +
                    "    float angle = atan(delta.x, delta.y);\n" +
                    "    float worldAngle = Progress * PI * 1.0;\n" +
                    "    if(angle < worldAngle)\n" +
                    "    {\n" +
                    "        vec2 tip = vec2(sin(worldAngle), cos(worldAngle)) * gradCenter * 200.0;\n" +
                    "        float tipDist = length(delta - tip);\n" +
                    "        float rad = 50.0 / lines;\n" +
                    "        float tipC = 1.0 - smoothstep(rad - 1.0, rad + 1.0, tipDist);\n" +
                    "        gl_FragColor = vec4(tipC+vColor.x,tipC+vColor.y,tipC+vColor.z,1);\n" +
                    "    }else\n" +
                    "    {\n" +
                    "    \tgl_FragColor = vec4(s+vColor.x,s+vColor.y,s+vColor.z,1);\n" +
                    "    }\n" +
                    "    gl_FragColor += color;\n" +
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

    public visCircles(TreeMap<Integer, float[]> phaseColors)
    {
        super(phaseColors);

        colors = phaseColors;
    }


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
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
            if (p_duration[current_phase] != 0)
            {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                long now = System.currentTimeMillis();
                FrameCounter += (float) (now - prevTime) / 1000.0;
                shDrawer.draw(mMVPMatrix, getColor(), resolution, progress, FrameCounter);
            }
            if(Constants.D)
                Log.d("visCircle", "Phase:" + p_type[current_phase] + " Progress:" + progress);
        } else
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
