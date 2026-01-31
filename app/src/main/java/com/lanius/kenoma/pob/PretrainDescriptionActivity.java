package com.lanius.kenoma.pob;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.Constants;
import com.lanius.kenoma.pob.Classes.Exercise;

import java.util.ArrayList;

public class PretrainDescriptionActivity extends Activity
{
    Exercise ex = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.activity_pretrain_description);
        //InitPickers();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        int id = getIntent().getIntExtra("id", 0);
        int Cycles = getIntent().getIntExtra("cycles", 10);
        ArrayList<Exercise> exercises = Exercise.LoadExList(getSharedPreferences(Constants.PROG_PREF + "_EX", 0), getResources().getXml(R.xml.exercises));
        ArrayList<Exercise> add_ex = PretrainActivity.loadAdditionalExercises(this);
        if(add_ex!=null)
            exercises.addAll(add_ex);

        for (int i = 0; i < exercises.size(); i++)
            if (exercises.get(i).id == id)
            {
                ex = exercises.get(i);
                ex.Cycles = Cycles;
                TextView tv = (TextView) findViewById(R.id.desciption_ex_name);
                tv.setText(ex.name);
                tv = (TextView) findViewById(R.id.desciption_ex_description);
                tv.setText(ex.detailedDescription);
                //updateInterfacePickers();
                break;
            }
    }

    @Override
    public void onBackPressed()
    {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("id", ex.id);
        if (ex.enableTuning)
        {
            resultIntent.putExtra("duration", ex.events.get(0));
            resultIntent.putExtra("cycles", ex.Cycles);
        }
        setResult(Activity.RESULT_OK, resultIntent);
        super.onBackPressed();

    }



}
