package com.example.mastermate.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils; // Добавлен импорт
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout; // Нужен для поиска родителя чипа
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat; // Нужен для tint иконки
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mastermate.R;
import com.example.mastermate.adapters.ReviewAdapter;
import com.example.mastermate.models.Review;
import com.example.mastermate.models.WorkingDay; // Нужен для карты workingHours
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap; // Нужен для workingHours
import java.util.List;
import java.util.Map; // Нужен для workingHours
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileMasterActivity extends BaseActivity {

    private static final String TAG = "ProfileMasterActivity";

    private CircleImageView profileImageView;
    private TextView tvCompletedOrdersCount;
    private TextView nameValueTextView, specializationValueTextView, descriptionValueTextView;
    private TextView phoneValueTextView;
    private TextView emailStatusTextView;
    private TextView addressValueTextView;
    private Button editProfileButton;
    private ImageView phoneVerifiedIcon;
    private Button verifyPhoneButton;
    private RecyclerView reviewsRecyclerView;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();
    private TextView reviewsTitleTextView;

    private FirebaseAuth mAuth;
    private MaterialButton verifyEmailButton;
    private FirebaseUser firebaseCurrentUser;

    private CountDownTimer resendEmailTimer;
    private boolean isResendTimerRunning = false;
    private final long RESEND_TIMEOUT_MILLISECONDS = 60000;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    private Button btnEditServices;

    private String masterPhoneNumberForVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_master);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseCurrentUser == null && mAuth != null) {
            firebaseCurrentUser = mAuth.getCurrentUser();
        }

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            initializeUI();
            if (reviewsRecyclerView != null) {
                setupRecyclerView();
            }
            setupButtonListeners();
        } else {
            Toast.makeText(this, "Ошибка аутентификации", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userRef != null) {
        }
        if (firebaseCurrentUser != null) {
            firebaseCurrentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseCurrentUser != null) {
                        updateEmailVerificationStatus();
                        if (userRef != null && (nameValueTextView.getText().toString().equals("Нет данных") || nameValueTextView.getText().toString().isEmpty())) {
                            loadUserData();
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to reload user.", task.getException());
                }
            });
        }
    }
    private void initializeUI() {
        profileImageView = findViewById(R.id.profileImageView);
        nameValueTextView = findViewById(R.id.nameValueTextView);
        specializationValueTextView = findViewById(R.id.specializationValueTextView);
        descriptionValueTextView = findViewById(R.id.descriptionValueTextView);
        phoneValueTextView = findViewById(R.id.phoneValueTextView);
        addressValueTextView = findViewById(R.id.addressValueTextView);
        editProfileButton = findViewById(R.id.editProfileButton);
        phoneVerifiedIcon = findViewById(R.id.phoneVerifiedIcon);
        emailStatusTextView = findViewById(R.id.emailStatusTextView);
        verifyEmailButton = findViewById(R.id.verifyEmailButton);
        verifyPhoneButton = findViewById(R.id.verifyPhoneButton);
        tvCompletedOrdersCount = findViewById(R.id.tvCompletedOrdersCount);
        btnEditServices = findViewById(R.id.btnEditServices);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsTitleTextView = findViewById(R.id.reviewsTitleTextView);
    }

    private void setupRecyclerView() {
        if (reviewsRecyclerView == null) {
            Log.e(TAG, "RecyclerView (reviewsRecyclerView) is null in setupRecyclerView. Check layout ID.");
            return;
        }
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setHasFixedSize(false);
        reviewsRecyclerView.setNestedScrollingEnabled(false);
        reviewAdapter = new ReviewAdapter(this, reviewList);
        reviewsRecyclerView.setAdapter(reviewAdapter);
        com.google.android.material.divider.MaterialDividerItemDecoration divider =
                new com.google.android.material.divider.MaterialDividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        reviewsRecyclerView.addItemDecoration(divider);
    }

    private void setupButtonListeners() {
        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileMasterActivity.this, EditProfileMasterActivity.class);
                startActivity(intent);
            });
        }
        if (verifyPhoneButton != null) {
            verifyPhoneButton.setOnClickListener(v -> startPhoneVerification());
        }

        if (btnEditServices != null) {
            btnEditServices.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileMasterActivity.this, EditServicesActivity.class);
                startActivity(intent);
            });
        }

        if (verifyEmailButton != null) {
            verifyEmailButton.setOnClickListener(v -> sendVerificationEmail());
        }
    }

    private void sendVerificationEmail() {
        if (firebaseCurrentUser == null) {
            Toast.makeText(this, "Ошибка: пользователь не определен.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseCurrentUser.isEmailVerified()) {
            Toast.makeText(this, "Ваш email уже подтвержден.", Toast.LENGTH_SHORT).show();
            updateEmailVerificationStatus();
            return;
        }

        if (isResendTimerRunning) {
            Toast.makeText(this, "Пожалуйста, подождите перед повторной отправкой.", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyEmailButton.setEnabled(false);
        Toast.makeText(this, "Отправка письма...", Toast.LENGTH_SHORT).show();

        firebaseCurrentUser.sendEmailVerification()
                .addOnCompleteListener(this, task -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email verification sent.");
                        Toast.makeText(ProfileMasterActivity.this,
                                "Письмо для подтверждения отправлено на " + firebaseCurrentUser.getEmail() + ". Проверьте почту (включая папку 'Спам').",
                                Toast.LENGTH_LONG).show();
                        startResendEmailTimer();
                    } else {
                        Log.e(TAG, "sendEmailVerification", task.getException());
                        Toast.makeText(ProfileMasterActivity.this,
                                "Не удалось отправить письмо: " + (task.getException() != null ? task.getException().getMessage() : "Попробуйте позже."),
                                Toast.LENGTH_LONG).show();
                        if (verifyEmailButton != null) verifyEmailButton.setEnabled(true);
                    }
                });
    }

    private void startResendEmailTimer() {
        if (verifyEmailButton == null) return;

        isResendTimerRunning = true;
        verifyEmailButton.setEnabled(false);
        verifyEmailButton.setVisibility(View.GONE);

        resendEmailTimer = new CountDownTimer(RESEND_TIMEOUT_MILLISECONDS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                isResendTimerRunning = false;
                if (!isFinishing() && !isDestroyed() && verifyEmailButton != null) {
                    if (firebaseCurrentUser != null) {
                        firebaseCurrentUser.reload().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
                            updateEmailVerificationStatus();
                        });
                    } else {
                        updateEmailVerificationStatus();
                    }
                }
            }
        }.start();
    }

    private void loadUserData() {
        if (userRef == null) {
            Log.e(TAG, "userRef is null, cannot load user data.");
            Toast.makeText(this, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Loading user data for " + userRef.getKey());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    Log.d(TAG, "Snapshot exists, updating UI.");
                    com.example.mastermate.models.Master master = snapshot.getValue(com.example.mastermate.models.Master.class);

                    if (master != null) {
                        nameValueTextView.setText(master.getName() != null ? master.getName() : "Нет данных");
                        specializationValueTextView.setText(!master.getSpecializations().isEmpty() ? TextUtils.join(", ", master.getSpecializations()) : (master.getSpecialization() != null && !master.getSpecialization().isEmpty() ? master.getSpecialization() : "Не указаны"));
                        descriptionValueTextView.setText(master.getDescription() != null ? master.getDescription() : "Нет данных");
                        addressValueTextView.setText(master.getAddress() != null && !master.getAddress().isEmpty() ? master.getAddress() : "Адрес не указан");

                        if (phoneValueTextView != null) {
                            phoneValueTextView.setText(master.getPhoneNumber() != null && !master.getPhoneNumber().isEmpty() ? master.getPhoneNumber() : "Не указан");
                        }
                        masterPhoneNumberForVerification = master.getPhoneNumber();
                        updateVerificationUI(master.getPhoneNumber(), master.isPhoneVerified());
                        if (tvCompletedOrdersCount != null) {
                            tvCompletedOrdersCount.setText(String.valueOf(master.getCompletedOrdersCount()));
                            Log.d(TAG, "ProfileMasterActivity - Completed orders: " + master.getCompletedOrdersCount());
                        } else {
                            Log.e(TAG, "ProfileMasterActivity - tvCompletedOrdersCount is NULL!");
                        }

                        if (reviewsRecyclerView != null) {
                            loadReviews(snapshot.child("reviews"));
                        }

                        if (master.getImageUrl() != null && !master.getImageUrl().isEmpty() && profileImageView != null) {
                            Glide.with(ProfileMasterActivity.this)
                                    .load(master.getImageUrl())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(profileImageView);
                        } else if (profileImageView != null) {
                            profileImageView.setImageResource(R.drawable.ic_person);
                        }

                    } else {
                        Log.e(TAG, "Failed to parse Master object from snapshot for user: " + userRef.getKey());
                        Toast.makeText(ProfileMasterActivity.this, "Ошибка данных профиля.", Toast.LENGTH_SHORT).show();
                        clearProfileFields();
                    }
                } else {
                    Log.w(TAG, "User data snapshot does not exist for user: " + userRef.getKey());
                    Toast.makeText(ProfileMasterActivity.this, "Данные профиля не найдены", Toast.LENGTH_SHORT).show();
                    clearProfileFields();}

                    updateEmailVerificationStatus();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Ошибка загрузки профиля", error.toException());
                Toast.makeText(ProfileMasterActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseCurrentUser != null) {
            Log.d(TAG, "onResume: Reloading user for email verification status.");
            firebaseCurrentUser.reload().addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;
                if (task.isSuccessful()) {
                    firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseCurrentUser != null) {
                        Log.d(TAG, "User reloaded. Email verified: " + firebaseCurrentUser.isEmailVerified());
                        updateEmailVerificationStatus();
                    }
                } else {
                    Log.e(TAG, "onResume: Failed to reload user.", task.getException());
                }
            });
        } else {
            Log.w(TAG, "onResume: firebaseCurrentUser is null.");
            mAuth = FirebaseAuth.getInstance();
            firebaseCurrentUser = mAuth.getCurrentUser();
            if(firebaseCurrentUser != null) {
                onResume();
            } else {
                updateEmailVerificationStatus();
            }
        }
    }



    private void clearProfileFields() {
        if (nameValueTextView != null) nameValueTextView.setText("Нет данных");
        if (specializationValueTextView != null) specializationValueTextView.setText("Не указаны");
        if (descriptionValueTextView != null) descriptionValueTextView.setText("Нет данных");
        if (addressValueTextView != null) addressValueTextView.setText("Адрес не указан");
        if (phoneValueTextView != null) phoneValueTextView.setText("Не указан");
        if (profileImageView != null) profileImageView.setImageResource(R.drawable.ic_person);
        if (phoneVerifiedIcon != null) phoneVerifiedIcon.setVisibility(View.GONE);
        if (verifyPhoneButton != null) verifyPhoneButton.setVisibility(View.GONE);

        if (reviewList != null) reviewList.clear();
        if (reviewAdapter != null) reviewAdapter.notifyDataSetChanged();
        if (reviewsTitleTextView != null) reviewsTitleTextView.setVisibility(View.GONE);
        if (reviewsRecyclerView != null) reviewsRecyclerView.setVisibility(View.GONE);
    }

    private void loadReviews(DataSnapshot reviewsSnapshot) {
        if (reviewsRecyclerView == null || reviewAdapter == null || reviewsTitleTextView == null) {
            Log.w(TAG, "Review UI elements are null, cannot display reviews.");
            return;
        }

        if (!reviewsSnapshot.exists()) {
            reviewList.clear();
            reviewAdapter.notifyDataSetChanged();
            reviewsTitleTextView.setVisibility(View.GONE);
            reviewsRecyclerView.setVisibility(View.GONE);
            Log.d(TAG, "No reviews found for this master.");
            return;
        }

        reviewList.clear();
        long reviewCount = 0;
        for (DataSnapshot snapshot : reviewsSnapshot.getChildren()) {
            Review review = snapshot.getValue(Review.class);
            if (review != null) {
                reviewList.add(review);
                reviewCount++;
            }
        }
        reviewAdapter.notifyDataSetChanged();
        Log.d(TAG, "Loaded " + reviewCount + " reviews.");

        boolean hasReviews = !reviewList.isEmpty();
        reviewsTitleTextView.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
        reviewsRecyclerView.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
    }

    private void updateVerificationUI(String phoneNumber, boolean isVerified) {
        if (phoneVerifiedIcon == null || verifyPhoneButton == null) {
            Log.w(TAG, "Verification UI elements are null.");
            return;
        }

        boolean hasPhone = phoneNumber != null && !phoneNumber.isEmpty();

        if (hasPhone) {
            if (isVerified) {
                phoneVerifiedIcon.setImageResource(R.drawable.ic_verified);
                phoneVerifiedIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryBlue)));
                phoneVerifiedIcon.setVisibility(View.VISIBLE);
                verifyPhoneButton.setVisibility(View.GONE);
            } else {
                phoneVerifiedIcon.setImageResource(R.drawable.ic_warning);
                phoneVerifiedIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorErrorLight)));
                phoneVerifiedIcon.setVisibility(View.VISIBLE);
                verifyPhoneButton.setVisibility(View.VISIBLE);
            }
        } else {
            phoneVerifiedIcon.setVisibility(View.GONE);
            verifyPhoneButton.setVisibility(View.GONE);
        }
    }



    private void updateEmailVerificationStatus() {
        if (firebaseCurrentUser == null || isFinishing() || isDestroyed()) {
            if(verifyEmailButton != null) verifyEmailButton.setVisibility(View.GONE);
            if(emailStatusTextView != null) emailStatusTextView.setText("Email: (ошибка загрузки статуса)");
            return;
        }

        String email = firebaseCurrentUser.getEmail();
        boolean isEmailVerified = firebaseCurrentUser.isEmailVerified();

        if (emailStatusTextView != null) {
            if (email != null && !email.isEmpty()) {
                String statusText = isEmailVerified ? "(Подтвержден)" : "(Не подтвержден)";
                String fullEmailText = "Email: " + email + " " + statusText;
                emailStatusTextView.setText(fullEmailText);
                emailStatusTextView.setTextColor(ContextCompat.getColor(this,
                        isEmailVerified ? R.color.colorSuccess : R.color.colorWarning));
            } else {
                emailStatusTextView.setText("Email: не указан");
            }
        }

        if (verifyEmailButton != null) {
            if (isEmailVerified || isResendTimerRunning) {
                verifyEmailButton.setVisibility(View.GONE);
            } else {
                verifyEmailButton.setVisibility(View.VISIBLE);
                verifyEmailButton.setText("Подтвердить Email");
                verifyEmailButton.setEnabled(true);
            }
        }
    }
    private void startPhoneVerification() {
        if (currentUser == null) {
            Toast.makeText(this, "Ошибка: Пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        if (masterPhoneNumberForVerification != null && !masterPhoneNumberForVerification.isEmpty()) {
            Intent intent = new Intent(ProfileMasterActivity.this, VerifyPhoneActivity.class);
            intent.putExtra("phoneNumber", masterPhoneNumberForVerification);
            intent.putExtra("userId", currentUser.getUid());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Сначала укажите номер телефона в профиле.", Toast.LENGTH_SHORT).show();
            Intent editIntent = new Intent(ProfileMasterActivity.this, EditProfileMasterActivity.class);
            startActivity(editIntent);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendEmailTimer != null) {
            resendEmailTimer.cancel();
        }
    }
}