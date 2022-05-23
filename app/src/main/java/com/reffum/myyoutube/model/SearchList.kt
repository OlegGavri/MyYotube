package com.reffum.myyoutube.model

import androidx.lifecycle.MutableLiveData

object SearchList {
    var list = MutableLiveData<List<VideoData>>()
    var current : VideoData? = null
}