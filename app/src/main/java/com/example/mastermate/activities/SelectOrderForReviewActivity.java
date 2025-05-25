package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.adapters.SelectableOrderAdapter;
import com.example.mastermate.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectOrderForReviewActivity extends BaseActivity implements SelectableOrderAdapter.OnOrderSelectedListener {

    private static final String TAG = "SelectOrderForReview";

    private Toolbar toolbar;
    private RecyclerView selectableOrdersRecyclerView;
    private SelectableOrderAdapter selectableOrderAdapter;
    private List<Order> allClientOrdersList;
    private List<Order> ordersAvailableForReviewList;
    private ProgressBar loadingProgressBar;
    private TextView emptyTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userOrdersIdsRef;
    private DatabaseReference allOrdersDataRef;

    private ValueEventListener clientOrderIdsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_order_for_review);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        userOrdersIdsRef = database.getReference("users").child(currentUser.getUid()).child("orders");
        allOrdersDataRef = database.getReference("orders");

        initializeUI();
        setupToolbar();
        setupRecyclerView();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar_select_order_review);
        selectableOrdersRecyclerView = findViewById(R.id.selectableOrdersRecyclerView);
        loadingProgressBar = findViewById(R.id.selectableOrdersLoadingProgressBar);
        emptyTextView = findViewById(R.id.emptySelectableOrdersTextView);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
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

    private void setupRecyclerView() {
        allClientOrdersList = new ArrayList<>();
        ordersAvailableForReviewList = new ArrayList<>();
        selectableOrderAdapter = new SelectableOrderAdapter(this, ordersAvailableForReviewList, this);
        selectableOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        selectableOrdersRecyclerView.setAdapter(selectableOrderAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Loading client's order IDs to select for review.");
        loadClientOrderIds();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Detaching Firebase listeners.");
        detachFirebaseListeners();
    }

    private void showLoading(boolean show) {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (selectableOrdersRecyclerView != null) selectableOrdersRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (emptyTextView != null) emptyTextView.setVisibility(View.GONE);
    }

    private void updateEmptyStateVisibility() {
        if (emptyTextView != null && selectableOrdersRecyclerView != null) {
            boolean listIsEmpty = ordersAvailableForReviewList.isEmpty();
            emptyTextView.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
            selectableOrdersRecyclerView.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void loadClientOrderIds() {
        showLoading(true);
        detachFirebaseListeners();
        allClientOrdersList.clear();
        ordersAvailableForReviewList.clear();
        if (selectableOrderAdapter != null) selectableOrderAdapter.notifyDataSetChanged();

        if (userOrdersIdsRef == null) {
            Log.e(TAG, "userOrdersIdsRef is null.");
            showLoading(false); updateEmptyStateVisibility();
            return;
        }

        clientOrderIdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                List<String> orderIds = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                        if (idSnapshot.getKey() != null) orderIds.add(idSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Fetched " + orderIds.size() + " order IDs for client.");
                if (!orderIds.isEmpty()) {
                    loadOrderDetailsForSelection(orderIds);
                } else {
                    showLoading(false);
                    processAndDisplayOrdersForReview();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Failed to load client order IDs.", databaseError.toException());
                showLoading(false); updateEmptyStateVisibility();
                Toast.makeText(SelectOrderForReviewActivity.this, "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show();
            }
        };
        userOrdersIdsRef.addValueEventListener(clientOrderIdsListener);
    }

    private void loadOrderDetailsForSelection(List<String> orderIds) {
        allClientOrdersList.clear();
        final AtomicInteger loadedCounter = new AtomicInteger(0);
        final int totalToLoad = orderIds.size();
        Log.d(TAG, "Preparing to load details for " + totalToLoad + " orders.");

        if (totalToLoad == 0) {
            processAndDisplayOrdersForReview();
            return;
        }

        for (String orderId : orderIds) {
            if (orderId == null || orderId.isEmpty()) {
                if (loadedCounter.incrementAndGet() >= totalToLoad) processAndDisplayOrdersForReview();
                continue;
            }
            DatabaseReference singleOrderRef = allOrdersDataRef.child(orderId);
            singleOrderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (dataSnapshot.exists()) {
                        Order order = dataSnapshot.getValue(Order.class);
                        if (order != null) {
                            order.setOrderId(dataSnapshot.getKey());
                            allClientOrdersList.add(order);
                        }
                    }
                    if (loadedCounter.incrementAndGet() >= totalToLoad) {
                        processAndDisplayOrdersForReview();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "Failed to load order detail for ID: " + orderId, databaseError.toException());
                    if (loadedCounter.incrementAndGet() >= totalToLoad) {
                        processAndDisplayOrdersForReview();
                    }
                }
            });
        }
    }

    private void processAndDisplayOrdersForReview() {
        if (isFinishing() || isDestroyed()) return;
        ordersAvailableForReviewList.clear();
        Log.d(TAG, "Processing " + allClientOrdersList.size() + " total client orders to find those available for review.");

        for (Order order : allClientOrdersList) {
            if (order != null &&
                    Order.STATUS_CONFIRMED_CLIENT.equals(order.getStatus()) &&
                    !order.isClientReviewLeft()) {
                ordersAvailableForReviewList.add(order);
            }
        }
        Collections.sort(ordersAvailableForReviewList, (o1, o2) -> {
            long ts1 = o1.getClientConfirmationTimestampLong();
            if (ts1 == 0) ts1 = o1.getCreationTimestampLong();

            long ts2 = o2.getClientConfirmationTimestampLong();
            if (ts2 == 0) ts2 = o2.getCreationTimestampLong();

            return Long.compare(ts2, ts1);
        });

        Log.d(TAG, "Found " + ordersAvailableForReviewList.size() + " orders available for review.");
        if (selectableOrderAdapter != null) {
            selectableOrderAdapter.notifyDataSetChanged();
        }
        showLoading(false);
        updateEmptyStateVisibility();
    }

    @Override
    public void onOrderSelected(Order order) {
        if (isFinishing() || isDestroyed()) return;
        if (order == null || order.getMasterId() == null || order.getOrderId() == null) {
            Log.e(TAG, "Invalid order data for starting ReviewActivity.");
            Toast.makeText(this, "Не удалось выбрать заказ.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Order selected for review: ID " + order.getOrderId() + ", Master ID: " + order.getMasterId());
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("masterId", order.getMasterId());
        intent.putExtra("orderId", order.getOrderId());
        startActivity(intent);
    }

    private void detachFirebaseListeners() {
        if (userOrdersIdsRef != null && clientOrderIdsListener != null) {
            userOrdersIdsRef.removeEventListener(clientOrderIdsListener);
            clientOrderIdsListener = null;
        }
        Log.d(TAG, "SelectOrderForReviewActivity Firebase listeners detached.");
    }
}