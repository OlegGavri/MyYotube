package com.reffum.myyoutube

import android.app.Application
import android.util.Log
import kotlin.Throws
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*

class App : Application() {

    companion object {
        private const val TAG = "App"
        val applicationScope = MainScope()
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try{
                withContext(Dispatchers.IO) { initLibraries() }
                Log.d(Companion.TAG, "Youtube-dl initialization success")
            } catch (e : Throwable) {
                Log.e(Companion.TAG, "Youtube-dl initialization error")
                e.printStackTrace()
            }
        }
    }

    @Throws(YoutubeDLException::class)
    private fun initLibraries(){
        YoutubeDL.getInstance().init(this)
    }
}