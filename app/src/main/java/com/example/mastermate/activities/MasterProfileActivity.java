package com.example.mastermate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import com.example.mastermate.adapters.ServiceDisplayAdapter;
import com.example.mastermate.models.ServiceItem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mastermate.R;
import com.example.mastermate.adapters.ReviewAdapter;
import com.example.mastermate.models.Master;
import com.example.mastermate.models.Review;
import com.example.mastermate.models.WorkingDay;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MasterProfileActivity extends AppCompatActivity {

    private MaterialButton createOrderButton;

    private CircleImageView masterProfileImageView;
    private TextView masterProfileNameTextView;
    private TextView masterProfileSpecializationTextView;
    private RatingBar masterProfileRatingBar;
    private TextView masterProfileDescriptionTextView;
    private MaterialButton contactButton;
    private Toolbar toolbar;
    private MaterialButton leaveReviewButton;
    private MaterialButton addToFavoritesButton;
    private RecyclerView reviewsRecyclerView;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();
    private DatabaseReference masterNodeRef;
    private DatabaseReference reviewsRef;
    private DatabaseReference userFavoritesRef;
    private ValueEventListener favoriteStatusListener;
    private String masterId;
    private String masterPhoneNumber;
    private boolean isFavorite = false;
    private static final String TAG = "MasterProfileActivity";
    private static final int CALL_PHONE_PERMISSION_CODE = 101;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Handler dataLoadHandler;
    private TextView reviewsTitleTextView;
    private TextView tvCompletedOrdersCount;
    private LinearLayout workingHoursDisplayContainer;
    private TextView workingHoursSectionTitleTextView;
    private TextView mondayHoursTextView;
    private TextView tuesdayHoursTextView;
    private TextView wednesdayHoursTextView;

    private RecyclerView masterServicesRecyclerView;
    private ServiceDisplayAdapter serviceDisplayAdapter;
    private List<ServiceItem> masterServiceList;
    private TextView servicesTitleTextView;
    private TextView noServicesTextView;
    private View dividerBeforeServices;
    private TextView thursdayHoursTextView;
    private TextView fridayHoursTextView;
    private TextView saturdayHoursTextView;
    private TextView sundayHoursTextView;
    private Map<String, TextView> dayKeyToTextViewMap = new HashMap<>();
    private final List<String> displayDaysOrder = Arrays.asList(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    );

    private TextView masterCurrentStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_profile);

        dataLoadHandler = new Handler(Looper.getMainLooper());

        masterId = getIntent().getStringExtra("masterId");
        if (masterId == null || masterId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID мастера не найден", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Master ID is null or empty.");
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initializeUI();
        setupRecyclerViewForReviews();
        setupRecyclerViewForServices();
        initializeFirebaseReferences();
        initializeFirebaseReferences();
        setupToolbar();
        setupButtonListeners();
        scheduleDataLoad();
    }

    private void initializeUI() {
        createOrderButton = findViewById(R.id.createOrderButton);
        masterProfileImageView = findViewById(R.id.masterProfileImageView);
        masterProfileNameTextView = findViewById(R.id.masterProfileNameTextView);
        masterProfileSpecializationTextView = findViewById(R.id.masterProfileSpecializationTextView);
        masterProfileRatingBar = findViewById(R.id.masterProfileRatingBar);
        masterProfileDescriptionTextView = findViewById(R.id.masterProfileDescriptionTextView);
        contactButton = findViewById(R.id.contactButton);
        toolbar = findViewById(R.id.toolbar);
        tvCompletedOrdersCount = findViewById(R.id.tvCompletedOrdersCount);
        leaveReviewButton = findViewById(R.id.leaveReviewButton);
        addToFavoritesButton = findViewById(R.id.addToFavoritesButton);
        masterServicesRecyclerView = findViewById(R.id.masterServicesRecyclerView);
        servicesTitleTextView = findViewById(R.id.servicesTitleTextView);
        noServicesTextView = findViewById(R.id.noServicesTextView);
        dividerBeforeServices = findViewById(R.id.divider_before_services);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        tvCompletedOrdersCount = findViewById(R.id.tvCompletedOrdersCount);
        reviewsTitleTextView = findViewById(R.id.reviewsTitleTextView);

        workingHoursDisplayContainer = findViewById(R.id.workingHoursDisplayContainer);
        workingHoursSectionTitleTextView = findViewById(R.id.workingHoursSectionTitleTextView);
        mondayHoursTextView = findViewById(R.id.mondayHoursTextView);
        tuesdayHoursTextView = findViewById(R.id.tuesdayHoursTextView);
        wednesdayHoursTextView = findViewById(R.id.wednesdayHoursTextView);
        thursdayHoursTextView = findViewById(R.id.thursdayHoursTextView);
        fridayHoursTextView = findViewById(R.id.fridayHoursTextView);
        saturdayHoursTextView = findViewById(R.id.saturdayHoursTextView);
        sundayHoursTextView = findViewById(R.id.sundayHoursTextView);

        dayKeyToTextViewMap.put("monday", mondayHoursTextView);
        dayKeyToTextViewMap.put("tuesday", tuesdayHoursTextView);
        dayKeyToTextViewMap.put("wednesday", wednesdayHoursTextView);
        dayKeyToTextViewMap.put("thursday", thursdayHoursTextView);
        dayKeyToTextViewMap.put("friday", fridayHoursTextView);
        dayKeyToTextViewMap.put("saturday", saturdayHoursTextView);
        dayKeyToTextViewMap.put("sunday", sundayHoursTextView);

        masterCurrentStatusTextView = findViewById(R.id.masterCurrentStatusTextView);
    }

    private void setupRecyclerViewForReviews() {
        if (reviewsRecyclerView == null) return;
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setHasFixedSize(false);
        reviewsRecyclerView.setNestedScrollingEnabled(false);
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        reviewsRecyclerView.setAdapter(reviewAdapter);
    }

    private void setupRecyclerViewForServices() {
        if (masterServicesRecyclerView == null) {
            Log.e(TAG, "masterServicesRecyclerView is null. Cannot setup services display.");
            return;
        }
        masterServiceList = new ArrayList<>();
        serviceDisplayAdapter = new ServiceDisplayAdapter(this, masterServiceList);
        masterServicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        masterServicesRecyclerView.setHasFixedSize(false);
        masterServicesRecyclerView.setNestedScrollingEnabled(false);
        masterServicesRecyclerView.setAdapter(serviceDisplayAdapter);
    }

    private void initializeFirebaseReferences() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        masterNodeRef = database.getReference("users").child(masterId);
        reviewsRef = masterNodeRef.child("reviews");
        if (currentUser != null) {
            userFavoritesRef = database.getReference("users").child(currentUser.getUid()).child("favorites");
        } else {
            userFavoritesRef = null;
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }


    private void setupButtonListeners() {
        if (contactButton != null) {
            contactButton.setOnClickListener(v -> {
                if (masterPhoneNumber != null && !masterPhoneNumber.isEmpty()) {
                    requestCallPermissionOrCall();
                } else {
                    Toast.makeText(this, "Номер телефона не указан", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "Contact Button is null");
        }
        if (leaveReviewButton != null) {
            leaveReviewButton.setOnClickListener(v -> {
                if (currentUser != null) {
                    Intent intent = new Intent(MasterProfileActivity.this, ReviewActivity.class);
                    intent.putExtra("masterId", masterId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Войдите, чтобы оставить отзыв", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "Leave Review Button is null");
        }

        if (addToFavoritesButton != null) {
            addToFavoritesButton.setOnClickListener(v -> handleFavoriteButtonClick());
        } else {
            Log.w(TAG, "Add To Favorites Button is null");
        }

        if (createOrderButton != null) {
            createOrderButton.setOnClickListener(v -> {
                if (currentUser != null) {
                    DatabaseReference currentUserRoleRef = FirebaseDatabase.getInstance().getReference("users")
                            .child(currentUser.getUid()).child("role");
                    currentUserRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!isFinishing() && !isDestroyed()) {
                                String role = snapshot.getValue(String.class);
                                if ("master".equals(role)) {
                                    Toast.makeText(MasterProfileActivity.this, "Мастера не могут создавать заявки", Toast.LENGTH_SHORT).show();
                                } else {

                                    Intent intent = new Intent(MasterProfileActivity.this, CreateOrderActivity.class);
                                    intent.putExtra("masterId", masterId);
                                    startActivity(intent);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (!isFinishing() && !isDestroyed()) {
                                Log.e(TAG, "Failed to check current user role", error.toException());
                                Toast.makeText(MasterProfileActivity.this, "Не удалось проверить роль пользователя", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Войдите, чтобы создать заявку", Toast.LENGTH_SHORT).show();
                }
            });
            createOrderButton.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);
        } else {
            Log.w(TAG, "Create Order Button is null");
        }
    }

    private void scheduleDataLoad() {
        dataLoadHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Log.d(TAG, "Handler executing delayed data load...");
                loadMasterData();
                if (currentUser != null && userFavoritesRef != null) {
                    attachFavoriteStatusListener();
                } else {
                    updateFavoriteButtonStateForGuest();
                }
            } else {
                Log.w(TAG, "Handler delayed execution skipped: Activity is finishing or destroyed.");
            }
        }, 100);
    }

    private void loadMasterData() {
        if (masterNodeRef == null) {
            Log.e(TAG, "masterNodeRef is null in loadMasterData");
            Toast.makeText(MasterProfileActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "loadMasterData called for masterId: " + masterId);
        masterNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    Master master = dataSnapshot.getValue(Master.class);
                    if (master != null) {
                        loadMasterServices();
                        if (master.getId() == null || master.getId().isEmpty()) {
                            master.setId(masterId);
                        }
                        displayMasterData(master);
                        displayWorkingHours(master.getWorkingHours());
                        updateMasterCurrentStatus(master.getWorkingHours());
                        loadReviews();
                    } else {
                        Log.e(TAG, "Master data not found for ID: " + masterId);
                        Toast.makeText(MasterProfileActivity.this, "Ошибка: Мастер не найден", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isFinishing() && !isDestroyed()) {
                    Log.e(TAG, "loadMasterData:onCancelled", databaseError.toException());
                    Toast.makeText(MasterProfileActivity.this, "Ошибка загрузки данных мастера", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMasterServices() {
        if (masterNodeRef == null || serviceDisplayAdapter == null) {
            Log.e(TAG, "Cannot load services: masterNodeRef or serviceDisplayAdapter is null.");
            updateServicesVisibility(false);
            return;
        }

        DatabaseReference masterServicesDataRef = masterNodeRef.child("services");
        Log.d(TAG, "Loading services from: " + masterServicesDataRef.toString());

        masterServicesDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                List<ServiceItem> loadedServices = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot serviceSnap : dataSnapshot.getChildren()) {
                        ServiceItem service = serviceSnap.getValue(ServiceItem.class);
                        if (service != null) {
                            service.setServiceId(serviceSnap.getKey());
                            loadedServices.add(service);
                        }
                    }
                    Log.d(TAG, "Successfully loaded " + loadedServices.size() + " services for master: " + masterId);
                } else {
                    Log.d(TAG, "No services found in Firebase for master: " + masterId);
                }
                masterServiceList.clear();
                masterServiceList.addAll(loadedServices);
                serviceDisplayAdapter.notifyDataSetChanged();
                updateServicesVisibility(!masterServiceList.isEmpty());
                loadReviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Failed to load services for master: " + masterId, databaseError.toException());
                Toast.makeText(MasterProfileActivity.this, "Ошибка загрузки услуг", Toast.LENGTH_SHORT).show();
                updateServicesVisibility(false);
                loadReviews();
            }
        });
    }

    private void updateServicesVisibility(boolean hasServices) {
        if (isFinishing() || isDestroyed()) return;

        if (servicesTitleTextView != null) servicesTitleTextView.setVisibility(hasServices ? View.VISIBLE : View.GONE);
        if (masterServicesRecyclerView != null) masterServicesRecyclerView.setVisibility(hasServices ? View.VISIBLE : View.GONE);
        if (noServicesTextView != null) noServicesTextView.setVisibility(hasServices ? View.GONE : View.VISIBLE);
        if (dividerBeforeServices != null) dividerBeforeServices.setVisibility(hasServices ? View.VISIBLE : View.GONE);
        View dividerBeforeReviews = findViewById(R.id.divider_before_reviews);
        if (dividerBeforeReviews != null) {
            dividerBeforeReviews.setVisibility(hasServices ? View.VISIBLE : View.GONE);
        }
    }


    private void displayMasterData(Master master) {
        masterProfileNameTextView.setText(master.getName());
        masterProfileSpecializationTextView.setText(master.getSpecializationsString());
        masterProfileRatingBar.setRating(master.getRating() > 0 ? (float) master.getRating() : 0f);
        masterProfileDescriptionTextView.setText(master.getDescription() != null ? master.getDescription() : "Описание отсутствует.");

        if (tvCompletedOrdersCount != null) {
            if (master != null) {
                tvCompletedOrdersCount.setText(String.valueOf(master.getCompletedOrdersCount()));
                Log.d(TAG, "Displayed completed orders: " + master.getCompletedOrdersCount());
            } else {
                tvCompletedOrdersCount.setText("0");
                Log.w(TAG, "Master object was null in displayMasterData, cannot display completed orders count.");
            }
        } else {
            Log.e(TAG, "tvCompletedOrdersCount is null, cannot display completed orders count.");
        }


        String imageUrl = master.getImageUrl();
        if (masterProfileImageView != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(masterProfileImageView);
        }

        masterPhoneNumber = master.getPhoneNumber();
        if (contactButton != null) {
            contactButton.setEnabled(masterPhoneNumber != null && !masterPhoneNumber.isEmpty());
            contactButton.setText(masterPhoneNumber != null && !masterPhoneNumber.isEmpty() ? "Связаться" : "Номер не указан");
        }
    }

    private void displayWorkingHours(Map<String, WorkingDay> workingHours) {
        View dividerAfterDesc = findViewById(R.id.divider_after_description);
        View dividerBeforeReviews = findViewById(R.id.divider_before_reviews);

        if (workingHours == null || workingHours.isEmpty()) {
            if (workingHoursDisplayContainer != null) workingHoursDisplayContainer.setVisibility(View.GONE);
            if (workingHoursSectionTitleTextView != null) workingHoursSectionTitleTextView.setVisibility(View.GONE);
            if (dividerAfterDesc != null) dividerAfterDesc.setVisibility(View.GONE);
            if (dividerBeforeReviews != null) dividerBeforeReviews.setVisibility(View.GONE);
            Log.d(TAG, "Working hours data is null or empty. Hiding section.");
            return;
        }

        if (workingHoursDisplayContainer != null) workingHoursDisplayContainer.setVisibility(View.VISIBLE);
        if (workingHoursSectionTitleTextView != null) workingHoursSectionTitleTextView.setVisibility(View.VISIBLE);
        if (dividerAfterDesc != null) dividerAfterDesc.setVisibility(View.VISIBLE);
        if (dividerBeforeReviews != null) dividerBeforeReviews.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDayKeyForHighlight = getCurrentDayKey(currentDayOfWeek);

        for (String dayKey : displayDaysOrder) {
            TextView timeTextView = dayKeyToTextViewMap.get(dayKey);
            if (timeTextView == null) {
                Log.w(TAG, "TextView for time not found for day key: " + dayKey);
                continue;
            }

            View parentView = (View) timeTextView.getParent();
            LinearLayout dayRowLayout = null;
            if (parentView instanceof LinearLayout) {
                dayRowLayout = (LinearLayout) parentView;
            }
            if (dayRowLayout == null) {
                Log.w(TAG, "Parent LinearLayout not found for day key: " + dayKey);
                continue;
            }
            TextView dayNameTextView = dayRowLayout.findViewById(R.id.dayNameTextView);

            WorkingDay dayData = workingHours.get(dayKey);

            dayRowLayout.setBackgroundColor(Color.TRANSPARENT);
            if (dayNameTextView != null) dayNameTextView.setTypeface(null, Typeface.NORMAL);
            timeTextView.setTypeface(null, Typeface.NORMAL);

            if (dayKey.equals(currentDayKeyForHighlight)) {
                int highlightColor = ContextCompat.getColor(this, R.color.colorSurfaceVariantLight);
                dayRowLayout.setBackgroundColor(highlightColor);
                if (dayNameTextView != null) dayNameTextView.setTypeface(dayNameTextView.getTypeface(), Typeface.BOLD);
                timeTextView.setTypeface(timeTextView.getTypeface(), Typeface.BOLD);
            }

            String displayText;
            int textColorRes;

            if (dayData != null && dayData.isWorking()
                    && dayData.getStartTime() != null && !dayData.getStartTime().isEmpty() && !dayData.getStartTime().equals("--:--")
                    && dayData.getEndTime() != null && !dayData.getEndTime().isEmpty() && !dayData.getEndTime().equals("--:--"))
            {
                displayText = String.format("%s - %s", dayData.getStartTime(), dayData.getEndTime());
                textColorRes = R.color.colorOnSurfaceLight;
            } else {
                displayText = "Выходной";
                textColorRes = R.color.textColorSecondaryLight;
            }

            timeTextView.setText(displayText);
            try {
                timeTextView.setTextColor(ContextCompat.getColor(this, textColorRes));
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Color resource not found: " + textColorRes, e);
                timeTextView.setTextColor(ContextCompat.getColor(this, R.color.colorOnSurfaceLight));
            }
        }
        Log.d(TAG, "Working hours displayed successfully with highlight.");
    }
    private void updateMasterCurrentStatus(Map<String, WorkingDay> workingHours) {
        if (masterCurrentStatusTextView == null || isFinishing() || isDestroyed()) {
            return;
        }
        if (workingHours == null || workingHours.isEmpty()) {
            masterCurrentStatusTextView.setVisibility(View.GONE);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDayKey = getCurrentDayKey(dayOfWeek);

        if (currentDayKey == null) {
            masterCurrentStatusTextView.setVisibility(View.GONE);
            return;
        }

        WorkingDay todayWorkingData = workingHours.get(currentDayKey);
        String statusText;
        int statusColorRes;
        int iconTintRes;

        if (todayWorkingData != null && todayWorkingData.isWorking()) {
            String startTimeStr = todayWorkingData.getStartTime();
            String endTimeStr = todayWorkingData.getEndTime();

            if (startTimeStr != null && !startTimeStr.isEmpty() && !startTimeStr.equals("--:--")
                    && endTimeStr != null && !endTimeStr.isEmpty() && !endTimeStr.equals("--:--")) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                try {
                    Date startTime = sdf.parse(startTimeStr);
                    Date endTime = sdf.parse(endTimeStr);
                    Calendar nowCal = Calendar.getInstance();
                    Date currentTime = sdf.parse(sdf.format(nowCal.getTime()));

                    Calendar calStart = Calendar.getInstance(); calStart.setTime(startTime);
                    calStart.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));

                    Calendar calEnd = Calendar.getInstance(); calEnd.setTime(endTime);
                    calEnd.set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH));

                    if (calEnd.before(calStart) || calEnd.equals(calStart)) {
                        if (!currentTime.before(startTime) || !currentTime.after(endTime)) {
                            statusText = "Работает (до " + endTimeStr + ")";
                            statusColorRes = R.color.colorPrimaryBlue;
                            iconTintRes = R.color.colorPrimaryBlue;
                        } else {
                            statusText = "Начнет в " + startTimeStr;
                            statusColorRes = R.color.textColorSecondaryLight;
                            iconTintRes = R.color.textColorSecondaryLight;
                        }
                    } else {
                        if (!currentTime.before(startTime) && currentTime.before(endTime)) {
                            statusText = "Работает (до " + endTimeStr + ")";
                            statusColorRes = R.color.colorPrimaryBlue;
                            iconTintRes = R.color.colorPrimaryBlue;
                        } else if (currentTime.before(startTime)) {
                            statusText = "Начнет в " + startTimeStr;
                            statusColorRes = R.color.textColorSecondaryLight;
                            iconTintRes = R.color.textColorSecondaryLight;
                        } else {
                            statusText = "Закончил в " + endTimeStr;
                            statusColorRes = R.color.textColorSecondaryLight;
                            iconTintRes = R.color.textColorSecondaryLight;
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing working time: " + startTimeStr + " - " + endTimeStr, e);
                    statusText = "Сегодня работает";
                    statusColorRes = R.color.textColorSecondaryLight;
                    iconTintRes = R.color.textColorSecondaryLight;
                }
            } else {
                statusText = "Сегодня работает";
                statusColorRes = R.color.textColorSecondaryLight;
                iconTintRes = R.color.textColorSecondaryLight;
            }
        } else {
            statusText = "Сегодня выходной";
            statusColorRes = R.color.colorErrorLight;
            iconTintRes = R.color.colorErrorLight;
        }

        masterCurrentStatusTextView.setText(statusText);
        masterCurrentStatusTextView.setTextColor(ContextCompat.getColor(this, statusColorRes));

        Drawable[] drawables = masterCurrentStatusTextView.getCompoundDrawablesRelative();
        Drawable startDrawable = drawables[0];
        if (startDrawable != null) {
            Drawable wrappedDrawable = DrawableCompat.wrap(startDrawable.mutate());
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, iconTintRes));
            masterCurrentStatusTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(wrappedDrawable, null, null, null);
        }

        masterCurrentStatusTextView.setVisibility(View.VISIBLE);
    }
    private String getCurrentDayKey(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY:    return "monday";
            case Calendar.TUESDAY:   return "tuesday";
            case Calendar.WEDNESDAY: return "wednesday";
            case Calendar.THURSDAY:  return "thursday";
            case Calendar.FRIDAY:    return "friday";
            case Calendar.SATURDAY:  return "saturday";
            case Calendar.SUNDAY:    return "sunday";
            default: return null;
        }
    }


    private void loadReviews() {
        Log.d(TAG, "loadReviews called");
        if (reviewsRef == null || reviewAdapter == null || reviewsTitleTextView == null) {
            Log.w(TAG, "Review UI elements or reviewsRef is null, cannot display reviews.");
            updateReviewSectionVisibility(false); // Скрываем секцию отзывов
            return;
        }
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    reviewList.clear();
                    if (dataSnapshot.exists()){
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Review review = snapshot.getValue(Review.class);
                            if (review != null) reviewList.add(review);
                        }
                    }
                    reviewAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + reviewList.size() + " reviews.");
                    updateReviewSectionVisibility(!reviewList.isEmpty());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isFinishing() && !isDestroyed()) {
                    Log.e(TAG, "loadReviews:onCancelled", databaseError.toException());
                    updateReviewSectionVisibility(false);
                }
            }
        });
    }

    private void updateReviewSectionVisibility(boolean hasReviews) {
        if (isFinishing() || isDestroyed()) return;
        if (reviewsTitleTextView != null) reviewsTitleTextView.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
        if (reviewsRecyclerView != null) reviewsRecyclerView.setVisibility(hasReviews ? View.VISIBLE : View.GONE);

        View dividerBeforeReviews = findViewById(R.id.divider_before_reviews);
        if (dividerBeforeReviews != null) {
            boolean servicesAreVisible = (masterServicesRecyclerView != null && masterServicesRecyclerView.getVisibility() == View.VISIBLE);
            dividerBeforeReviews.setVisibility(hasReviews || servicesAreVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void attachFavoriteStatusListener() {
        Log.d(TAG, "Attaching favoriteStatusListener for masterId: " + masterId);
        if (userFavoritesRef == null || masterId == null) {
            Log.w(TAG, "Cannot attach favorite listener, userFavoritesRef or masterId is null.");
            return;
        }
        detachFavoriteStatusListener();

        favoriteStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    updateFavoriteButton(dataSnapshot.exists());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isFinishing() && !isDestroyed()) {
                    Log.e(TAG, "favoriteStatusListener:onCancelled", databaseError.toException());
                    if (addToFavoritesButton != null) addToFavoritesButton.setEnabled(false);
                }
            }
        };
        userFavoritesRef.child(masterId).addValueEventListener(favoriteStatusListener);
    }

    private void detachFavoriteStatusListener() {
        if (userFavoritesRef != null && favoriteStatusListener != null && masterId != null) {
            try {
                userFavoritesRef.child(masterId).removeEventListener(favoriteStatusListener);
                Log.d(TAG, "Detached favoriteStatusListener for masterId: " + masterId);
            } catch (Exception e) {
                Log.e(TAG, "Error detaching favoriteStatusListener", e);
            }
            favoriteStatusListener = null;
        }
    }

    private void handleFavoriteButtonClick() {
        if (currentUser == null || userFavoritesRef == null) {
            Toast.makeText(this, "Войдите, чтобы управлять избранным", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference currentUserRoleRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid()).child("role");

        currentUserRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFinishing() && !isDestroyed()) {
                    String role = snapshot.getValue(String.class);
                    if ("master".equals(role)) {
                        Toast.makeText(MasterProfileActivity.this, "Мастера не могут добавлять в избранное", Toast.LENGTH_SHORT).show();
                    } else {
                        toggleFavorite();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) {
                    Log.e(TAG, "Error checking user role", error.toException());
                    Toast.makeText(MasterProfileActivity.this, "Не удалось проверить роль", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void toggleFavorite() {
        if (userFavoritesRef == null || masterId == null) return;
        DatabaseReference specificFavoriteRef = userFavoritesRef.child(masterId);
        if (isFavorite) {
            specificFavoriteRef.removeValue()
                    .addOnSuccessListener(aVoid -> { if (!isFinishing()) Toast.makeText(MasterProfileActivity.this, "Удалено из избранного", Toast.LENGTH_SHORT).show(); })
                    .addOnFailureListener(e -> { if (!isFinishing()) Log.e(TAG, "Error removing favorite", e); Toast.makeText(MasterProfileActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show(); });
        } else {
            specificFavoriteRef.setValue(true)
                    .addOnSuccessListener(aVoid -> { if (!isFinishing()) Toast.makeText(MasterProfileActivity.this, "Добавлено в избранное", Toast.LENGTH_SHORT).show(); })
                    .addOnFailureListener(e -> { if (!isFinishing()) Log.e(TAG, "Error adding favorite", e); Toast.makeText(MasterProfileActivity.this, "Ошибка добавления", Toast.LENGTH_SHORT).show(); });
        }
    }

    private void updateFavoriteButton(boolean isCurrentlyFavorite) {
        isFavorite = isCurrentlyFavorite;
        runOnUiThread(() -> {
            if (addToFavoritesButton == null || isFinishing() || isDestroyed()) return;
            if (isFavorite) {
                addToFavoritesButton.setText("В избранном");
                addToFavoritesButton.setIconResource(R.drawable.ic_favorite_filled);
                addToFavoritesButton.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondaryLight));
                addToFavoritesButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorErrorLight)));
                addToFavoritesButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorOutlineLight)));
                addToFavoritesButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSurfaceLight)));
            } else {
                addToFavoritesButton.setText("В избранное");
                addToFavoritesButton.setIconResource(R.drawable.ic_favorite_border);
                addToFavoritesButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryBlue));
                addToFavoritesButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryBlue)));
                addToFavoritesButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryBlue)));
                addToFavoritesButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSurfaceLight)));
            }
            addToFavoritesButton.setEnabled(currentUser != null);
        });
    }

    private void updateFavoriteButtonStateForGuest() {
        runOnUiThread(() -> {
            if (addToFavoritesButton != null && !isFinishing() && !isDestroyed()) {
                addToFavoritesButton.setEnabled(false);
                addToFavoritesButton.setText("Войдите, чтобы добавить");
                addToFavoritesButton.setIconResource(R.drawable.ic_favorite_border);
                addToFavoritesButton.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondaryLight));
                addToFavoritesButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.textColorSecondaryLight)));
                addToFavoritesButton.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorOutlineLight)));
                addToFavoritesButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSurfaceLight)));
            }
        });
    }

    private void requestCallPermissionOrCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PHONE_PERMISSION_CODE);
        } else {
            makePhoneCall();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PHONE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            } else {
                Toast.makeText(this, "Разрешение на звонок не предоставлено", Toast.LENGTH_SHORT).show();
                dialPhoneNumber();
            }
        }
    }

    private void makePhoneCall() {
        if (masterPhoneNumber != null && !masterPhoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + masterPhoneNumber));
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException making phone call", e);
                dialPhoneNumber();
            }
        } else {
            if (!isFinishing()) Toast.makeText(this, "Номер телефона не указан", Toast.LENGTH_SHORT).show();
        }
    }

    private void dialPhoneNumber() {
        if (masterPhoneNumber != null && !masterPhoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + masterPhoneNumber));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                if (!isFinishing()) Toast.makeText(this, "Приложение для звонка не найдено", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (!isFinishing()) Toast.makeText(this, "Номер телефона не указан", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataLoadHandler != null) {
            dataLoadHandler.removeCallbacksAndMessages(null);
        }
        detachFavoriteStatusListener();
    }
}