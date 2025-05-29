package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.textfield.TextInputLayout;
import com.example.mastermate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
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

    private ChipGroup specializationChipGroupMaster;
    private EditText editTextDescriptionMaster, editTextExperienceMaster, editTextPhoneMaster, editTextCityMaster, editTextAddressMaster;

    private TextInputLayout clientPhoneInputLayout;
    private EditText editTextClientPhoneNumber;

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
            clearSpecificFormReferences();

            if (checkedId == R.id.clientRadioButton) {
                specificForm = getLayoutInflater().inflate(R.layout.activity_register_client, registrationFormContainer, false);
                if (specificForm != null) {
                    clientPhoneInputLayout = specificForm.findViewById(R.id.clientPhoneInputLayout);
                    editTextClientPhoneNumber = specificForm.findViewById(R.id.editTextClientPhoneNumber);
                }
            } else if (checkedId == R.id.masterRadioButton) {
                specificForm = getLayoutInflater().inflate(R.layout.activity_register_master, registrationFormContainer, false);
                if (specificForm != null) {
                    specializationChipGroupMaster = specificForm.findViewById(R.id.specializationChipGroupRegister);
                    editTextDescriptionMaster = specificForm.findViewById(R.id.editTextDescription);
                    editTextExperienceMaster = specificForm.findViewById(R.id.editTextExperience);
                    editTextPhoneMaster = specificForm.findViewById(R.id.editTextPhone);
                    editTextCityMaster = specificForm.findViewById(R.id.editTextCity);
                    editTextAddressMaster = specificForm.findViewById(R.id.editTextAddress);
                    if (specializationChipGroupMaster != null) {
                        setupSpecializationChips(specializationChipGroupMaster);
                    }
                }
            }

            if (specificForm != null) {
                registrationFormContainer.addView(specificForm);
            }
        });

        registerButton.setOnClickListener(v -> attemptRegisterUser());
        roleRadioGroup.check(R.id.clientRadioButton); // Load client form by default
    }

    private void clearSpecificFormReferences() {
        specializationChipGroupMaster = null;
        editTextDescriptionMaster = null;
        editTextExperienceMaster = null;
        editTextPhoneMaster = null;
        editTextCityMaster = null;
        editTextAddressMaster = null;
        clientPhoneInputLayout = null;
        editTextClientPhoneNumber = null;
    }

    private void setupSpecializationChips(ChipGroup chipGroup) {
        if (getResources() == null || chipGroup == null) {
            Log.e(TAG, "Cannot setup specialization chips: resources or chipGroup is null.");
            return;
        }
        String[] specializations = getResources().getStringArray(R.array.master_specializations_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        chipGroup.removeAllViews();

        for (String specializationName : specializations) {
            if (specializationName.equalsIgnoreCase("Все")) continue; // Пропускаем "Все"
            try {
                Chip chip = (Chip) inflater.inflate(R.layout.item_chip_filter, chipGroup, false);
                chip.setText(specializationName);
                chip.setCheckable(true);
                chip.setCloseIconVisible(false);
                chipGroup.addView(chip);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating chip for specialization: " + specializationName, e);
            }
        }
    }

    private void attemptRegisterUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String role = clientRadioButton.isChecked() ? "client" : "master";
        String clientPhoneNumber = "";
        List<String> selectedSpecializations = new ArrayList<>();

        if (TextUtils.isEmpty(name)) { nameEditText.setError("Введите имя"); nameEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(email)) { emailEditText.setError("Введите email"); emailEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(password)) { passwordEditText.setError("Введите пароль"); passwordEditText.requestFocus(); return; }
        if (password.length() < 6) { passwordEditText.setError("Пароль не менее 6 символов"); passwordEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(confirmPassword)) { confirmPasswordEditText.setError("Подтвердите пароль"); confirmPasswordEditText.requestFocus(); return; }
        if (!password.equals(confirmPassword)) { confirmPasswordEditText.setError("Пароли не совпадают"); confirmPasswordEditText.requestFocus(); return; }

        if (role.equals("master")) {
            if (specializationChipGroupMaster == null || specializationChipGroupMaster.getCheckedChipIds().isEmpty()) {
                Toast.makeText(this, "Мастер должен выбрать специализацию", Toast.LENGTH_SHORT).show(); return;
            }
            for (int chipId : specializationChipGroupMaster.getCheckedChipIds()) {
                Chip chip = specializationChipGroupMaster.findViewById(chipId);
                if (chip != null) selectedSpecializations.add(chip.getText().toString());
            }
            if (editTextPhoneMaster != null && TextUtils.isEmpty(editTextPhoneMaster.getText().toString().trim())) {
                ((TextInputLayout)editTextPhoneMaster.getParent().getParent()).setError("Номер телефона мастера обязателен"); // Предполагаем, что EditText внутри TextInputLayout
                editTextPhoneMaster.requestFocus(); return;
            }

        } else if (role.equals("client")) {
            if (editTextClientPhoneNumber != null && clientPhoneInputLayout != null) {
                clientPhoneNumber = editTextClientPhoneNumber.getText().toString().trim();
                if (TextUtils.isEmpty(clientPhoneNumber)) {
                    clientPhoneInputLayout.setError("Номер телефона обязателен");
                    editTextClientPhoneNumber.requestFocus(); return;
                } else if (clientPhoneNumber.length() < 7) { // Простая проверка длины
                    clientPhoneInputLayout.setError("Некорректный номер");
                    editTextClientPhoneNumber.requestFocus(); return;
                } else {
                    clientPhoneInputLayout.setError(null);
                }
            } else {
                Log.e(TAG, "Client phone EditText/Layout is null!");
                Toast.makeText(this, "Ошибка формы клиента.", Toast.LENGTH_SHORT).show(); return;
            }
        }

        registerButton.setEnabled(false);
        Toast.makeText(this, "Регистрация...", Toast.LENGTH_SHORT).show();

        createUserInAuth(name, email, password, role, selectedSpecializations, clientPhoneNumber);
    }

    private void createUserInAuth(String name, String email, String password, String role, List<String> specializations, String clientPhoneNumberForSave) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Log.e(TAG, "FirebaseUser is null after creation.");
                            Toast.makeText(RegisterActivity.this, "Ошибка регистрации.", Toast.LENGTH_SHORT).show();
                            if (registerButton != null) registerButton.setEnabled(true);
                            return;
                        }
                        saveUserDataToDatabase(user.getUid(), name, email, role, specializations, clientPhoneNumberForSave);
                    } else {
                        if (registerButton != null) registerButton.setEnabled(true);
                        Exception exception = task.getException();
                        String errorMessage = "Ошибка регистрации.";
                        if (exception instanceof FirebaseAuthUserCollisionException) errorMessage = "Этот email уже зарегистрирован.";
                        else if (exception instanceof FirebaseAuthWeakPasswordException) errorMessage = "Пароль слишком слабый.";
                        else if (exception instanceof FirebaseAuthInvalidCredentialsException) errorMessage = "Неверный формат email.";
                        else if (exception != null && exception.getMessage() != null) errorMessage = exception.getMessage();
                        Log.w(TAG, "createUserWithEmail:failure", exception);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDataToDatabase(String userId, String name, String email, String role, List<String> specializations, String clientPhoneToSave) {
        DatabaseReference newUserRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("id", userId);
        userData.put("phoneVerified", false);
        userData.put("imageUrl", ""); // Пустой URL для фото по умолчанию
        userData.put("address", "");
        userData.put("city", "");
        userData.put("latitude", 0.0);
        userData.put("longitude", 0.0);


        if (role.equals("master")) {
            userData.put("specializations", specializations);
            userData.put("description", editTextDescriptionMaster != null ? editTextDescriptionMaster.getText().toString().trim() : "");
            userData.put("experience", editTextExperienceMaster != null ? editTextExperienceMaster.getText().toString().trim() : "");
            userData.put("phoneNumber", editTextPhoneMaster != null ? editTextPhoneMaster.getText().toString().trim() : ""); // Телефон мастера
            userData.put("address", editTextAddressMaster != null ? editTextAddressMaster.getText().toString().trim() : ""); // Адрес мастера, если есть отдельное поле
            userData.put("city", editTextCityMaster != null ? editTextCityMaster.getText().toString().trim() : ""); // Город мастера
            userData.put("rating", 0.0);
            userData.put("reviewCount", 0L);
            userData.put("ratingSum", 0.0);
            userData.put("completedOrdersCount", 0L);
            userData.put("workingHours", createDefaultWorkingHours());
            // clientAverageRating и т.д. для мастера не нужны, они для пользователя-клиента
        } else if (role.equals("client")) {
            userData.put("phoneNumber", clientPhoneToSave); // Телефон клиента
            // Поля для рейтинга клиента (если мастера их оценивают)
            userData.put("clientAverageRating", 0.0);
            userData.put("clientRatingSum", 0.0);
            userData.put("clientRatedByMastersCount", 0L);
        }

        newUserRef.setValue(userData)
                .addOnCompleteListener(task1 -> {
                    if (registerButton != null) registerButton.setEnabled(true);
                    if (isFinishing() || isDestroyed()) return;

                    if (task1.isSuccessful()) {
                        Log.d(TAG, "User data saved successfully for userId: " + userId);
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        if (fbUser != null) {
                            fbUser.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) Log.d(TAG, "Verification email sent.");
                                        else Log.w(TAG, "Failed to send verification email.", emailTask.getException());
                                    });
                        }
                        Intent nextIntent = new Intent(RegisterActivity.this, MainActivity.class);
                        nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(nextIntent);
                        finish();
                    } else {
                        Log.e(TAG, "Failed to save user data", task1.getException());
                        Toast.makeText(RegisterActivity.this, "Ошибка сохранения данных: " + safeGetErrorMessage(task1.getException()), Toast.LENGTH_SHORT).show();
                        FirebaseUser userToDelete = mAuth.getCurrentUser(); // Попытка удалить пользователя из Auth, если данные не сохранились
                        if(userToDelete != null) {
                            userToDelete.delete().addOnCompleteListener(deleteTask -> {
                                if(deleteTask.isSuccessful()){ Log.w(TAG, "User deleted from Auth due to DB save failure.");}
                                else { Log.e(TAG, "Failed to delete user from Auth after DB save failure.", deleteTask.getException());}
                            });
                        }
                    }
                });
    }

    private Map<String, Object> createDefaultWorkingHours() {
        Map<String, Object> defaultHours = new HashMap<>();
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : days) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("working", false);
            dayData.put("startTime", "09:00");
            dayData.put("endTime", "18:00");
            defaultHours.put(day, dayData);
        }
        return defaultHours;
    }

    private String safeGetErrorMessage(Exception e) {
        return (e != null && e.getMessage() != null) ? e.getMessage() : "Неизвестная ошибка.";
    }
}