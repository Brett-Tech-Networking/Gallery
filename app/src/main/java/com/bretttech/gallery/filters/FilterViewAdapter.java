package com.bretttech.gallery.filters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.R;
import java.util.ArrayList;
import java.util.List;
import ja.burhanrashid52.photoeditor.PhotoFilter; // <-- CORRECTED IMPORT

public class FilterViewAdapter extends RecyclerView.Adapter<FilterViewAdapter.ViewHolder> {

    private final FilterListener mFilterListener;
    private final List<PhotoFilter> mPhotoFilters = new ArrayList<>();

    public FilterViewAdapter(FilterListener filterListener) {
        mFilterListener = filterListener;
        setupFilters();
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
    }

    @Override
    public int getItemCount() {
        return mPhotoFilters.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTxtFilterName;

        ViewHolder(View itemView) {
            super(itemView);
            mTxtFilterName = itemView.findViewById(R.id.filter_name);
            itemView.setOnClickListener(v -> mFilterListener.onFilterSelected(mPhotoFilters.get(getAdapterPosition())));
        }
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
        mPhotoFilters.add(PhotoFilter.FLIP_HORIZONTAL);
        mPhotoFilters.add(PhotoFilter.FLIP_VERTICAL);
        mPhotoFilters.add(PhotoFilter.ROTATE);
    }
}