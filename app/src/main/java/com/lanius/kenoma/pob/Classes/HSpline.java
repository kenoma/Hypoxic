package com.lanius.kenoma.pob.Classes;

import android.util.Pair;

import java.util.List;

public class HSpline
{
    private static double[] x;
    private static double[] m;
    private static double[] y;
    private static int max_capacity =0;
    private static int arr_len =0;


    public HSpline(List<Pair<Integer, Double>> frames)
    {
        int len = frames.size();
        x = new double[len];
        y = new double[len];
        for (int i = 0; i < y.length; i++)
        {
            x[i] = (double) frames.get(i).first;
            y[i] = (double) frames.get(i).second;
        }
        m = new double[len];
        m[0] = (y[1] - y[0]) / ((x[1] - x[0]));
        for (int i = 1; i < m.length - 1; i++)
            m[i] = (y[i + 1] - y[i]) / (2.0 * (x[i + 1] - x[i])) +
                    (y[i] - y[i - 1]) / (2.0 * (x[i] - x[i - 1]));
        m[m.length - 1] = (y[m.length - 1] - y[m.length - 1 - 1]) / ((x[m.length - 1] - x[m.length - 1 - 1]));
    }

    public HSpline(int max_cap)
    {
        x = new double[max_cap];
        y = new double[max_cap];
        m = new double[max_cap];
        max_capacity = max_cap;
    }

    public void RecomputeSpline(int[] X, int[] Y, int len)
    {
        if (len > max_capacity)
            return;

        arr_len = len;
        for (int i = 0; i < len; i++)
        {
            x[i] = (double) X[i];
            y[i] = (double) Y[i];
        }

        m[0] = (y[1] - y[0]) / ((x[1] - x[0]));
        for (int i = 1; i < len - 1; i++)
            m[i] = (y[i + 1] - y[i]) / (2.0 * (x[i + 1] - x[i])) +
                    (y[i] - y[i - 1]) / (2.0 * (x[i] - x[i - 1]));
        m[len - 1] = (y[len - 1] - y[len - 1 - 1]) / ((x[len - 1] - x[len - 1 - 1]));
    }


    public HSpline(int[] X, int[] Y)
    {
        x = new double[X.length];
        y = new double[Y.length];
        for (int i = 0; i < y.length; i++)
        {
            x[i] = (double) X[i];
            y[i] = (double) Y[i];
        }
        m = new double[X.length];
        m[0] = (y[1] - y[0]) / ((x[1] - x[0]));
        for (int i = 1; i < m.length - 1; i++)
            m[i] = (y[i + 1] - y[i]) / (2.0 * (x[i + 1] - x[i])) +
                    (y[i] - y[i - 1]) / (2.0 * (x[i] - x[i - 1]));
        m[m.length - 1] = (y[m.length - 1] - y[m.length - 1 - 1]) / ((x[m.length - 1] - x[m.length - 1 - 1]));
    }

    public HSpline(int[] RR)
    {
        if (RR == null || RR.length == 0)
            return;

        y = new double[RR.length];
        for (int i = 0; i < y.length; i++)
            y[i] = (double) RR[i];

        x = new double[RR.length];
        m = new double[RR.length];
        double sum = 0;
        for (int i = 0; i < y.length; i++)
        {
            sum += y[i];
            x[i] = sum;
        }

        m[0] = (y[1] - y[0]) / ((x[1] - x[0]));
        for (int i = 1; i < m.length - 1; i++)
            m[i] = (y[i + 1] - y[i]) / (2.0 * (x[i + 1] - x[i])) +
                    (y[i] - y[i - 1]) / (2.0 * (x[i] - x[i - 1]));

        m[m.length - 1] = (y[m.length - 1] - y[m.length - 1 - 1]) / ((x[m.length - 1] - x[m.length - 1 - 1]));
    }

    public HSpline(double[] RR)
    {
        if (RR == null || RR.length == 0)
            return;

        this.y = RR.clone();
        x = new double[RR.length];
        m = new double[RR.length];
        double sum = 0;
        for (int i = 0; i < y.length; i++)
        {
            sum += y[i];
            x[i] = sum;
        }

        m[0] = (y[1] - y[0]) / (x[1] - x[0]);
        for (int i = 1; i < m.length - 1; i++)
            m[i] = (y[i + 1] - y[i]) / (2.0 * (x[i + 1] - x[i])) +
                    (y[i] - y[i - 1]) / (2.0 * (x[i] - x[i - 1]));
        m[m.length - 1] = (y[m.length - 1] - y[m.length - 1 - 1]) / (x[m.length - 1] - x[m.length - 1 - 1]);
    }

    static public int findInterval(double[] x, double t)
    {
        if (x == null || x.length == 0)
            return -1;
        int p0 = x.length / 2;
        int p1 = x.length / 2 + 1;

        int L = x.length / 2;


        while (!(x[p0] <= t && x[p1] >= t))
        {
            L /= 2;
            L = L == 0 ? 1 : L;
            if (t < x[p0])
            {
                p0 = p0 - L;
                p1 = p0 + 1;

            } else if (t > x[p1])
            {
                p1 = p1 + L;
                p0 = p1 - 1;
            }

            if (p0 < 0 || p1 >= x.length)
                return -1;
        }

        return p0;
    }

    static public int sfindInterval(double t)
    {
        if (arr_len == 0)
            return -1;
        int p0 = arr_len / 2;
        int p1 = arr_len / 2 + 1;
        int L = arr_len / 2;

        while (!(x[p0] <= t && x[p1] >= t))
        {
            L /= 2;
            L = L == 0 ? 1 : L;
            if (t < x[p0])
            {
                p0 = p0 - L;
                p1 = p0 + 1;

            } else if (t > x[p1])
            {
                p1 = p1 + L;
                p0 = p1 - 1;
            }

            if (p0 < 0 || p1 >= arr_len)
                return -1;
        }

        return p0;
    }

    public double interpolate(double time)
    {
        int p0 = findInterval(x, time);
        if (p0 == -1)
            return Double.NaN;

        double t = (time - x[p0]) / (x[p0 + 1] - x[p0]);

        return (1.0 + 2.0 * t) * (1.0 - t) * (1.0 - t) * y[p0] +
                t * (1 - t) * (1 - t) * m[p0] +
                t * t * (3.0 - 2.0 * t) * y[p0 + 1] +
                t * t * (t - 1.0) * m[p0 + 1];
    }

    ////for use with RecomputeSpline
    public double special_interpolate(double time)
    {
        int p0 = sfindInterval(time);
        if (p0 == -1)
            return Double.NaN;

        double t = (time - x[p0]) / (x[p0 + 1] - x[p0]);

        return (1.0 + 2.0 * t) * (1.0 - t) * (1.0 - t) * y[p0] +
                t * (1 - t) * (1 - t) * m[p0] +
                t * t * (3.0 - 2.0 * t) * y[p0 + 1] +
                t * t * (t - 1.0) * m[p0 + 1];
    }
}
