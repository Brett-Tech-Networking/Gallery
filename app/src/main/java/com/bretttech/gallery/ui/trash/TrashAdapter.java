package com.bretttech.gallery.ui.trash;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.ui.pictures.Image;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

    private final List<Image> trashedImages = new ArrayList<>();
    private final List<Image> selectedImages = new ArrayList<>(); // NEW: Track selected items
    private final OnTrashItemClickListener listener;
    private final OnTrashItemLongClickListener longClickListener; // NEW: Long click listener

    public interface OnTrashItemClickListener {
        void onTrashItemClick(Image image);
    }

    // NEW interface
    public interface OnTrashItemLongClickListener {
        void onTrashItemLongClick(Image image);
    }

    // UPDATED constructor
    public TrashAdapter(OnTrashItemClickListener clickListener, OnTrashItemLongClickListener longClickListener) {
        this.listener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trash_image, parent, false);
        return new TrashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        Image image = trashedImages.get(position);
        Uri uri = image.getUri();

        Glide.with(holder.imageView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.imageView);

        // NEW: Selection state indicator
        if (selectedImages.contains(image)) {
            holder.selectionOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.selectionOverlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onTrashItemClick(image));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onTrashItemLongClick(image);
            return true; // Consume long click
        });
    }

    @Override
    public int getItemCount() {
        return trashedImages.size();
    }

    public void setTrashedImages(List<Image> images) {
        this.trashedImages.clear();
        if (images != null) {
            this.trashedImages.addAll(images);
        }
        this.selectedImages.clear(); // Clear selection on new data load
        notifyDataSetChanged();
    }

    // NEW: Selection logic methods
    public boolean toggleSelection(Image image) {
        boolean selected;
        if (selectedImages.contains(image)) {
            selectedImages.remove(image);
            selected = false;
        } else {
            selectedImages.add(image);
            selected = true;
        }
        notifyDataSetChanged();
        return selected;
    }

    public void clearSelection() {
        selectedImages.clear();
        notifyDataSetChanged();
    }

    public List<Image> getSelectedImages() {
        return selectedImages;
    }

    static class TrashViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View selectionOverlay; // NEW: View for selection indicator

        public TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay); // NEW: Find the overlay view
        }
    }
}