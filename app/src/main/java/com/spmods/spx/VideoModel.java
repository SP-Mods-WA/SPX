package com.spmods.spx;

import android.net.Uri;

public class VideoModel {

    private long id;
    private String title;
    private Uri uri;
    private long duration;   // milliseconds
    private long size;       // bytes
    private String path;
    private long dateAdded;

    public VideoModel(long id, String title, Uri uri, long duration, long size, String path, long dateAdded) {
        this.id = id;
        this.title = title;
        this.uri = uri;
        this.duration = duration;
        this.size = size;
        this.path = path;
        this.dateAdded = dateAdded;
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public Uri getUri() { return uri; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public String getPath() { return path; }
    public long getDateAdded() { return dateAdded; }

    /**
     * Returns formatted duration: mm:ss or hh:mm:ss
     */
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Returns formatted file size: KB or MB
     */
    public String getFormattedSize() {
        if (size >= 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%d KB", size / 1024);
        }
    }
}
