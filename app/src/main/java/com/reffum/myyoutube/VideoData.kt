package com.reffum.myyoutube

//TODO: move to model
import android.graphics.Bitmap

// YouTube video data
class VideoData(

        //Video title
        val title: String,

        // Number of views
        val views: String,

        //Release date
        val date : String,

        // YouTube video ID
        val id: String,

        // image, icon
        val image: Bitmap,
        )