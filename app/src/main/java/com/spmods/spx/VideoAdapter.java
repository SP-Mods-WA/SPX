package com.spmods.spx.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spmods.spx.R;
import com.spmods.spx.model.VideoModel;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video);
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
        holder.bind(video);
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

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlayIcon  = itemView.findViewById(R.id.ivPlayIcon);
            tvTitle     = itemView.findViewById(R.id.tvVideoTitle);
            tvDuration  = itemView.findViewById(R.id.tvDuration);
            tvSize      = itemView.findViewById(R.id.tvSize);
            vDivider    = itemView.findViewById(R.id.vDivider);
        }

        void bind(VideoModel video) {
            tvTitle.setText(video.getTitle());
            tvDuration.setText(video.getFormattedDuration());
            tvSize.setText(video.getFormattedSize());

            // Hide divider on last item
            vDivider.setVisibility(
                    getAdapterPosition() == videoList.size() - 1 ? View.GONE : View.VISIBLE);

            // Load thumbnail asynchronously
            ivThumbnail.setImageResource(R.drawable.ic_video_placeholder);
            loadThumbnailAsync(video, ivThumbnail);

            itemView.setOnClickListener(v -> listener.onVideoClick(video));
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
