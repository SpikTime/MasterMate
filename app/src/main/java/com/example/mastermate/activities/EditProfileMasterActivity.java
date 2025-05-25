package com.example.mastermate.activities;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.mastermate.R;
import com.example.mastermate.models.Master;
import com.example.mastermate.models.WorkingDay;

import com.example.mastermate.utils.ImageUploadCallback;
import com.example.mastermate.utils.ImageUploadManager;
import com.example.mastermate.utils.LoadingDialog;
import com.example.mastermate.utils.NetworkUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;



import de.hdodenhof.circleimageview.CircleImageView;


public class EditProfileMasterActivity extends AppCompatActivity{

    private static final String TAG = "EditProfileMaster";

    private Toolbar toolbar;

    private Uri selectedImageUri = null;

    private ActivityResultLauncher<String> activityResultLauncherForImagePicker;
    private EditText nameEditText, descriptionEditText, phoneEditText;
    private TextView locationTextView;

    private ImageUploadManager imageUploader;
    private ActivityResultLauncher<Intent> activityResultLauncherForImage;
    // private Uri selectedImageUri;
    private LoadingDialog loadingNewImageDialog;
    private SharedPreferences myPref;
    private final String UPLOAD_IMAGE_PHP_URL = "http://n91734m1.beget.tech/upload_avatar.php";
    private String currentUploadedImageUrl = null;
    private Button saveButton, selectLocationButton;
    private ChipGroup specializationChipGroupEdit;
    private CircleImageView editProfileImageView;
    private LinearLayout workingHoursContainer;
    private ProgressBar profileLoadingProgressBar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userDbRef;

    private String initialPhoneNumber;
    private String currentImageUrlFromDb;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private Button changePhotoButton;
    private Map<String, WorkingDay> currentWorkingHours = new HashMap<>();

    private static final String MONDAY = "monday";
    private static final String TUESDAY = "tuesday";
    private static final String WEDNESDAY = "wednesday";
    private static final String THURSDAY = "thursday";
    private static final String FRIDAY = "friday";
    private static final String SATURDAY = "saturday";
    private static final String SUNDAY = "sunday";
    private final String[] dayKeys = {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY};
    private Map<String, Integer> dayKeyToIncludeIdMap = new HashMap<>();

