package com.example.mastermate.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mastermate.R;
import com.example.mastermate.models.Review;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class ReviewActivity extends AppCompatActivity {

    private RatingBar reviewRatingBar;
    private EditText reviewEditText;
    private TextInputLayout reviewInputLayout;
    private Button submitReviewButton;
    private Toolbar toolbar;
    private ProgressBar submitReviewProgressBar;

    private String masterId;
    private String orderId;
    private FirebaseAuth mAuth;
    private DatabaseReference masterNodeRef;
    private DatabaseReference masterReviewsRef;
    private DatabaseReference orderNodeRef;

    private static final String TAG = "ReviewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        masterId = getIntent().getStringExtra("masterId");
        orderId = getIntent().getStringExtra("orderId");

        if (masterId == null || masterId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID мастера не найден.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Master ID is null or empty in Intent extras.");
            finish();
            return;
        }
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID заказа не найден.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Order ID is null or empty in Intent extras.");
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        masterNodeRef = database.getReference("users").child(masterId);
        masterReviewsRef = masterNodeRef.child("reviews");
        orderNodeRef = database.getReference("orders").child(orderId);

        initializeUI();
        setupToolbar();
        setupListeners();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar_review);
        reviewRatingBar = findViewById(R.id.reviewRatingBar);
        reviewInputLayout = findViewById(R.id.reviewInputLayout);
        reviewEditText = findViewById(R.id.reviewEditText);
        submitReviewButton = findViewById(R.id.submitReviewButton);
        submitReviewProgressBar = findViewById(R.id.submitReviewProgressBar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Оставить отзыв");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        } else {
            setTitle("Оставить отзыв");
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupListeners() {
        if (reviewRatingBar != null && reviewInputLayout != null) {
            reviewRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (rating <= 3.0f && rating > 0.1f) {
                    reviewInputLayout.setHint("Пожалуйста, опишите причину (обязательно)");
                } else {
                    reviewInputLayout.setHint("Ваш комментарий (необязательно)");
                }
            });
        }

        if (submitReviewButton != null) {
            submitReviewButton.setOnClickListener(v -> attemptSubmitReview());
        }
    }

    private void attemptSubmitReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Вы должны войти, чтобы оставить отзыв", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = reviewRatingBar.getRating();
        String text = reviewEditText.getText().toString().trim();
        String userId = currentUser.getUid();

        if (rating < 0.5f) {
            Toast.makeText(this, "Пожалуйста, поставьте оценку", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating <= 3.0f && TextUtils.isEmpty(text)) {
            reviewInputLayout.setError("При низкой оценке необходимо оставить комментарий.");
            reviewInputLayout.requestFocus();
            Toast.makeText(this, "Пожалуйста, опишите причину низкой оценки", Toast.LENGTH_LONG).show();
            return;
        } else if (reviewInputLayout != null) {
            reviewInputLayout.setError(null);
        }

        showSubmittingState(true);
        Log.d(TAG, "Attempting to submit review for masterId: " + masterId + ", orderId: " + orderId);
        DatabaseReference userProfileRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userProfileRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                String userName = "Пользователь";
                if (nameSnapshot.exists() && nameSnapshot.getValue(String.class) != null && !nameSnapshot.getValue(String.class).isEmpty()) {
                    userName = nameSnapshot.getValue(String.class);
                } else if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()){
                    userName = currentUser.getDisplayName();
                } else if (currentUser.getEmail() != null) {
                    userName = currentUser.getEmail();
                }

                Log.d(TAG, "User name for review: " + userName);
                Review review = new Review(userId, userName, text, rating, orderId);

                sendReviewToFirebase(review, rating);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch user name for review", databaseError.toException());
                Toast.makeText(ReviewActivity.this, "Ошибка получения данных пользователя.", Toast.LENGTH_SHORT).show();
                showSubmittingState(false);
            }
        });
    }

    private void onReviewSuccessfullySubmitted() {
        Log.i(TAG, "Review submitted and master rating updated successfully.");
        Toast.makeText(ReviewActivity.this, "Отзыв отправлен и рейтинг обновлен!", Toast.LENGTH_SHORT).show();
        setOrderAsReviewed();
        finish();
    }


    private void sendReviewToFirebase(Review review, float newRatingValue) {
        if (masterReviewsRef == null) {
            Log.e(TAG, "masterReviewsRef is null. Cannot send review.");
            Toast.makeText(this, "Ошибка сохранения отзыва.", Toast.LENGTH_SHORT).show();
            showSubmittingState(false);
            return;
        }
        String reviewId = masterReviewsRef.push().getKey();
        if (reviewId == null) {
            Log.e(TAG, "Failed to generate a unique ID for the review.");
            Toast.makeText(this, "Не удалось создать ID для отзыва.", Toast.LENGTH_SHORT).show();
            showSubmittingState(false);
            return;
        }

        masterReviewsRef.child(reviewId).setValue(review)
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (task.isSuccessful()) {
                        Log.i(TAG, "Review data written successfully to /users/" + masterId + "/reviews/" + reviewId);
                        updateMasterRatingWithTransaction(newRatingValue);
                        setOrderAsReviewed();
                    } else {
                        Log.e(TAG, "Failed to write review data", task.getException());
                        Toast.makeText(ReviewActivity.this, "Ошибка при отправке отзыва: " + safeGetErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        showSubmittingState(false);
                    }
                });
    }

    private void setOrderAsReviewed() {
        if (orderId == null || orderId.isEmpty() || orderNodeRef == null) {
            Log.e(TAG, "Cannot mark order as reviewed: orderId or orderNodeRef is null.");
            return;
        }
        orderNodeRef.child("clientReviewLeft").setValue(true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Order " + orderId + " marked as reviewed (clientReviewLeft=true)."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark order " + orderId + " as reviewed.", e));
    }

    private void updateMasterRatingWithTransaction(float newRating) {
        if (masterNodeRef == null) {
            Log.e(TAG, "masterNodeRef is null. Cannot update rating.");
            showSubmittingState(false);
            finish();
            return;
        }
        Log.d(TAG, "Starting rating update transaction for master: " + masterId);

        masterNodeRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {

                Long currentReviewCount = mutableData.child("reviewCount").getValue(Long.class);
                Double currentRatingSum = mutableData.child("ratingSum").getValue(Double.class);

                long reviewCount = (currentReviewCount != null) ? currentReviewCount : 0L;
                double ratingSum = (currentRatingSum != null) ? currentRatingSum : 0.0;

                reviewCount++;
                ratingSum += newRating;

                double newAverageRating = (reviewCount > 0) ? ratingSum / reviewCount : 0.0;
                newAverageRating = Math.round(newAverageRating * 10.0) / 10.0;

                Log.d(TAG, "Transaction: OldCount=" + (currentReviewCount != null ? currentReviewCount:0) + ", OldSum=" + (currentRatingSum != null ? currentRatingSum:0.0));
                Log.d(TAG, "Transaction: NewRating=" + newRating + ", NewCount=" + reviewCount + ", NewSum=" + ratingSum + ", NewAvg=" + newAverageRating);

                mutableData.child("reviewCount").setValue(reviewCount);
                mutableData.child("ratingSum").setValue(ratingSum);
                mutableData.child("rating").setValue(newAverageRating);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                showSubmittingState(false);

                if (committed && databaseError == null) {
                    onReviewSuccessfullySubmitted();
                    Log.i(TAG, "Master rating transaction committed successfully.");
                    Toast.makeText(ReviewActivity.this, "Отзыв отправлен и рейтинг обновлен!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Master rating transaction failed/aborted. Committed=" + committed, databaseError != null ? databaseError.toException() : null);
                    if (databaseError != null) {
                        Toast.makeText(ReviewActivity.this, "Не удалось обновить рейтинг мастера: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ReviewActivity.this, "Не удалось обновить рейтинг (данные могли измениться). Попробуйте еще раз.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void showSubmittingState(boolean isSubmitting) {
        if (isFinishing() || isDestroyed()) return;
        if (submitReviewProgressBar != null) {
            submitReviewProgressBar.setVisibility(isSubmitting ? View.VISIBLE : View.GONE);
        }
        if (submitReviewButton != null) {
            submitReviewButton.setEnabled(!isSubmitting);
        }
        if (reviewRatingBar != null) reviewRatingBar.setEnabled(!isSubmitting);
        if (reviewEditText != null) reviewEditText.setEnabled(!isSubmitting);
    }

    private String safeGetErrorMessage(Exception e) {
        return (e != null && e.getMessage() != null) ? e.getMessage() : "Неизвестная ошибка.";
    }
}