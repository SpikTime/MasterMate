package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.example.mastermate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private RadioGroup roleRadioGroup;
    private RadioButton clientRadioButton, masterRadioButton;
    private Button registerButton;
    private FrameLayout registrationFormContainer;
    private ChipGroup specializationChipGroup;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        clientRadioButton = findViewById(R.id.clientRadioButton);
        masterRadioButton = findViewById(R.id.masterRadioButton);
        registerButton = findViewById(R.id.registerButton);
        registrationFormContainer = findViewById(R.id.registrationFormContainer);

        mAuth = FirebaseAuth.getInstance();

        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            registrationFormContainer.removeAllViews();
            View specificForm = null;
            specializationChipGroup = null;

            if (checkedId == R.id.clientRadioButton) {
                specificForm = getLayoutInflater().inflate(R.layout.activity_register_client, registrationFormContainer, false);
            } else if (checkedId == R.id.masterRadioButton) {
                specificForm = getLayoutInflater().inflate(R.layout.activity_register_master, registrationFormContainer, false);
                if (specificForm != null) {
                    specializationChipGroup = specificForm.findViewById(R.id.specializationChipGroupRegister);
                    if (specializationChipGroup != null) {
                        setupSpecializationChips(specializationChipGroup);
                    } else {
                        Log.e(TAG, "ChipGroup specializationChipGroupRegister not found in master registration form!");
                    }
                }
            }

            if (specificForm != null) {
                registrationFormContainer.addView(specificForm);
            }
        });

        registerButton.setOnClickListener(v -> registerUser());

        roleRadioGroup.check(R.id.clientRadioButton);
    }

    private void setupSpecializationChips(ChipGroup chipGroup) {
        if (getResources() == null) {
            Log.e(TAG, "getResources() returned null in setupSpecializationChips");
            return;
        }
        String[] specializations = getResources().getStringArray(R.array.master_specializations_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        chipGroup.removeAllViews();

        for (String specialization : specializations) {
            try {
                Chip chip = (Chip) inflater.inflate(R.layout.item_chip_filter, chipGroup, false);
                chip.setText(specialization);
                chip.setCheckable(true);
                chip.setCloseIconVisible(false);
                chipGroup.addView(chip);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating or adding chip: " + specialization, e);
            }
        }
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String role = clientRadioButton.isChecked() ? "client" : "master";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show(); return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show(); return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают!", Toast.LENGTH_SHORT).show(); return;
        }
        if (roleRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Пожалуйста, выберите роль", Toast.LENGTH_SHORT).show(); return;
        }

        List<String> selectedSpecializations = new ArrayList<>();
        if (role.equals("master")) {
            if (specializationChipGroup != null) {
                List<Integer> checkedChipIds = specializationChipGroup.getCheckedChipIds();
                if (checkedChipIds.isEmpty()) {
                    Toast.makeText(this, "Выберите хотя бы одну специализацию", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int chipId : checkedChipIds) {
                    Chip chip = specializationChipGroup.findViewById(chipId);
                    if (chip != null) {
                        selectedSpecializations.add(chip.getText().toString());
                    }
                }
                Log.d(TAG, "Selected specializations: " + selectedSpecializations);
            } else {
                Log.e(TAG, "specializationChipGroup is null when trying to register master!");
                Toast.makeText(this, "Ошибка: Не удалось найти поле специализаций.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        registerButton.setEnabled(false);
        Toast.makeText(this, "Регистрация...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Log.e(TAG, "FirebaseUser is null after successful creation.");
                            Toast.makeText(RegisterActivity.this, "Ошибка регистрации: не удалось получить пользователя.", Toast.LENGTH_SHORT).show();
                            registerButton.setEnabled(true);
                            return;
                        }
                        String userId = user.getUid();

                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        DatabaseReference newUserRef = usersRef.child(userId);

                        HashMap<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("role", role);
                        userData.put("id", userId);
                        userData.put("phoneVerified", false);

                        if (role.equals("master")) {
                            EditText descriptionEditText = registrationFormContainer.findViewById(R.id.editTextDescription);
                            EditText experienceEditText = registrationFormContainer.findViewById(R.id.editTextExperience);
                            EditText phoneEditText = registrationFormContainer.findViewById(R.id.editTextPhone);
                            EditText cityEditText = registrationFormContainer.findViewById(R.id.editTextCity);
                            EditText addressEditText = registrationFormContainer.findViewById(R.id.editTextAddress);

                            userData.put("specializations", selectedSpecializations);

                            userData.put("description", descriptionEditText != null ? descriptionEditText.getText().toString().trim() : "");
                            userData.put("experience", experienceEditText != null ? experienceEditText.getText().toString().trim() : "");
                            userData.put("phoneNumber", phoneEditText != null ? phoneEditText.getText().toString().trim() : "");
                            userData.put("city", cityEditText != null ? cityEditText.getText().toString().trim() : "");
                            userData.put("address", addressEditText != null ? addressEditText.getText().toString().trim() : "");
                            userData.put("rating", 0.0);
                            userData.put("reviewCount", 0L);
                            userData.put("ratingSum", 0.0);
                            userData.put("workingHours", createDefaultWorkingHours());
                            userData.put("latitude", 0.0);
                            userData.put("longitude", 0.0);
                            userData.put("imageUrl", "");
                        }

                        newUserRef.setValue(userData)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.d(TAG, "User data saved successfully for userId: " + userId);
                                        Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                                        Intent nextIntent;
                                        if (role.equals("master")) {
                                            nextIntent = new Intent(RegisterActivity.this, ProfileMasterActivity.class);
                                        } else {
                                            nextIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                        }
                                        nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(nextIntent);
                                        finish();

                                    } else {
                                        Log.e(TAG, "Failed to save user data", task1.getException());
                                        Toast.makeText(RegisterActivity.this, "Ошибка сохранения данных: " + safeGetErrorMessage(task1.getException()), Toast.LENGTH_SHORT).show();
                                        registerButton.setEnabled(true);
                                    }
                                });
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Ошибка регистрации: " + safeGetErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        registerButton.setEnabled(true);
                    }
                });
    }

    private Map<String, Object> createDefaultWorkingHours() {
        Map<String, Object> defaultHours = new HashMap<>();
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : days) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("working", false);
            dayData.put("startTime", "");
            dayData.put("endTime", "");
            defaultHours.put(day, dayData);
        }
        return defaultHours;
    }

    private String safeGetErrorMessage(Exception e) {
        return (e != null && e.getMessage() != null) ? e.getMessage() : "Неизвестная ошибка";
    }
}