package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mastermate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_logout) {
            logoutUser();
            return true;
        } else if (itemId == R.id.action_profile) {
            openProfile();
            return true;
        } else if (itemId == R.id.action_map) {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_my_orders) {
            openMyOrders();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void logoutUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            unsubscribeFromTopic(user.getUid());
        }

        mAuth.signOut();
        Log.i(TAG, "User logged out successfully.");
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    protected void openProfile() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            Log.d(TAG, "Attempting to open profile for user: " + userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing() || isDestroyed()) return;

                    if (dataSnapshot.exists()) {
                        String role = dataSnapshot.child("role").getValue(String.class);
                        Log.d(TAG, "User role for profile: " + role);
                        Intent intent;
                        if ("master".equals(role)) {
                            intent = new Intent(BaseActivity.this, ProfileMasterActivity.class);
                        } else if ("client".equals(role)) {
                            intent = new Intent(BaseActivity.this, ProfileClientActivity.class);
                        } else {
                            Log.w(TAG, "Unknown role for profile: " + role + ". Defaulting or showing error.");
                            Toast.makeText(BaseActivity.this, "Не удалось определить тип профиля.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startActivity(intent);
                    } else {
                        Log.w(TAG, "User node doesn't exist for profile: " + userId);
                        Toast.makeText(BaseActivity.this, "Ошибка данных профиля.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "Error fetching user role for profile", databaseError.toException());
                    Toast.makeText(BaseActivity.this, "Ошибка при загрузке профиля.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "Attempted to open profile, but user is not logged in. Redirecting to Login.");
            Toast.makeText(this, "Необходимо войти в систему.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }

    protected void openMyOrders() {
        Log.d(TAG, "openMyOrders() called.");
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            Log.d(TAG, "Current user for My Orders: " + user.getUid());
            DatabaseReference userNodeRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

            userNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.d(TAG, "My Orders - onDataChange for user node. Snapshot exists: " + snapshot.exists());

                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        Log.d(TAG, "My Orders - User role from DB: '" + role + "'");

                        Intent intent = null;
                        if ("master".equals(role)) {
                            Log.i(TAG, "User is a master. Starting MasterOrdersActivity.");
                            intent = new Intent(BaseActivity.this, MasterOrdersActivity.class);
                        } else if ("client".equals(role)) {
                            Log.i(TAG, "User is a client. Starting ClientOrdersActivity.");
                            intent = new Intent(BaseActivity.this, ClientOrdersActivity.class);
                        } else {
                            Log.w(TAG, "Unknown or missing role for My Orders. User: " + user.getUid() + ", Role: '" + role + "'");
                            Toast.makeText(BaseActivity.this, "Не удалось определить вашу роль для отображения заказов.", Toast.LENGTH_SHORT).show();
                        }

                        if (intent != null) {
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Error starting My Orders activity (Role: " + role + ")", e);
                                Toast.makeText(BaseActivity.this, "Не удалось открыть экран заказов.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.w(TAG, "User node does not exist in DB for My Orders: " + user.getUid());
                        Toast.makeText(BaseActivity.this, "Данные пользователя не найдены.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "My Orders - onCancelled for user role", error.toException());
                    Toast.makeText(BaseActivity.this, "Ошибка загрузки данных пользователя.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w(TAG, "Current user is null in openMyOrders. Redirecting to Login.");
            Toast.makeText(this, "Сначала войдите в систему.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }

    protected void subscribeToTopic(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot subscribe to topic, userId is null or empty.");
            return;
        }
        FirebaseMessaging.getInstance().subscribeToTopic(userId)
                .addOnCompleteListener(task -> {
                    String msg;
                    if (task.isSuccessful()) {
                        msg = "Подписан на уведомления (тема: " + userId + ")";
                        Log.d(TAG, msg);
                    } else {
                        msg = "Ошибка подписки на уведомления (тема: " + userId + ")";
                        Log.e(TAG, msg, task.getException());
                    }
                });
    }

    protected void unsubscribeFromTopic(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot unsubscribe from topic, userId is null or empty.");
            return;
        }
        FirebaseMessaging.getInstance().unsubscribeFromTopic(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from topic: " + userId);
                    } else {
                        Log.e(TAG, "Error unsubscribing from topic: " + userId, task.getException());
                    }
                });
    }
}