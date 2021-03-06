package com.dudu.aios.ui.map.adapter;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dudu.aios.ui.map.observable.MapListItemObservable;
import com.dudu.android.launcher.R;
import com.dudu.android.launcher.databinding.AddressItemBinding;

import java.util.ArrayList;

/**
 * Created by Robi on 2016-03-18 15:22.
 */
public class AddressResultAdapter  extends RecyclerView.Adapter<AddressResultAdapter.MapListItemHolder> {

    private ArrayList<MapListItemObservable> mapListObservableArrayList;

    private MapListItemClickListener itemClickListener;

    public AddressResultAdapter(ArrayList<MapListItemObservable> mapListObservableArrayList, MapListItemClickListener itemClickListener) {
        this.mapListObservableArrayList = mapListObservableArrayList;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public MapListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_item, parent, false);

        return new MapListItemHolder(itemView, itemClickListener);
    }

    @Override
    public void onBindViewHolder(MapListItemHolder holder, int position) {
        holder.bind(mapListObservableArrayList.get(position));
    }

    @Override
    public int getItemCount() {
        return mapListObservableArrayList.size();
    }


    class MapListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private AddressItemBinding mapListItemLayoutBinding;
        private MapListItemClickListener itemClickListener;

        public MapListItemHolder(View itemView, MapListItemClickListener itemClickListener) {
            super(itemView);
            mapListItemLayoutBinding = DataBindingUtil.bind(itemView);
            this.itemClickListener = itemClickListener;
            itemView.setOnClickListener(this);
        }

        public void bind(@NonNull MapListItemObservable mapListItemObservable) {
            mapListItemLayoutBinding.setMapItem(mapListItemObservable);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(v, getPosition());
            }
        }
    }
}
