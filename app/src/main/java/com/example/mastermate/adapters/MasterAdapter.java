package com.example.mastermate.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mastermate.R;
import com.example.mastermate.models.Master;
import com.example.mastermate.models.StatusInfo;
import com.example.mastermate.utils.WorkStatusHelper;

import java.util.List;

public class MasterAdapter extends RecyclerView.Adapter<MasterAdapter.MasterViewHolder> {

    private static final String TAG = "MasterAdapter";
    private Context context;
    private List<Master> masterList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Master master);
    }

    public MasterAdapter(Context context, List<Master> masterList, OnItemClickListener listener) {
        this.context = context;
        this.masterList = masterList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MasterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_master, parent, false);
        return new MasterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MasterViewHolder holder, int position) {
        Master master = masterList.get(position);
        if (master == null) {
            Log.w(TAG, "Master object at position " + position + " is null.");
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);
        holder.masterNameTextView.setText(master.getName() != null ? master.getName() : "Имя не указано");

        holder.masterSpecializationTextView.setText(master.getSpecializationsString());
        holder.masterRatingBar.setRating((float) master.getRating());

        Glide.with(context)
                .load(master.getImageUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .override(128, 128)
                .centerCrop()
                .into(holder.masterImageView);

        if (holder.masterStatusTextView != null) {
            StatusInfo statusInfo = WorkStatusHelper.getCurrentStatus(master.getWorkingHours());

            if (statusInfo != null) {
                holder.masterStatusTextView.setText(statusInfo.getStatusText());
                try {
                    int statusColor = ContextCompat.getColor(context, statusInfo.getStatusColorRes());
                    int iconTintColor = ContextCompat.getColor(context, statusInfo.getIconTintRes());
                    holder.masterStatusTextView.setTextColor(statusColor);

                    Drawable[] drawables = holder.masterStatusTextView.getCompoundDrawablesRelative();
                    Drawable startDrawable = drawables[0];
                    if (startDrawable != null) {
                        Drawable wrappedDrawable = DrawableCompat.wrap(startDrawable.mutate());
                        DrawableCompat.setTint(wrappedDrawable, iconTintColor);

                    }
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Color resource not found for status: " + statusInfo.getStatusText(), e);
                    holder.masterStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.textColorSecondaryLight));

                    Drawable[] drawables = holder.masterStatusTextView.getCompoundDrawablesRelative();
                    Drawable startDrawable = drawables[0];
                    if (startDrawable != null) {
                        Drawable wrappedDrawable = DrawableCompat.wrap(startDrawable.mutate());
                        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, R.color.textColorSecondaryLight));
                    }
                }
                holder.masterStatusTextView.setVisibility(View.VISIBLE);
            } else {
                holder.masterStatusTextView.setVisibility(View.GONE);
            }
        } else {
            Log.w(TAG, "masterStatusTextView is null for position " + position);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(master);
            }
        });
    }

    @Override
    public int getItemCount() {
        return masterList != null ? masterList.size() : 0;
    }

    public static class MasterViewHolder extends RecyclerView.ViewHolder {
        ImageView masterImageView;
        TextView masterNameTextView;
        TextView masterSpecializationTextView;
        TextView masterStatusTextView;
        RatingBar masterRatingBar;

        public MasterViewHolder(@NonNull View itemView) {
            super(itemView);
            masterImageView = itemView.findViewById(R.id.masterImageView);
            masterNameTextView = itemView.findViewById(R.id.masterNameTextView);
            masterSpecializationTextView = itemView.findViewById(R.id.masterSpecializationTextView);
            masterStatusTextView = itemView.findViewById(R.id.masterStatusTextView);
            masterRatingBar = itemView.findViewById(R.id.masterRatingBar);
        }
    }

    public void updateMasters(List<Master> newMasterList) {
        this.masterList.clear();
        if (newMasterList != null) {
            this.masterList.addAll(newMasterList);
        }
        notifyDataSetChanged();
        Log.d(TAG, "Adapter data updated. New size: " + getItemCount());
    }
}