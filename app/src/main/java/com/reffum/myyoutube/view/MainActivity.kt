package com.reffum.myyoutube.view


import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
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
    SearchRecycleViewAdapter.ItemClickListener,
    MediaController.MediaPlayerControl
{
    companion object {
        private const val TAG = "MyYoutube"
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

    private lateinit var mediaBrowser : MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private var playbackTransportControls: MediaControllerCompat.TransportControls? = null
    private var connectionCallbacks : MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                mediaBrowser.sessionToken.also { token ->
                    val mediaController = MediaControllerCompat(
                        this@MainActivity,
                        token
                    )

                    MediaControllerCompat.setMediaController(
                        this@MainActivity,
                        mediaController
                    )
                }

                mediaController = MediaControllerCompat(
                    this@MainActivity,
                    mediaBrowser.sessionToken
                )

                mediaController.registerCallback(controllerCallback)
                playbackTransportControls = mediaController.transportControls

                setSurfaceHolderInMediaService()
            }

            override fun onConnectionSuspended() {
                // Service was crashed. Disable transport controls
                Log.d(TAG, "onConnectionSuspended() the service was crashed.")
                playbackTransportControls = null
            }

            override fun onConnectionFailed() {
                Log.d(TAG, "onConnectionFailed: the service hasn't been able to connect")
            }
        }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onSessionDestroyed() {
            mediaBrowser.disconnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        initViews()

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            null
        )

        Log.d(TAG, "onCreate()")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart(). Connect media browser")
        mediaBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop(). Disconnect media browser")
        MediaControllerCompat.getMediaController(this).unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
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
     * @param videoId Youtube ID
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
                SearchList.list = videoList
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
            SearchList.current!!.directUrl = directUrl

            if(directUrl.isNotEmpty()) {
                playVideo(directUrl)
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

    /**
     * Play video by URL
     * @param directUrl
     */
    private fun playVideo(directUrl: String) {
        Log.d(TAG, "playVideo($directUrl)")

        assert(directUrl.isNotEmpty())

        var bundle = Bundle()

        playbackTransportControls?.play()
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
        mediaControllerWidget.setMediaPlayer(this)
        mediaControllerWidget.setAnchorView(surfaceView)
    }

    /**
     * Implementation of MediaController.MediaPlayerControl
     */
    override fun start() {
        playbackTransportControls?.play()
    }

    override fun pause() {
        Log.d(TAG, "pause()")
    }

    override fun getDuration(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        return 0
    }

    override fun seekTo(pos: Int) {
    }

    override fun isPlaying(): Boolean {
        //Log.d(TAG, "isPlaying(): ${mediaPlayer?.isPlaying}")
        return false
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return false
    }

    override fun canSeekBackward(): Boolean {
        return false
    }

    override fun canSeekForward(): Boolean {
        return false
    }

    override fun getAudioSessionId(): Int {
        TODO("Not yet implemented")
    }

    private fun setSurfaceHolderInMediaService() {
        val bundle = Bundle()
        mediaController.sendCommand()
    }
}