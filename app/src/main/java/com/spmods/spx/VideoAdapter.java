package com.spmods.spx;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video);
        void onVideoDeleted(int position);
    }

    private final Context context;
    private final List<VideoModel> videoList;
    private final OnVideoClickListener listener;

    public VideoAdapter(Context context, List<VideoModel> videoList, OnVideoClickListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        holder.bind(video, position);
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivThumbnail;
        private final ImageView ivPlayIcon;
        private final TextView tvTitle;
        private final TextView tvDuration;
        private final TextView tvSize;
        private final View vDivider;
        private final ImageView ivMore;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlayIcon  = itemView.findViewById(R.id.ivPlayIcon);
            tvTitle     = itemView.findViewById(R.id.tvVideoTitle);
            tvDuration  = itemView.findViewById(R.id.tvDuration);
            tvSize      = itemView.findViewById(R.id.tvSize);
            vDivider    = itemView.findViewById(R.id.vDivider);
            ivMore      = itemView.findViewById(R.id.ivMore);
        }

        void bind(VideoModel video, int position) {
            tvTitle.setText(video.getTitle());
            tvDuration.setText(video.getFormattedDuration());
            tvSize.setText(video.getFormattedSize());

            vDivider.setVisibility(
                    position == videoList.size() - 1 ? View.GONE : View.VISIBLE);

            ivThumbnail.setImageResource(R.drawable.ic_video_placeholder);
            loadThumbnailAsync(video, ivThumbnail);

            itemView.setOnClickListener(v -> listener.onVideoClick(video));

            ivMore.setOnClickListener(v -> showOptionsMenu(video, position));
        }

        private void showOptionsMenu(VideoModel video, int position) {
            String[] options = {"▶  Play", "✏  Rename", "↗  Share", "🗑  Delete", "ℹ  Info"};
            new AlertDialog.Builder(context)
                    .setTitle(video.getTitle())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: listener.onVideoClick(video); break;
                            case 1: showRenameDialog(video, position); break;
                            case 2: shareVideo(video); break;
                            case 3: showDeleteConfirm(video, position); break;
                            case 4: showVideoInfo(video); break;
                        }
                    })
                    .show();
        }

        private void showRenameDialog(VideoModel video, int position) {
            EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(video.getTitle());
            input.selectAll();

            FrameLayout container = new FrameLayout(context);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(48);
            params.setMarginEnd(48);
            input.setLayoutParams(params);
            container.addView(input);

            new AlertDialog.Builder(context)
                    .setTitle("Rename Video")
                    .setView(container)
                    .setPositiveButton("Rename", (d, w) -> {
                        String newName = input.getText().toString().trim();
                        if (newName.isEmpty()) return;
                        try {
                            ContentValues cv = new ContentValues();
                            cv.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                            context.getContentResolver().update(video.getUri(), cv, null, null);
                            video.setTitle(newName);
                            notifyItemChanged(position);
                            Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void shareVideo(VideoModel video) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/*");
            share.putExtra(Intent.EXTRA_STREAM, video.getUri());
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share Video"));
        }

        private void showDeleteConfirm(VideoModel video, int position) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Video")
                    .setMessage("Delete \"" + video.getTitle() + "\"?\nThis cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        try {
                            int rows = context.getContentResolver().delete(
                                    video.getUri(), null, null);
                            if (rows > 0) {
                                videoList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, videoList.size());
                                listener.onVideoDeleted(position);
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(context, "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showVideoInfo(VideoModel video) {
            String info = "Title: " + video.getTitle() +
                    "\nSize: " + video.getFormattedSize() +
                    "\nDuration: " + video.getFormattedDuration() +
                    "\nPath: " + video.getPath();
            new AlertDialog.Builder(context)
                    .setTitle("Video Info")
                    .setMessage(info)
                    .setPositiveButton("OK", null)
                    .show();
        }

        private void loadThumbnailAsync(VideoModel video, ImageView imageView) {
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        return ThumbnailUtils.createVideoThumbnail(
                                video.getPath(),
                                MediaStore.Images.Thumbnails.MINI_KIND);
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }.execute();
        }
    }
}
