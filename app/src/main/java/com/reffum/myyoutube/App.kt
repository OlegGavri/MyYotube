package com.reffum.myyoutube

import android.app.Application
import android.util.Log
import kotlin.Throws
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalScope.launch {
            try{
                withContext(Dispatchers.IO) { initLibraries() }
                Log.d(TAG, "Youtube-dl initialization success")
            } catch (e : Throwable) {
                Log.e(TAG, "Youtube-dl initialization error")
                e.printStackTrace()
            }
        }
    }

    @Throws(YoutubeDLException::class)
    private suspend fun initLibraries(){
        YoutubeDL.getInstance().init(this)
    }

    private val TAG = "App"
}