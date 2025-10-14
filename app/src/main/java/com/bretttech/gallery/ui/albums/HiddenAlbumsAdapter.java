package com.bretttech.gallery.ui.albums;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HiddenAlbumsAdapter extends RecyclerView.Adapter<HiddenAlbumsAdapter.ViewHolder> {

    private List<Album> albums = new ArrayList<>();
    private Set<String> hiddenPaths;
    private final OnAlbumToggleListener listener;
    private final String secureFolderPath;

    public interface OnAlbumToggleListener {
        // MODIFIED: Pass Album object and position
        void onAlbumToggled(Album album, boolean isHidden, int position);
    }

    public HiddenAlbumsAdapter(Set<String> hiddenPaths, String secureFolderPath, OnAlbumToggleListener listener) {
        this.hiddenPaths = hiddenPaths;
        this.secureFolderPath = secureFolderPath;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hide_album_toggle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumName.setText(album.getName());

        // Ensure state is correctly read
        boolean isHidden = hiddenPaths.contains(album.getFolderPath());
        holder.hideSwitch.setChecked(isHidden);

        // Load album cover using Glide
        Uri coverUri = album.getCoverImageUri();
        if (coverUri != null) {
            ObjectKey cacheSignature = new ObjectKey(album.getFolderPath() + "_" + album.getCacheBusterId());

            Glide.with(holder.albumCover.getContext())
                    .load(coverUri)
                    .signature(cacheSignature)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .placeholder(R.drawable.ic_album_placeholder)
                    .into(holder.albumCover);
        } else {
            holder.albumCover.setImageResource(R.drawable.ic_album_placeholder);
        }

        holder.videoIndicator.setVisibility(album.isCoverVideo() ? View.VISIBLE : View.GONE);
        holder.hideSwitch.setText(isHidden ? R.string.status_hidden : R.string.status_visible);

        // Remove previous listener to prevent unintended calls
        holder.hideSwitch.setOnCheckedChangeListener(null);

        holder.hideSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // UPDATED: Pass the album object and position to the listener.
            listener.onAlbumToggled(album, isChecked, position);

            // FIX for Instant Revert: Optimistically update the internal set and the UI text
            // This holds the visual state until the full LiveData reload occurs.
            if (isChecked) {
                hiddenPaths.add(album.getFolderPath());
                buttonView.setText(R.string.status_hidden);
            } else {
                hiddenPaths.remove(album.getFolderPath());
                buttonView.setText(R.string.status_visible);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void setAlbums(List<Album> newAlbums, Set<String> newHiddenPaths) {
        // Filter out Secure Folder albums
        this.albums.clear();
        for (Album album : newAlbums) {
            // Check if the album is in the public section (not under the app's secure directory)
            if (!album.getFolderPath().contains(secureFolderPath)) {
                this.albums.add(album);
            }
        }
        this.hiddenPaths = newHiddenPaths;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        ImageView videoIndicator;
        TextView albumName;
        SwitchCompat hideSwitch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.album_cover_preview);
            videoIndicator = itemView.findViewById(R.id.album_video_indicator);
            albumName = itemView.findViewById(R.id.album_name_text_view);
            hideSwitch = itemView.findViewById(R.id.hide_album_switch);
        }
    }
}