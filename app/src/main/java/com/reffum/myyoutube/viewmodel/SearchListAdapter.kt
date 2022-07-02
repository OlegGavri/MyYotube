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
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

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
                itemViews.text = prettyCount(value?.views!!) + " views"
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

    /**
     * Return pretty count as 1k, 1.2M etc
     */
    private fun prettyCount(number : Int) : String {
        val suffix = listOf(' ', 'k', 'M', 'B', 'T', 'P', 'E')
        val base = floor(log10(number.toDouble())).toInt()
        val suffixNum = base / 3

        if(base >= 3 && suffixNum < suffix.size) {
            return DecimalFormat("#0.0")
                .format(number / 10f.pow(base.toInt())) + suffix[suffixNum]
        } else {
            return DecimalFormat("#,##0")
                .format(number)
        }
    }
}