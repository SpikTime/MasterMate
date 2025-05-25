package com.example.mastermate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mastermate.R;
import com.example.mastermate.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.database.ServerValue;


public class OrderDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailsActivity";

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout contentLayout;
    private TextView tvStatus, tvClientName, tvClientPhone, tvAddress, tvProblemDescription, tvCreationDate;
    private Button btnOrderAction;

    private String orderId;
    private DatabaseReference orderRef;
    private ValueEventListener orderListener;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        orderId = getIntent().getStringExtra("orderId");

        initializeViews();
        setupToolbar();

        if (orderId == null || orderId.isEmpty()) {
            Log.e(TAG, "Order ID is missing from Intent.");
            Toast.makeText(this, "Ошибка: ID заказа не найден.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        attachOrderListener();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_order_details);
        progressBar = findViewById(R.id.orderDetailsProgressBar);
        contentLayout = findViewById(R.id.orderDetailsContent);
        tvStatus = findViewById(R.id.tvOrderStatus);
        tvClientName = findViewById(R.id.tvOrderClientName);
        tvClientPhone = findViewById(R.id.tvOrderClientPhone);
        tvAddress = findViewById(R.id.tvOrderAddress);
        tvProblemDescription = findViewById(R.id.tvOrderProblemDescription);
        tvCreationDate = findViewById(R.id.tvOrderCreationDate);
        btnOrderAction = findViewById(R.id.btnOrderAction);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void attachOrderListener() {
        showLoading(true);
        if (orderListener != null && orderRef != null) {
            orderRef.removeEventListener(orderListener);
        }
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "Order with ID " + orderId + " does not exist.");
                    Toast.makeText(OrderDetailsActivity.this, "Заказ не найден.", Toast.LENGTH_LONG).show();
                    showLoading(false);
                    finish();
                    return;
                }
                currentOrder = snapshot.getValue(Order.class);
                if (currentOrder != null) {
                    currentOrder.setOrderId(snapshot.getKey());
                    displayOrderDetails(currentOrder);
                    setupActionButtons(currentOrder);
                } else {
                    Log.e(TAG, "Failed to parse order data for ID: " + orderId);
                    Toast.makeText(OrderDetailsActivity.this, "Ошибка загрузки данных заказа.", Toast.LENGTH_SHORT).show();
                }
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load order details for ID: " + orderId, error.toException());
                Toast.makeText(OrderDetailsActivity.this, "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        };
        orderRef.addValueEventListener(orderListener);
    }

    private void displayOrderDetails(Order order) {
        tvStatus.setText(getDisplayableOrderStatus(order.getStatus()));
        tvClientName.setText(order.getClientName() != null ? order.getClientName() : "Не указано");
        tvClientPhone.setText(order.getClientPhoneNumber() != null ? order.getClientPhoneNumber() : "Не указан");
        if (order.getClientPhoneNumber() != null && !order.getClientPhoneNumber().isEmpty()) {
            tvClientPhone.setOnClickListener(v -> dialPhoneNumber(order.getClientPhoneNumber()));
        } else {
            tvClientPhone.setOnClickListener(null);
        }
        tvAddress.setText(order.getClientAddress() != null ? order.getClientAddress() : "Не указан");
        tvProblemDescription.setText(order.getProblemDescription() != null ? order.getProblemDescription() : "Описание отсутствует.");

        if (order.getCreationTimestampLong() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault());
            tvCreationDate.setText(sdf.format(new Date(order.getCreationTimestampLong())));
        } else {
            tvCreationDate.setText("Не указана");
        }
    }

    private void setupActionButtons(Order order) {
        String status = order.getStatus();
        if (status == null) {
            btnOrderAction.setVisibility(View.GONE);
            return;
        }

        btnOrderAction.setVisibility(View.VISIBLE);
        btnOrderAction.setOnClickListener(null);

        switch (status.toLowerCase()) {
            case Order.STATUS_NEW:
                btnOrderAction.setText("Принять заказ");
                btnOrderAction.setOnClickListener(v -> updateOrderStatus(Order.STATUS_ACCEPTED, ServerValue.TIMESTAMP, "acceptedTimestamp"));
                break;
            case Order.STATUS_ACCEPTED:
            case Order.STATUS_IN_PROGRESS:
                btnOrderAction.setText("Завершить заказ");
                btnOrderAction.setOnClickListener(v -> updateOrderStatus(Order.STATUS_COMPLETED_MASTER, ServerValue.TIMESTAMP, "masterCompletionTimestamp"));
                break;
            default:
                btnOrderAction.setVisibility(View.GONE);
                break;
        }
    }

    private void updateOrderStatus(String newStatus, Object timestampValue, String timestampField) {
        if (orderRef == null || newStatus == null) return;
        showLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        if (timestampField != null && timestampValue != null) {
            updates.put(timestampField, timestampValue);
        }

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Order status updated to " + newStatus);
                    Toast.makeText(OrderDetailsActivity.this, "Статус заказа обновлен", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update order status", e);
                    Toast.makeText(OrderDetailsActivity.this, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }


    private String getDisplayableOrderStatus(String status) {
        if (status == null) return "Неизвестно";
        switch (status.toLowerCase()) {
            case Order.STATUS_NEW: return "Новый";
            case Order.STATUS_ACCEPTED: return "Принят";
            case Order.STATUS_IN_PROGRESS: return "В работе";
            case Order.STATUS_COMPLETED_MASTER: return "Завершен вами (ожидает клиента)";
            case Order.STATUS_CONFIRMED_CLIENT: return "Заказ выполнен и подтвержден";
            case Order.STATUS_REJECTED_MASTER: return "Отклонен вами";
            case Order.STATUS_CANCELLED_CLIENT: return "Отменен клиентом";
            case Order.STATUS_DISPUTED: return "Открыт спор";
            default: return status;
        }
    }

    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Приложение для звонка не найдено.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null && orderRef != null) {
            orderRef.removeEventListener(orderListener);
        }
    }
}