    private ActivityResultLauncher<Intent> selectLocationLauncher;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_master);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Ошибка аутентификации.", Toast.LENGTH_LONG).show();
            finishAffinity();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        userDbRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        initializeViews();
        setupToolbar();
        initializeLocationLauncher();
        initializeImagePickerLauncher();
        setupListeners();
        loadUserData();
    }

    private void initializeViews() {
        editProfileImageView = findViewById(R.id.editProfileImageView);
        nameEditText = findViewById(R.id.nameEditText);
        specializationChipGroupEdit = findViewById(R.id.specializationChipGroupEdit);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        locationTextView = findViewById(R.id.locationTextView);
        selectLocationButton = findViewById(R.id.selectLocationButton);
        workingHoursContainer = findViewById(R.id.workingHoursContainer);
        saveButton = findViewById(R.id.saveButton);

        if (specializationChipGroupEdit != null) {
            setupSpecializationChips(specializationChipGroupEdit);
        }
        if (workingHoursContainer != null) {
            initializeWorkingHoursMapAndUI();
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Редактирование профиля");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            setTitle("Редактирование профиля");
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

    private void initializeLocationLauncher() {
        selectLocationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                selectedLatitude = data.getDoubleExtra("latitude", 0.0);
                selectedLongitude = data.getDoubleExtra("longitude", 0.0);
                String address = data.getStringExtra("address");
                if (locationTextView != null) locationTextView.setText(address != null ? address : "Адрес не определен");
            }
        });
    }

    

    private void setupListeners() {

        if (changePhotoButton != null) {
            changePhotoButton.setOnClickListener(view -> {
                Log.d(TAG, "Change photo button clicked.");
                if (NetworkUtils.isInternetAvailable(this)) {
                    if (activityResultLauncherForImagePicker != null) {
                        Log.d(TAG, "Launching image picker (GetContent)...");
                        activityResultLauncherForImagePicker.launch("image/*");
                    } else {
                        Log.e(TAG, "activityResultLauncherForImagePicker is not initialized!");
                        Toast.makeText(this, "Ошибка инициализации выбора фото.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Нет подключения к интернету.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (saveButton != null) saveButton.setOnClickListener(v -> saveProfileData());
        if (selectLocationButton != null) selectLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileMasterActivity.this, SelectLocationActivity.class);
            if (selectedLatitude != 0.0 || selectedLongitude != 0.0) {
                intent.putExtra("initialLatitude", selectedLatitude);
                intent.putExtra("initialLongitude", selectedLongitude);
            }
            if (selectLocationLauncher != null) selectLocationLauncher.launch(intent);
        });
        if (workingHoursContainer != null) setupWorkingHoursListeners();
        if (changePhotoButton != null) {
            changePhotoButton.setOnClickListener(view -> {
                if (NetworkUtils.isInternetAvailable(this)) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    if (activityResultLauncherForImage != null) {
                        activityResultLauncherForImage.launch(intent);
                    }
                } else {
                    Toast.makeText(this, "Нет подключения к интернету.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }





    private void loadUserData() {
        if (profileLoadingProgressBar != null) profileLoadingProgressBar.setVisibility(View.VISIBLE);
        if (saveButton != null) saveButton.setEnabled(false);

        userDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (profileLoadingProgressBar != null) profileLoadingProgressBar.setVisibility(View.GONE);
                if (saveButton != null) saveButton.setEnabled(true);

                if (snapshot.exists()) {
                    Master master = snapshot.getValue(Master.class);
                    if (master != null) {
                        if(nameEditText!=null) nameEditText.setText(master.getName());
                        if(descriptionEditText!=null) descriptionEditText.setText(master.getDescription());
                        initialPhoneNumber = master.getPhoneNumber();
                        if(phoneEditText!=null) phoneEditText.setText(initialPhoneNumber);
                        if(locationTextView!=null) locationTextView.setText(master.getAddress());
                        selectedLatitude = master.getLatitude() != null ? master.getLatitude() : 0.0;
                        selectedLongitude = master.getLongitude() != null ? master.getLongitude() : 0.0;

                        currentImageUrlFromDb = master.getImageUrl();
                        if (currentImageUrlFromDb != null && !currentImageUrlFromDb.isEmpty() && editProfileImageView != null) {
                            Glide.with(EditProfileMasterActivity.this)
                                    .load(currentImageUrlFromDb)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .into(editProfileImageView);
                        } else {
                            if(editProfileImageView!=null) editProfileImageView.setImageResource(R.drawable.ic_person);
                        }
                        if(specializationChipGroupEdit != null) updateSpecializationChipsSelection(master.getSpecializations());
                        if(workingHoursContainer != null && master.getWorkingHours() != null) {
                            currentWorkingHours.clear();
                            currentWorkingHours.putAll(master.getWorkingHours());
                            updateWorkingHoursUI();
                        }
                    }
                } else { Toast.makeText(EditProfileMasterActivity.this, "Профиль не найден.", Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                if (profileLoadingProgressBar != null) profileLoadingProgressBar.setVisibility(View.GONE);
                if (saveButton != null) saveButton.setEnabled(true);
                Log.e(TAG, "Failed to load user data", error.toException());
                Toast.makeText(EditProfileMasterActivity.this, "Ошибка загрузки профиля.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initializeImagePickerLauncher() {
        activityResultLauncherForImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), // <<<--- ИСПОЛЬЗУЕМ GetContent()
                uri -> { // Колбэк теперь напрямую получает Uri
                    if (uri != null) {
                        selectedImageUri = uri;
                        if (editProfileImageView != null) {
                            Glide.with(this).load(selectedImageUri).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(editProfileImageView);
                        }
                        Drawable imageToUpload = getDrawableFromContentUri(this, selectedImageUri);
                        if (imageToUpload != null) {
                            String userIdForFilename = myPref.getString("userID", currentUser.getUid());
                            String desiredFilename = "avatar_" + userIdForFilename + ".png";
                            if (imageUploader != null) { // Проверка на null для imageUploader
                                imageUploader.uploadImage(imageToUpload, UPLOAD_IMAGE_PHP_URL, desiredFilename, currentUser.getUid());
                            } else {
                                Log.e(TAG, "imageUploader is null when trying to upload!");
                                Toast.makeText(this, "Ошибка: сервис загрузки не инициализирован.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Не удалось получить изображение.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Image selection cancelled or failed (uri is null).");
                    }
                });
    }

    private Drawable getDrawableFromContentUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());
            if (inputStream != null) inputStream.close();
            return drawable;
        } catch (IOException e) {
            Log.e(TAG, "Error creating drawable from URI", e);
        }
        return null;
    }

    private void saveProfileData() {
        if (userDbRef == null || currentUser == null) return;

        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String newPhoneNumber = phoneEditText.getText().toString().trim();
        String address = locationTextView.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { nameEditText.setError("Имя обязательно"); nameEditText.requestFocus(); return; }
        else nameEditText.setError(null);
        if (!TextUtils.isEmpty(newPhoneNumber) && newPhoneNumber.length() < 7) { phoneEditText.setError("Некорректный номер"); phoneEditText.requestFocus(); return; }
        else phoneEditText.setError(null);

        List<String> selectedSpecializations = new ArrayList<>();
        if (specializationChipGroupEdit != null) {
            for (int i = 0; i < specializationChipGroupEdit.getChildCount(); i++) {
                Chip chip = (Chip) specializationChipGroupEdit.getChildAt(i);
                if (chip.isChecked()) selectedSpecializations.add(chip.getText().toString());
            }
        }
        if (selectedSpecializations.isEmpty()) { Toast.makeText(this, "Выберите хотя бы одну специализацию.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("name", name);
        profileUpdates.put("description", description);
        profileUpdates.put("phoneNumber", newPhoneNumber);
        profileUpdates.put("address", address.equals("Адрес не выбран") || address.equals("Адрес не определен") ? "" : address);
        profileUpdates.put("latitude", selectedLatitude);
        profileUpdates.put("longitude", selectedLongitude);
        profileUpdates.put("specializations", selectedSpecializations);

        Map<String, Object> workingHoursForFirebase = new HashMap<>();
        if (currentWorkingHours != null) {
            for (Map.Entry<String, WorkingDay> entry : currentWorkingHours.entrySet()) {
                workingHoursForFirebase.put(entry.getKey(), entry.getValue());
            }
        }
        profileUpdates.put("workingHours", workingHoursForFirebase);

        if (!Objects.equals(initialPhoneNumber, newPhoneNumber)) {
            profileUpdates.put("phoneVerified", false);
        }

        if (currentImageUrlFromDb != null) { // Сохраняем существующий URL, если он был
            profileUpdates.put("imageUrl", currentImageUrlFromDb);
        }


        if (saveButton != null) saveButton.setEnabled(false);
        if (profileLoadingProgressBar != null) profileLoadingProgressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Сохранение...", Toast.LENGTH_SHORT).show();

        userDbRef.updateChildren(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (saveButton != null) saveButton.setEnabled(true);
                    if (profileLoadingProgressBar != null) profileLoadingProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfileMasterActivity.this, "Профиль обновлен", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditProfileMasterActivity.this, "Ошибка сохранения: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initializeWorkingHoursMapAndUI() {
        dayKeyToIncludeIdMap.put(MONDAY, R.id.includeMonday);
        dayKeyToIncludeIdMap.put(TUESDAY, R.id.includeTuesday);
        dayKeyToIncludeIdMap.put(WEDNESDAY, R.id.includeWednesday);
        dayKeyToIncludeIdMap.put(THURSDAY, R.id.includeThursday);
        dayKeyToIncludeIdMap.put(FRIDAY, R.id.includeFriday);
        dayKeyToIncludeIdMap.put(SATURDAY, R.id.includeSaturday);
        dayKeyToIncludeIdMap.put(SUNDAY, R.id.includeSunday);

        for (String dayKey : dayKeys) {
            Integer includeId = dayKeyToIncludeIdMap.get(dayKey);
            if (includeId != null) {
                View dayView = findViewById(includeId);
                if (dayView != null) {
                    TextView dayNameTextView = dayView.findViewById(R.id.dayNameTextView);
                    if (dayNameTextView != null) dayNameTextView.setText(getDayNameForKey(dayKey));
                }
            }
            if (!currentWorkingHours.containsKey(dayKey)) {
                currentWorkingHours.put(dayKey, new WorkingDay(false, "09:00", "18:00"));
            }
        }
        updateWorkingHoursUI();
    }

    private void updateWorkingHoursUI() {
        if (currentWorkingHours == null || workingHoursContainer == null || isFinishing()) return;
        for (String dayKey : dayKeys) {
            Integer includeId = dayKeyToIncludeIdMap.get(dayKey);
            WorkingDay dayData = currentWorkingHours.get(dayKey);
            if (includeId == null || dayData == null) continue;
            View dayView = findViewById(includeId);
            if (dayView != null) {
                try {
                    MaterialSwitch isWorkingSwitch = dayView.findViewById(R.id.isWorkingSwitch);
                    TextView startTimeTextView = dayView.findViewById(R.id.startTimeTextView);
                    TextView endTimeTextView = dayView.findViewById(R.id.endTimeTextView);
                    TextView timeSeparatorTextView = dayView.findViewById(R.id.timeSeparatorTextView);
                    if (isWorkingSwitch == null || startTimeTextView == null || endTimeTextView == null) continue;

                    isWorkingSwitch.setChecked(dayData.isWorking());
                    boolean isEnabled = dayData.isWorking();
                    startTimeTextView.setEnabled(isEnabled);
                    endTimeTextView.setEnabled(isEnabled);
                    if (timeSeparatorTextView != null) timeSeparatorTextView.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);
                    startTimeTextView.setText(isEnabled && !TextUtils.isEmpty(dayData.getStartTime()) ? dayData.getStartTime() : "--:--");
                    endTimeTextView.setText(isEnabled && !TextUtils.isEmpty(dayData.getEndTime()) ? dayData.getEndTime() : "--:--");
                    int textColor = ContextCompat.getColor(this, isEnabled ? R.color.colorPrimaryBlue : R.color.textColorHintLight);
                    startTimeTextView.setTextColor(textColor);
                    endTimeTextView.setTextColor(textColor);
                } catch (Exception e) { Log.e(TAG, "Error updating UI for day: " + dayKey, e); }
            }
        }
    }

    private void setupWorkingHoursListeners() {
        if (workingHoursContainer == null || currentWorkingHours == null) return;
        for (String dayKey : dayKeys) {
            Integer includeId = dayKeyToIncludeIdMap.get(dayKey);
            if (includeId == null) continue;
            View dayView = findViewById(includeId);
            if (dayView != null) {
                try {
                    MaterialSwitch isWorkingSwitch = dayView.findViewById(R.id.isWorkingSwitch);
                    TextView startTimeTextView = dayView.findViewById(R.id.startTimeTextView);
                    TextView endTimeTextView = dayView.findViewById(R.id.endTimeTextView);
                    if (isWorkingSwitch == null || startTimeTextView == null || endTimeTextView == null) continue;

                    isWorkingSwitch.setOnCheckedChangeListener(null);
                    isWorkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        WorkingDay dayData = currentWorkingHours.get(dayKey);
                        if (dayData != null) {
                            dayData.setWorking(isChecked);
                            if (!isChecked) { dayData.setStartTime(""); dayData.setEndTime(""); }
                            else {
                                if (TextUtils.isEmpty(dayData.getStartTime())) dayData.setStartTime("09:00");
                                if (TextUtils.isEmpty(dayData.getEndTime())) dayData.setEndTime("18:00");
                            }
                            updateWorkingHoursUI();
                        }
                    });
                    startTimeTextView.setOnClickListener(v -> { WorkingDay d = currentWorkingHours.get(dayKey); if (d != null && d.isWorking()) showTimePickerDialog(startTimeTextView, dayKey, true); });
                    endTimeTextView.setOnClickListener(v -> { WorkingDay d = currentWorkingHours.get(dayKey); if (d != null && d.isWorking()) showTimePickerDialog(endTimeTextView, dayKey, false); });
                } catch (Exception e) { Log.e(TAG, "Error setting listeners for day: " + dayKey, e); }
            }
        }
    }

    private void showTimePickerDialog(final TextView timeTextView, final String dayKey, final boolean isStartTime) {
        if (isFinishing()) return;
        Calendar calendar = Calendar.getInstance();
        int currentHour = isStartTime ? 9 : 18; int currentMinute = 0;
        String existingTime = timeTextView.getText().toString();
        if (existingTime.matches("\\d{2}:\\d{2}")) {
            try { String[] parts = existingTime.split(":"); currentHour = Integer.parseInt(parts[0]); currentMinute = Integer.parseInt(parts[1]); }
            catch (NumberFormatException e) { Log.w(TAG, "Could not parse existing time: " + existingTime, e); }
        }
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            WorkingDay dayData = currentWorkingHours.get(dayKey);
            if (dayData != null) {
                String otherTime = isStartTime ? dayData.getEndTime() : dayData.getStartTime();
                boolean isValidTime = true;
                if (!TextUtils.isEmpty(otherTime) && otherTime.matches("\\d{2}:\\d{2}")) {
                    if (isStartTime && formattedTime.compareTo(otherTime) >= 0 && !otherTime.equals("00:00")) { Toast.makeText(this, "Начало >= окончания", Toast.LENGTH_LONG).show(); isValidTime = false; }
                    else if (!isStartTime && formattedTime.compareTo(otherTime) <= 0 && !formattedTime.equals("00:00")) { Toast.makeText(this, "Окончание <= начала", Toast.LENGTH_LONG).show(); isValidTime = false; }
                }
                if (isValidTime) { if (isStartTime) dayData.setStartTime(formattedTime); else dayData.setEndTime(formattedTime); updateWorkingHoursUI(); }
            }
        }, currentHour, currentMinute, true);
        timePickerDialog.show();
    }

    private String getDayNameForKey(String dayKey) {
        switch (dayKey) {
            case MONDAY: return "Понедельник"; case TUESDAY: return "Вторник"; case WEDNESDAY: return "Среда";
            case THURSDAY: return "Четверг"; case FRIDAY: return "Пятница"; case SATURDAY: return "Суббота";
            case SUNDAY: return "Воскресенье"; default: return dayKey.substring(0, 1).toUpperCase() + dayKey.substring(1);
        }
    }

    private void setupSpecializationChips(ChipGroup chipGroup) {
        if (getResources() == null || chipGroup == null) return;
        String[] specializationsArray = getResources().getStringArray(R.array.master_specializations_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        chipGroup.removeAllViews();
        for (String specializationName : specializationsArray) {
            if (specializationName.equalsIgnoreCase("Все")) continue;
            try {
                Chip chip = (Chip) inflater.inflate(R.layout.item_chip_filter, chipGroup, false);
                chip.setText(specializationName); chip.setCheckable(true); chip.setCloseIconVisible(false);
                chipGroup.addView(chip);
            } catch (Exception e) { Log.e(TAG, "Error inflating chip: " + specializationName, e); }
        }
    }

    private void updateSpecializationChipsSelection(List<String> selectedSpecs) {
        if (specializationChipGroupEdit == null || selectedSpecs == null) return;
        for (int i = 0; i < specializationChipGroupEdit.getChildCount(); i++) {
            View child = specializationChipGroupEdit.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(selectedSpecs.contains(chip.getText().toString()));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);
            String address = data.getStringExtra("address");
            if (locationTextView != null) {
                locationTextView.setText(address != null ? address : "Адрес не определен");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageUploader != null) {
            imageUploader.shutdown();
        }
        if (loadingNewImageDialog != null && loadingNewImageDialog.isDialogShowing()) {
            loadingNewImageDialog.dismissDialog();
        }
    }
}