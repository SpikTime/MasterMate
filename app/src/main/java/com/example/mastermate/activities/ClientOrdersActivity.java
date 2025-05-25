package com.example.mastermate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

public class ClientOrdersActivity extends BaseActivity implements OrderAdapter.OrderActionListener {

    private static final String TAG = "ClientOrdersActivity";

    private RecyclerView clientOrdersRecyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> fullOrderList;
    private List<Order> displayedOrderList;
    private ProgressBar clientOrdersLoadingProgressBar;
    private LinearLayout emptyClientOrdersLayout;
    private TextView emptyClientOrdersTextView;
    private Toolbar clientOrdersToolbar;
    private TabLayout tabLayout;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference usersRef;
    private DatabaseReference userClientOrdersIdsRef;
    private DatabaseReference allOrdersDataRef;

    private ValueEventListener userClientOrderIdsListener;
    private Map<String, ValueEventListener> orderDetailListeners = new HashMap<>();

    private String currentFilterType = "active_client";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_orders);
        Log.d(TAG, "onCreate called");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "CurrentUser is null. Finishing activity.");
            Toast.makeText(this, "Сначала войдите в систему", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        userClientOrdersIdsRef = usersRef.child(currentUser.getUid()).child("orders");
        allOrdersDataRef = database.getReference("orders");

        initializeUI();
        setupToolbarIfPresent();
        setupRecyclerView();
        setupTabs();
    }

    private void initializeUI() {
        clientOrdersRecyclerView = findViewById(R.id.clientOrdersRecyclerView);
        clientOrdersLoadingProgressBar = findViewById(R.id.clientOrdersLoadingProgressBar);
        emptyClientOrdersLayout = findViewById(R.id.emptyClientOrdersLayout);
        emptyClientOrdersTextView = findViewById(R.id.emptyClientOrdersTextView);
        clientOrdersToolbar = findViewById(R.id.clientOrdersToolbar);
        tabLayout = findViewById(R.id.tabLayout_client_orders);
        Log.d(TAG, "UI Initialized");
    }

    private void setupToolbarIfPresent() {
        if (clientOrdersToolbar != null) {
            setSupportActionBar(clientOrdersToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Мои заявки");
            }
        } else {
            setTitle("Мои заявки");
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
        if (clientOrdersRecyclerView == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }
        fullOrderList = new ArrayList<>();
        displayedOrderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(this, displayedOrderList, "client", this);
        clientOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        clientOrdersRecyclerView.setAdapter(orderAdapter);
        Log.d(TAG, "RecyclerView setup complete for client.");
    }

    private void setupTabs() {
        if (tabLayout == null) {
            Log.w(TAG, "TabLayout not found. Filtering disabled.");
            currentFilterType = "all_client_orders";
            return;
        }
        tabLayout.addTab(tabLayout.newTab().setText("Активные"));
        tabLayout.addTab(tabLayout.newTab().setText("Завершенные"));
        tabLayout.addTab(tabLayout.newTab().setText("Другие"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Client Tab selected: " + tab.getPosition() + " - " + tab.getText());
                switch (tab.getPosition()) {
                    case 0:
                        currentFilterType = "active_client";
                        break;
                    case 1:
                        currentFilterType = Order.STATUS_CONFIRMED_CLIENT;
                        break;
                    case 2:
                        currentFilterType = "other_client_orders";
                        break;
                    default:
                        currentFilterType = "active_client";
                        break;
                }
                applyFilterAndRefreshList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                applyFilterAndRefreshList();
            }
        });
        currentFilterType = "active_client";
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called, loading client order IDs.");
        if (currentUser != null) loadClientOrderIds();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called, detaching listeners.");
        detachAllFirebaseListeners();
    }

    private void showLoadingState(boolean isLoading) {
        if (isFinishing() || isDestroyed()) return;
        if (clientOrdersLoadingProgressBar != null)
            clientOrdersLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            if (clientOrdersRecyclerView != null) clientOrdersRecyclerView.setVisibility(View.GONE);
            if (emptyClientOrdersLayout != null) emptyClientOrdersLayout.setVisibility(View.GONE);
        }
    }

    private void checkEmptyState() {
        if (isFinishing() || isDestroyed()) return;
        boolean isLoading = clientOrdersLoadingProgressBar != null && clientOrdersLoadingProgressBar.getVisibility() == View.VISIBLE;
        if (isLoading) return;

        boolean isEmpty = (displayedOrderList == null || displayedOrderList.isEmpty());
        if (clientOrdersRecyclerView != null)
            clientOrdersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (emptyClientOrdersLayout != null) {
            emptyClientOrdersLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty && emptyClientOrdersTextView != null)
                emptyClientOrdersTextView.setText("Нет заказов в этой категории");
        }
        Log.d(TAG, "checkEmptyState - displayed list isEmpty: " + isEmpty);
    }

    private void loadClientOrderIds() {
        showLoadingState(true);
        detachAllFirebaseListeners();
        fullOrderList.clear();
        displayedOrderList.clear();
        if (orderAdapter != null) orderAdapter.notifyDataSetChanged();

        if (userClientOrdersIdsRef == null) {
            Log.e(TAG, "userClientOrdersIdsRef is null.");
            showLoadingState(false);
            checkEmptyState();
            return;
        }

        Log.d(TAG, "Attaching listener to: " + userClientOrdersIdsRef.toString());
        userClientOrderIdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                List<String> orderIds = new ArrayList<>();
                Log.d(TAG, "loadClientOrderIds - onDataChange. Snapshot exists: " + dataSnapshot.exists() + ", Children count: " + dataSnapshot.getChildrenCount());
                if (dataSnapshot.exists()) {
                    for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                        String orderId = idSnapshot.getKey();
                        if (orderId != null) {
                            Log.d(TAG, "Found orderId reference for client: " + orderId);
                            orderIds.add(orderId);
                        }
                    }
                }
                if (!orderIds.isEmpty()) {
                    Log.d(TAG, "Client has " + orderIds.size() + " order IDs. Loading details...");
                    loadOrderDetailsForIds(orderIds);
                } else {
                    Log.d(TAG, "Client has no order IDs.");
                    showLoadingState(false);
                    applyFilterAndRefreshList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Failed to load client order IDs.", databaseError.toException());
                showLoadingState(false);
                checkEmptyState();
                Toast.makeText(ClientOrdersActivity.this, "Ошибка загрузки списка заказов", Toast.LENGTH_SHORT).show();
            }
        };
        userClientOrdersIdsRef.addValueEventListener(userClientOrderIdsListener);
    }

    private void loadOrderDetailsForIds(List<String> orderIds) {
        fullOrderList.clear();
        final AtomicInteger loadedCounter = new AtomicInteger(0);
        final int totalToLoad = orderIds.size();
        Log.d(TAG, "loadOrderDetailsForIds: Preparing to load details for " + totalToLoad + " orders.");

        if (totalToLoad == 0) {
            applyFilterAndRefreshList();
            return;
        }

        for (String orderId : orderIds) {
            if (orderId == null || orderId.isEmpty()) {
                Log.w(TAG, "Skipping null or empty orderId in loadOrderDetailsForIds.");
                if (loadedCounter.incrementAndGet() >= totalToLoad) applyFilterAndRefreshList();
                continue;
            }
            DatabaseReference singleOrderRef = allOrdersDataRef.child(orderId);
            Log.d(TAG, "Attaching ONE-TIME listener for order details: " + singleOrderRef.toString());
            singleOrderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (dataSnapshot.exists()) {
                        Order order = dataSnapshot.getValue(Order.class);
                        if (order != null) {
                            order.setOrderId(dataSnapshot.getKey());
                            fullOrderList.add(order);
                            Log.d(TAG, "Loaded order detail: " + order.getOrderId() + " with status: " + order.getStatus());
                        } else {
                            Log.w(TAG, "Order data is null for ID: " + dataSnapshot.getKey());
                        }
                    } else {
                        Log.w(TAG, "Order with ID: " + dataSnapshot.getKey() + " does not exist in /orders.");
                    }
                    if (loadedCounter.incrementAndGet() >= totalToLoad) {
                        Log.d(TAG, "All order details loaded/attempted. Total orders in fullOrderList: " + fullOrderList.size());
                        applyFilterAndRefreshList();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "Failed to load order detail for ID: " + singleOrderRef.getKey(), databaseError.toException());
                    if (loadedCounter.incrementAndGet() >= totalToLoad) {
                        applyFilterAndRefreshList();
                    }
                }
            });
        }
    }

    private void applyFilterAndRefreshList() {
        if (isFinishing() || isDestroyed()) return;
        displayedOrderList.clear();
        Log.d(TAG, "Client: Applying filter: " + currentFilterType + ". Full list size: " + fullOrderList.size());
        for (Order order : fullOrderList) {
            if (order == null || order.getStatus() == null) continue;
            String status = order.getStatus().toLowerCase();
            boolean matches = false;
            switch (currentFilterType) {
                case "active_client":
                    if (Order.STATUS_NEW.equals(status) || Order.STATUS_ACCEPTED.equals(status) ||
                            Order.STATUS_IN_PROGRESS.equals(status) || Order.STATUS_COMPLETED_MASTER.equals(status)) {
                        matches = true;
                    }
                    break;
                case Order.STATUS_CONFIRMED_CLIENT:
                    if (Order.STATUS_CONFIRMED_CLIENT.equals(status)) matches = true;
                    break;
                case "other_client_orders":
                    if (Order.STATUS_REJECTED_MASTER.equals(status) || Order.STATUS_CANCELLED_CLIENT.equals(status) ||
                            Order.STATUS_DISPUTED.equals(status)) {
                        matches = true;
                    }
                    break;
                case "all_client_orders":
                default:
                    matches = true;
                    break;
            }
            if (matches) displayedOrderList.add(order);
        }
        Collections.sort(displayedOrderList, (o1, o2) -> Long.compare(o2.getCreationTimestampLong(), o1.getCreationTimestampLong()));
        Log.d(TAG, "Client: Filtered list size for display: " + displayedOrderList.size());
        if (orderAdapter != null) orderAdapter.notifyDataSetChanged();

        showLoadingState(false);
        checkEmptyState();
    }

    @Override
    public void onConfirmCompletionByClient(Order order) {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "onConfirmCompletionByClient: Activity is finishing or destroyed.");
            return;
        }
        if (order == null || order.getOrderId() == null || order.getMasterId() == null) {
            Toast.makeText(this, "Ошибка данных заказа для подтверждения.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onConfirmCompletionByClient: Order, OrderId, or MasterId is null.");
            return;
        }

        Log.d(TAG, "Client confirms completion for order: " + order.getOrderId());
        showLoadingState(true);

        DatabaseReference orderRef = allOrdersDataRef.child(order.getOrderId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Order.STATUS_CONFIRMED_CLIENT);
        updates.put("clientConfirmationTimestamp", ServerValue.TIMESTAMP);

        orderRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed())
                        return;
                    showLoadingState(false);

                    if (task.isSuccessful()) {
                        Log.i(TAG, "Order " + order.getOrderId() + " status updated to CONFIRMED_CLIENT.");
                        Toast.makeText(ClientOrdersActivity.this, "Выполнение заказа подтверждено!", Toast.LENGTH_SHORT).show();
                        incrementMasterCompletedOrders(order.getMasterId());
                        promptToLeaveReview(order);
                    } else {
                        Log.e(TAG, "Failed to confirm order completion for order: " + order.getOrderId(), task.getException());
                        Toast.makeText(ClientOrdersActivity.this, "Ошибка подтверждения: " +
                                (task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getShortDescription(String description) {
        if (description == null || description.isEmpty()) return "Без описания";
        return description.length() > 30 ? description.substring(0, 27) + "..." : description;
    }


    private void incrementMasterCompletedOrders(String masterId) {
        if (masterId == null || masterId.isEmpty() || usersRef == null) {
            Log.e(TAG, "Cannot increment master orders: masterId or usersRef is null");
            return;
        }
        DatabaseReference masterCompletedCountRef = usersRef.child(masterId).child("completedOrdersCount");
        masterCompletedCountRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Long currentValue = mutableData.getValue(Long.class);
                if (currentValue == null) {
                    Log.d(TAG, "Transaction: completedOrdersCount is null, setting to 1.");
                    mutableData.setValue(1L);
                } else {
                    Log.d(TAG, "Transaction: current completedOrdersCount = " + currentValue + ", incrementing.");
                    mutableData.setValue(currentValue + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction_IncrementCount_Failed: " + error.getMessage());
                } else if (committed) {
                    Log.i(TAG, "Master completedOrdersCount incremented successfully. New count: " + (currentData != null ? currentData.getValue() : "N/A"));
                } else {
                    Log.w(TAG, "Master completedOrdersCount transaction not committed (possibly due to contention or data changed).");
                }
            }
        });
    }

    private void promptToLeaveReview(Order order) {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "promptToLeaveReview: Activity is finishing or destroyed.");
            return;
        }
        if (order == null || order.getMasterId() == null || order.getOrderId() == null) {
            Log.e(TAG, "promptToLeaveReview: Order, MasterId or OrderId is null.");
            return;
        }


        DatabaseReference specificOrderRef = allOrdersDataRef.child(order.getOrderId());
        specificOrderRef.child("clientReviewLeft").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                boolean reviewAlreadyLeft = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));

                if (reviewAlreadyLeft) {
                    Log.d(TAG, "Review already left for order: " + order.getOrderId());
                    Toast.makeText(ClientOrdersActivity.this, "Вы уже оставили отзыв по этому заказу.", Toast.LENGTH_SHORT).show();
                } else {
                    String masterName = order.getMasterName() != null && !order.getMasterName().isEmpty() ? order.getMasterName() : "мастера";
                    new AlertDialog.Builder(ClientOrdersActivity.this)
                            .setTitle("Оставить отзыв")
                            .setMessage("Хотите оставить отзыв о работе " + masterName + "?")
                            .setPositiveButton("Да", (dialog, which) -> {
                                Intent intent = new Intent(ClientOrdersActivity.this, ReviewActivity.class);
                                intent.putExtra("masterId", order.getMasterId());
                                intent.putExtra("orderId", order.getOrderId());
                                startActivity(intent);
                            })
                            .setNegativeButton("Позже", null)
                            .setCancelable(false)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check clientReviewLeft flag for order: " + order.getOrderId(), error.toException());
                Toast.makeText(ClientOrdersActivity.this, "Не удалось проверить историю отзывов, попробуйте позже.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onOpenDisputeByClient(Order order) {
        Log.d(TAG, "Client opens dispute for order: " + order.getOrderId());
        if (order == null || order.getOrderId() == null) return;
        if (isFinishing() || isDestroyed()) return;

        // TODO: Показать диалог для ввода причины спора, если это необходимо.

        showLoadingState(true);
        DatabaseReference orderRef = allOrdersDataRef.child(order.getOrderId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Order.STATUS_DISPUTED);
        updates.put("disputeTimestamp", ServerValue.TIMESTAMP);

        orderRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoadingState(false);
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Dispute opened for order: " + order.getOrderId());
                        Toast.makeText(this, "Спор по заказу открыт.", Toast.LENGTH_SHORT).show();
                        // TODO: Отправить FCM мастеру
                    } else {
                        Log.e(TAG, "Failed to open dispute for order: " + order.getOrderId(), task.getException());
                        Toast.makeText(this, "Не удалось открыть спор: " + (task.getException() != null ? task.getException().getMessage() : "Ошибка"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onAcceptOrder(Order order) {
    }

    @Override
    public void onRejectOrder(Order order) {
    }

    @Override
    public void onCompleteOrder(Order order) {
    }

    @Override
    public void onCallClient(String phoneNumber) {

    }

    @Override
    public void onRateClient(Order order) {
    }

    @Override
    public void onCallMaster(String phoneNumber) {
        Log.d(TAG, "ACTION: Client calling master: " + phoneNumber);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть набор номера", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Номер мастера не указан", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(Order order) {
        Log.d(TAG, "ACTION: Client clicked on order item: " + order.getOrderId());
        // TODO: Реализовать переход на экран деталей заказа для клиента, если он нужен.
        Toast.makeText(this, "Детали заказа клиента (в разработке): " + order.getProblemDescription(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeaveReviewByClient(Order order) {
        Log.d(TAG, "Client wants to leave review for order: " + order.getOrderId());
        promptToLeaveReview(order);
    }

    private void detachAllFirebaseListeners() {
        if (userClientOrdersIdsRef != null && userClientOrderIdsListener != null) {
            userClientOrdersIdsRef.removeEventListener(userClientOrderIdsListener);
            userClientOrderIdsListener = null;
            Log.d(TAG, "UserClientOrderIdsListener detached.");
        }
        if (allOrdersDataRef != null && !orderDetailListeners.isEmpty()) {
            for (Map.Entry<String, ValueEventListener> entry : orderDetailListeners.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    allOrdersDataRef.child(entry.getKey()).removeEventListener(entry.getValue());
                }
            }
            orderDetailListeners.clear();
            Log.d(TAG, "OrderDetailsListeners detached.");
        }
        Log.d(TAG, "All ClientOrdersActivity Firebase listeners detached attempt complete.");
    }

    @Override
    protected void onDestroy() {
        detachAllFirebaseListeners();
        super.onDestroy();
    }
}