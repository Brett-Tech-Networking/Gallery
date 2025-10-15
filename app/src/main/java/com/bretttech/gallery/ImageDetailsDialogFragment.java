package com.bretttech.gallery;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bretttech.gallery.data.ImageDetails;
import com.bretttech.gallery.data.ImageDetailsManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailsDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_IMAGE_URI = "image_uri";
    private Uri imageUri;
    private ImageDetailsManager imageDetailsManager;
    private ImageDetails imageDetails;
    private ChipGroup chipGroup;

    public static ImageDetailsDialogFragment newInstance(Uri imageUri) {
        ImageDetailsDialogFragment fragment = new ImageDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_IMAGE_URI, imageUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable(ARG_IMAGE_URI);
        }
        imageDetailsManager = new ImageDetailsManager(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_details, null);
        dialog.setContentView(view);

        ImageView imagePreview = view.findViewById(R.id.image_preview);
        TextView detailFilename = view.findViewById(R.id.detail_filename);
        TextView detailDate = view.findViewById(R.id.detail_date);
        TextView detailSize = view.findViewById(R.id.detail_size);
        TextView detailDimensions = view.findViewById(R.id.detail_dimensions);
        chipGroup = view.findViewById(R.id.tag_chip_group);
        EditText editTextTag = view.findViewById(R.id.edit_text_tag);
        Button buttonAddTag = view.findViewById(R.id.button_add_tag);

        Glide.with(this).load(imageUri).into(imagePreview);
        setFileDetails(detailFilename, detailDate, detailSize, detailDimensions);

        imageDetailsManager.getImageDetails(imageUri, details -> {
            imageDetails = details;
            requireActivity().runOnUiThread(this::updateTags);
        });

        buttonAddTag.setOnClickListener(v -> {
            String tag = editTextTag.getText().toString().trim();
            if (!tag.isEmpty()) {
                if (imageDetails != null) {
                    imageDetails.addTag(tag);
                    imageDetailsManager.saveImageDetails(imageUri, imageDetails);
                    updateTags();
                    editTextTag.setText("");
                }
            }
        });

        return dialog;
    }

    private void updateTags() {
        chipGroup.removeAllViews();
        if (imageDetails != null) {
            for (String tag : imageDetails.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag);
                chipGroup.addView(chip);
            }
        }
    }

    private void setFileDetails(TextView filename, TextView date, TextView size, TextView dimensions) {
        // **THE FIX IS HERE:** Query for both DATE_TAKEN and DATE_ADDED
        String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
        };
        try (Cursor cursor = requireContext().getContentResolver().query(imageUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                long dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN));
                long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
                long sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
                int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT));

                filename.setText("Filename: " + name);

                long finalDate;
                if (dateTaken > 0) {
                    // Use date taken if available
                    finalDate = dateTaken;
                } else {
                    // Otherwise, use date added (needs conversion from seconds to milliseconds)
                    finalDate = dateAdded * 1000L;
                }
                date.setText("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date(finalDate)));

                size.setText("Size: " + android.text.format.Formatter.formatFileSize(getContext(), sizeBytes));

                if (width > 0 && height > 0) {
                    dimensions.setText("Dimensions: " + width + " x " + height);
                } else {
                    // Fallback for when dimensions aren't in MediaStore (e.g., some PNGs)
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(imageUri), null, options);
                        dimensions.setText("Dimensions: " + options.outWidth + " x " + options.outHeight);
                    } catch (Exception e) {
                        dimensions.setText("Dimensions: Unknown");
                    }
                }
            } else {
                // Fallback for files not in MediaStore (e.g., in secure folder)
                File file = new File(imageUri.getPath());
                if (file.exists()) {
                    filename.setText("Filename: " + file.getName());
                    date.setText("Date: " + new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date(file.lastModified())));
                    size.setText("Size: " + android.text.format.Formatter.formatFileSize(getContext(), file.length()));
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                        dimensions.setText("Dimensions: " + options.outWidth + " x " + options.outHeight);
                    } catch (Exception e) {
                        dimensions.setText("Dimensions: Unknown");
                    }
                }
            }
        } catch (Exception e) {
            // Handle any exceptions during query
            filename.setText("Filename: Not found");
            date.setText("Date: Unknown");
            size.setText("Size: Unknown");
            dimensions.setText("Dimensions: Unknown");
        }
    }
}