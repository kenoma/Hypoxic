package com.lanius.kenoma.pob.Classes;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class HRV
{
    static int N, m;

    static double[] COS;
    static double[] SIN;

    static
    {
        N = 1024;
        m = (int) (Math.log(N) / Math.log(2));

        if (N != (1 << m))
            throw new RuntimeException("N must be power of 2");

        COS = new double[N / 2];
        SIN = new double[N / 2];

        for (int i = 0; i < N / 2; i++)
        {
            COS[i] = Math.cos(-2.0 * Math.PI * i / N);
            SIN[i] = Math.sin(-2.0 * Math.PI * i / N);
        }
    }

    static public int[] filterRR(int[] RR)
    {
        ArrayList<Integer> list = new ArrayList<>();
        int count = 0;
        double alpha = 0.8;

        double aRR = average(RR);
        double sumRR = 0;
        for (int i = 1; i < RR.length - 1; i++)
        {
            if (ExerciseLog.abs(RR[i - 1] + RR[i + 1] - RR[i]) < 100 || (RR[i] - aRR) > 400)
            {
                int delta = RR[i - 1] - RR[i + 1];
                int e_1 = RR[i] / 2;
                int e_2 = e_1;
                e_1 -= delta / 4;
                e_2 += delta / 4;
                aRR = alpha * aRR + (1.0 - alpha) * e_1;
                sumRR += e_1;
                list.add(e_1);

                aRR = alpha * aRR + (1.0 - alpha) * e_2;
                sumRR += e_2;
                list.add(e_2);

            } else if (RR[i] < 1500 && RR[i] > 200)
            {
                aRR = alpha * aRR + (1.0 - alpha) * RR[i];
                sumRR += RR[i];
                list.add(RR[i]);
            }
        }

        int[] res = new int[list.size()];
        for (int i = 0; i < res.length; i++)
            res[i] = list.get(i);
        return res;
    }

    static public double average(int[] RR)
    {
        double retval = 0.0;
        for (int i = 0; i < RR.length; i++)
            retval += RR[i];
        return retval / RR.length;
    }

    public static HashMap<String, Double> analyse(int[] RR, int[] control, int[] p_duration, int[] p_types)
    {
        double RDNN = 0.0;
        double SDNN = 0.0;
        double p50 = 0.0;
        double rMSSD = 0.0;


        for (int i = 0; i < RR.length - 1; i++)
        {
            RDNN += RR[i];
            rMSSD += Math.pow(RR[i] - RR[i + 1], 2.0);
            //if (control != null && control.length != 0)
            //    CQ += Math.abs((1.0 - control[Math.min(control.length * i / RR.length, control.length - 1)] / (double) RR[i]));
        }
        RDNN /= RR.length - 1.0;
        rMSSD = Math.sqrt(rMSSD / (RR.length - 3.0));
        //CQ = 100.0 * (CQ / (RR.length - 1));

        for (int i = 0; i < RR.length - 1; i++)
        {
            SDNN += Math.pow(RR[i] - RDNN, 2.0);
            if (Math.abs(RR[i] - RR[i + 1]) >= 50)
                p50++;
        }

        SDNN = Math.sqrt(SDNN / (RR.length - 2.0));
        p50 = 100.0 * p50 / (RR.length - 1.0);

        //LFHF = fLFHF(RR, 512);
        HashMap<String, Double> map = new HashMap<>();
        if (RR != null && RR.length != 0)
        {
            map.put("length", (double) RR.length);
            map.put("RDNN", RDNN);
            map.put("SDANN", SDNN);
            map.put("p50", p50);
            map.put("RMSSD", rMSSD);
            if (p_duration != null && p_types != null)
                map.put("CQ", IndependenceAnalysis.getSynchronizationRate(RR, p_duration, p_types));

            if (RR.length > 0)
            {
                int[] tmp = RR.clone();
                Arrays.sort(tmp);
                map.put("perNN", (double) 60000.0 / tmp[99 * tmp.length / 100]);
            } else
                map.put("perNN", RDNN - 2.0 * SDNN);
        } else
        {
            map.put("length", 0.0);
            map.put("RDNN", Double.MIN_VALUE);
            map.put("SDANN", Double.MIN_VALUE);
            map.put("p50", Double.MIN_VALUE);
            map.put("RMSSD", Double.MIN_VALUE);
            map.put("CQ", Double.MIN_VALUE);
            map.put("perNN", Double.MIN_VALUE);
            map.put("perNN", Double.MIN_VALUE);
        }
        int stange = 0;
        int hench = 0;
        if (p_duration != null && p_types != null)
        {
            for (int i = 0; i < p_duration.length; i++)
            {
                if (p_types[i] == Constants.EXERCISE_PHASE_PAUSE)
                    stange = Math.max(stange, p_duration[i]);
                if (p_types[i] == Constants.EXERCISE_PHASE_HOLD)
                    hench = Math.max(hench, p_duration[i]);
            }
        }
        if (stange != 0)
            map.put("stange", (double) (stange / 1000));
        else
            map.put("stange", Double.MIN_VALUE);
        if (hench != 0)
            map.put("hench", (double) (hench / 1000));
        else
            map.put("hench", Double.MIN_VALUE);
        return map;
    }

    static private double fLFHF(int[] x, int n)
    {
        if (x.length == 0)
            return 0.0;

        double sum = 0.0;
        for (double aRR : x)
            sum += aRR;
        double Fs = sum / (n - 1);

        double[] arr = new double[n];
        double[] y = new double[n];
        double t = 0.0;
        sum = x[0];
        int cj = 0;
        for (int i = 0; i < n; i++)
        {
            arr[i] = x[cj] + (x[cj + 1] - x[cj]) * (t - sum + x[cj]) / (x[cj]);
            t += Fs;
            while (t > sum && cj < x.length - 2)
                sum += x[cj++];
        }

        fft(arr, y);

        double HF = 0.0;
        double LF = 0.0;

        for (int i = 0; i < n; i++)
        {
            double hz = (double) i * (1000.0 / Fs) / n;
            if (hz > 0.15 && hz < 0.4)
                HF += Math.sqrt(arr[i] * arr[i] + y[i] * y[i]);
            if (hz > 0.04 && hz < 0.15)
                LF += Math.sqrt(arr[i] * arr[i] + y[i] * y[i]);
        }

        return LF / HF;
    }

    static public Pair<Double, Double> getAverStd(ArrayList<Double> arr)
    {
        double average = 0.0;
        for (Double db : arr)
            average += db;
        average /= (double) arr.size();

        double std = 0.0;
        for (Double db : arr)
            std += (db - average) * (db - average);

        std = Math.sqrt(std / (double) (arr.size() - 1.0));

        return new Pair<>(average, std);
    }

    static private void fft(double[] x, double[] y)
    {
        int ci, cj, ck, par1, par2, a;
        double c, s, t1, t2;

        cj = 0;
        par2 = N / 2;
        for (ci = 1; ci < N - 1; ci++)
        {
            par1 = par2;
            while (cj >= par1)
            {
                cj = cj - par1;
                par1 = par1 / 2;
            }
            cj = cj + par1;

            if (ci < cj)
            {
                t1 = x[ci];
                x[ci] = x[cj];
                x[cj] = t1;
                t1 = y[ci];
                y[ci] = y[cj];
                y[cj] = t1;
            }
        }

        par2 = 1;
        par1 = 0;

        for (ci = 0; ci < m; ci++)
        {
            par1 = par2;
            par2 = par2 + par2;
            a = 0;

            for (cj = 0; cj < par1; cj++)
            {
                s = SIN[a];
                c = COS[a];
                a += 1 << (m - ci - 1);

                for (ck = cj; ck < N; ck = ck + par2)
                {
                    t1 = c * x[ck + par1] - s * y[ck + par1];
                    t2 = s * x[ck + par1] + c * y[ck + par1];
                    x[ck + par1] = x[ck] - t1;
                    y[ck + par1] = y[ck] - t2;
                    x[ck] = x[ck] + t1;
                    y[ck] = y[ck] + t2;
                }
            }
        }
    }
}





