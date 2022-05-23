package com.reffum.myyoutube.viewmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.reffum.myyoutube.R
import com.reffum.myyoutube.model.SearchList
import com.reffum.myyoutube.model.VideoData

class SearchListAdapter(itemClickListener : ItemClickedListener) :
    RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    interface ItemClickedListener {
        fun onVideoItemClick(videoData : VideoData)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Item views
        var itemTitle : TextView = itemView.findViewById(R.id.item_title)
        var itemViews : TextView = itemView.findViewById(R.id.item_views)
        var itemDate : TextView = itemView.findViewById(R.id.item_date)
        var itemImage : ImageView = itemView.findViewById(R.id.item_image)

        init {
            // Handle click on list element
            itemView.setOnClickListener {
                itemClickListener.onVideoItemClick(videoData!!)
            }
        }

        var videoData : VideoData? = null
            set(value) {
                field = value
                itemTitle.text = value?.title
                itemViews.text = "Unknown"
                itemImage.setImageBitmap(value?.image)
                itemDate.text = value?.date
            }
    }

    val itemClickListener = itemClickListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.videoData = SearchList.list.value?.get(position)
    }

    override fun getItemCount(): Int {
        return SearchList.list.value?.size ?: 0
    }
}