package com.reffum.myyoutube

import android.util.Log
import androidx.lifecycle.ViewModel
import com.reffum.myyoutube.model.SearchList
import com.reffum.myyoutube.model.VideoData
import com.reffum.myyoutube.model.Youtube
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivityViewModel : ViewModel() {

    companion object {
        private const val TAG = "MyViewModel"
        private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
        private const val APPLICATION_NAME = "myyoutube"
    }

    var mediaService : MediaPlaybackService? = null

    // Perform search string in Youtube. Return search result in
    // list of VideoData
    suspend fun searchYoutubeVideo(searchString : String) : List<VideoData> {
        val videoList : List<VideoData>
        withContext(Dispatchers.IO) {
            videoList = Youtube.searchYoutubeVideo(searchString)
        }
        SearchList.list = videoList
        return videoList
    }

    @Throws(IOException::class)
    suspend fun getYoutubeDirectVideoUrl(videoId : String) : String {
        val videoUrl = "http://www.youtube.com/watch?v=$videoId"

        val request : YoutubeDLRequest = YoutubeDLRequest(videoUrl)
        request.addOption("-f", "best")
        val videoInfo : VideoInfo

        withContext(Dispatchers.IO) {
            videoInfo = YoutubeDL.getInstance().getInfo(request)
        }

        SearchList.current!!.directUrl = videoInfo.url

        return videoInfo.url
    }

    /**
     * Play video by URL
     * @param directUrl
     */
    fun playVideo(directUrl: String) {
        Log.d(TAG, "playVideo($directUrl)")
        assert(directUrl.isNotEmpty())
        mediaService?.playUrl(directUrl)
    }
}
