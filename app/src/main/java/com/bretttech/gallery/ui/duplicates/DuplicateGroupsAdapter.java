package com.bretttech.gallery.ui.duplicates;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;
import com.bretttech.gallery.ui.pictures.Image;
import com.bretttech.gallery.utils.DuplicateFinder;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGroupsAdapter extends RecyclerView.Adapter<DuplicateGroupsAdapter.GroupViewHolder> {

    private final Context context;
    private final List<DuplicateFinder.DuplicateGroup> duplicateGroups = new ArrayList<>();
    private final List<Image> selectedImages = new ArrayList<>();

    public DuplicateGroupsAdapter(Context context) {
        this.context = context;
    }

    public void setDuplicateGroups(List<DuplicateFinder.DuplicateGroup> groups) {
        this.duplicateGroups.clear();
        this.duplicateGroups.addAll(groups);
        notifyDataSetChanged();
    }

    public List<Image> getSelectedDuplicates() {
        return new ArrayList<>(selectedImages);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_duplicate_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        DuplicateFinder.DuplicateGroup group = duplicateGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return duplicateGroups.size();
    }

    protected class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupTitle;
        private final RecyclerView imagesRecyclerView;
        private final DuplicateImagesAdapter imagesAdapter;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupTitle = itemView.findViewById(R.id.group_title);
            imagesRecyclerView = itemView.findViewById(R.id.images_recycler_view);

            imagesAdapter = new DuplicateImagesAdapter(context, selectedImages);
            imagesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            imagesRecyclerView.setAdapter(imagesAdapter);
        }

        public void bind(DuplicateFinder.DuplicateGroup group) {
            String matchType = group.matchType == 0 ? "Exact Match" : "Similar";
            groupTitle.setText(String.format("%s (%d images)", matchType, group.images.size()));
            imagesAdapter.setImages(group.images);
        }
    }

    /**
     * Adapter for displaying individual images within a duplicate group.
     */
    private static class DuplicateImagesAdapter extends RecyclerView.Adapter<DuplicateImagesAdapter.ImageViewHolder> {
        private final Context context;
        private final List<Image> images = new ArrayList<>();
        private final List<Image> selectedImages;

        public DuplicateImagesAdapter(Context context, List<Image> selectedImages) {
            this.context = context;
            this.selectedImages = selectedImages;
        }

        public void setImages(List<Image> images) {
            this.images.clear();
            this.images.addAll(images);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_duplicate_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Image image = images.get(position);
            holder.bind(image);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        protected class ImageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final CheckBox checkBox;
            private Image currentImage;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
                checkBox = itemView.findViewById(R.id.checkbox);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!selectedImages.contains(currentImage)) {
                            selectedImages.add(currentImage);
                        }
                    } else {
                        selectedImages.remove(currentImage);
                    }
                });
            }

            public void bind(Image image) {
                currentImage = image;
                Glide.with(context)
                        .load(image.getUri())
                        .thumbnail(0.3f)
                        .into(imageView);

                checkBox.setChecked(selectedImages.contains(image));
            }
        }
    }
}
