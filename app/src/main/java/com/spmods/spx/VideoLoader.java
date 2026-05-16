package com.spmods.spx;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class VideoLoader {

    public static List<VideoModel> getAllVideos(Context context) {
        List<VideoModel> videoList = new ArrayList<>();

        Uri collection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED
        };

        String selection = MediaStore.Video.Media.DURATION + " > 0 AND " +
                           MediaStore.Video.Media.SIZE + " > 0";
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, null, sortOrder)) {

            if (cursor == null) return videoList;

            int idCol       = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int dataCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

            while (cursor.moveToNext()) {
                long id        = cursor.getLong(idCol);
                String name    = cursor.getString(nameCol);
                long duration  = cursor.getLong(durationCol);
                long size      = cursor.getLong(sizeCol);
                String path    = cursor.getString(dataCol);
                long dateAdded = cursor.getLong(dateCol);

                if (name == null || name.isEmpty()) continue;
                if (duration < 1000) continue; // skip < 1 second clips

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

                String title = name.contains(".")
                        ? name.substring(0, name.lastIndexOf('.'))
                        : name;

                videoList.add(new VideoModel(id, title, contentUri, duration, size, path, dateAdded));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoList;
    }
}
