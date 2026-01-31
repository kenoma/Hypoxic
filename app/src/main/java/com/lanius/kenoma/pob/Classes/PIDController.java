package com.lanius.kenoma.pob.Classes;

import android.util.Log;

public class PIDController
{
    double k_p, k_i, k_d;
    private double e_i = 0.0;
    private double last_e = 0.0;
    private double Target = 0.0;
    private double CParameter = 0.0;

    public PIDController(double Target, double CParameter, double Kp, double Ki, double Kd)
    {
        this.Target = Target;
        k_p = Kp;
        k_i = Ki;
        k_d = Kd;
        this.CParameter = CParameter;
    }

    public void setTarget(double NewTarget)
    {
        Target = NewTarget;
    }

    public double E(double Current)
    {
        return Current - Target;
    }

    public double UpdatePar(double val)
    {
        double e = E(val);
        e_i += e;
        double e_d = e - last_e;
        last_e = e_d;
        double retval = CParameter + (k_p * e + k_p * k_i * e_i + k_p * k_d * e_d);
        if (retval <= 500)
            retval = 500;
        if (retval > 15000)
            retval = 15000;
        CParameter = retval;
        Log.d("PID", String.format("Return %s (%s), E:%s, I:%s, D: %s", retval, Target, e, e_i, e_d));
        return retval;
    }
}
