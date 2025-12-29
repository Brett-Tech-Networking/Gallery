package com.bretttech.gallery;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.ui.pictures.Image;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    public interface PhotoTapListener {
        void onPhotoTapped(Uri photoUri);
    }

    private final Context context;
    private final List<Image> images;
    private final PhotoTapListener listener;

    public PhotoPagerAdapter(Context context, List<Image> images, PhotoTapListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_pager, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Image image = images.get(position);
        Uri imageUri = image.getUri();

        // Reset state
        holder.videoView.setVisibility(View.GONE);
        holder.iconPlay.setVisibility(View.GONE);
        holder.photoView.setVisibility(View.VISIBLE);
        if (holder.videoView.isPlaying()) {
            holder.videoView.stopPlayback();
        }

        Glide.with(context)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).startPostponedEnterTransition();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).startPostponedEnterTransition();
                        }
                        return false;
                    }
                })
                .into(holder.photoView);

        holder.photoView.setTransitionName(imageUri.toString());
        holder.photoView.setAllowParentInterceptOnEdge(true);

        holder.photoView.setOnPhotoTapListener((view, x, y) -> {
            if (listener != null) {
                listener.onPhotoTapped(imageUri);
            }
        });

        if (image.isVideo()) {
            holder.iconPlay.setVisibility(View.VISIBLE);
            holder.iconPlay.setOnClickListener(v -> {
                holder.photoView.setVisibility(View.GONE);
                holder.iconPlay.setVisibility(View.GONE);
                holder.videoView.setVisibility(View.VISIBLE);
                holder.videoView.setVideoURI(imageUri);
                holder.videoView.start();
            });

            // Allow toggling controls when tapping video view too
            holder.videoView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoTapped(imageUri);
                }
            });

            holder.videoView.setOnCompletionListener(mp -> {
                holder.videoView.setVisibility(View.GONE);
                holder.photoView.setVisibility(View.VISIBLE);
                holder.iconPlay.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    @Override
    public void onViewRecycled(@NonNull PhotoViewHolder holder) {
        super.onViewRecycled(holder);
        stopPlayback(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull PhotoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        stopPlayback(holder);
    }

    private void stopPlayback(PhotoViewHolder holder) {
        if (holder.videoView.isPlaying()) {
            holder.videoView.stopPlayback();
        }
        holder.videoView.setVisibility(View.GONE);
        holder.photoView.setVisibility(View.VISIBLE);
        holder.iconPlay.setVisibility(View.VISIBLE);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        VideoView videoView;
        ImageView iconPlay;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photo_view);
            videoView = itemView.findViewById(R.id.video_view);
            iconPlay = itemView.findViewById(R.id.icon_play);
        }
    }
}