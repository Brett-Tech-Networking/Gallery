package com.bretttech.gallery.ui.albums;

import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.SettingsActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder> {

    private final List<Album> albums;
    private final OnAlbumClickListener listener;
    private final List<Album> selectedAlbums = new ArrayList<>();
    private int spanCount;


    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
        void onAlbumLongClick(Album album);
    }

    public AlbumsAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums != null ? albums : new ArrayList<>();
        this.listener = listener;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.bind(album, listener, selectedAlbums.contains(album), spanCount);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void setAlbums(List<Album> newAlbums) {
        albums.clear();
        if (newAlbums != null) {
            albums.addAll(newAlbums);
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(Album album) {
        if (selectedAlbums.contains(album)) {
            selectedAlbums.remove(album);
        } else {
            selectedAlbums.add(album);
        }
        notifyDataSetChanged();
    }

    public List<Album> getSelectedAlbums() {
        return selectedAlbums;
    }

    public void clearSelection() {
        selectedAlbums.clear();
        notifyDataSetChanged();
    }


    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final ImageView albumCover;
        private final TextView albumName;
        private final TextView albumImageCount;
        private final ImageView videoIndicator;
        private final View selectionOverlay;


        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.album_cover);
            albumName = itemView.findViewById(R.id.album_name);
            albumImageCount = itemView.findViewById(R.id.album_image_count);
            videoIndicator = itemView.findViewById(R.id.album_video_indicator);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
        }

        public void bind(final Album album, final OnAlbumClickListener listener, boolean isSelected, int spanCount) {
            albumName.setText(album.getName());
            albumImageCount.setText(album.getImageCount() + " Items");

            if (spanCount == 3) {
                albumName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            } else {
                albumName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }

            Uri coverUri = album.getCoverImageUri();
            if (coverUri != null) {
                // MODIFIED: This forces Glide to ignore its cache and reload the cover image
                ObjectKey cacheSignature = new ObjectKey(album.getFolderPath() + "_" + album.getCacheBusterId());

                Glide.with(itemView.getContext())
                        .load(coverUri)
                        .signature(cacheSignature)
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Force reload from disk
                        .skipMemoryCache(true) // Don't use memory cache
                        .centerCrop()
                        .placeholder(R.drawable.ic_album_placeholder)
                        .into(albumCover);
            } else {
                albumCover.setImageResource(R.drawable.ic_album_placeholder);
            }


            if (album.isCoverVideo()) {
                videoIndicator.setVisibility(View.VISIBLE);
            } else {
                videoIndicator.setVisibility(View.GONE);
            }

            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Apply album border preferences
            boolean showBorders = SettingsActivity.isAlbumBordersEnabled(itemView.getContext());
            View root = itemView;
            if (root instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) root;
                if (showBorders) {
                    int widthDp = SettingsActivity.getAlbumBorderWidthDp(itemView.getContext());
                    int color = SettingsActivity.getAlbumBorderColor(itemView.getContext());
                    card.setStrokeWidth(dpToPx(itemView.getContext(), widthDp));
                    card.setStrokeColor(color);
                } else {
                    card.setStrokeWidth(0);
                }
            }


            itemView.setOnClickListener(v -> listener.onAlbumClick(album));
            itemView.setOnLongClickListener(v -> {
                listener.onAlbumLongClick(album);
                return true;
            });
        }

        private int dpToPx(Context context, int dp) {
            float density = context.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}