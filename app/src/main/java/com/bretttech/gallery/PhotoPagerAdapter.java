package com.bretttech.gallery;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    public interface PhotoTapListener {
        void onPhotoTapped(Uri photoUri);
    }

    private final Context context;
    private final List<Uri> imageUris;
    private final PhotoTapListener listener;

    public PhotoPagerAdapter(Context context, List<Uri> imageUris, PhotoTapListener listener) {
        this.context = context;
        this.imageUris = imageUris;
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
        Uri imageUri = imageUris.get(position);

        Glide.with(context)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature(new ObjectKey(System.currentTimeMillis()))
                .into(holder.photoView);

        holder.photoView.setOnPhotoTapListener((view, x, y) -> {
            if (listener != null) {
                listener.onPhotoTapped(imageUri);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photo_view);
        }
    }
}