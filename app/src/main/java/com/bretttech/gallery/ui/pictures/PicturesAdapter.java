package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.databinding.ItemPictureBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.PictureViewHolder> {

    private List<Image> images = new ArrayList<>();
    private final OnPictureClickListener listener;

    // Interface for click handling
    public interface OnPictureClickListener {
        void onPictureClick(Image image);
    }

    public PicturesAdapter(OnPictureClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPictureBinding binding = ItemPictureBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PictureViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        Image image = images.get(position);
        Uri uri = image.getUri();

        Glide.with(holder.imageView.getContext())
                .load(uri)
                // Glide automatically handles video thumbnails if the URI is for a video
                .centerCrop()
                .into(holder.imageView);

        // NEW: Show video indicator if it's a video
        if (image.isVideo()) {
            holder.videoIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.videoIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPictureClick(image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setImages(List<Image> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    static class PictureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView videoIndicator; // NEW

        PictureViewHolder(ItemPictureBinding binding) {
            super(binding.getRoot());
            imageView = binding.imageView;
            videoIndicator = binding.getRoot().findViewById(com.bretttech.gallery.R.id.video_indicator); // NEW
        }
    }
}