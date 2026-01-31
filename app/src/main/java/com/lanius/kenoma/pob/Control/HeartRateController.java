package com.lanius.kenoma.pob.Control;

import android.util.Log;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.DataAnalysis;
import com.lanius.kenoma.pob.Classes.FIS;
import com.lanius.kenoma.pob.Classes.Mapping;

import java.util.HashMap;
import java.util.Random;

public class HeartRateController
{
    Random rand;
    int[] p_type;
    int[] p_duration;
    int[] RR;
    int[] p_pattern;
    private FIS fis;
    private double current_correlation = 0.0;
    private double[] aver_bphase;

    public HeartRateController()
    {
        double[][] mems = new double[][]{
                new double[]{-1.0, -0.8, -0.6, -0.4, -0.2, 0.0, 0.2, 0.4, 0.6, 0.8, 1.0} /// corr
        };
        HashMap<String, double[]> base = new HashMap<>();
        fis = new FIS(mems, base);
        rand = new Random();
    }

    public double getControlValue()
    {
        return current_correlation;
    }

    public void update(int[] p_type, int[] p_duration, int[] p_pattern, int[] RR, int CurrentPhase)
    {
        this.RR = RR;
        this.p_type = p_type;
        this.p_duration = p_duration;
        this.p_pattern = p_pattern;
    }

    private void updateCorr(int wlen)
    {
        if (RR != null && RR.length > wlen)
        {

            int[][] map = Mapping.regulation_ts(p_duration, RR, p_type);

            int reclen = map[0].length;
            if (reclen >= wlen)
            {
                int[] r = new int[wlen];
                int[] b = new int[wlen];

                aver_bphase = new double[4];
                double alpha = 0.8;
                for (int i = 0; i < wlen; i++)
                {
                    int shift = reclen - wlen + i - 1;
                    r[i] = map[0][shift];
                    b[i] = map[1][shift];
                    int index = -1;
                    if (map[3][shift] == Constants.EXERCISE_PHASE_INHALE)
                        index = 0;
                    else if (map[3][shift] == Constants.EXERCISE_PHASE_PAUSE)
                        index = 1;
                    else if (map[3][shift] == Constants.EXERCISE_PHASE_EXHALE)
                        index = 2;
                    else if (map[3][shift] == Constants.EXERCISE_PHASE_HOLD)
                        index = 3;
                    aver_bphase[index] = aver_bphase[index] == 0.0 ? map[2][shift] : (alpha * map[2][shift] + (1.0 - alpha) * aver_bphase[index]);
                }
                current_correlation = DataAnalysis.corr(r, b);
            }
        }
    }

    public int[] constructPhase(double targetParameter, int[] currentDurations)
    {
        int phases = currentDurations.length;
        updateCorr(20);
        double[] defVal = new double[phases];
        for (int i = 0; i < phases; i++)
            defVal[i] = currentDurations[i] + 500.0 * rand.nextDouble();

        double[] inp = new double[1];
        inp[0] = targetParameter;
        double[] _res = fis.inference(inp, defVal);
        if (aver_bphase != null)
            fis.train(new double[]{current_correlation}, aver_bphase.clone());
        int[] res = new int[_res.length];
        for (int i = 0; i < phases; i++)
            if (currentDurations[i] != 0)
            {
                if (i == 0 || i == 2)
                    res[i] = Math.min(Math.max((int) _res[i], 300), 10000);
                else
                    res[i] = Math.min(Math.max((int) _res[i], 0), 10000);
            }
        if (aver_bphase != null)
            Log.d("FIS", String.format("Target %s, Corr %.3f  PD [%.0f  %.0f %.0f %.0f]",
                    targetParameter,
                    current_correlation,
                    aver_bphase[0],
                    aver_bphase[1],
                    aver_bphase[2],
                    aver_bphase[3]
            ));
        Log.d("FIS", String.format("New [%s  %s %s %s]",
                res[0],
                res[1],
                res[2],
                res[3]
        ));

        return res;
    }

}
