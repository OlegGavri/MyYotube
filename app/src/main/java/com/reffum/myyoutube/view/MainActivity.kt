package com.reffum.myyoutube.view

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reffum.myyoutube.MediaPlaybackService
import com.reffum.myyoutube.MyViewModel
import com.reffum.myyoutube.R
import com.reffum.myyoutube.VideoData
import com.reffum.myyoutube.model.SearchList
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(),
    SearchRecycleViewAdapter.ItemClickListener
{
    companion object {
        private const val TAG = "MainActivity"
    }

    private val model : MyViewModel by viewModels()

    // Activity views
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var progressBar: ProgressBar
    private lateinit var videoListRecycleView : RecyclerView
    private lateinit var mediaControllerWidget : MediaController
    private val searchRecycleViewAdapter: SearchRecycleViewAdapter =
        SearchRecycleViewAdapter(this)

    // Our connection to the Media Service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if(service is MediaPlaybackService.MediaServiceBinder) {
                model.mediaService = service.getService()
            }
            model.mediaService!!.setSurfaceHolder(surfaceView)
            mediaControllerWidget.setMediaPlayer(
                model.mediaService!!.getMediaControllerCallback()
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected called. Service crashed.")
            model.mediaService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        initViews()

        // Start media service
        startForegroundService(Intent(this, MediaPlaybackService::class.java))
        bindService(
            Intent(this, MediaPlaybackService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        Log.d(TAG, "onCreate()")
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mediaControllerWidget.show()
        return false
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
     * @param videoData
     */
    override fun onVideoItemClick(videoData: VideoData?) {
        SearchList.current = videoData
        loadVideo(videoData!!.id)
    }

    /**
     * Handle only search intent. Toolbar search create it.
     */
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
        lifecycleScope.launch {
            // Show progress bar while video download
            progressBar.visibility = View.VISIBLE

            val directUrl: String = model.getYoutubeDirectVideoUrl(videoId)

            if(directUrl.isNotEmpty()) {
                model.playVideo(directUrl)
            } else {
                Toast.makeText(
                    applicationContext,
                    "Video load error",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Get stream url for $videoId error")
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun initViews() {
        videoListRecycleView = findViewById<RecyclerView>(R.id.video_list_recycler_view)!!
            .apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = searchRecycleViewAdapter
            }

        surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        mediaControllerWidget = MediaController(this)
        mediaControllerWidget.setAnchorView(surfaceView)
    }
}