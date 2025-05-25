package com.example.mastermate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mastermate.R;
import com.example.mastermate.models.ServiceItem;
import com.google.android.material.button.MaterialButton;


import java.util.List;

public class EditableServiceAdapter extends RecyclerView.Adapter<EditableServiceAdapter.ServiceViewHolder> {

    private Context context;
    private List<ServiceItem> serviceList;
    private ServiceActionsListener actionsListener;

    public interface ServiceActionsListener {
        void onEditService(ServiceItem serviceItem);
        void onDeleteService(ServiceItem serviceItem);
    }

    public EditableServiceAdapter(Context context, List<ServiceItem> serviceList, ServiceActionsListener listener) {
        this.context = context;
        this.serviceList = serviceList;
        this.actionsListener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_editable_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        ServiceItem service = serviceList.get(position);
        if (service == null) return;

        holder.serviceNameTextView.setText(service.getServiceName() != null ? service.getServiceName() : "Без названия");
        holder.servicePriceTextView.setText(service.getPriceDisplayString());

        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            holder.serviceDescriptionTextView.setText(service.getDescription());
            holder.serviceDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.serviceDescriptionTextView.setVisibility(View.GONE);
        }

        holder.editButton.setOnClickListener(v -> {
            if (actionsListener != null) {
                actionsListener.onEditService(service);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (actionsListener != null) {
                actionsListener.onDeleteService(service);
            }
        });
    }

    @Override
    public int getItemCount() {
        return serviceList != null ? serviceList.size() : 0;
    }

    // ViewHolder
    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView serviceNameTextView;
        TextView servicePriceTextView;
        TextView serviceDescriptionTextView;
        MaterialButton editButton;
        MaterialButton deleteButton;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceNameTextView = itemView.findViewById(R.id.serviceNameTextView);
            servicePriceTextView = itemView.findViewById(R.id.servicePriceTextView);
            serviceDescriptionTextView = itemView.findViewById(R.id.serviceDescriptionTextView);
            editButton = itemView.findViewById(R.id.editServiceButton);
            deleteButton = itemView.findViewById(R.id.deleteServiceButton);
        }
    }
}