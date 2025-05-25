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

import java.util.List;

public class ServiceDisplayAdapter extends RecyclerView.Adapter<ServiceDisplayAdapter.ServiceViewHolder> {

    private Context context;
    private List<ServiceItem> serviceList;

    public ServiceDisplayAdapter(Context context, List<ServiceItem> serviceList /*, OnServiceItemClickListener listener */) {
        this.context = context;
        this.serviceList = serviceList;
        // this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service_display, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        ServiceItem service = serviceList.get(position);
        if (service == null) return;

        holder.serviceNameTextView.setText(service.getServiceName() != null ? service.getServiceName() : "Услуга без названия");
        holder.servicePriceTextView.setText(service.getPriceDisplayString());

        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            holder.serviceDescriptionTextView.setText(service.getDescription());
            holder.serviceDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.serviceDescriptionTextView.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return serviceList != null ? serviceList.size() : 0;
    }
    public void updateServices(List<ServiceItem> newServiceList) {
        this.serviceList.clear();
        if (newServiceList != null) {
            this.serviceList.addAll(newServiceList);
        }
        notifyDataSetChanged();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView serviceNameTextView;
        TextView servicePriceTextView;
        TextView serviceDescriptionTextView;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceNameTextView = itemView.findViewById(R.id.serviceDisplayNameTextView);
            servicePriceTextView = itemView.findViewById(R.id.serviceDisplayPriceTextView);
            serviceDescriptionTextView = itemView.findViewById(R.id.serviceDisplayDescriptionTextView);
        }
    }
}