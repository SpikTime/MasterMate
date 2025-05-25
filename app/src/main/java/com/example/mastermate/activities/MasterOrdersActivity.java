package com.example.mastermate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.adapters.OrderAdapter;
import com.example.mastermate.models.Order;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.RatingBar;

import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class MasterOrdersActivity extends BaseActivity implements OrderAdapter.OrderActionListener {

    private static final String TAG = "MasterOrdersActivity";

    private RecyclerView masterOrdersRecyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> fullOrderList;
    private List<Order> displayedOrderList;
    private ProgressBar ordersLoadingProgressBar;
    private LinearLayout emptyOrdersLayout;
    private TextView emptyOrdersTextView;
    private Toolbar ordersToolbar;
    private TabLayout tabLayout;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private DatabaseReference allOrdersDataRef;

    private ValueEventListener userOrderIdsListener;
    private Map<String, ValueEventListener> orderDetailListeners = new HashMap<>();
    private DatabaseReference usersRef;
    private DatabaseReference userMasterOrdersIdsRef;
    private String currentFilterType = FILTER_TYPE_NEW;
    private static final String FILTER_TYPE_NEW = "FILTER_NEW";
    private static final String FILTER_TYPE_ACTIVE = "FILTER_ACTIVE";
    private static final String FILTER_TYPE_COMPLETED = "FILTER_COMPLETED";
    private static final String FILTER_TYPE_REJECTED = "FILTER_REJECTED";


    @Override
    public void onCallMaster(String phoneNumber) {
        Log.w(TAG, "onCallMaster called in MasterOrdersActivity - this should not happen for a master user regarding another master.");;
    }

    @Override
    public void onConfirmCompletionByClient(Order order) {
        Log.d(TAG, "onConfirmCompletionByClient called in MasterOrdersActivity - no action taken.");
    }

    @Override
    public void onOpenDisputeByClient(Order order) {
        Log.d(TAG, "onOpenDisputeByClient called in MasterOrdersActivity - no action taken.");
    }

    @Override
    public void onLeaveReviewByClient(Order order) {
        Log.d(TAG, "onLeaveReviewByClient called in MasterOrdersActivity - no action taken.");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_orders);
        Log.d(TAG, "onCreate called");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "CurrentUser is null. Finishing activity.");
            Toast.makeText(this, "Сначала войдите в систему", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        userMasterOrdersIdsRef = usersRef.child(currentUser.getUid()).child("orders");
        allOrdersDataRef = database.getReference("orders");

        initializeUI();
        setupToolbarIfPresent();
        setupRecyclerView();
        setupTabs();
    }
    private void initializeUI() {
        masterOrdersRecyclerView = findViewById(R.id.masterOrdersRecyclerView);
        ordersLoadingProgressBar = findViewById(R.id.ordersLoadingProgressBar);
        emptyOrdersLayout = findViewById(R.id.emptyOrdersLayout);
        emptyOrdersTextView = findViewById(R.id.emptyOrdersTextView);
        ordersToolbar = findViewById(R.id.ordersToolbar);
        tabLayout = findViewById(R.id.tabLayout_master_orders);
        Log.d(TAG, "UI Initialized");
    }

    private void setupToolbarIfPresent() {
        if (ordersToolbar != null) {
            setSupportActionBar(ordersToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Мои заказы");
            }
        } else {
            setTitle("Мои заказы");
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
        if (masterOrdersRecyclerView == null) {
            Log.e(TAG, "RecyclerView (masterOrdersRecyclerView) is null!");
            return;
        }
        fullOrderList = new ArrayList<>();
        displayedOrderList = new ArrayList<>();

        orderAdapter = new OrderAdapter(this, displayedOrderList, "master", this);
        masterOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        masterOrdersRecyclerView.setAdapter(orderAdapter);
        Log.d(TAG, "RecyclerView setup complete");
    }

    private void setupTabs() {
        if (tabLayout == null) {
            Log.e(TAG, "TabLayout (tabLayout_master_orders) not found in layout!");
            currentFilterType = FILTER_TYPE_NEW;
            return;
        }
        tabLayout.addTab(tabLayout.newTab().setText("Новые"));
        tabLayout.addTab(tabLayout.newTab().setText("Активные"));
        tabLayout.addTab(tabLayout.newTab().setText("Завершенные"));
        tabLayout.addTab(tabLayout.newTab().setText("Отклоненные"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected: " + tab.getText());
                switch (tab.getPosition()) {
                    case 0: currentFilterType = FILTER_TYPE_NEW; break;
                    case 1: currentFilterType = FILTER_TYPE_ACTIVE; break;
                    case 2: currentFilterType = FILTER_TYPE_COMPLETED; break;
                    case 3: currentFilterType = FILTER_TYPE_REJECTED; break;
                    default: currentFilterType = FILTER_TYPE_NEW; break;
                }
                applyFilterAndRefreshList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) { applyFilterAndRefreshList(); }
        });
        currentFilterType = FILTER_TYPE_NEW;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called, loading order IDs.");
        if (currentUser != null) {
            loadMasterOrderIds();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called, detaching listeners.");
        detachAllFirebaseListeners();
    }

    private void showLoadingState(boolean isLoading) {
        if (ordersLoadingProgressBar != null) ordersLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            if (masterOrdersRecyclerView != null) masterOrdersRecyclerView.setVisibility(View.GONE);
            if (emptyOrdersLayout != null) emptyOrdersLayout.setVisibility(View.GONE);
        }
    }

    private void checkEmptyState() {
        boolean isLoading = ordersLoadingProgressBar != null && ordersLoadingProgressBar.getVisibility() == View.VISIBLE;

        boolean isEmpty = displayedOrderList.isEmpty();
        if (masterOrdersRecyclerView != null) {
            masterOrdersRecyclerView.setVisibility(!isLoading && isEmpty ? View.GONE : View.VISIBLE);
        }
        if (emptyOrdersLayout != null) {
            emptyOrdersLayout.setVisibility(!isLoading && isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty && !isLoading && emptyOrdersTextView != null) {

            }
        }
        Log.d(TAG, "checkEmptyState - isLoading: " + isLoading + ", isEmpty: " + isEmpty);
    }
    private void loadMasterOrderIds() {
        showLoadingState(true);
        detachAllFirebaseListeners();
        fullOrderList.clear();
        displayedOrderList.clear();
        if (orderAdapter != null) orderAdapter.notifyDataSetChanged();

        if (userMasterOrdersIdsRef == null) {
            Log.e(TAG, "userMasterOrdersIdsRef is null.");
            showLoadingState(false); checkEmptyState(); return;
        }

        userOrderIdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> orderIds = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                        if (idSnapshot.getKey() != null) orderIds.add(idSnapshot.getKey());
                    }
                }
                Log.d(TAG, "Fetched " + orderIds.size() + " order IDs.");
                if (!orderIds.isEmpty()) {
                    loadOrderDetailsForIds(orderIds);
                } else {
                    showLoadingState(false);
                    applyFilterAndRefreshList();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load order IDs.", databaseError.toException());
                showLoadingState(false); checkEmptyState();
                Toast.makeText(MasterOrdersActivity.this, "Ошибка загрузки списка заказов", Toast.LENGTH_SHORT).show();
            }
        };
        userMasterOrdersIdsRef.addValueEventListener(userOrderIdsListener);
    }

    private void loadOrderDetailsForIds(List<String> orderIds) {

        final int totalToLoad = orderIds.size();
        Log.d(TAG, "loadOrderDetailsForIds: Preparing to attach listeners for " + totalToLoad + " order details.");

        if (totalToLoad == 0) {
            applyFilterAndRefreshList();
            return;
        }
        int listenersAttachedCount = 0;

        for (String orderId : orderIds) {
            if (orderId == null || orderId.isEmpty()) {
                Log.w(TAG, "Skipping null or empty orderId in loadOrderDetailsForIds.");
                listenersAttachedCount++;
                if (listenersAttachedCount >= totalToLoad) {
                    showLoadingState(false);
                    applyFilterAndRefreshList();
                }
                continue;
            }


            if (orderDetailListeners.containsKey(orderId) && orderDetailListeners.get(orderId) != null) {
                allOrdersDataRef.child(orderId).removeEventListener(orderDetailListeners.get(orderId));
                orderDetailListeners.remove(orderId);
            }

            DatabaseReference singleOrderRef = allOrdersDataRef.child(orderId);
            ValueEventListener detailListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing() || isDestroyed()) return;

                    String currentOrderId = dataSnapshot.getKey();
                    Order existingOrder = null;
                    int existingOrderIndex = -1;

                    for (int i = 0; i < fullOrderList.size(); i++) {
                        if (fullOrderList.get(i) != null && fullOrderList.get(i).getOrderId() != null &&
                                fullOrderList.get(i).getOrderId().equals(currentOrderId)) {
                            existingOrder = fullOrderList.get(i);
                            existingOrderIndex = i;
                            break;
                        }
                    }

                    if (dataSnapshot.exists()) {
                        Order updatedOrder = dataSnapshot.getValue(Order.class);
                        if (updatedOrder != null) {
                            updatedOrder.setOrderId(currentOrderId);
                            if (existingOrderIndex != -1) {
                                fullOrderList.set(existingOrderIndex, updatedOrder);
                                Log.d(TAG, "Order detail UPDATED: " + updatedOrder.getOrderId() + " with status: " + updatedOrder.getStatus());
                            } else {
                                fullOrderList.add(updatedOrder);
                                Log.d(TAG, "Order detail ADDED: " + updatedOrder.getOrderId() + " with status: " + updatedOrder.getStatus());
                            }
                        } else {
                            Log.w(TAG, "Order data is null for ID: " + currentOrderId + " (snapshot exists). Removing if was present.");
                            if (existingOrderIndex != -1) fullOrderList.remove(existingOrderIndex);
                        }
                    } else {
                        Log.w(TAG, "Order with ID: " + currentOrderId + " does not exist in /orders node. Removing if was present.");
                        if (existingOrderIndex != -1) fullOrderList.remove(existingOrderIndex);
                    }
                    applyFilterAndRefreshList();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing() || isDestroyed()) return;
                    String currentOrderId = singleOrderRef.getKey();
                    Log.e(TAG, "Failed to load order details for ID: " + currentOrderId, databaseError.toException());
                    int indexToRemove = -1;
                    for (int i = 0; i < fullOrderList.size(); i++) {
                        if (fullOrderList.get(i) != null && fullOrderList.get(i).getOrderId() != null &&
                                fullOrderList.get(i).getOrderId().equals(currentOrderId)) {
                            indexToRemove = i;
                            break;
                        }
                    }
                    if (indexToRemove != -1) fullOrderList.remove(indexToRemove);

                    applyFilterAndRefreshList();
                }
            };
            singleOrderRef.addValueEventListener(detailListener);
            orderDetailListeners.put(orderId, detailListener);
            Log.d(TAG, "Attached persistent ValueEventListener for order details: " + singleOrderRef.toString());

            listenersAttachedCount++;
            if (listenersAttachedCount >= totalToLoad) {
                Log.d(TAG, "All detail listeners attached. UI will update as data arrives.");
            }
        }
    }

    private void applyFilterAndRefreshList() {
        if (isFinishing() || isDestroyed()) return;
        displayedOrderList.clear();

        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Applying filter: " + currentFilterType + ". Full list size: " + fullOrderList.size());
        for (Order order : fullOrderList) {
            if (order == null || order.getStatus() == null) continue;
            String status = order.getStatus().toLowerCase();
            boolean matches = false;
            switch (currentFilterType) {
                case FILTER_TYPE_NEW: if (Order.STATUS_NEW.equals(status)) matches = true; break;
                case FILTER_TYPE_ACTIVE: if (Order.STATUS_ACCEPTED.equals(status) || Order.STATUS_IN_PROGRESS.equals(status)) matches = true; break;
                case FILTER_TYPE_COMPLETED: if (Order.STATUS_COMPLETED_MASTER.equals(status) || Order.STATUS_CONFIRMED_CLIENT.equals(status)) matches = true; break;
                case FILTER_TYPE_REJECTED: if (Order.STATUS_REJECTED_MASTER.equals(status) || Order.STATUS_CANCELLED_CLIENT.equals(status)) matches = true; break;
            }
            if (matches) displayedOrderList.add(order);
        }
        boolean noDataYetOrAllFilteredOut = fullOrderList.isEmpty() || displayedOrderList.isEmpty();
        Collections.sort(displayedOrderList, (o1, o2) -> Long.compare(o2.getCreationTimestampLong(), o1.getCreationTimestampLong()));
        Log.d(TAG, "Filtered list size for display: " + displayedOrderList.size());

        if (orderAdapter != null) orderAdapter.notifyDataSetChanged();
        showLoadingState(false);
        checkEmptyState();
    }

    @Override
    public void onAcceptOrder(Order order) {
        Log.d(TAG, "ACTION: Accept order: " + order.getOrderId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Order.STATUS_ACCEPTED);
        updates.put("acceptedTimestamp", ServerValue.TIMESTAMP);
        order.setStatus(Order.STATUS_ACCEPTED);
        int index = findOrderIndexInFullList(order.getOrderId());
        if (index != -1) {
            fullOrderList.set(index, order);
        } else {
        }
        applyFilterAndRefreshList();

        updateOrderInFirebase(order.getOrderId(), updates);
    }

    private int findOrderIndexInFullList(String orderId) {
        if (orderId == null || fullOrderList == null) return -1;
        for (int i = 0; i < fullOrderList.size(); i++) {
            if (fullOrderList.get(i) != null && orderId.equals(fullOrderList.get(i).getOrderId())) {
                return i;
            }
        }
        return -1;
    }
    @Override
    public void onRejectOrder(Order order) {
        Log.d(TAG, "ACTION: Reject order: " + order.getOrderId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Order.STATUS_REJECTED_MASTER);
        updates.put("rejectionTimestamp", ServerValue.TIMESTAMP);
        updateOrderInFirebase(order.getOrderId(), updates);
        // TODO: Отправить FCM клиенту
    }

    @Override
    public void onCompleteOrder(Order order) {
        Log.d(TAG, "ACTION: Attempting to complete order: " + order.getOrderId());
        if (isFinishing() || isDestroyed()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_complete_order_master, null);
        final EditText editTextFinalPrice = dialogView.findViewById(R.id.editTextFinalPrice);
        final EditText editTextMasterComment = dialogView.findViewById(R.id.editTextMasterComment);

        if (order.getFinalPrice() > 0) editTextFinalPrice.setText(String.valueOf(order.getFinalPrice()));
        if (order.getMasterComment() != null && !order.getMasterComment().isEmpty()) editTextMasterComment.setText(order.getMasterComment());

        new AlertDialog.Builder(this)
                .setTitle("Завершение заказа")
                .setView(dialogView)
                .setPositiveButton("Завершить", (dialog, which) -> {
                    String finalPriceStr = editTextFinalPrice.getText().toString().trim();
                    String masterComment = editTextMasterComment.getText().toString().trim();
                    double finalPrice = 0.0;

                    if (finalPriceStr.isEmpty()) { Toast.makeText(this, "Укажите итоговую цену", Toast.LENGTH_SHORT).show(); return; }
                    try {
                        finalPrice = Double.parseDouble(finalPriceStr);
                        if (finalPrice < 0) { Toast.makeText(this, "Цена не может быть отрицательной", Toast.LENGTH_SHORT).show(); return; }
                    } catch (NumberFormatException e) { Toast.makeText(this, "Некорректный формат цены", Toast.LENGTH_SHORT).show(); return; }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", Order.STATUS_COMPLETED_MASTER);
                    updates.put("masterCompletionTimestamp", ServerValue.TIMESTAMP);
                    updates.put("finalPrice", finalPrice);
                    updates.put("masterComment", masterComment);
                    updateOrderInFirebase(order.getOrderId(), updates);
                    // TODO: Отправить FCM клиенту
                    Toast.makeText(this, "Заказ отмечен как завершенный", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onCallClient(String phoneNumber) {
        Log.d(TAG, "ACTION: Call client: " + phoneNumber);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            try { startActivity(intent); }
            catch (Exception e) { Toast.makeText(this, "Не удалось открыть набор номера", Toast.LENGTH_SHORT).show(); }
        } else { Toast.makeText(this, "Номер клиента не указан", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    public void onRateClient(Order order) {
        if (order == null || order.getOrderId() == null || order.getClientId() == null || isFinishing() || isDestroyed()) {
            return;
        }
        Log.d(TAG, "ACTION: Rate client for order: " + order.getOrderId());

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_rate_client, null);
        final RatingBar ratingBar = dialogView.findViewById(R.id.ratingBarClient);
        final TextInputLayout commentLayout = dialogView.findViewById(R.id.masterCommentForClientLayout);
        final EditText editTextComment = dialogView.findViewById(R.id.editTextMasterCommentForClient);

        ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
            if (rating <= 3.0f && rating > 0) {
                commentLayout.setVisibility(View.VISIBLE);
                commentLayout.setHint("Комментарий (обязательно при низкой оценке)");
            } else {
                commentLayout.setVisibility(View.GONE);
                commentLayout.setHint("Комментарий о клиенте");
            }
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Отправить оценку", (dialog, which) -> {
                    float clientRating = ratingBar.getRating();
                    String masterComment = editTextComment.getText().toString().trim();

                    if (clientRating < 0.5f) {
                        Toast.makeText(this, "Пожалуйста, поставьте оценку клиенту", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (clientRating <= 3.0f && masterComment.isEmpty()) {
                        Toast.makeText(this, "Пожалуйста, укажите причину низкой оценки", Toast.LENGTH_LONG).show();
                        return;
                    }

                    saveClientRating(order, clientRating, masterComment);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveClientRating(Order order, float clientRating, String masterComment) {
        if (allOrdersDataRef == null || usersRef == null) {
            Log.e(TAG, "Database references are null in saveClientRating");
            return;
        }
        DatabaseReference orderSpecificRef = allOrdersDataRef.child(order.getOrderId());
        Map<String, Object> orderUpdates = new HashMap<>();
        orderUpdates.put("masterRatingForClient", clientRating);
        orderUpdates.put("masterCommentForClient", masterComment);
        orderUpdates.put("masterRatedClientTimestamp", ServerValue.TIMESTAMP);

        orderSpecificRef.updateChildren(orderUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Master's rating for client saved in order " + order.getOrderId());
                    Toast.makeText(MasterOrdersActivity.this, "Оценка клиенту отправлена", Toast.LENGTH_SHORT).show();
                    updateClientOverallRating(order.getClientId(), clientRating);
                    // TODO: Можно отправить FCM уведомление администратору или для статистики, но не клиенту
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save master's rating for client in order " + order.getOrderId(), e);
                    Toast.makeText(MasterOrdersActivity.this, "Ошибка сохранения оценки в заказе", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateClientOverallRating(String clientId, float newRatingFromMaster) {
        if (clientId == null || clientId.isEmpty() || usersRef == null) return;
        DatabaseReference clientNodeRef = usersRef.child(clientId);

        clientNodeRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Double currentSum = mutableData.child("clientRatingSum").getValue(Double.class);
                Long currentCount = mutableData.child("clientRatedByMastersCount").getValue(Long.class);

                double newSum = (currentSum == null) ? 0.0 : currentSum;
                long newCount = (currentCount == null) ? 0L : currentCount;

                newSum += newRatingFromMaster;
                newCount++;

                double newAverage = (newCount > 0) ? newSum / newCount : 0.0;
                newAverage = Math.round(newAverage * 10.0) / 10.0;

                mutableData.child("clientRatingSum").setValue(newSum);
                mutableData.child("clientRatedByMastersCount").setValue(newCount);
                mutableData.child("clientAverageRating").setValue(newAverage);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction to update client's overall rating failed: " + error.getMessage());
                } else if (committed) {
                    Log.i(TAG, "Client's overall rating updated successfully for clientId: " + clientId);
                } else {
                    Log.w(TAG, "Client's overall rating transaction not committed for clientId: " + clientId);
                }
            }
        });
    }



    @Override
    public void onItemClick(Order order) {
        Log.d(TAG, "MasterOrdersActivity - onItemClick called for order: " + order.getOrderId());
        if (order == null || order.getOrderId() == null) {
            Toast.makeText(this, "Ошибка данных заказа", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        intent.putExtra("userRole", "master");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting OrderDetailsActivity from master's list", e);
            Toast.makeText(this, "Не удалось открыть детали заказа", Toast.LENGTH_SHORT).show();
        }
    }
    private void updateOrderInFirebase(String orderId, Map<String, Object> updates) {
        if (orderId == null || updates == null || updates.isEmpty() || allOrdersDataRef == null) return;
        allOrdersDataRef.child(orderId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Order " + orderId + " updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update order " + orderId, e));
    }

    private void detachAllFirebaseListeners() {
        if (userMasterOrdersIdsRef != null && userOrderIdsListener != null) {
            userMasterOrdersIdsRef.removeEventListener(userOrderIdsListener);
            userOrderIdsListener = null;
            Log.d(TAG, "UserMasterOrderIdsListener detached.");
        }
        Log.d(TAG, "All MasterOrders Firebase listeners detached.");
    }

    private void detachOrderDetailsListeners() {
        if (allOrdersDataRef != null && !orderDetailListeners.isEmpty()) {
            for (Map.Entry<String, ValueEventListener> entry : orderDetailListeners.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    allOrdersDataRef.child(entry.getKey()).removeEventListener(entry.getValue());
                    Log.d(TAG, "Detached detail listener for order ID: " + entry.getKey());
                }
            }
            orderDetailListeners.clear();
        }
        Log.d(TAG, "All OrderDetailsListeners detached.");
    }
}