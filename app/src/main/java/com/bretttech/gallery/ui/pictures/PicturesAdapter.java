package com.bretttech.gallery.ui.pictures;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PicturesAdapter extends RecyclerView.Adapter<PicturesAdapter.PictureViewHolder> {

    private List<Image> images = new ArrayList<>();
    private final OnImageClickListener clickListener;
    private final OnImageClickListener longClickListener;
    private final List<Image> selectedImages = new ArrayList<>();

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

        // MODIFIED: This forces Glide to ignore its cache and reload the thumbnail from disk
        Glide.with(holder.imageView.getContext())
                .load(image.getUri())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> clickListener.onImageClick(image));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onImageClick(image);
            return true;
        });

        holder.itemView.setActivated(selectedImages.contains(image));
        holder.videoIndicator.setVisibility(image.isVideo() ? View.VISIBLE : View.GONE);

    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setImages(List<Image> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    public void toggleSelection(Image image) {
        if (selectedImages.contains(image)) {
            selectedImages.remove(image);
        } else {
            selectedImages.add(image);
        }
        notifyDataSetChanged();
    }

    public List<Image> getSelectedImages() {
        return selectedImages;
    }

    public void clearSelection() {
        selectedImages.clear();
        notifyDataSetChanged();
    }

    // NEW: Added the missing method back to fix the compilation error
    public void removeImagesByUri(List<Uri> movedUris) {
        List<Image> imagesToRemove = images.stream()
                .filter(image -> movedUris.contains(image.getUri()))
                .collect(Collectors.toList());
        images.removeAll(imagesToRemove);
        notifyDataSetChanged();
    }

    static class PictureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView videoIndicator;


        PictureViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            videoIndicator = itemView.findViewById(R.id.video_indicator);
        }
    }
}