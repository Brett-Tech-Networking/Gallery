package com.bretttech.gallery.ui.albums;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder> {

    private final List<Album> albums;
    private final OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumsAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums != null ? albums : new ArrayList<>();
        this.listener = listener;
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
        holder.bind(album, listener);
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

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final ImageView albumCover;
        private final TextView albumName;
        private final TextView albumImageCount;
        private final ImageView videoIndicator; // NEW

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.album_cover);
            albumName = itemView.findViewById(R.id.album_name);
            albumImageCount = itemView.findViewById(R.id.album_image_count);
            videoIndicator = itemView.findViewById(R.id.album_video_indicator); // NEW
        }

        public void bind(final Album album, final OnAlbumClickListener listener) {
            albumName.setText(album.getName());
            albumImageCount.setText(album.getImageCount() + " Photos");

            Uri coverUri = album.getCoverImageUri();
            Glide.with(itemView.getContext())
                    .load(coverUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_album_placeholder)
                    .into(albumCover);

            // NEW: Show video indicator if the album cover is a video
            if (album.isCoverVideo()) {
                videoIndicator.setVisibility(View.VISIBLE);
            } else {
                videoIndicator.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onAlbumClick(album));
        }
    }
}