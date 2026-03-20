package com.bretttech.gallery.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bretttech.gallery.R;

import java.util.ArrayList;
import java.util.List;

import ja.burhanrashid52.photoeditor.PhotoFilter;

public class FilterViewAdapter extends RecyclerView.Adapter<FilterViewAdapter.ViewHolder> {

    private static final int PREVIEW_SIZE_PX = 140;

    private final FilterListener mFilterListener;
    private final List<PhotoFilter> mPhotoFilters = new ArrayList<>();
    private final List<Bitmap> mPreviewBitmaps = new ArrayList<>();
    private final Bitmap sourceBitmap;

    public FilterViewAdapter(FilterListener filterListener, Bitmap sourceBitmap) {
        mFilterListener = filterListener;
        this.sourceBitmap = sourceBitmap;
        setupFilters();
        setupPreviews();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhotoFilter filter = mPhotoFilters.get(position);
        String filterName = filter.name().replace("_", " ");
        holder.mTxtFilterName.setText(filterName);
        holder.mImgFilterPreview.setImageBitmap(mPreviewBitmaps.get(position));
    }

    @Override
    public int getItemCount() {
        return mPhotoFilters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mImgFilterPreview;
        TextView mTxtFilterName;

        ViewHolder(View itemView) {
            super(itemView);
            mImgFilterPreview = itemView.findViewById(R.id.filter_image_preview);
            mTxtFilterName = itemView.findViewById(R.id.filter_name);
            itemView.setOnClickListener(v -> {
                int adapterPosition = getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    mFilterListener.onFilterSelected(mPhotoFilters.get(adapterPosition));
                }
            });
        }
    }

    private void setupPreviews() {
        if (sourceBitmap == null || sourceBitmap.isRecycled()) {
            return;
        }

        Bitmap basePreview = Bitmap.createScaledBitmap(sourceBitmap, PREVIEW_SIZE_PX, PREVIEW_SIZE_PX, true);
        for (PhotoFilter filter : mPhotoFilters) {
            mPreviewBitmaps.add(applyPreviewFilter(basePreview, filter));
        }
    }

    private Bitmap applyPreviewFilter(Bitmap src, PhotoFilter filter) {
        switch (filter) {
            case NONE:
                return src.copy(Bitmap.Config.ARGB_8888, false);
            case AUTO_FIX:
                return applyTone(src, 1.1f, 1.1f, 8f);
            case BRIGHTNESS:
                return applyTone(src, 1f, 1f, 25f);
            case CONTRAST:
                return applyTone(src, 1.35f, 1f, 0f);
            case DOCUMENTARY:
                return applyTone(applySaturation(src, 0f), 1.35f, 1f, 0f);
            case DUE_TONE:
                return applyTint(applySaturation(src, 0.6f), Color.parseColor("#7048E8"), 70);
            case FILL_LIGHT:
                return applyTone(src, 1f, 1.05f, 16f);
            case FISH_EYE:
                return applyTone(src, 1.2f, 1.2f, 0f);
            case GRAIN:
                return applyTint(src, Color.parseColor("#8B5A2B"), 30);
            case GRAY_SCALE:
                return applySaturation(src, 0f);
            case LOMISH:
                return applyTone(applyTint(src, Color.BLACK, 25), 1.35f, 1.25f, 0f);
            case NEGATIVE:
                return applyNegative(src);
            case POSTERIZE:
                return applyPosterize(src, 6);
            case SATURATE:
                return applySaturation(src, 1.8f);
            case SEPIA:
                return applySepia(src);
            case SHARPEN:
                return applyTone(src, 1.25f, 1.1f, 0f);
            case TEMPERATURE:
                return applyTint(src, Color.parseColor("#FF8A00"), 40);
            case TINT:
                return applyTint(src, Color.parseColor("#7B2CBF"), 65);
            case VIGNETTE:
                return applyTint(src, Color.BLACK, 45);
            case CROSS_PROCESS:
                return applyTone(applyTint(src, Color.parseColor("#0099CC"), 25), 1.15f, 1.3f, 0f);
            case BLACK_WHITE:
                return applyTone(applySaturation(src, 0f), 1.6f, 1f, 0f);
            default:
                return src.copy(Bitmap.Config.ARGB_8888, false);
        }
    }

    private Bitmap applyTone(Bitmap src, float contrast, float saturation, float brightness) {
        ColorMatrix colorMatrix = new ColorMatrix();

        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturation);
        colorMatrix.postConcat(saturationMatrix);

        float translation = (1f - contrast) * 128f + brightness;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                contrast, 0, 0, 0, translation,
                0, contrast, 0, 0, translation,
                0, 0, contrast, 0, translation,
                0, 0, 0, 1, 0
        });
        colorMatrix.postConcat(contrastMatrix);

        return applyColorMatrix(src, colorMatrix);
    }

    private Bitmap applySaturation(Bitmap src, float saturation) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(saturation);
        return applyColorMatrix(src, colorMatrix);
    }

    private Bitmap applySepia(Bitmap src) {
        ColorMatrix sepiaMatrix = new ColorMatrix(new float[] {
                0.393f, 0.769f, 0.189f, 0, 0,
                0.349f, 0.686f, 0.168f, 0, 0,
                0.272f, 0.534f, 0.131f, 0, 0,
                0, 0, 0, 1, 0
        });
        return applyColorMatrix(src, sepiaMatrix);
    }

    private Bitmap applyNegative(Bitmap src) {
        ColorMatrix negativeMatrix = new ColorMatrix(new float[] {
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0
        });
        return applyColorMatrix(src, negativeMatrix);
    }

    private Bitmap applyTint(Bitmap src, int tintColor, int alpha) {
        Bitmap output = src.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(alpha, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)));
        canvas.drawRect(0, 0, output.getWidth(), output.getHeight(), paint);
        return output;
    }

    private Bitmap applyPosterize(Bitmap src, int levels) {
        Bitmap output = src.copy(Bitmap.Config.ARGB_8888, true);
        int width = output.getWidth();
        int height = output.getHeight();
        int[] pixels = new int[width * height];
        output.getPixels(pixels, 0, width, 0, 0, width, height);

        int step = Math.max(1, 256 / levels);
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int r = (Color.red(pixel) / step) * step;
            int g = (Color.green(pixel) / step) * step;
            int b = (Color.blue(pixel) / step) * step;
            pixels[index] = Color.argb(Color.alpha(pixel), r, g, b);
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height);
        return output;
    }

    private Bitmap applyColorMatrix(Bitmap src, ColorMatrix colorMatrix) {
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(src, 0, 0, paint);
        return output;
    }

    private void setupFilters() {
        mPhotoFilters.add(PhotoFilter.NONE);
        mPhotoFilters.add(PhotoFilter.AUTO_FIX);
        mPhotoFilters.add(PhotoFilter.BRIGHTNESS);
        mPhotoFilters.add(PhotoFilter.CONTRAST);
        mPhotoFilters.add(PhotoFilter.DOCUMENTARY);
        mPhotoFilters.add(PhotoFilter.DUE_TONE);
        mPhotoFilters.add(PhotoFilter.FILL_LIGHT);
        mPhotoFilters.add(PhotoFilter.FISH_EYE);
        mPhotoFilters.add(PhotoFilter.GRAIN);
        mPhotoFilters.add(PhotoFilter.GRAY_SCALE);
        mPhotoFilters.add(PhotoFilter.LOMISH);
        mPhotoFilters.add(PhotoFilter.NEGATIVE);
        mPhotoFilters.add(PhotoFilter.POSTERIZE);
        mPhotoFilters.add(PhotoFilter.SATURATE);
        mPhotoFilters.add(PhotoFilter.SEPIA);
        mPhotoFilters.add(PhotoFilter.SHARPEN);
        mPhotoFilters.add(PhotoFilter.TEMPERATURE);
        mPhotoFilters.add(PhotoFilter.TINT);
        mPhotoFilters.add(PhotoFilter.VIGNETTE);
        mPhotoFilters.add(PhotoFilter.CROSS_PROCESS);
        mPhotoFilters.add(PhotoFilter.BLACK_WHITE);
    }
}
