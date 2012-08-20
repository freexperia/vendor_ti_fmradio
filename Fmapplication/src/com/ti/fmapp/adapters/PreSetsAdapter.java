package com.ti.fmapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.ti.fmapp.R;
import com.ti.fmapp.logic.PreSetRadio;

import java.util.ArrayList;


public class PreSetsAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private ArrayList<PreSetRadio> preSetRadios = null;

    @SuppressWarnings("unchecked")
    public PreSetsAdapter(Context context, ArrayList<PreSetRadio> preSetRadios) {
        this.preSetRadios = (ArrayList<PreSetRadio>) preSetRadios.clone();
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return preSetRadios.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        AlarmViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.preset_elem, null, false);
            holder = new AlarmViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.tv_station_name);
            holder.frequency = (TextView) convertView.findViewById(R.id.tv_station_frequency);
            holder.addNewStation = (TextView) convertView.findViewById(R.id.tv_add_new_station);
            convertView.setTag(holder);
        } else {
            holder = (AlarmViewHolder) convertView.getTag();
        }

        if (preSetRadios.get(position).isStationSet()) {
            holder.name.setText(preSetRadios.get(position).getStationName());
            holder.name.setVisibility(View.VISIBLE);
            holder.frequency.setText(preSetRadios.get(position).getStationFrequency());
            holder.frequency.setVisibility(View.VISIBLE);
            holder.addNewStation.setVisibility(View.GONE);
        } else {
            //if station is not set present 'add station' display instead
            holder.addNewStation.setVisibility(View.VISIBLE);
            holder.name.setVisibility(View.INVISIBLE);
            holder.frequency.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    class AlarmViewHolder {
        TextView name;
        TextView frequency;
        TextView addNewStation;
    }

}
