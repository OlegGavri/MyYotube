package com.reffum.myyoutube.view

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reffum.myyoutube.MediaPlaybackService
import com.reffum.myyoutube.R
import com.reffum.myyoutube.model.SearchList
import com.reffum.myyoutube.model.VideoData
import com.reffum.myyoutube.viewmodel.MainActivityViewModel
import com.reffum.myyoutube.viewmodel.SearchListAdapter
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(),
    SearchListAdapter.ItemClickedListener,
    View.OnClickListener
{
    companion object {
        private const val TAG = "MainActivity"
    }

    private val model : MainActivityViewModel by viewModels()

    // Activity views
    private lateinit var surfaceView: SurfaceView
    private lateinit var videoTitle : TextView
    private lateinit var accountButton : Button
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var progressBar: ProgressBar
    private lateinit var videoListRecycleView : RecyclerView
    private lateinit var mediaControllerWidget : MediaController
    private val searchRecycleViewAdapter: SearchListAdapter =
        SearchListAdapter(this)

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
    override fun onVideoItemClick(videoData: VideoData) {
        videoTitle.text = videoData.title
        SearchList.current = videoData
        loadVideo(videoData.id)
    }

    /**
     * Handle only search intent. Toolbar search create it.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if(intent == null)
            return

        // User initial search
        if(Intent.ACTION_SEARCH == intent.action) {
            lifecycleScope.launch {
                val searchString = intent.getStringExtra(SearchManager.QUERY)!!
                progressBar.visibility = View.VISIBLE
                model.searchYoutubeVideo(searchString)
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.accounts_button -> {
                Log.d(TAG, "onClick called")
                requestOauth2()
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

        // Observer for SearchList data
        val searchListObserver = Observer<List<VideoData>> {
            searchRecycleViewAdapter.notifyDataSetChanged()
        }

        SearchList.list.observe(this, searchListObserver)

        surfaceView = findViewById(R.id.surface_view)
        videoTitle = findViewById(R.id.video_title)
        surfaceHolder = surfaceView.holder
        progressBar = findViewById(R.id.progress_bar)
        mediaControllerWidget = MediaController(this)
        mediaControllerWidget.setAnchorView(surfaceView)

        accountButton = findViewById(R.id.accounts_button)
        accountButton.setOnClickListener(this)
    }

    private fun requestGoogleAccount(): Account {
        val am = AccountManager.get(this)
        val accounts = am.getAccountsByType("com.android.email")

        // System accounts list can't be empty. It must have at least 1
        assert(accounts.isNotEmpty())

        return accounts[0]
    }

    private fun requestOauth2() {
        val googleAccount = requestGoogleAccount()

        Log.d(TAG, "Google account: ${googleAccount.name}")

        val am : AccountManager = AccountManager.get(this)
        val options = Bundle()

        Log.d(TAG, "Request OAuth2")
        val result = am.getAuthToken(
            googleAccount,
            "Manage your task",
            options,
            this,

            {result : AccountManagerFuture<Bundle> ->
                Log.d(TAG, "Account success")
//                val bundle = result.result
//
//                // The token is a named value in the bundle. The name of the value
//                // is stored in the constant AccountManager.KEY_AUTHTOKEN.
//                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)

//                Log.d(TAG, "TOKEN = $token")
            },

            Handler {
                Log.d(TAG, "Account error")
                false
            }
        )

        Log.d(TAG, "Account done: ${result.isDone}")
        Log.d(TAG, "Account canceled: ${result.isCancelled}")

    }
}
