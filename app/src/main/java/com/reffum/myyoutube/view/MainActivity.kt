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
import com.reffum.myyoutube.model.YoutubeVideoList
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

                buildTransportControls()
            }

            override fun onConnectionSuspended() {
                TODO("Not implement yet")
            }

            override fun onConnectionFailed() {
                TODO("Not implement yer")
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
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
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
    override fun onVideoItemClick(videoId: String?) {
        loadVideo(videoId!!)
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
        val videoUrl = "http://www.youtube.com/watch?v=$videoId"

        lifecycleScope.launch {
            // Show progress bar while video download
            progressBar.visibility = View.VISIBLE

            val directUrl: String = YoutubeVideoList.getYoutubeDirectVideoUrl(videoUrl)

            if(directUrl.isNotEmpty()) {
                playVideo(directUrl)
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
     * Play video by URL
     * @param directUrl
     */
    private fun playVideo(directUrl: String) {
        Log.d(TAG, "playVideo($directUrl)")

        assert(directUrl.isNotEmpty())

        //TODO: play video
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

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        mediaController.registerCallback(controllerCallback)
    }

    /**
     * Implementation of MediaController.MediaPlayerControl
     */
    override fun start() {

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



}