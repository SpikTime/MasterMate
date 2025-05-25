package com.example.mastermate.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.mastermate.R;
import com.example.mastermate.models.Master;
import com.example.mastermate.models.Order;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateOrderActivity extends AppCompatActivity {

    private static final String TAG = "CreateOrderActivity";

    private Toolbar toolbar;
    private CircleImageView orderMasterImageView;
    private TextView orderMasterNameTextView;
    private TextView orderMasterSpecTextView;
    private ProgressBar masterInfoProgressBar;
    private EditText problemDescriptionEditText;
    private TextView selectedAddressTextView;
    private MaterialButton selectAddressButton;
    private ProgressBar addressProgressBar;
    private TextView clientPhoneNumberTextView;
    private ProgressBar phoneProgressBar;
    private MaterialButton submitOrderButton;
    private ProgressBar orderSubmitProgressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRootRef;
    private DatabaseReference ordersRef;
    private DatabaseReference masterRef;
    private DatabaseReference clientRef;
    private String masterId;
    private Master selectedMaster;
    private String currentClientName;
    private String currentClientPhoneNumber;
    private String currentSelectedAddress;
    private Double currentSelectedLatitude;

    private Double currentSelectedLongitude;

    private ActivityResultLauncher<Intent> selectLocationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        masterId = getIntent().getStringExtra("masterId");
        if (masterId == null || masterId.isEmpty()) {
            Log.e(TAG, "Master ID is missing in Intent.");
            Toast.makeText(this, "Ошибка: Не найден ID мастера.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User is not logged in. Redirecting to LoginActivity.");
            Toast.makeText(this, "Ошибка: Вы не авторизованы.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }

        databaseRootRef = FirebaseDatabase.getInstance().getReference();
        ordersRef = databaseRootRef.child("orders");
        masterRef = databaseRootRef.child("users").child(masterId);
        clientRef = databaseRootRef.child("users").child(currentUser.getUid());

        initializeUI();
        setupToolbar();
        initializeLocationLauncher();
        setupListeners();
        loadMasterInfo();
        loadClientInfo();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar_create_order);
        orderMasterImageView = findViewById(R.id.orderMasterImageView);
        orderMasterNameTextView = findViewById(R.id.orderMasterNameTextView);
        orderMasterSpecTextView = findViewById(R.id.orderMasterSpecTextView);
        masterInfoProgressBar = findViewById(R.id.masterInfoProgressBar);
        problemDescriptionEditText = findViewById(R.id.problemDescriptionEditText);
        selectedAddressTextView = findViewById(R.id.selectedAddressTextView);
        selectAddressButton = findViewById(R.id.selectAddressButton);
        addressProgressBar = findViewById(R.id.addressProgressBar);
        clientPhoneNumberTextView = findViewById(R.id.clientPhoneNumberTextView);
        phoneProgressBar = findViewById(R.id.phoneProgressBar);
        submitOrderButton = findViewById(R.id.submitOrderButton);
        orderSubmitProgressBar = findViewById(R.id.orderSubmitProgressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeLocationLauncher() {
        selectLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        currentSelectedAddress = data.getStringExtra("address");
                        currentSelectedLatitude = data.getDoubleExtra("latitude", 0.0);
                        currentSelectedLongitude = data.getDoubleExtra("longitude", 0.0);

                        if (selectedAddressTextView != null && currentSelectedAddress != null) {
                            selectedAddressTextView.setText(currentSelectedAddress);
                            // selectedAddressTextView.setError(null); // Для TextView это не нужно
                            Log.d(TAG, "Location selected: " + currentSelectedAddress + " (" + currentSelectedLatitude + ", " + currentSelectedLongitude + ")");
                        }
                    } else {
                        Log.d(TAG, "Location selection cancelled or failed.");
                        if (selectedAddressTextView != null && TextUtils.isEmpty(selectedAddressTextView.getText())) {
                            selectedAddressTextView.setHint("Нажмите, чтобы выбрать адрес на карте");
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        if (submitOrderButton != null) {
            submitOrderButton.setOnClickListener(v -> attemptToCreateOrder());
        }
        if (selectAddressButton != null) {
            selectAddressButton.setOnClickListener(v -> {
                Intent intent = new Intent(CreateOrderActivity.this, SelectOrderLocationActivity.class);
                if (currentSelectedLatitude != null && currentSelectedLongitude != null &&
                        (currentSelectedLatitude != 0.0 || currentSelectedLongitude != 0.0)) {
                    intent.putExtra("initialLatitude", currentSelectedLatitude);
                    intent.putExtra("initialLongitude", currentSelectedLongitude);
                }
                selectLocationLauncher.launch(intent);
            });
        }
        if (selectedAddressTextView != null) {
            selectedAddressTextView.setOnClickListener(v -> selectAddressButton.performClick());
        }
    }

    private void loadMasterInfo() {
        if (masterInfoProgressBar != null) masterInfoProgressBar.setVisibility(View.VISIBLE);
        masterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (masterInfoProgressBar != null) masterInfoProgressBar.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    selectedMaster = snapshot.getValue(Master.class);
                    if (selectedMaster != null) {
                        selectedMaster.setId(masterId);
                        orderMasterNameTextView.setText(selectedMaster.getName());
                        orderMasterSpecTextView.setText(selectedMaster.getSpecializationsString());
                        if (orderMasterImageView != null && selectedMaster.getImageUrl() != null && !selectedMaster.getImageUrl().isEmpty()) {
                            Glide.with(CreateOrderActivity.this)
                                    .load(selectedMaster.getImageUrl())
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(orderMasterImageView);
                        } else if (orderMasterImageView != null) {
                            orderMasterImageView.setImageResource(R.drawable.ic_person);
                        }
                    } else { handleLoadError("Не удалось загрузить данные мастера (объект null)."); }
                } else { handleLoadError("Мастер с ID " + masterId + " не найден."); }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                if (masterInfoProgressBar != null) masterInfoProgressBar.setVisibility(View.GONE);
                handleLoadError("Ошибка загрузки данных мастера: " + safeGetErrorMessage(error.toException()));
            }
        });
    }

    private void loadClientInfo() {
        if (phoneProgressBar != null) phoneProgressBar.setVisibility(View.VISIBLE);
        clientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (phoneProgressBar != null) phoneProgressBar.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    currentClientName = snapshot.child("name").getValue(String.class);
                    currentClientPhoneNumber = snapshot.child("phoneNumber").getValue(String.class);

                    if (currentClientName == null || currentClientName.isEmpty()) {
                        currentClientName = currentUser.getDisplayName();
                        if (currentClientName == null || currentClientName.isEmpty()) {
                            currentClientName = currentUser.getEmail() != null ? currentUser.getEmail() : "Клиент";
                        }
                    }

                    if (clientPhoneNumberTextView != null && currentClientPhoneNumber != null && !currentClientPhoneNumber.isEmpty()) {
                        clientPhoneNumberTextView.setText(currentClientPhoneNumber);
                    } else if (clientPhoneNumberTextView != null) {
                        clientPhoneNumberTextView.setText("Номер не указан в профиле");
                        try { clientPhoneNumberTextView.setTextColor(ContextCompat.getColor(CreateOrderActivity.this, R.color.colorErrorLight)); }
                        catch (Resources.NotFoundException e) { Log.e(TAG, "Color resource R.color.colorErrorLight not found", e); }
                    }
                } else {
                    Log.w(TAG, "Client data node not found for user: " + currentUser.getUid());
                    currentClientName = currentUser.getEmail() != null ? currentUser.getEmail() : "Клиент";
                    if (clientPhoneNumberTextView != null) clientPhoneNumberTextView.setText("Номер не найден");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                if (phoneProgressBar != null) phoneProgressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load client info", error.toException());
                Toast.makeText(CreateOrderActivity.this, "Ошибка загрузки данных профиля", Toast.LENGTH_SHORT).show();
                currentClientName = currentUser.getEmail() != null ? currentUser.getEmail() : "Клиент";
                if (clientPhoneNumberTextView != null) clientPhoneNumberTextView.setText("Ошибка загрузки номера");
            }
        });
    }

    private void handleLoadError(String message) {
        if (!isFinishing()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (submitOrderButton != null) submitOrderButton.setEnabled(false);
    }

    private void attemptToCreateOrder() {
        String problemDesc = problemDescriptionEditText.getText().toString().trim();

        boolean isValid = true;
        if (TextUtils.isEmpty(problemDesc)) {
            problemDescriptionEditText.setError("Опишите проблему");
            problemDescriptionEditText.requestFocus();
            isValid = false;
        } else {
            problemDescriptionEditText.setError(null);
        }

        if (TextUtils.isEmpty(currentSelectedAddress) || currentSelectedLatitude == null || currentSelectedLongitude == null ||
                (currentSelectedLatitude == 0.0 && currentSelectedLongitude == 0.0)) {
            Toast.makeText(this, "Пожалуйста, выберите адрес на карте", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(currentClientPhoneNumber)) {
            Toast.makeText(this, "В вашем профиле не указан номер телефона. Обновите профиль.", Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (selectedMaster == null) {
            Toast.makeText(this, "Данные мастера не загружены. Пожалуйста, подождите или попробуйте позже.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        if (currentClientName == null || currentClientName.isEmpty()) {
            Toast.makeText(this, "Не удалось получить имя клиента.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        if (orderSubmitProgressBar != null) orderSubmitProgressBar.setVisibility(View.VISIBLE);
        if (submitOrderButton != null) submitOrderButton.setEnabled(false);

        createOrderInFirebase(problemDesc, currentSelectedAddress, currentSelectedLatitude, currentSelectedLongitude);
    }

    private void createOrderInFirebase(String description, String address, double latitude, double longitude) {
        String orderId = ordersRef.push().getKey();
        if (orderId == null) {
            showSubmitError("Не удалось сгенерировать ID заказа.");
            return;
        }


        String clientUserId = currentUser.getUid();
        String currentClientNameStr = (this.currentClientName != null && !this.currentClientName.isEmpty()) ? this.currentClientName : "Клиент";
        String currentClientPhoneStr = (this.currentClientPhoneNumber != null) ? this.currentClientPhoneNumber : "";

        String currentMasterIdStr = this.masterId;
        String currentMasterNameStr = (selectedMaster != null && selectedMaster.getName() != null) ? selectedMaster.getName() : "Мастер";
        String currentMasterSpecStr = (selectedMaster != null) ? selectedMaster.getSpecializationsString() : "";
        String currentMasterPhoneStr = (selectedMaster != null && selectedMaster.getPhoneNumber() != null) ? selectedMaster.getPhoneNumber() : "";

        Order newOrder = new Order(
                orderId,
                clientUserId,
                currentClientNameStr,
                currentClientPhoneStr,
                currentMasterIdStr,
                currentMasterNameStr,
                currentMasterSpecStr,
                currentMasterPhoneStr,
                description,
                address,
                Double.valueOf(latitude),
                Double.valueOf(longitude)
        );
        Log.d(TAG, "Creating order object with ID: " + orderId);
        Toast.makeText(this, "Отправка заявки...", Toast.LENGTH_SHORT).show();

        ordersRef.child(orderId).setValue(newOrder)
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Order created successfully in /orders node.");
                        addOrderReferenceToUser(clientRef.child("orders"), orderId);
                        addOrderReferenceToUser(masterRef.child("orders"), orderId);
                        Toast.makeText(CreateOrderActivity.this, "Заявка успешно отправлена!", Toast.LENGTH_LONG).show();
                        // TODO: Отправить FCM уведомление мастеру
                        finish();
                    } else {
                        Log.e(TAG, "Failed to create order in /orders node.", task.getException());
                        showSubmitError("Не удалось создать заявку: " + safeGetErrorMessage(task.getException()));
                    }
                });
    }

    private void addOrderReferenceToUser(DatabaseReference userSpecificOrdersRef, String orderId) {
        if (userSpecificOrdersRef != null && orderId != null) {
            userSpecificOrdersRef.child(orderId).setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Order reference '" + orderId + "' added for user: " + userSpecificOrdersRef.getParent().getKey()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add order reference '" + orderId + "' for user: " + userSpecificOrdersRef.getParent().getKey(), e));
        }
    }

    private void showSubmitError(String message) {
        if (message != null && !isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        if (orderSubmitProgressBar != null) orderSubmitProgressBar.setVisibility(View.GONE);
        if (submitOrderButton != null) submitOrderButton.setEnabled(true);
    }

    private String safeGetErrorMessage(Exception e) {
        return (e != null && e.getMessage() != null) ? e.getMessage() : "Неизвестная ошибка";
    }
}