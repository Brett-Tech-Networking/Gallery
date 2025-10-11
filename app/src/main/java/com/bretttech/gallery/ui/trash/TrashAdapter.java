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

    private List<Image> trashedImages = new ArrayList<>();
    private final OnTrashItemClickListener listener;

    public interface OnTrashItemClickListener {
        void onTrashItemClick(Image image);
    }

    public TrashAdapter(OnTrashItemClickListener listener) {
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> listener.onTrashItemClick(image));
    }

    @Override
    public int getItemCount() {
        return trashedImages.size();
    }

    public void setTrashedImages(List<Image> images) {
        this.trashedImages = images;
        notifyDataSetChanged();
    }

    static class TrashViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}