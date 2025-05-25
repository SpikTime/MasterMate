package com.example.mastermate.adapters;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.models.Review;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }


    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        if (reviewList == null || position < 0 || position >= reviewList.size()) {
            Log.e("ReviewAdapter", "Invalid position or reviewList is null/empty.");
            return;
        }
        Review review = reviewList.get(position);
        if (review == null) {
            Log.e("ReviewAdapter", "Review object at position " + position + " is null.");
            return;
        }

        holder.reviewTextTextView.setText(review.getText() != null ? review.getText() : "");
        holder.reviewRatingBar.setRating(review.getRating());

        String userId = review.getUserId();
        if (review.getUserName() != null && !review.getUserName().isEmpty()) {
            holder.reviewUserNameTextView.setText(review.getUserName());
        } else if (userId != null && !userId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (holder.getAdapterPosition() == position) {
                        String userName = snapshot.getValue(String.class);
                        holder.reviewUserNameTextView.setText(userName != null && !userName.isEmpty() ? userName : "Пользователь");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (holder.getAdapterPosition() == position) {
                        holder.reviewUserNameTextView.setText("Ошибка имени");
                    }
                    Log.e("ReviewAdapter", "Failed to load user name: " + error.getMessage());
                }
            });
        } else {
            holder.reviewUserNameTextView.setText("Аноним");
        }

        if (holder.reviewDateTextView != null) {
            long timestamp = review.getTimestampLong();
            if (timestamp > 0) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    holder.reviewDateTextView.setText(sdf.format(new Date(timestamp)));
                    holder.reviewDateTextView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    holder.reviewDateTextView.setText("");
                    holder.reviewDateTextView.setVisibility(View.GONE);
                }
            } else {
                holder.reviewDateTextView.setText("Недавно");
                holder.reviewDateTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }
    public void updateReviews(List<Review> newReviewList) {
        this.reviewList.clear();
        if (newReviewList != null) {
            this.reviewList.addAll(newReviewList);
        }
        notifyDataSetChanged();
    }


    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewUserNameTextView;
        TextView reviewTextTextView;
        RatingBar reviewRatingBar;
        TextView reviewDateTextView;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewUserNameTextView = itemView.findViewById(R.id.reviewUserNameTextView);
            reviewTextTextView = itemView.findViewById(R.id.reviewTextTextView);
            reviewRatingBar = itemView.findViewById(R.id.reviewRatingBar);
            reviewDateTextView = itemView.findViewById(R.id.reviewDateTextView);
        }
    }
}