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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.util.ArrayList;
import java.util.List;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.PictureViewHolder> {

    private List<Image> images = new ArrayList<>();
    private final List<Image> selectedImages = new ArrayList<>();
    private final OnImageClickListener clickListener;
    private final OnImageClickListener longClickListener;

    public interface OnImageClickListener {
        void onImageClick(Image image);
    }

    public PicturesAdapter(OnImageClickListener clickListener, OnImageClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture, parent, false);
        return new PictureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        Image image = images.get(position);
        Uri uri = image.getUri();

        Glide.with(holder.imageView.getContext())
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                .centerCrop()
                .into(holder.imageView);

        holder.videoIndicator.setVisibility(image.isVideo() ? View.VISIBLE : View.GONE);
        holder.selectionOverlay.setVisibility(selectedImages.contains(image) ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(image);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onImageClick(image);
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
            notifyDataSetChanged();
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

        PictureViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            videoIndicator = itemView.findViewById(R.id.video_indicator);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
        }
    }
}