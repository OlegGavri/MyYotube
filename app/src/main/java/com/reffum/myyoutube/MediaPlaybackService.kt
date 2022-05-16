package com.reffum.myyoutube

import android.content.ComponentName
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat(),
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener
{

    companion object {
        private const val LOG_TAG = "MediaPlaybackService"
        private const val channelId = "com.reffum.myyoutube.NOTIFICATION_CHANNEL_ID"
    }

    private lateinit var mediaPlayer : MediaPlayer
    private var mediaSession : MediaSessionCompat? = null
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        //TODO: Implement this
    }

    override fun onCreate() {
        super.onCreate()

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
        }

    }


}
