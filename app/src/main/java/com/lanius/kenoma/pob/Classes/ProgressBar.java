package com.lanius.kenoma.pob.Classes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;

import java.util.TreeMap;

/**
 * Created by Kenoma on 12.09.2014.
 */
public class ProgressBar
{

    public float totsum;
    public int width;
    public int height;
    TreeMap<Integer, Integer> color_map;
    private Canvas canvas;
    private Paint paint;
    private Bitmap bitmap;
    ImageView image;

    public ProgressBar(ImageView img, int w, int h, TreeMap<Integer, Integer> colors, int[] p_durations, int[] p_type)
    {
        image = img;
        color_map = colors;
        width = w;
        height = h;
        bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        for (int d : p_durations)
            totsum += d;

        float local_position = 0;
        for (int i = 0; i < p_durations.length; i++)
            if (p_durations[i] != 0)
            {
                paint.setColor(color_map.get(p_type[i]));
                float leftx = (local_position / totsum) * width;

                float topy = 0;
                float rightx = ((local_position + p_durations[i]) / totsum) * width;
                local_position += p_durations[i];
                canvas.drawRect(leftx, topy, rightx, height, paint);
            }

        img.setImageBitmap(bitmap);
    }

    public void drawProgress(long elapsedtime)
    {
        paint.setColor(Color.WHITE);
        float leftx = 0;

        float topy = 5;
        float rightx = (Math.min(1.0f, elapsedtime / totsum)) * width;
        canvas.drawRect(leftx, topy, rightx, height - 5, paint);
        image.postInvalidate();
    }


}
