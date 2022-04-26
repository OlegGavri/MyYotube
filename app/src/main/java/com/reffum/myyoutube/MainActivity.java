package com.reffum.myyoutube;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity  {

    private static final String APPLICATION_NAME = "myyoutube";
    private final String TAG = "MyYoutube";
    private final String API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU";

    YouTube mYouTube;

    private SearchRecycleViewAdapter searchRecycleViewAdapter;

    // Activity views
    private VideoView videoView;
    private MediaController mediaController;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Youtube service, provide YouTube Data API v3.
        // The YouTube Data API v3 is an API that provides access to YouTube data,
        // such as videos, playlists, and channels.
        try {
            mYouTube = getService();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        searchRecycleViewAdapter = new SearchRecycleViewAdapter();
        RecyclerView video_list_recycle_view = findViewById(R.id.video_list_recycler_view);
        video_list_recycle_view.setLayoutManager(new LinearLayoutManager(this));
        video_list_recycle_view.setAdapter(searchRecycleViewAdapter);

        initViews();
        initListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // User initial search
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String querry = intent.getStringExtra(SearchManager.QUERY);

            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    YouTube.Search.List request = mYouTube.search().list(Arrays.asList("id,snippet"));
                    SearchListResponse response = request.setQ(querry)
                            .setType(Arrays.asList("video"))
                            .setMaxResults(50L)
                            .execute();

                    ArrayList<VideoData> videoList = new ArrayList<>();

                    for (SearchResult searchResult : response.getItems()) {
                        ResourceId id = searchResult.getId();
                        SearchResultSnippet snippet = searchResult.getSnippet();
                        ThumbnailDetails thumbnailDetails = snippet.getThumbnails();

                        // Get video id, title and image
                        String videoId = id.getVideoId();
                        String title = snippet.getTitle();
                        String detail = snippet.getDescription();
                        String imageUrl = thumbnailDetails.getDefault().getUrl();

                        // Load image
                        URL url = new URL(imageUrl);
                        Bitmap bmp = BitmapFactory.decodeStream(url.
                                openConnection().
                                getInputStream());

                        videoList.add(new VideoData(title, detail, videoId, bmp));
                    }

                    runOnUiThread( () -> {
                       searchRecycleViewAdapter.setVideoList(videoList);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    runOnUiThread( () -> {
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        // Get the SearchView and set searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName())
        );
        searchView.setIconifiedByDefault(false);

        return true;
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        progressBar = findViewById(R.id.progress_bar);
    }

    private void initListeners() {
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaController.setAnchorView(videoView);
                videoView.start();
            }
        });
    }

    /**
     * Load youtube video in WebView
     * @param videoId
     */
    public void loadVideo(String videoId) {
        // Play youtube video
        String url = "http://www.youtube.com/watch?v=" + videoId;

        progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> {
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("-f", "best");
            return YoutubeDL.getInstance().getInfo(request);
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    progressBar.setVisibility(View.GONE);
                    String videoUrl = streamInfo.getUrl();
                    if(videoUrl.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "failed to get stream url",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        setupVideoView(videoUrl);
                    }
                }, e -> {
                    progressBar.setVisibility(View.GONE);
                    if(BuildConfig.DEBUG)
                        Log.d(TAG, "failed to get stream url", e);
                    Toast.makeText(getApplicationContext(), "streaming failed.",
                            Toast.LENGTH_SHORT).show();
                });

    }

    private void setupVideoView(String videoUrl) {
        videoView.setVideoURI(Uri.parse(videoUrl));
    }

    public YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new YouTube.Builder(httpTransport, new GsonFactory(), request -> {
            // Add Google API_KEY in request
            GenericUrl url = request.getUrl();
            url.set("key", API_KEY);
            Log.d(TAG, "URL: " + url.toString());
        }).setApplicationName(APPLICATION_NAME)
          .build();
    }
}
