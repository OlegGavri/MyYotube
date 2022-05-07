package com.reffum.myyoutube

import androidx.lifecycle.ViewModel
import com.reffum.myyoutube.model.YoutubeVideoList

//TODO: rename to MainActivityViewModel
class MyViewModel : ViewModel() {

    // Perform search string in Youtube. Return search result in
    // list of VideoData
    suspend fun searchYoutubeVideo(searchString : String) : List<VideoData> {
        return YoutubeVideoList.searchYoutubeVideo(searchString)
    }
}
