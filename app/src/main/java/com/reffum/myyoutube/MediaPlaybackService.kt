package com.reffum.myyoutube

import android.content.ComponentName
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaController
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.reffum.myyoutube.model.YoutubeVideoList

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat(),
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener
{
    companion object {
        private const val LOG_TAG = "MediaPlaybackService"
        private const val channelId = "com.reffum.myyoutube.NOTIFICATION_CHANNEL_ID"
    }

    private var isServiceStarted : Boolean = false

    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var transportControls: MediaControllerCompat.TransportControls

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()

            // If service not started start it.
            if(!isServiceStarted) {
                Log.d(LOG_TAG, "Service was called to be started")
                startService(Intent(applicationContext, MediaPlaybackService::class.java))
            }

            // Start playing of current video
            val currentVideo = YoutubeVideoList.current
            if(currentVideo == null){
                Log.d(LOG_TAG, "onPlay() called, but current video empty")
                return
            }

            mediaPlayer.setDataSource(currentVideo.url)
            mediaPlayer.prepareAsync()
        }
    }

    override fun onCreate() {
        super.onCreate()

        initMediaPlayer()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand() entered")
        isServiceStarted = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        return result.sendResult(null)
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

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(
            applicationContext,
            "MyYoutubeMediaService",
            mediaButtonReceiver,
            null).apply {
                setCallback(mediaSessionCallback)
            }
        transportControls = mediaSession.controller.transportControls
        sessionToken = mediaSession.sessionToken
    }

    /**
     * MediaPlayer callbacks
     */
    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "Song completed.")
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "onPrepared(). MediaPlayer ready to play")
        mediaSession.isActive = true
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
}
