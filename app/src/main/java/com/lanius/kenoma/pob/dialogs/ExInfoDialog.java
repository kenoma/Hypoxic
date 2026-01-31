package com.lanius.kenoma.pob.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.Exercise;
import com.lanius.kenoma.pob.PretrainActivity;
import com.lanius.kenoma.pob.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

/**
 * Created by Kenoma on 22.08.2014.
 */

public class ExInfoDialog extends DialogFragment
{

    DialogListener mListener;
    Exercise ex = null;
    private ArrayList<String> phaseInExValues = new ArrayList<String>();
    private ArrayList<String> phaseHoldValues = new ArrayList<String>();
    private ArrayList<String> cycleLimit = new ArrayList<String>();
    /**
     * 0- Inhale
     * 1 - Pause 1
     * 2 - Exhale
     * 3 - Pause 2
     * 4 - Cycles
     */
    private WheelView[] wheels = new WheelView[5];
    private TextView[] unwheel = new TextView[5];

    public ExInfoDialog()
    {
        // Empty constructor required for DialogFragment
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        try
        {
            mListener = (DialogListener) activity;
        } catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_pretrain_description, container);
        getDialog().setTitle(getString(R.string.pretrain_description_title));

        Bundle args = getArguments();

        int id = args.getInt("id", 0);
        int Cycles = args.getInt("cycles", 10);
        ArrayList<Exercise> exercises = Exercise.LoadExList(getActivity().getSharedPreferences(Constants.PROG_PREF + "_EX", 0), getResources().getXml(R.xml.exercises));
        ArrayList<Exercise> add_ex = PretrainActivity.loadAdditionalExercises(getActivity());
        if(add_ex!=null)
            exercises.addAll(add_ex);

        for (int i = 0; i < exercises.size(); i++)
            if (exercises.get(i).id == id)
            {
                ex = exercises.get(i);
                ex.Cycles = Cycles;

                TextView tv = (TextView) view.findViewById(R.id.desciption_ex_name);
                tv.setText(ex.name);
                tv = (TextView) view.findViewById(R.id.desciption_ex_description);
                tv.setText(ex.detailedDescription);
                break;
            }

