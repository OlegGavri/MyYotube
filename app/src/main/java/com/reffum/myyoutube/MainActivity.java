package com.reffum.myyoutube;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SearchView;

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

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity  {

    private static final String APPLICATION_NAME = "myyoutube";
    private final String TAG = "MyYoutube";
    private final String API_KEY = "AIzaSyBGMvsvE5t8D8p213pxuNglIQEfO--1wXU";

    YouTube mYouTube;

    private SearchRecycleViewAdapter searchRecycleViewAdapter;

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

        // In webView open new link in it.
        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }
        });

        searchRecycleViewAdapter = new SearchRecycleViewAdapter();
        RecyclerView video_list_recycle_view = findViewById(R.id.video_list_recycler_view);
        video_list_recycle_view.setLayoutManager(new LinearLayoutManager(this));
        video_list_recycle_view.setAdapter(searchRecycleViewAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // User initial search
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String querry = intent.getStringExtra(SearchManager.QUERY);

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
                }
                catch (IOException e) {
                    e.printStackTrace();
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

    /**
     * Load youtube video in WebView
     * @param videoId
     */
    public void loadVideo(String videoId) {
        // Play youtube video

        // HTML page for WebView that play youtube iframe
        String frameVideo =
                "<html>" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<style>" +
                "* {" +
                "padding: 0;" +
                "margin: 0;" +
                "</style>" +
                "<body>" +
                "<iframe src=\"https://www.youtube.com/embed/" + videoId + "\" frameborder=\"0\" allowfullscreen " +
                "width=\"100%\" height=\"100%\">" +
                "</iframe>" +
                "</body>" +
                "</html>";

        WebView webView = (WebView) findViewById(R.id.web_view);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webView.loadData(frameVideo, "text/html", "utf-8");

        Log.d(TAG, "WIDTH: " + webView.getWidth());
        Log.d(TAG, "HEIGHT: " + webView.getHeight());
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
