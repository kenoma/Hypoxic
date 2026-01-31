package com.lanius.kenoma.pob.dialogs;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lanius.kenoma.pob.Classes.StatVar;
import com.lanius.kenoma.pob.R;

import java.util.ArrayList;

public class InfoDialog extends DialogFragment
{
    public InfoDialog()
    {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_info, container);

        getDialog().setTitle(getString(R.string.info_variables));
        String data = "";
        ArrayList<StatVar> vars = StatVar.load(getResources());
        for (StatVar var : vars)
            data += String.format(getString(R.string.variables_description), var.human_readable, var.description);

        TextView tv = (TextView) view.findViewById(R.id.description);
        tv.setText(Html.fromHtml(data));

        return view;
    }
}