        if (ex != null)
        {
            InitPickers(view);
            updateInterfacePickers();
        }
        return view;
    }

    @Override
    public void onDismiss(final DialogInterface dialog)
    {
        super.onDismiss(dialog);
        Bundle resultIntent = getArguments();
        resultIntent.putInt("id", ex.id);
        if (ex.enableTuning)
        {
            resultIntent.putIntArray("duration", ex.events.get(0));
            resultIntent.putInt("cycles", ex.Cycles);
        }

        mListener.onDialogPositiveClick(ExInfoDialog.this);
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (getDialog() == null)
        {
            return;
        }
//        WindowManager wm = (WindowManager) getDialog().getContext().getSystemService(Context.WINDOW_SERVICE);
//        Display display = wm.getDefaultDisplay();
//
//        int dialogWidth = display.getWidth(); // specify a value here
//        int dialogHeight = display.getHeight(); // specify a value here
//
//        getDialog().getWindow().setLayout(dialogWidth, dialogHeight);

    }

    @Override
    public void onResume()
    {
        super.onResume();

        Window window = getDialog().getWindow();
       //LinearLayout container = (LinearLayout)getDialog().findViewById(R.id.container);

        window.setGravity(Gravity.CENTER);
        WindowManager wm = (WindowManager) getDialog().getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int dialogWidth = display.getWidth(); // specify a value here
        int dialogHeight = display.getHeight(); // specify a value here


        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;


        window.setAttributes(lp);//.setLayout(dialogWidth, dialogHeight);
    }

    private void MakeTextRepresentation(Exercise ex)
    {
        //int[] Durations =ex.events.get(0);
        String capts = "";
        NumberFormat formatter = new DecimalFormat("#0.0");
        int last_key = 0;
        for (int k : ex.events.keySet())
            last_key = k;

        for (int wphase = 0; wphase < 4; wphase++)
        {
            capts = "";
            int dur;
            for (int k : ex.events.keySet())
                if (k >= 0)
                {
                    dur = 0;
                    for (int j = 0; j < ex.events.get(k).length; j++)
                    {
                        if (ex.pattern[j] == Constants.EXERCISE_PHASE_INHALE && wphase == 0)
                            dur += ex.events.get(k)[j];
                        if (ex.pattern[j] == Constants.EXERCISE_PHASE_PAUSE && wphase == 1)
                            dur += ex.events.get(k)[j];
                        if (ex.pattern[j] == Constants.EXERCISE_PHASE_EXHALE && wphase == 2)
                            dur += ex.events.get(k)[j];
                        if (ex.pattern[j] == Constants.EXERCISE_PHASE_HOLD && wphase == 3)
                            dur += ex.events.get(k)[j];
                    }
                    if (dur != 0)
                        capts += String.format("\n%s", formatter.format(dur / 1000.0));
                    else
                        capts += String.format("\nX");
                }
            unwheel[wphase].setText(capts.trim());
        }

        if (ex.events.size() == 1)
        {
            capts = String.format("\n%s - %s", getString(R.string.alternative_text_start), ex.Cycles);
        } else
        {
            int prev_key = 0;
            for (int k : ex.events.keySet())
                if (k >= 0 && k != prev_key)
                {
                    if (prev_key == 0)
                        capts = String.format("\n%s - %s", getString(R.string.alternative_text_start), k);
                    else if (k != last_key)
                        capts += String.format("\n%s - %s", prev_key, k);
                    else
                    {
                        capts += String.format("\n%s - %s", prev_key, k);
                        capts += String.format("\n%s - %s", k, ex.Cycles);
                    }
                    prev_key = k;
                }
        }
        unwheel[4].setText(capts.trim());
    }

    private void proceedUserChangePattern(int changedPhase, int newValue)
    {
        if (ex != null)
        {
            if (ex.enableTuning)
            {
                int[] Durations = ex.events.get(0);

                for (int i = 0; i < 4; i++)
                {
                    ArrayList<String> arr = (i == 0 || i == 2) ? phaseInExValues : phaseHoldValues;
                    String tmp = arr.get(changedPhase == i ? newValue : wheels[i].getCurrentItem()).replace(",", ".");
                    int duration = 0;
                    if (!tmp.equals("X"))
                        duration = (int) (1000.0 * Double.parseDouble(tmp));
                    Durations[i] = duration;
                }

                ex.events.put(0, Durations);
            } else
            {
                unwheel[4].setText(getString(R.string.alternative_text_start));

            }
        }
    }

    private void updateInterfacePickers()
    {
        NumberFormat formatter = new DecimalFormat("#0.0");

        for (int i = 0; i < 4; i++)
        {
            ArrayList<String> arr = (i == 0 || i == 2) ? phaseInExValues : phaseHoldValues;

            int ind = 0;
            int[] durations = ex.events.get(0);
            if (durations[i] != 0)
                ind = arr.indexOf(formatter.format(durations[i] / 1000.0));
            wheels[i].setCurrentItem(ind);
            wheels[i].setEnabled(ex.enableTuning);
        }

        for (int i = 0; i < 5; i++)
        {
            if (ex.enableTuning)
            {
                wheels[i].setVisibility(View.VISIBLE);
                unwheel[i].setVisibility(View.GONE);
            } else
            {
                MakeTextRepresentation(ex);
                wheels[i].setVisibility(View.GONE);
                unwheel[i].setVisibility(View.VISIBLE);

            }
        }

        int cind = cycleLimit.indexOf(String.valueOf(ex.Cycles));
        wheels[4].setCurrentItem(cind);


    }

    private void InitPickers(View view)
    {
        NumberFormat formatter = new DecimalFormat("#0.0");

        String[] tempor = new String[]
                {"5", "8", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100", "110", "120", "130", "140", "150", "160", "170", "180", "190", "200", "250", "300", "350", "400"};
        for (int i = 0; i < tempor.length; i++)
            cycleLimit.add(tempor[i]);


        if (!ex.mode.equals("hypoxic"))
        {
            for (double i = 0.5; i < 1.0; i += 0.1)
                phaseInExValues.add(formatter.format(i));
            for (double i = 1.5; i < 20.0; i += 0.5)
                phaseInExValues.add(formatter.format(i));
            for (double i = 21; i < 60; i += 1.0)
                phaseInExValues.add(formatter.format(i));
            for (double i = 65; i < 300; i += 5.0)
                phaseInExValues.add(formatter.format(i));

            phaseHoldValues.add("X");
            phaseHoldValues.addAll(phaseInExValues);
        } else
        {
            for (double i = 10.0; i < 300.0; i += 5)
                phaseHoldValues.add(formatter.format(i));
            phaseInExValues.addAll(phaseHoldValues);

            RelativeLayout pane = (RelativeLayout) view.findViewById(R.id.pane_inhale);
            pane.setVisibility(View.GONE);
            pane = (RelativeLayout) view.findViewById(R.id.pane_pause);
            pane.setVisibility(View.GONE);
            pane = (RelativeLayout) view.findViewById(R.id.pane_exhale);
            pane.setVisibility(View.GONE);
            pane = (RelativeLayout) view.findViewById(R.id.pane_cycles);
            pane.setVisibility(View.GONE);

            TextView tv = (TextView) view.findViewById(R.id.col_capt_hold_2);
            tv.setText(R.string.exercize_hypoxic_hold);
            tv.setTextColor(getResources().getColor(R.color.hypoxic_hold));

        }

        WheelView phase = (WheelView) view.findViewById(R.id.picker_phase_inhale);
        wheels[0] = phase;
        TextView tphase = (TextView) view.findViewById(R.id.text_phase_inhale);
        unwheel[0] = tphase;

        ArrayWheelAdapter<String> timePicks = new ArrayWheelAdapter<String>(getActivity(), (String[]) phaseInExValues.toArray(new String[0]));

        timePicks.setItemResource(R.layout.wheel_units);
        timePicks.setItemTextResource(R.id.text);
        timePicks.setEmptyItemResource(R.layout.wheel_units);
        phase.setViewAdapter(timePicks);
        phase.setVisibleItems(5); // Number of items
        phase.setWheelBackground(R.drawable.wheel_bg);
        phase.setWheelForeground(R.drawable.wheel_val_red);
        phase.setShadowColor(0x00000000, 0x00000000, 0x00000000);
        phase.addChangingListener(new OnWheelChangedListener()
        {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue)
            {
                int changedValue = 0;
                updatePickers(newValue, changedValue);
            }
        });

        phase = (WheelView) view.findViewById(R.id.picker_phase_pause_1);
        wheels[1] = phase;
        tphase = (TextView) view.findViewById(R.id.text_phase_pause);
        unwheel[1] = tphase;
        timePicks = new ArrayWheelAdapter<String>(getActivity(), (String[]) phaseHoldValues.toArray(new String[0]));
        timePicks.setItemResource(R.layout.wheel_units);
        timePicks.setItemTextResource(R.id.text);
        timePicks.setEmptyItemResource(R.layout.wheel_units);
        phase.setViewAdapter(timePicks);
        phase.setVisibleItems(5); // Number of items
        phase.setWheelBackground(R.drawable.wheel_bg);
        phase.setWheelForeground(R.drawable.wheel_val_gray);
        phase.setShadowColor(0x00000000, 0x00000000, 0x00000000);
        phase.addChangingListener(new OnWheelChangedListener()
        {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue)
            {
                updatePickers(newValue, 1);
            }
        });

        phase = (WheelView) view.findViewById(R.id.picker_phase_exhale);
        wheels[2] = phase;
        tphase = (TextView) view.findViewById(R.id.text_phase_exhale);
        unwheel[2] = tphase;
        timePicks = new ArrayWheelAdapter<String>(getActivity(), (String[]) phaseInExValues.toArray(new String[0]));
        timePicks.setItemResource(R.layout.wheel_units);
        timePicks.setItemTextResource(R.id.text);
        timePicks.setEmptyItemResource(R.layout.wheel_units);
        phase.setViewAdapter(timePicks);
        phase.setVisibleItems(5); // Number of items
        phase.setWheelBackground(R.drawable.wheel_bg);
        phase.setWheelForeground(R.drawable.wheel_val_blue);
        phase.setShadowColor(0x00000000, 0x00000000, 0x00000000);
        phase.addChangingListener(new OnWheelChangedListener()
        {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue)
            {
                updatePickers(newValue, 2);
            }
        });

        phase = (WheelView) view.findViewById(R.id.picker_phase_pause_2);
        wheels[3] = phase;
        tphase = (TextView) view.findViewById(R.id.text_phase_hold);
        unwheel[3] = tphase;
        timePicks = new ArrayWheelAdapter<String>(getActivity(), (String[]) phaseHoldValues.toArray(new String[0]));
        timePicks.setItemResource(R.layout.wheel_units);
        timePicks.setItemTextResource(R.id.text);
        timePicks.setEmptyItemResource(R.layout.wheel_units);
        phase.setViewAdapter(timePicks);
        phase.setVisibleItems(5); // Number of items
        phase.setWheelBackground(R.drawable.wheel_bg);
        phase.setWheelForeground(R.drawable.wheel_val_gray);
        phase.setShadowColor(0x00000000, 0x00000000, 0x00000000);
        phase.addChangingListener(new OnWheelChangedListener()
        {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue)
            {
                updatePickers(newValue, 3);
            }
        });

        phase = (WheelView) view.findViewById(R.id.picker_cycles);
        wheels[4] = phase;
        tphase = (TextView) view.findViewById(R.id.text_phase_cycles);
        unwheel[4] = tphase;
        timePicks = new ArrayWheelAdapter<String>(getActivity(), cycleLimit.toArray(new String[0]));
        timePicks.setItemResource(R.layout.wheel_units);
        timePicks.setItemTextResource(R.id.text);
        timePicks.setEmptyItemResource(R.layout.wheel_units);
        phase.setViewAdapter(timePicks);
        phase.setVisibleItems(5); // Number of items
        phase.setWheelBackground(R.drawable.wheel_bg);
        phase.setWheelForeground(R.drawable.wheel_val);
        phase.setShadowColor(0x00000000, 0x00000000, 0x00000000);
        phase.addChangingListener(new OnWheelChangedListener()
        {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue)
            {
                int Cycles = Integer.parseInt(cycleLimit.get(wheel.getCurrentItem()));
                ex.Cycles = Cycles;
                updateInterfacePickers();
            }
        });


    }

    private void updatePickers(int newValue, int changedValue)
    {
        updateInterfacePickers();

        if (ex.enableTuning)
        {
            int[] copy = ex.events.get(0).clone();
            proceedUserChangePattern(changedValue, newValue);

            SharedPreferences settings = getActivity().getSharedPreferences(Constants.PROG_PREF + "_EX", 0);
            SharedPreferences.Editor ed = settings.edit();
            StringBuilder builder = new StringBuilder();
            for (int i : ex.events.get(0))
                builder.append(i + ",");

            ed.putString("d_" + ex.id, builder.toString());
            ed.commit();

            updateInterfacePickers();
        }
    }

    public interface DialogListener
    {
        public void onDialogPositiveClick(DialogFragment dialog);
    }
}

