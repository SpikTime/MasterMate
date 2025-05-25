package com.example.mastermate.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mastermate.R;
import com.example.mastermate.network.ApiClient;
import com.example.mastermate.network.SmsRuApi;
import com.example.mastermate.network.SmsRuSendResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyPhoneActivity extends AppCompatActivity {

    private static final String TAG = "VerifyPhoneActivity";
    private static final long RESEND_TIMEOUT_SECONDS = 60;
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private final String SMS_RU_API_ID = "8CBC9C2B-2075-AF1F-F197-EBBF7E0C158F";
    private final String SMS_RU_SENDER_NAME = "";
    private final int SMS_RU_TEST_MODE = 1;

    private EditText codeEditText;
    private Button verifyCodeButton, resendCodeButton;
    private TextView instructionTextView, statusTextView;
    private ProgressBar verificationProgressBar;

    private String phoneNumber;
    private String userIdToUpdate;
    private String currentVerificationCode;
    private CountDownTimer resendTimer;
    private boolean operationInProgress = false;

    private SmsRuApi smsRuApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        userIdToUpdate = getIntent().getStringExtra("userId");

        if (SMS_RU_API_ID.equals("ВАШ_API_ID") || SMS_RU_API_ID.isEmpty()) {
            Log.e(TAG, "!!! Укажите api_id SMS.RU в коде VerifyPhoneActivity !!!");
            Toast.makeText(this, "Ошибка конфигурации SMS API", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (phoneNumber == null || phoneNumber.isEmpty() || userIdToUpdate == null || userIdToUpdate.isEmpty()) {
            Log.e(TAG, "Phone number or user ID is missing in Intent extras.");
            Toast.makeText(this, "Ошибка: Необходимые данные отсутствуют.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        smsRuApiService = ApiClient.getSmsRuApi();

        initializeUI();
        setupListeners();
        sendVerificationCode();
    }


    private void initializeUI() {
        codeEditText = findViewById(R.id.codeEditText);
        verifyCodeButton = findViewById(R.id.verifyCodeButton);
        resendCodeButton = findViewById(R.id.resendCodeButton);
        instructionTextView = findViewById(R.id.instructionTextView);
        statusTextView = findViewById(R.id.statusTextView);
        verificationProgressBar = findViewById(R.id.verificationProgressBar);

        instructionTextView.setText(String.format("Введите %d-значный код, отправленный на номер %s", VERIFICATION_CODE_LENGTH, phoneNumber));
        statusTextView.setText("");
        showLoading(false);
        verifyCodeButton.setEnabled(false);
        resendCodeButton.setEnabled(false);
    }

    private void setupListeners() {
        codeEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                verifyCodeButton.setEnabled(s.length() == VERIFICATION_CODE_LENGTH && !operationInProgress);
            }
        });

        verifyCodeButton.setOnClickListener(v -> {
            String enteredCode = codeEditText.getText().toString().trim();
            if (currentVerificationCode != null && enteredCode.length() == VERIFICATION_CODE_LENGTH) {
                verifySmsCode(enteredCode);
            } else if (currentVerificationCode == null){
                updateStatus("Ошибка: Код еще не был отправлен.", true);
            }
        });

        resendCodeButton.setOnClickListener(v -> {
            sendVerificationCode();
        });
    }


    private String generateVerificationCode() {
        Random random = new Random();
        int min = (int) Math.pow(10, VERIFICATION_CODE_LENGTH - 1);
        int max = (int) Math.pow(10, VERIFICATION_CODE_LENGTH) - 1;
        int code = random.nextInt(max - min + 1) + min;
        return String.valueOf(code);
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("8") && cleaned.length() == 11) {
            cleaned = "7" + cleaned.substring(1);
        }
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if ((cleaned.startsWith("7") && cleaned.length() == 11) || cleaned.length() == 10) {
            return cleaned;
        } else {
            Log.w(TAG, "Phone number format might be incorrect for SMS.RU: " + cleaned + " (Original: " + phone + ")");
            return cleaned;
        }
    }

    private void sendVerificationCode() {
        if (operationInProgress) {
            Log.d(TAG, "Operation already in progress, skipping sendVerificationCode");
            return;
        }

        operationInProgress = true;
        showLoading(true);
        updateStatus("Отправка SMS с кодом...", false);
        resendCodeButton.setEnabled(false);
        codeEditText.setText("");
        verifyCodeButton.setEnabled(false);

        currentVerificationCode = generateVerificationCode();
        String messageText = "MasterMate code: " + currentVerificationCode;
        String cleanedPhoneNumber = cleanPhoneNumber(phoneNumber);

        Log.d(TAG, "Sending code: " + currentVerificationCode + " to cleaned phone: " + cleanedPhoneNumber + " (Test Mode: " + SMS_RU_TEST_MODE + ")");

        if (cleanedPhoneNumber.isEmpty()) { return; }
        if (smsRuApiService == null) { return; }

        Call<SmsRuSendResponse> call = smsRuApiService.sendSms(
                SMS_RU_API_ID,
                cleanedPhoneNumber,
                messageText,
                1,
                SMS_RU_SENDER_NAME,
                SMS_RU_TEST_MODE == 1 ? 1 : null
        );

        call.enqueue(new Callback<SmsRuSendResponse>() {
            @Override
            public void onResponse(@NonNull Call<SmsRuSendResponse> call, @NonNull Response<SmsRuSendResponse> response) {
                operationInProgress = false;
                showLoading(false);
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    SmsRuSendResponse smsResponse = response.body();
                    boolean successOverall = smsResponse.isSuccess();
                    SmsRuSendResponse.SmsStatus numberStatus = smsResponse.getSmsDetails() != null ? smsResponse.getSmsDetails().get(cleanedPhoneNumber) : null;
                    boolean successForNumber = numberStatus != null && numberStatus.isSuccessPerNumber();

                    if (successOverall && successForNumber) {
                        String statusMsg = "Код отправлен" + (SMS_RU_TEST_MODE == 1 ? " (Тест)" : "") + ". Введите его выше.";
                        Log.i(TAG, "SMS sent successfully via SMS.RU. SMS ID: " + numberStatus.getSmsId() + (SMS_RU_TEST_MODE == 1 ? " (Test Mode)" : ""));
                        updateStatus(statusMsg, false);
                        startResendTimer();
                    } else {

                        String errorMsg = "Ошибка отправки SMS";
                        if (!successOverall) {
                            errorMsg += ": " + smsResponse.getStatusText() + " (код " + smsResponse.getStatusCode() + ")";
                            Log.e(TAG, "SMS.RU API Error (Overall): Code=" + smsResponse.getStatusCode() + ", Msg=" + smsResponse.getStatusText());
                        } else if (numberStatus != null && !successForNumber) {
                            errorMsg += " для номера: " + numberStatus.getStatusText() + " (код " + numberStatus.getStatusCode() + ")";
                            Log.e(TAG, "SMS.RU API Error (For Number): Code=" + numberStatus.getStatusCode() + ", Msg=" + numberStatus.getStatusText());
                        } else {
                            errorMsg += ". Неизвестная ошибка API.";
                            Log.e(TAG, "SMS.RU API Error: Unknown error in response structure. Overall status: " + smsResponse.getStatus());
                        }
                        updateStatus(errorMsg, true);
                        enableResendButton();
                    }
                } else {
                    Log.e(TAG, "SMS sending failed (HTTP Error). Code: " + response.code() + ", Message: " + response.message());
                    updateStatus("Ошибка сети при отправке SMS (код: " + response.code() + ")", true);
                    enableResendButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SmsRuSendResponse> call, @NonNull Throwable t) {
                operationInProgress = false;
                showLoading(false);
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "SMS sending network failure", t);
                updateStatus("Ошибка сети: " + t.getMessage(), true);
                enableResendButton();
            }
        });
    }
    private void verifySmsCode(String enteredCode) {
        if (operationInProgress) return;
        operationInProgress = true;
        showLoading(true);
        updateStatus("Проверка кода...", false);
        verifyCodeButton.setEnabled(false);
        resendCodeButton.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentVerificationCode != null && currentVerificationCode.equals(enteredCode)) {
                Log.i(TAG, "Verification code matched.");
                updatePhoneVerifiedFlag();
            } else {
                Log.w(TAG, "Verification code mismatch. Expected: " + currentVerificationCode + ", Entered: " + enteredCode);
                operationInProgress = false;
                showLoading(false);
                updateStatus("Неверный код подтверждения.", true);
                if (codeEditText != null) {
                    codeEditText.requestFocus();
                    codeEditText.selectAll();
                }
                verifyCodeButton.setEnabled(true);
                enableResendButton();
            }
        }, 500);
    }

    private void updatePhoneVerifiedFlag() {
        if (userIdToUpdate == null || userIdToUpdate.isEmpty()) { return; }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) { return; }

        DatabaseReference userDbRef = FirebaseDatabase.getInstance().getReference("users").child(userIdToUpdate);
        Log.i(TAG, "Updating phoneVerified flag to true for user: " + userIdToUpdate);
        updateStatus("Обновление статуса...", false);

        userDbRef.child("phoneVerified").setValue(true)
                .addOnCompleteListener(task -> {
                    operationInProgress = false;
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.i(TAG, "phoneVerified flag updated successfully for user: " + userIdToUpdate);
                        Toast.makeText(VerifyPhoneActivity.this, "Номер телефона подтвержден!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Log.e(TAG, "Failed to update phoneVerified flag", task.getException());
                        Toast.makeText(VerifyPhoneActivity.this, "Не удалось обновить статус в профиле (ошибка: " + task.getException().getMessage() + ")", Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
    }


    private void showLoading(boolean isLoading) {
        if (isFinishing() || isDestroyed()) return;
        if (verificationProgressBar == null || verifyCodeButton == null || resendCodeButton == null || codeEditText == null) { return; }

        verificationProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        boolean enableCodeInput = !isLoading;
        boolean enableVerifyButton = !isLoading && codeEditText.getText().length() == VERIFICATION_CODE_LENGTH;
        boolean enableResendButtonState = !isLoading && (resendTimer == null);

        codeEditText.setEnabled(enableCodeInput);
        verifyCodeButton.setEnabled(enableVerifyButton);
        if (!isLoading && resendTimer == null) { enableResendButton(); }
        else if (isLoading) { resendCodeButton.setEnabled(false); }
    }

    private void updateStatus(String message, boolean isError) {
        if (statusTextView != null && !isFinishing() && !isDestroyed()) {
            statusTextView.setText(message);
            try {
                statusTextView.setTextColor(ContextCompat.getColor(this,
                        isError ? R.color.colorErrorLight : R.color.textColorSecondaryLight));
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Color resource not found", e);
            }
            statusTextView.setVisibility(View.VISIBLE);
        }
    }

    private void startResendTimer() {
        if (resendCodeButton == null) return;
        resendCodeButton.setEnabled(false);
        if (resendTimer != null) { resendTimer.cancel(); }
        resendTimer = new CountDownTimer(RESEND_TIMEOUT_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (resendCodeButton != null) {
                    long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1;
                    resendCodeButton.setText(String.format(Locale.getDefault(),"Отправить повторно (%d)", secondsLeft));
                }
            }
            @Override
            public void onFinish() {
                resendTimer = null;
                enableResendButton();
            }
        }.start();
    }

    private void enableResendButton() {
        if (resendTimer != null) { resendTimer.cancel(); resendTimer = null; }
        if (resendCodeButton != null && !operationInProgress) {
            resendCodeButton.setText("Отправить код повторно");
            resendCodeButton.setEnabled(true);
        } else if (resendCodeButton != null) {
            resendCodeButton.setText("Отправить код повторно");
            resendCodeButton.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) { resendTimer.cancel(); }
    }
}