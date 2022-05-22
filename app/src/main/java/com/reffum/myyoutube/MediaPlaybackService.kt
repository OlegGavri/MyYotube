package com.reffum.myyoutube

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.MediaController
import androidx.annotation.RequiresApi


class MediaPlaybackService : Service(),
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener
{
    companion object {
        private const val LOG_TAG = "MediaPlaybackService"
        private const val NOTIFICATOIN_ID = 1
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

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate called")
        initMediaPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "onBind called")
        return MediaServiceBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy called")
        mediaPlayer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand() entered")
        if(!isServiceStarted) {
            isServiceStarted = true

            createNotificationChannel(channelId, "MediaPlaybackService")

            val notification = Notification.Builder(this, channelId).apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle("My music")
                setContentText("My text")
            }.build()

            Log.d(LOG_TAG, "Start foreground service")

            startForeground(NOTIFICATOIN_ID, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun playUrl(url : String) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepareAsync()
    }

    fun setSurfaceHolder(surfaceView : SurfaceView) {
        Log.d(LOG_TAG, "Change surface")

        this.surfaceView = surfaceView
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(LOG_TAG, "surface changed")
                mediaPlayer.setSurface(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(LOG_TAG, "Surface destroyed")
                mediaPlayer.setSurface(null)
            }
        })

        if(surfaceView.holder.surface.isValid)
            mediaPlayer.setDisplay(surfaceView.holder)

        if(mediaPlayer.isPlaying){
            Log.d(LOG_TAG, "Start prepareAsync")
            mediaPlayer.stop()
            mediaPlayer.prepareAsync()
        }
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
     * Return callback, that can be attached to MediaController widget, if it used.
     */
    fun getMediaControllerCallback() : MediaController.MediaPlayerControl {
        return object : MediaController.MediaPlayerControl {
            override fun start() {
                mediaPlayer.start()
            }

            override fun pause() {
                mediaPlayer.pause()
            }

            override fun getDuration(): Int {
                return mediaPlayer.duration
            }

            override fun getCurrentPosition(): Int {
                return mediaPlayer.currentPosition
            }

            override fun seekTo(pos: Int) {
                mediaPlayer.seekTo(pos)
            }

            override fun isPlaying(): Boolean {
                return mediaPlayer.isPlaying
            }

            override fun getBufferPercentage(): Int {
                return 0
            }

            override fun canPause(): Boolean {
                return mediaPlayer.isPlaying
            }

            override fun canSeekBackward(): Boolean {
                return true
            }

            override fun canSeekForward(): Boolean {
                return true
            }

            // Now it is not used
            override fun getAudioSessionId(): Int {
                return 0
            }
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
            else -> {
                Log.d(LOG_TAG, "MediaPlayer error: $what, extra = $extra")
                return false
            }
        }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        channelName: String): String {

        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )

        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)

        return channelId
    }
}
