package com.bretttech.gallery;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher; // NEW IMPORT

import java.util.List;

public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    // NEW INTERFACE: Used to notify the containing Activity when a photo is tapped
    public interface PhotoTapListener {
        void onPhotoTapped(Uri photoUri);
    }

    private final Context context;
    private final List<Uri> imageUris;
    private final PhotoTapListener listener; // NEW FIELD

    // UPDATED CONSTRUCTOR: Takes the listener
    public PhotoPagerAdapter(Context context, List<Uri> imageUris, PhotoTapListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.listener = listener; // ASSIGN LISTENER
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
                .into(holder.photoView);

        // NEW: Set the PhotoView's tap listener
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