package com.reffum.myyoutube.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import java.io.IOException
import java.net.URL
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Methods for exchange data with Youtube.
 */
object Youtube {

    /* Exception types */
    class ResponseException(msg : String) : Exception() {

    }

    // Google Youtube API
    private val mYouTube : YouTube = getYoutubeService()

    /**
     * Perform search string in Youtube. Return search result in
     * list of VideoData
     */
    fun searchYoutubeVideo(searchString : String) : List<VideoData> {
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
            val date = Date(snippet.publishedAt.value)
            val imageUrl : String = thumbnails.default.url

            // Load image
            val url = URL(imageUrl)
            val bitmap : Bitmap = BitmapFactory
                .decodeStream(url.openConnection().getInputStream())

            val videoData = VideoData(
                title,
                0,
                formatDate(date),
                videoId,
                bitmap
            )

            videoList.add(videoData)
        }

        // Add view counts value for each video
        val ids = List(videoList.size) {
            videoList[it].id
        }

        val views = getViews(ids)

        for(i in videoList.indices) {
            videoList[i].views = views[i]
        }

        return videoList
    }

    // Return views count for given video
    @Throws(ResponseException::class)
    private fun getViews(ids : List<String>): List<Int> {
        val request = mYouTube.videos().list(listOf("statistics"))
        val response = request.setId(ids).execute()

        if(response.items.size != ids.size) {
            throw ResponseException("Response size is not equal request size")
        }

        return List(ids.size) {
            response.items[it].statistics.viewCount.toInt()
        }
    }


    @Throws(IOException::class, GeneralSecurityException::class)
    private fun getYoutubeService() : YouTube {
        val builder = YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory()
        ) { request : HttpRequest ->
            val url = request.url
            url.set("key", API_KEY)
        }.setApplicationName(Constants.APPLICATION_NAME)

        return builder.build()
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm").format(date)
    }

    private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
}