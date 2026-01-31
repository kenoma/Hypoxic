package com.lanius.kenoma.pob.Classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kenoma on 26.08.2014.
 */
public final class DataAnalysis
{
    public static double corr(int[] X, int[] Y)
    {
        if (X.length != Y.length)
            return 0.0;

        int x_aver = 0;
        int y_aver = 0;
        int len = X.length;
        for (int i = 0; i < len; i++)
        {
            x_aver += X[i];
            y_aver += Y[i];
        }

        double xaver = (double) x_aver / (double) len;
        double yaver = (double) y_aver / (double) len;

        double x_std = 0;
        double y_std = 0;

        double c = 0;
        for (int i = 0; i < len; i++)
        {
            c += (X[i] - xaver) * (Y[i] - yaver);
            x_std += (X[i] - xaver) * (X[i] - xaver);
            y_std += (Y[i] - yaver) * (Y[i] - yaver);
        }

        x_std /= len;
        y_std /= len;

        return c / (Math.sqrt(x_std * y_std) * len);
    }

    public static double average(List<Double> input)
    {
        double aver = 0.0;
        for (double db : input)
            aver += db;
        double len = input.size();
        if (len != 0.0)
            return aver / len;
        else
            return 0.0;
    }

    public static double std(List<Double> input, double aver)
    {
        double std = 0.0;
        for (double db : input)
            std += (db - aver) * (db - aver);

        double len = input.size();
        if (len != 0.0 && std != 0.0)
            return Math.sqrt(std / len);
        else
            return 0.0;
    }

    public static List<Double> smooth(List<Double> input, double alpha)
    {
        List<Double> retval = new ArrayList<>();
        double val = input.get(0);
        for (double nval : input)
        {
            val = alpha * nval + (1.0 - alpha) * val;
            retval.add(val);
        }
        return retval;
    }

    public static int autocorr_per(List<Double> X, int K)
    {
        double aver = average(X);
        double std = std(X, aver);
        double len = X.size();
        double cup = -1.0 / (len - 1) + 1.6449 / Math.sqrt(len);

        double[] retval = new double[K - 1];
        for (int k = 1; k < K; k++)
        {
            double autocor = 0.0;
            for (int t = k; t < len; t++)
                autocor += (X.get(t) - aver) * (X.get(t - k) - aver);
            retval[k - 1] = autocor / (len * std);
        }

        for (int k = 1; k < K - 2; k++)
            if (retval[k] > cup && retval[k] > retval[k - 1] && retval[k] > retval[k + 1])
                return k + 1;
        return -1;
    }

    public static List<Double> norm(List<Double> input)
    {
        double aver = average(input);
        double std = std(input, aver);
        List<Double> retval = new ArrayList<>();
        for (double val : input)
            retval.add((val - aver) / std);
        return retval;
    }


}
