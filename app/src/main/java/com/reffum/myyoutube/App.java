package com.reffum.myyoutube;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        configureRxJavaErrorHandler();
        Completable.fromAction(this::initLibraries)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        // it worked
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if(BuildConfig.DEBUG)
                            Log.d(TAG,  "failed to initialize youtubedl-android", e);
                        Toast.makeText(getApplicationContext(), "initialize failed: " +
                                e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            if(e instanceof UndeliverableException) {
                e = e.getCause();
            }

            if(e instanceof InterruptedException) {
                return;
            }

            Log.e(TAG, "Undeliverable exception received, not sure what to do", e);
        });
    }

    private void initLibraries() throws YoutubeDLException {
        YoutubeDL.getInstance().init(this);
    }

}
