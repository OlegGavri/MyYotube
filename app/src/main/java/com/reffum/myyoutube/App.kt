package com.reffum.myyoutube

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class App : Application() {

    companion object {
        private const val TAG = "App"
        val applicationScope = MainScope()
    }

    override fun onCreate() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
        )

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .penaltyDeath()
            .build()
        )

        super.onCreate()
        applicationScope.launch {
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
    private fun initLibraries(){
        YoutubeDL.getInstance().init(this)
    }
}