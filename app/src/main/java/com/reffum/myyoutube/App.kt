package com.reffum.myyoutube

import android.app.Application
import android.util.Log
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import com.reffum.myyoutube.App
import android.widget.Toast
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.exceptions.UndeliverableException
import kotlin.Throws
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        configureRxJavaErrorHandler()
        Completable.fromAction { initLibraries() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        // it worked
                    }

                    override fun onError(e: Throwable) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "failed to initialize youtubedl-android", e)
                        Toast.makeText(applicationContext, "initialize failed: " +
                                e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler {_e: Throwable ->
            var e = _e
            if (e is UndeliverableException) {
                e = e.cause!!
            }
            if (e is InterruptedException) {
                return@setErrorHandler
            }
            Log.e(TAG, "Undeliverable exception received, not sure what to do", e)
        }
    }

    @Throws(YoutubeDLException::class)
    private fun initLibraries() {
        YoutubeDL.getInstance().init(this)
    }

    private val TAG = "App"
}