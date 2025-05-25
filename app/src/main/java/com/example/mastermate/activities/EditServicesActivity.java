package com.example.mastermate.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.adapters.EditableServiceAdapter;
import com.example.mastermate.models.ServiceItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.Map;
import java.util.HashMap; // Для обновлений

public class EditServicesActivity extends BaseActivity implements EditableServiceAdapter.ServiceActionsListener {

    private static final String TAG = "EditServicesActivity";

    private Toolbar toolbar;
    private RecyclerView servicesRecyclerView;
    private EditableServiceAdapter servicesAdapter;
    private List<ServiceItem> serviceItemList;
    private FloatingActionButton fabAddService;
    private ProgressBar servicesLoadingProgressBar;
    private TextView emptyServicesTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference masterServicesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_services);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Ошибка: Пользователь не авторизован.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User is not authenticated.");
            finish();
            return;
        }

        // Ссылка на узел услуг текущего мастера
        masterServicesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid())
                .child("services");

        initializeUI();
        setupToolbar();
        setupRecyclerView();
        setupFab();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar_edit_services);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        fabAddService = findViewById(R.id.fabAddService);
        servicesLoadingProgressBar = findViewById(R.id.servicesLoadingProgressBar);
        emptyServicesTextView = findViewById(R.id.emptyServicesTextView);
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
        serviceItemList = new ArrayList<>();
        servicesAdapter = new EditableServiceAdapter(this, serviceItemList, this);
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        servicesRecyclerView.setAdapter(servicesAdapter);
    }

    private void showEditServiceDialog(@Nullable final ServiceItem existingService) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_service, null);
        builder.setView(dialogView);

        final TextInputLayout serviceNameLayout = dialogView.findViewById(R.id.serviceNameInputLayout);
        final EditText editTextServiceName = dialogView.findViewById(R.id.editTextServiceName);
        final EditText editTextServiceDescription = dialogView.findViewById(R.id.editTextServiceDescription);
        final TextInputLayout priceMinLayout = dialogView.findViewById(R.id.priceMinInputLayout);
        final EditText editTextPriceMin = dialogView.findViewById(R.id.editTextPriceMin);
        final TextInputLayout priceMaxLayout = dialogView.findViewById(R.id.priceMaxInputLayout);
        final EditText editTextPriceMax = dialogView.findViewById(R.id.editTextPriceMax);
        final Spinner spinnerPriceUnit = dialogView.findViewById(R.id.spinnerPriceUnit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.price_units_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriceUnit.setAdapter(adapter);

        if (existingService != null) {
            builder.setTitle("Редактировать услугу");
            editTextServiceName.setText(existingService.getServiceName());
            editTextServiceDescription.setText(existingService.getDescription());
            if (existingService.getPriceMin() > 0) editTextPriceMin.setText(String.valueOf(existingService.getPriceMin()));
            if (existingService.getPriceMax() > 0) editTextPriceMax.setText(String.valueOf(existingService.getPriceMax()));

            if (existingService.getPriceUnit() != null) {
                int spinnerPosition = adapter.getPosition(existingService.getPriceUnit());
                if (spinnerPosition >= 0) {
                    spinnerPriceUnit.setSelection(spinnerPosition);
                } else {
                    int defaultPos = adapter.getPosition("по договоренности");
                    spinnerPriceUnit.setSelection(defaultPos >=0 ? defaultPos : 0);
                }
            }
        } else {
            builder.setTitle("Добавить новую услугу");
            int defaultPos = adapter.getPosition("за услугу");
            if (defaultPos >= 0) {
                spinnerPriceUnit.setSelection(defaultPos);
            }
        }

        builder.setPositiveButton("Сохранить", null);
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editTextServiceName.getText().toString().trim();
            String description = editTextServiceDescription.getText().toString().trim();
            String priceMinStr = editTextPriceMin.getText().toString().trim();
            String priceMaxStr = editTextPriceMax.getText().toString().trim();
            String unit = "";
            if (spinnerPriceUnit.getSelectedItem() != null) {
                unit = spinnerPriceUnit.getSelectedItem().toString();
            }

            double priceMin = 0.0;
            double priceMax = 0.0;
            boolean valid = true;

            serviceNameLayout.setError(null);
            priceMinLayout.setError(null);
            priceMaxLayout.setError(null);

            if (TextUtils.isEmpty(name)) {
                serviceNameLayout.setError("Название услуги обязательно");
                valid = false;
            }

            if (TextUtils.isEmpty(unit) || unit.equals(getResources().getStringArray(R.array.price_units_array)[0]) ) {
            }


            try {
                if (!priceMinStr.isEmpty()) priceMin = Double.parseDouble(priceMinStr);
                if (priceMin < 0) { priceMinLayout.setError("Цена не может быть < 0"); valid = false; }
            } catch (NumberFormatException e) {
                if (!priceMinStr.isEmpty()) { priceMinLayout.setError("Неверный формат"); valid = false; }
            }

            try {
                if (!priceMaxStr.isEmpty()) priceMax = Double.parseDouble(priceMaxStr);
                if (priceMax < 0) { priceMaxLayout.setError("Цена не может быть < 0"); valid = false; }
            } catch (NumberFormatException e) {
                if (!priceMaxStr.isEmpty()) { priceMaxLayout.setError("Неверный формат"); valid = false; }
            }

            if (priceMin > 0 && priceMax > 0 && priceMin > priceMax) {
                priceMinLayout.setError("Цена 'от' > 'до'");
                priceMaxLayout.setError("Цена 'до' < 'от'");
                valid = false;
            }
            if (unit.equalsIgnoreCase("по договоренности")) {
                priceMin = 0;
                priceMax = 0;
                priceMinLayout.setError(null);
                priceMaxLayout.setError(null);
            } else if (priceMin == 0 && priceMax == 0) {
                priceMinLayout.setError("Укажите цену");
                valid = false;
            }


            if (valid) {
                String serviceId;
                ServiceItem serviceToSave;

                if (existingService != null) {
                    serviceId = existingService.getServiceId();
                    serviceToSave = existingService;
                } else {
                    serviceId = masterServicesRef.push().getKey();
                    serviceToSave = new ServiceItem();
                    if (serviceId == null) {
                        Toast.makeText(EditServicesActivity.this, "Не удалось создать ID", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); return;
                    }
                    serviceToSave.setServiceId(serviceId);
                }

                serviceToSave.setServiceName(name);
                serviceToSave.setDescription(description);
                serviceToSave.setPriceMin(priceMin);
                serviceToSave.setPriceMax(priceMax);
                serviceToSave.setPriceUnit(unit);

                masterServicesRef.child(serviceId).setValue(serviceToSave)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditServicesActivity.this, "Услуга сохранена", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(EditServicesActivity.this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void setupFab() {
        fabAddService.setOnClickListener(view -> {
            // TODO: Открыть диалог для добавления новой услуги
            showEditServiceDialog(null);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: Отписаться от слушателя masterServicesRef, если он будет постоянным
    }

    private void loadServices() {
        showLoading(true);
        masterServicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                serviceItemList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ServiceItem service = snapshot.getValue(ServiceItem.class);
                        if (service != null) {
                            service.setServiceId(snapshot.getKey());
                            serviceItemList.add(service);
                        }
                    }
                    Log.d(TAG, "Loaded " + serviceItemList.size() + " services.");
                } else {
                    Log.d(TAG, "No services found for this master.");
                }
                // TODO: Можно добавить сортировку serviceItemList, если нужно
                servicesAdapter.notifyDataSetChanged();
                checkEmptyState();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load services", databaseError.toException());
                Toast.makeText(EditServicesActivity.this, "Ошибка загрузки услуг: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                checkEmptyState();
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (servicesLoadingProgressBar != null) servicesLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (servicesRecyclerView != null) servicesRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (emptyServicesTextView != null) emptyServicesTextView.setVisibility(View.GONE);
    }

    private void checkEmptyState() {
        if (emptyServicesTextView != null && servicesRecyclerView != null) {
            if (serviceItemList.isEmpty()) {
                servicesRecyclerView.setVisibility(View.GONE);
                emptyServicesTextView.setVisibility(View.VISIBLE);
            } else {
                servicesRecyclerView.setVisibility(View.VISIBLE);
                emptyServicesTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onEditService(ServiceItem serviceItem) {
        Log.d(TAG, "Editing service: " + serviceItem.getServiceName());
        // TODO: Открыть диалог для редактирования serviceItem
        showEditServiceDialog(serviceItem);
    }

    @Override
    public void onDeleteService(ServiceItem serviceItem) {
        Log.d(TAG, "Deleting service: " + serviceItem.getServiceName() + " with ID: " + serviceItem.getServiceId());
        if (serviceItem.getServiceId() == null || serviceItem.getServiceId().isEmpty()) {
            Toast.makeText(this, "Ошибка: ID услуги не найден.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Удалить услугу?")
                .setMessage("Вы уверены, что хотите удалить услугу '" + serviceItem.getServiceName() + "'?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    masterServicesRef.child(serviceItem.getServiceId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(EditServicesActivity.this, "Услуга '" + serviceItem.getServiceName() + "' удалена", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(EditServicesActivity.this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


}