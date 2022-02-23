package com.reffum.myyoutube;

import android.graphics.Bitmap;

// 1 YouTube video data
public class VideoData {

    //Video title
    private final String title;

    // Detail description
    private final String detail;

    // Yotube video ID
    private final String id;

    // image, icon
    private final Bitmap image;

    public VideoData(String title,  String detail, String id, Bitmap image){
        this.title = title;
        this.detail = detail;
        this.id = id;
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getDetail() {
        return detail;
    }
}
