package com.example.mastermate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mastermate.R;
import com.example.mastermate.models.Order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SelectableOrderAdapter extends RecyclerView.Adapter<SelectableOrderAdapter.SelectableOrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private OnOrderSelectedListener listener;

    public interface OnOrderSelectedListener {
        void onOrderSelected(Order order);
    }

    public SelectableOrderAdapter(Context context, List<Order> orderList, OnOrderSelectedListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SelectableOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selectable_order_for_review, parent, false);
        return new SelectableOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectableOrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null) return;

        holder.masterNameTextView.setText(order.getMasterName() != null ? "Мастер: " + order.getMasterName() : "Мастер не указан");
        holder.problemDescTextView.setText(order.getProblemDescription() != null ? order.getProblemDescription() : "Описание отсутствует");
        long timestampToDisplay = 0;
        if (order.getClientConfirmationTimestampLong() > 0) {
            timestampToDisplay = order.getClientConfirmationTimestampLong();
        } else if (order.getCreationTimestampLong() > 0) {
            timestampToDisplay = order.getCreationTimestampLong();
        }

        if (timestampToDisplay > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                holder.dateTextView.setText("Заказ от: " + sdf.format(new Date(timestampToDisplay)));
            } catch (Exception e) {
                holder.dateTextView.setText("Дата не указана");
            }
        } else {
            holder.dateTextView.setText("Дата не указана");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderSelected(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateOrders(List<Order> newOrders) {
        this.orderList.clear();
        if (newOrders != null) {
            this.orderList.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    static class SelectableOrderViewHolder extends RecyclerView.ViewHolder {
        TextView masterNameTextView;
        TextView dateTextView;
        TextView problemDescTextView;

        public SelectableOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            masterNameTextView = itemView.findViewById(R.id.selectableOrderMasterNameTextView);
            dateTextView = itemView.findViewById(R.id.selectableOrderDateTextView);
            problemDescTextView = itemView.findViewById(R.id.selectableOrderProblemDescTextView);
        }
    }
}