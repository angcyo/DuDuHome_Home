package com.dudu.weather.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dudu.android.launcher.R;
import com.dudu.android.launcher.utils.WeatherUtils;
import com.dudu.weather.model.WeatherItem;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/6/29.
 */
public class WeatherListAdapter extends RecyclerView.Adapter<WeatherListAdapter.ViewHolder> {

    private ArrayList<WeatherItem> listData;
    private int width;

    public WeatherListAdapter(int width, ArrayList<WeatherItem> listData) {
        this.listData = listData;
        this.width = width;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.list_weather_item, null);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.weekTxt.setWidth(width / 7);
        if (position == 3) {
            holder.weekTxt.setText("今天");
        } else {
            holder.weekTxt.setText(listData.get(position).getWeek());
        }
        holder.typeIcon.setImageResource(WeatherUtils
                .getWeatherIcon(WeatherUtils.getWeatherType(listData.get(position).getType())));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public void setViewWidth(int width) {
        this.width = width;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView weekTxt;
        public ImageView typeIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            weekTxt = (TextView) itemView.findViewById(R.id.txt_week);
            typeIcon = (ImageView) itemView.findViewById(R.id.icon_weather_type);
        }
    }
}
