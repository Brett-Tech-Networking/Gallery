package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.databinding.ItemPictureBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.PictureViewHolder> {

    private List<Image> images = new ArrayList<>();
    private final List<Image> selectedImages = new ArrayList<>();
    private final OnPictureClickListener clickListener;
    private final OnPictureLongClickListener longClickListener;

    public interface OnPictureClickListener {
        void onPictureClick(Image image);
    }

    public interface OnPictureLongClickListener {
        void onPictureLongClick(Image image);
    }

    public PicturesAdapter(OnPictureClickListener clickListener, OnPictureLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
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
                .centerCrop()
                .into(holder.imageView);

        holder.videoIndicator.setVisibility(image.isVideo() ? View.VISIBLE : View.GONE);
        holder.selectionOverlay.setVisibility(selectedImages.contains(image) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPictureClick(image);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPictureLongClick(image);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setImages(List<Image> images) {
        this.images.clear();
        if (images != null) {
            this.images.addAll(images);
        }
        notifyDataSetChanged();
    }

    /**
     * Removes a list of images from the adapter based on their URIs and notifies the RecyclerView.
     * This provides an immediate visual update.
     * @param urisToRemove The list of URIs for the images to be removed.
     */
    public void removeImagesByUri(List<Uri> urisToRemove) {
        if (urisToRemove == null || urisToRemove.isEmpty() || this.images.isEmpty()) {
            return;
        }

        List<Image> itemsToRemove = new ArrayList<>();
        for (Uri uri : urisToRemove) {
            for (Image image : this.images) {
                if (image.getUri().equals(uri)) {
                    itemsToRemove.add(image);
                    break;
                }
            }
        }

        if (!itemsToRemove.isEmpty()) {
            this.images.removeAll(itemsToRemove);
            notifyDataSetChanged(); // Use notifyDataSetChanged for simplicity and robustness here.
        }
    }


    public void toggleSelection(Image image) {
        if (selectedImages.contains(image)) {
            selectedImages.remove(image);
        } else {
            selectedImages.add(image);
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedImages.clear();
        notifyDataSetChanged();
    }

    public List<Image> getSelectedImages() {
        return new ArrayList<>(selectedImages);
    }

    static class PictureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView videoIndicator;
        View selectionOverlay;

        PictureViewHolder(ItemPictureBinding binding) {
            super(binding.getRoot());
            imageView = binding.imageView;
            videoIndicator = binding.videoIndicator;
            selectionOverlay = binding.selectionOverlay;
        }
    }
}