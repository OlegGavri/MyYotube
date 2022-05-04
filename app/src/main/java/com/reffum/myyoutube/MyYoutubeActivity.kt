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

//TODO: video controls

//TODO: improve description(size, shows)
//TODO: Restore last video
//TODO: background play and controls in notification
//TODO: Save video and it position
//TODO: save audio
//TODO: account
//TODO: Show chat
//TODO: likes, shared
//TODO: only audio mode

//TODO: fail if videoID referenced to unaviable stream

class MyYoutubeActivity : AppCompatActivity(),
    SearchRecycleViewAdapter.ItemClickListener,
    SurfaceHolder.Callback,
    MediaController.MediaPlayerControl {

    private val model : MyViewModel by viewModels()

    // Activity views
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var progressBar: ProgressBar
    private lateinit var videoListRecycleView : RecyclerView
    private lateinit var mediaController: MediaController
    private var mediaPlayer: MediaPlayer? = null
    private val searchRecycleViewAdapter: SearchRecycleViewAdapter = SearchRecycleViewAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_youtube_activity)

        initViews()

        Log.d(TAG, "onCreate()")
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
     * SurfaceHolder.Callback implementation. It is for video SurfaceView
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        // If MediaPlayer created, set holder for it
        mediaPlayer?.setSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mediaController.show()
        return false
    }

    /**
     * Implementation of MediaController.MediaPlayerControl
     */
    override fun start() {
        mediaPlayer?.start()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }

    override fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    override fun seekTo(pos: Int) {
        mediaPlayer?.seekTo(pos)
    }

    override fun isPlaying(): Boolean {
        Log.d(TAG, "isPlaying(): ${mediaPlayer?.isPlaying}")
        return mediaPlayer?.isPlaying ?: false
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        Log.d(TAG, "canPause() return $mediaPlayer")
        return mediaPlayer != null
    }

    override fun canSeekBackward(): Boolean {
        return mediaPlayer != null
    }

    override fun canSeekForward(): Boolean {
        return mediaPlayer != null
    }

    override fun getAudioSessionId(): Int {
        TODO("Not yet implemented")
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

        // If MediaPlayer already play video, reset it
        if(mediaPlayer == null)
            mediaPlayer = MediaPlayer()
        else
            mediaPlayer?.reset()

        mediaPlayer!!.setSurface(surfaceHolder.surface)
        mediaPlayer!!.setDataSource(directUrl)
        mediaPlayer!!.prepare()

        // Adjust surfaceView size to video size
        var videoWidth = mediaPlayer!!.videoWidth.toFloat()
        var videoHeight = mediaPlayer!!.videoHeight.toFloat()
        var screenWidth = windowManager.defaultDisplay.width

        var lp  = surfaceView.layoutParams.also {
            it.width = screenWidth
            it.height = (videoHeight / videoWidth * screenWidth).toInt()
        }

        surfaceView.layoutParams = lp
        mediaPlayer!!.start()
    }

    private fun initViews() {
        videoListRecycleView = findViewById<RecyclerView>(R.id.video_list_recycler_view)!!
            .apply {
                layoutManager = LinearLayoutManager(this@MyYoutubeActivity)
                adapter = searchRecycleViewAdapter
            }

        surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        mediaController = MediaController(this)
        mediaController.setMediaPlayer(this)
        mediaController.setAnchorView(surfaceView)
    }

    companion object {
        private const val APPLICATION_NAME = "myyoutube"
        private const val TAG = "MyYoutube"
        private const val API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU"
    }


}