package com.reffum.myyoutube;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SearchRecycleViewAdapter extends RecyclerView.Adapter {

    interface ItemClickListener {
        void onVideoItemClick(String videoId);
    }

    ItemClickListener clickListener;

    public SearchRecycleViewAdapter(ItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    ArrayList<VideoData> videoList = new ArrayList<VideoData>();

    public void setVideoList(ArrayList<VideoData> videoList) {
        this.videoList = videoList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemTitle;
        TextView itemViews;
        TextView itemDate;
        ImageView itemImage;

        String videoId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemTitle = itemView.findViewById(R.id.item_title);
            itemViews = itemView.findViewById(R.id.item_views);
            itemImage = itemView.findViewById(R.id.item_image);
            itemDate = itemView.findViewById(R.id.item_date);
            itemView.setOnClickListener(view -> clickListener.onVideoItemClick(videoId));
        }

        public void setVideoImage(Bitmap image) {
            itemImage.setImageBitmap(image);
        }
        public void setVideoTitle(String title) {
            itemTitle.setText(title);
        }
        public void setVideoDetail(String detail) {
            itemViews.setText(detail);}
        public void setVideoId(String id) {videoId = id;}
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.setVideoImage(videoList.get(position).getImage());
        viewHolder.setVideoTitle(videoList.get(position).getTitle());
        viewHolder.setVideoId(videoList.get(position).getId());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }
}
