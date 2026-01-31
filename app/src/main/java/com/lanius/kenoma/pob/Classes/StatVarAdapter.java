package com.lanius.kenoma.pob.Classes;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lanius.kenoma.pob.R;

import java.util.ArrayList;

public class StatVarAdapter extends ArrayAdapter<StatVar>
{
    private Context context;
    private ArrayList<StatVar> values;


    public StatVarAdapter(Context context, ArrayList<StatVar> statvar)
    {
        super(context, R.layout.statvar, statvar);
        this.context = context;
        Resources res = context.getResources();
        this.values = statvar;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.statvar, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.ex_name);
        textView.setText(values.get(position).human_readable);

        return rowView;
    }

}
