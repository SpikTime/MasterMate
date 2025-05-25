package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mastermate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> loginUser());
        }
        if (registerTextView != null) {
            registerTextView.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }

        if (emailEditText != null) {
            Log.d("LoginActivity", "Email EditText is focusable: " + emailEditText.isFocusable());
            Log.d("LoginActivity", "Email EditText is enabled: " + emailEditText.isEnabled());
        } else {
            Log.e("LoginActivity", "Email EditText not found!");
        }
        if (passwordEditText != null) {
            Log.d("LoginActivity", "Password EditText is focusable: " + passwordEditText.isFocusable());
            Log.d("LoginActivity", "Password EditText is enabled: " + passwordEditText.isEnabled());
        } else {
            Log.e("LoginActivity", "Password EditText not found!");
        }
    }

    private void loginUser() {
        String email = (emailEditText != null) ? emailEditText.getText().toString().trim() : "";
        String password = (passwordEditText != null) ? passwordEditText.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Вход успешен!", Toast.LENGTH_SHORT).show();
                            subscribeToTopic(user.getUid());
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Ошибка входа: пользователь не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w("LoginActivity", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Ошибка входа: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}