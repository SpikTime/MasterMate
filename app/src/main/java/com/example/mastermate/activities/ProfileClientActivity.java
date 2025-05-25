package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mastermate.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileClientActivity extends BaseActivity {

    private static final String TAG = "ProfileClientActivity";

    private Toolbar toolbar;
    private TextView nameValueTextView, emailValueTextView;
    private MaterialButton btnSelectOrderForReview;
    private MaterialButton btnEditClientProfile;


    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_client);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Ошибка аутентификации.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        initializeUI();
        setupToolbar();
        setupButtonListeners();
        loadUserData();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar_profile_client);
        nameValueTextView = findViewById(R.id.nameValueTextView);
        emailValueTextView = findViewById(R.id.emailValueTextView);
        btnSelectOrderForReview = findViewById(R.id.btn_select_order_for_review);
        btnEditClientProfile = findViewById(R.id.btn_edit_client_profile);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    private void setupButtonListeners() {
        if (btnSelectOrderForReview != null) {
            btnSelectOrderForReview.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileClientActivity.this, SelectOrderForReviewActivity.class);
                startActivity(intent);
            });
        }

        if (btnEditClientProfile != null) {
            btnEditClientProfile.setOnClickListener(v -> {
                // TODO: Создать и запустить EditProfileClientActivity
                Toast.makeText(this, "Редактирование профиля клиента (TODO)", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadUserData() {
        if (userRef == null) return;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);

                    if (nameValueTextView!=null) nameValueTextView.setText(name != null ? name : "Имя не указано");
                    if (emailValueTextView!=null) emailValueTextView.setText(email != null ? email : "Email не указан");
                } else {
                    Toast.makeText(ProfileClientActivity.this, "Данные профиля не найдены.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileClientActivity.this, "Ошибка при загрузке данных: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}