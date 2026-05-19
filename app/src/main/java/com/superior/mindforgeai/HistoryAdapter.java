package com.superior.mindforgeai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> items;
    private final OnItemClickListener listener;
    private int lastPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(HistoryEntity entity);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(HistoryEntity entity);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(HistoryEntity entity);
    }

    private OnItemLongClickListener longClickListener;
    private OnItemDeleteListener deleteListener;

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.longClickListener = l;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener l) {
        this.deleteListener = l;
    }

    public HistoryAdapter(List<Object> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<HistoryEntity> entities) {
        List<Object> grouped = new ArrayList<>();
        String currentHeader = null;

        for (HistoryEntity entity : entities) {
            String dateStr = getHeaderLabel(entity.getCreatedAt());

            if (!dateStr.equals(currentHeader)) {
                currentHeader = dateStr;
                grouped.add(dateStr);
            }
            grouped.add(entity);
        }
        this.items = grouped;
        lastPosition = -1; // Reset animation state
        notifyDataSetChanged();
    }

    private String getHeaderLabel(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long days = diff / (1000 * 60 * 60 * 24);

        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        if (days < 30) return "Last week";
        return "Older";
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        setAnimation(holder.itemView, position);
        
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) items.get(position));
        } else {
            HistoryEntity entity = (HistoryEntity) items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.tvTopic.setText(entity.getTopic());
            String detail = entity.getStyle() + " • " + getTimeAgo(entity.getCreatedAt());
            itemHolder.tvDate.setText(detail);

            itemHolder.itemView.setOnClickListener(v -> listener.onItemClick(entity));
            itemHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onItemLongClick(entity);
                return true;
            });
            itemHolder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onItemDelete(entity);
            });
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.fall_down);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public HistoryEntity getEntity(int position) {
        Object item = items.get(position);
        return item instanceof HistoryEntity ? (HistoryEntity) item : null;
    }

    public void removeAt(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = (TextView) itemView;
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDate;
        ImageButton btnDelete;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tvHistoryTopic);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}
