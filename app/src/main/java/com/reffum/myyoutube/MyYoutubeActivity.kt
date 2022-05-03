package com.reffum.myyoutube


import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.util.Log
import android.view.*
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.ArrayList

class MyYoutubeActivity : AppCompatActivity(), SearchRecycleViewAdapter.ItemClickListener {
    private val model : MyViewModel by viewModels()

    // Activity views
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var progressBar: ProgressBar
    private lateinit var mediaController: MediaController
    private lateinit var mediaPlayer: MediaPlayer
    private val searchRecycleViewAdapter: SearchRecycleViewAdapter = SearchRecycleViewAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_youtube_activity)

        initViews()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Get the SearchView and set searchable configuration
        val searchManager : SearchManager = getSystemService(Context.SEARCH_SERVICE)
                as SearchManager

        val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(componentName)
        )
        searchView.isIconifiedByDefault = false
        return true
    }

    /**
     * Handle click on video list in RecycleView
     * @param videoId Youtube ID
     */
    override fun onVideoItemClick(videoId: String?) {
        loadVideo(videoId!!)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if(intent == null)
            return

        // User initial search
        if(Intent.ACTION_SEARCH.equals(intent.action)) {
            lifecycleScope.launch {
                val searchString = intent.getStringExtra(SearchManager.QUERY)!!
                progressBar.visibility = View.VISIBLE
                val videoList : List<VideoData> = model.searchYoutubeVideo(searchString)
                searchRecycleViewAdapter.setVideoList(videoList as ArrayList<VideoData>?)
                progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Load youtube video by its id and start play it in MediaPlayer
     * @param videoId Youtube video id
     */
    private fun loadVideo(videoId : String) {
        val videoUrl = "http://www.youtube.com/watch?v=$videoId"

        lifecycleScope.launch {
            // Show progress bar while video download
            progressBar.visibility = View.VISIBLE

            //
            val directUrl: String = model.getYoutubeDirectVideoUrl(videoUrl)

            if(directUrl.isNotEmpty()) {
                setupVideoView(directUrl)
            } else {
                Toast.makeText(
                    applicationContext,
                    "Video load error",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Get stream url for $videoUrl error")
            }
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Play video by URL directUrl
     */
    private fun setupVideoView(directUrl: String) {
        // Create new MediaPlayer. Pass to it url and surface view
        // for video and play.
        mediaPlayer = MediaPlayer()
        mediaPlayer.setSurface(surfaceHolder.surface)
        mediaPlayer.setDataSource(directUrl)
        mediaPlayer.prepare()
        //TODO: adjust size of video surface
        mediaPlayer.start()
    }

    private fun initViews() {
        val videoListRecycleView = findViewById<RecyclerView>(R.id.video_list_recycler_view)!!
            .apply {
                layoutManager = LinearLayoutManager(this@MyYoutubeActivity)
                adapter = searchRecycleViewAdapter
            }

        surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        mediaController = MediaController(this)
        mediaController.setAnchorView(surfaceView)
    }

    companion object {
        private const val APPLICATION_NAME = "myyoutube"
        private const val TAG = "MyYoutube"
        private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
    }

}