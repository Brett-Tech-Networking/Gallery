package com.bretttech.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdjustmentsAdapter extends RecyclerView.Adapter<AdjustmentsAdapter.ViewHolder> {

    public static class AdjustmentTool {
        public final String name;
        public final int icon;
        public boolean isSelected;

        public AdjustmentTool(String name, int icon, boolean isSelected) {
            this.name = name;
            this.icon = icon;
            this.isSelected = isSelected;
        }
    }

    private final List<AdjustmentTool> tools;
    private final OnToolClickListener listener;
    private final Context context;

    public interface OnToolClickListener {
        void onToolClick(AdjustmentTool tool);
    }

    public AdjustmentsAdapter(Context context, List<AdjustmentTool> tools, OnToolClickListener listener) {
        this.context = context;
        this.tools = tools;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adjustment_tool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdjustmentTool tool = tools.get(position);
        holder.name.setText(tool.name);
        holder.icon.setImageResource(tool.icon);

        if (tool.isSelected) {
            holder.name.setTextColor(ContextCompat.getColor(context, R.color.teal_200));
            holder.icon.setColorFilter(ContextCompat.getColor(context, R.color.teal_200));
            holder.background.setBackgroundResource(R.drawable.adjustment_tool_selected_bg);
        } else {
            holder.name.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            holder.icon.setColorFilter(ContextCompat.getColor(context, android.R.color.white));
            holder.background.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) return;
            listener.onToolClick(tools.get(holder.getAdapterPosition()));
            for (int i = 0; i < tools.size(); i++) {
                tools.get(i).isSelected = (i == holder.getAdapterPosition());
            }
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return tools.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        FrameLayout background;
        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.adjustment_icon);
            name = itemView.findViewById(R.id.adjustment_name);
            background = itemView.findViewById(R.id.adjustment_bg);
        }
    }
}