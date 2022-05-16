package com.reffum.myyoutube.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.security.GeneralSecurityException
import com.reffum.myyoutube.VideoData

// Contain last search list and methods for get data from Youtube
object YoutubeVideoList {
    private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
    private const val APPLICATION_NAME = "myyoutube"

    // Google Youtube API
    private val mYouTube : YouTube = getYoutubeService()

    // Perform search string in Youtube. Return search result in
    // list of VideoData
    suspend fun searchYoutubeVideo(searchString : String) : List<VideoData> =
        withContext(Dispatchers.IO) {
            val request :YouTube.Search.List = mYouTube.search().list(listOf("id,snippet"))
            val response : SearchListResponse = request.setQ(searchString)
                .setType(listOf("video"))
                .setMaxResults(50)
                .execute()

            val videoList : MutableList<VideoData> = mutableListOf()

            // Parse searched videos
            for(item in response.items) {
                // Get video id, title and image
                val id = item.id
                val snippet = item.snippet
                val thumbnails = snippet.thumbnails

                val videoId : String = id.videoId
                val title : String = snippet.title
                val date : DateTime = snippet.publishedAt
                val imageUrl : String = thumbnails.medium.url

                // Load image
                val url = URL(imageUrl)
                val bitmap : Bitmap = BitmapFactory
                    .decodeStream(url.openConnection().getInputStream())

                val videoData = VideoData(
                    title,
                    "Unknown",
                    date.toString(),
                    videoId,
                    bitmap )

                videoList.add(videoData)
            }

            videoList
        }

    @Throws(IOException::class)
    suspend fun getYoutubeDirectVideoUrl(url : String) : String {
        val request : YoutubeDLRequest = YoutubeDLRequest(url)
        request.addOption("-f", "best")
        val videoInfo : VideoInfo

        withContext(Dispatchers.IO) {videoInfo = YoutubeDL.getInstance().getInfo(request)}

        return videoInfo.url
    }


    @Throws(IOException::class, GeneralSecurityException::class)
    private fun getYoutubeService() : YouTube {
        val builder = YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory()
        ) { request : HttpRequest ->
            val url = request.url
            url.set("key", API_KEY)
        }.setApplicationName(APPLICATION_NAME)

        return builder.build()
    }
}