// brett-tech-networking/gallery/Gallery-b8f232f66292c1bcf65ca17067d173829c633af8/app/src/main/java/com/bretttech/gallery/text/ColorPickerAdapter.java
package com.bretttech.gallery.text;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bretttech.gallery.R;
import java.util.ArrayList;
import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private final List<Integer> colorPickerColors;
    private OnColorPickerClickListener onColorPickerClickListener;

    public interface OnColorPickerClickListener {
        void onColorPickerClickListener(int colorCode);
    }

    public ColorPickerAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
        this.colorPickerColors = getDefaultColors(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.layout_color_picker_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.colorPickerView.setBackgroundColor(colorPickerColors.get(position));
    }

    @Override
    public int getItemCount() {
        return colorPickerColors.size();
    }

    public void setOnColorPickerClickListener(OnColorPickerClickListener onColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View colorPickerView;
        public ViewHolder(View itemView) {
            super(itemView);
            colorPickerView = itemView.findViewById(R.id.color_picker_view);
            itemView.setOnClickListener(v -> {
                if (onColorPickerClickListener != null) {
                    onColorPickerClickListener.onColorPickerClickListener(colorPickerColors.get(getAdapterPosition()));
                }
            });
        }
    }

    public static List<Integer> getDefaultColors(Context context) {
        ArrayList<Integer> colorPickerColors = new ArrayList<>();
        colorPickerColors.add(Color.parseColor("#ffffff"));
        colorPickerColors.add(Color.parseColor("#000000"));
        colorPickerColors.add(Color.parseColor("#EF5350"));
        colorPickerColors.add(Color.parseColor("#EC407A"));
        colorPickerColors.add(Color.parseColor("#AB47BC"));
        colorPickerColors.add(Color.parseColor("#7E57C2"));
        colorPickerColors.add(Color.parseColor("#5C6BC0"));
        colorPickerColors.add(Color.parseColor("#42A5F5"));
        colorPickerColors.add(Color.parseColor("#29B6F6"));
        colorPickerColors.add(Color.parseColor("#26C6DA"));
        colorPickerColors.add(Color.parseColor("#26A69A"));
        colorPickerColors.add(Color.parseColor("#66BB6A"));
        colorPickerColors.add(Color.parseColor("#9CCC65"));
        colorPickerColors.add(Color.parseColor("#D4E157"));
        colorPickerColors.add(Color.parseColor("#FFEE58"));
        colorPickerColors.add(Color.parseColor("#FFCA28"));
        colorPickerColors.add(Color.parseColor("#FFA726"));
        colorPickerColors.add(Color.parseColor("#FF7043"));
        colorPickerColors.add(Color.parseColor("#8D6E63"));
        colorPickerColors.add(Color.parseColor("#BDBDBD"));
        colorPickerColors.add(Color.parseColor("#78909C"));
        return colorPickerColors;
    }
}