package com.lanius.kenoma.pob.visualizations;


import android.opengl.GLES10;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class ShaderDrawer
{
    int COORDS_PER_VERTEX = 3;
    float triangleCoords[] = {   // in counterclockwise order:
            -1.0f, 1.0f, 0.0f, // top
            -1.0f, -1.0f, 0.0f, // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, 1.0f, 0.0f,  // bottom right
    };
    short[] indices = new short[]{0, 1, 2, 0, 2, 3};
    private int mProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    public ShaderDrawer(String fragmentShader, String vertexShader)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(triangleCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

        int ivertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int ifragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, ivertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, ifragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);

    }

    public static int loadShader(int type, String shaderCode)
    {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void draw(float[] mvpMatrix, float[] mainColor, float[] resolution, float progress, float time)
    {
        GLES20.glUseProgram(mProgram);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        int mFrameHandle = GLES20.glGetUniformLocation(mProgram, "Frame");
        GLES20.glUniform1f(mFrameHandle, time);

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        if (mainColor != null && resolution != null)
        {
            int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(mColorHandle, 1, mainColor, 0);
            int mResolution = GLES20.glGetUniformLocation(mProgram, "iResolution");
            GLES20.glUniform2fv(mResolution, 1, resolution, 0);
            int mProgress = GLES20.glGetUniformLocation(mProgram, "Progress");
            float val = progress;// * 3.141592653589f / 2.0f;
            GLES20.glUniform1f(mProgress, val);

            int mFrame = GLES20.glGetUniformLocation(mProgram, "Frame");

            GLES20.glUniform1f(mFrame, time);
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}
