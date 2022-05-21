package com.reffum.myyoutube

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager


class MediaPlaybackService : Service(),
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener
{
    companion object {
        private const val LOG_TAG = "MediaPlaybackService"
        private const val channelId = "com.reffum.myyoutube.NOTIFICATION_CHANNEL_ID"
    }

    inner class MediaServiceBinder : Binder() {
        fun getService() : MediaPlaybackService {
            return this@MediaPlaybackService
        }
    }

    private var isServiceStarted : Boolean = false
    private lateinit var mediaPlayer : MediaPlayer

    // SurfaceView in which output video. If null, video not playing.
    private var surfaceView : SurfaceView? = null

    fun playUrl(url : String) {
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepareAsync()
    }

    fun setSurfaceHolder(surfaceView : SurfaceView) {
//        mediaPlayer = MediaPlayer()
//        mediaPlayer.setSurface(surfaceHolder.surface)
//        mediaPlayer.setDataSource(directUrl)
//        mediaPlayer.prepare()
//
//        // Adjust surfaceView size to video size
//        var videoWidth = mediaPlayer.videoWidth.toFloat()
//        var videoHeight = mediaPlayer.videoHeight.toFloat()
//        var screenWidth = windowManager.defaultDisplay.width
//
//        var lp  = surfaceView.layoutParams.also {
//            it.width = screenWidth
//            it.height = (videoHeight / videoWidth * screenWidth).toInt()
//        }
//
//        surfaceView.layoutParams = lp
//        mediaPlayer.start()
        this.surfaceView = surfaceView
        mediaPlayer.setSurface(surfaceView.holder.surface)
    }

    fun resetSurfaceHolder() {
        mediaPlayer.setSurface(null)
    }

    override fun onCreate() {
        super.onCreate()
        initMediaPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return MediaServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand() entered")
        isServiceStarted = true
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            setVolume(1.0f, 1.0f)

            setOnErrorListener(this@MediaPlaybackService)
            setOnPreparedListener(this@MediaPlaybackService)
            setOnCompletionListener(this@MediaPlaybackService )
        }
    }

    /**
     * MediaPlayer callbacks
     */
    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "Song completed.")
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "onPrepared(). MediaPlayer ready to play")
        adjustSurfaceViewSize()
        mediaPlayer.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        when(what) {
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                Log.d(LOG_TAG, "MediaPlayer server died, extra = $extra. " +
                        "Instantiate a new one.")
                initMediaPlayer()
                return true
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                Log.d(LOG_TAG, "MediaPlayer error unknown. extra = $extra")
                return false
            }
        }
        // onError() description said that only this 2 what may be.
        assert(false)
        return false
    }

    private fun adjustSurfaceViewSize() {
        if (surfaceView != null) {
            val windowManager = (getSystemService(WINDOW_SERVICE) as WindowManager)
            var videoWidth = mediaPlayer.videoWidth.toFloat()
            var videoHeight = mediaPlayer.videoHeight.toFloat()
            var screenWidth = windowManager.defaultDisplay.width

            var lp = surfaceView?.layoutParams.also {
                it?.width = screenWidth
                it?.height = (videoHeight / videoWidth * screenWidth).toInt()
            }

            surfaceView?.layoutParams = lp
        }
    }
}
