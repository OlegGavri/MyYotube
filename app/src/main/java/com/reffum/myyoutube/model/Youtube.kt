package com.reffum.myyoutube.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import java.io.IOException
import java.net.URL
import java.security.GeneralSecurityException

/**
 * Methods for exchange data with Youtube.
 */
object Youtube {

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
            val date : DateTime = snippet.publishedAt
            val imageUrl : String = thumbnails.default.url

            // Load image
            val url = URL(imageUrl)
            val bitmap : Bitmap = BitmapFactory
                .decodeStream(url.openConnection().getInputStream())

            val videoData = VideoData(
                title,
                "Unknown",
                date.toString(),
                videoId,
                bitmap
            )

            videoList.add(videoData)
        }
        return videoList
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

    private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
}