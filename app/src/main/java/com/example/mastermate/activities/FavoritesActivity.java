package com.example.mastermate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.adapters.MasterAdapter;
import com.example.mastermate.models.Master;
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
import java.util.concurrent.atomic.AtomicInteger;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecyclerView;
    private MasterAdapter masterAdapter;
    private List<Master> masterList = new ArrayList<>();
    private TextView emptyFavoritesTextView;
    private ProgressBar loadingProgressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference favoritesRef;
    private DatabaseReference usersRef;
    private ValueEventListener favoritesListener;

    private static final String TAG = "FavoritesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        emptyFavoritesTextView = findViewById(R.id.emptyFavoritesTextView);
        loadingProgressBar = findViewById(R.id.favoritesLoadingProgressBar);

        setupRecyclerView();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "User logged in: " + userId);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            favoritesRef = database.getReference("users").child(userId).child("favorites");
            usersRef = database.getReference("users");
            attachFavoritesListener();
        } else {
            Log.w(TAG, "User not logged in.");
            handleUserNotLoggedIn();
        }
    }

    private void setupRecyclerView() {
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setHasFixedSize(true);


        masterAdapter = new MasterAdapter(this, masterList, master -> {
            if (master != null && master.getId() != null && !isFinishing()) {
                Intent intent = new Intent(FavoritesActivity.this, MasterProfileActivity.class);
                intent.putExtra("masterId", master.getId());
                startActivity(intent);
            }
        });
        favoritesRecyclerView.setAdapter(masterAdapter);
    }

    private void handleUserNotLoggedIn() {
        if (emptyFavoritesTextView != null) {
            emptyFavoritesTextView.setText("Войдите, чтобы увидеть избранное");
            emptyFavoritesTextView.setVisibility(View.VISIBLE);
        }
        if (favoritesRecyclerView != null) {
            favoritesRecyclerView.setVisibility(View.GONE);
        }
        showLoading(false);
    }

    private void attachFavoritesListener() {
        showLoading(true);

        if (favoritesListener == null && favoritesRef != null) {
            Log.d(TAG, "Attaching listener to: " + favoritesRef.getPath());
            favoritesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isFinishing() || isDestroyed()) return;

                    List<String> favoriteMasterIds = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String masterId = snapshot.getKey();
                            if (masterId != null && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                                favoriteMasterIds.add(masterId);
                            }
                        }
                        Log.d(TAG, "Found favorite IDs: " + favoriteMasterIds);
                    } else {
                        Log.d(TAG, "Favorites node is empty or doesn't exist.");
                    }
                    updateViewAndLoadDetails(favoriteMasterIds);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "Favorites listener cancelled", databaseError.toException());
                    Toast.makeText(FavoritesActivity.this, "Ошибка загрузки избранного", Toast.LENGTH_SHORT).show();
                    updateViewAndLoadDetails(Collections.emptyList());
                    showLoading(false);
                }
            };
            favoritesRef.addValueEventListener(favoritesListener);
        } else {
            Log.w(TAG, "Cannot attach listener. Ref: " + favoritesRef + ", Listener: " + favoritesListener);
            showLoading(false);
            updateViewAndLoadDetails(Collections.emptyList());
        }
    }


    private void updateViewAndLoadDetails(List<String> masterIds) {
        if (masterIds.isEmpty()) {
            if (emptyFavoritesTextView != null) emptyFavoritesTextView.setVisibility(View.VISIBLE);
            if (favoritesRecyclerView != null) favoritesRecyclerView.setVisibility(View.GONE);
            masterList.clear();
            masterAdapter.notifyDataSetChanged();
            showLoading(false);
        } else {
            if (emptyFavoritesTextView != null) emptyFavoritesTextView.setVisibility(View.GONE);
            if (favoritesRecyclerView != null) favoritesRecyclerView.setVisibility(View.VISIBLE);
            loadMasterDetails(masterIds);
        }
    }


    private void loadMasterDetails(List<String> masterIds) {

        final List<Master> loadedMasters = Collections.synchronizedList(new ArrayList<>());
        final int totalMastersToLoad = masterIds.size();
        final AtomicInteger loadedCount = new AtomicInteger(0);

        if (usersRef == null) {
            Log.e(TAG, "usersRef is null. Cannot load details.");
            updateAdapterData(Collections.emptyList());
            showLoading(false);
            return;
        }

        Log.d(TAG, "Loading details for " + totalMastersToLoad + " masters...");

        masterList.clear();
        masterAdapter.notifyDataSetChanged();

        for (String masterId : masterIds) {
            final String currentMasterId = masterId;
            usersRef.child(currentMasterId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Master master = dataSnapshot.getValue(Master.class);
                    if (master != null) {
                        if (master.getId() == null || master.getId().isEmpty()) {
                            master.setId(currentMasterId);
                        }
                        loadedMasters.add(master);
                        Log.d(TAG, "Loaded details for: " + currentMasterId);
                    } else {
                        Log.w(TAG, "Master details not found for ID: " + currentMasterId);
                    }
                    checkCompletionAndUpdateAdapter(loadedCount, totalMastersToLoad, loadedMasters);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load details for ID: " + currentMasterId, databaseError.toException());
                    checkCompletionAndUpdateAdapter(loadedCount, totalMastersToLoad, loadedMasters);
                }
            });
        }
    }

    private void checkCompletionAndUpdateAdapter(AtomicInteger counter, int total, List<Master> loadedMasters) {
        if (counter.incrementAndGet() >= total) {
            Log.d(TAG, "Finished loading all master details. Count: " + loadedMasters.size());
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    updateAdapterData(new ArrayList<>(loadedMasters));
                    showLoading(false);
                }
            });
        }
    }

    private void updateAdapterData(List<Master> masters) {
        if (masterAdapter != null && masterList != null) {
            masterList.clear();
            masterList.addAll(masters);
            masterAdapter.notifyDataSetChanged();
            Log.d(TAG,"Adapter notified. Item count: " + masterAdapter.getItemCount());
            if (masterList.isEmpty() && emptyFavoritesTextView != null) {
                emptyFavoritesTextView.setVisibility(View.VISIBLE);
                favoritesRecyclerView.setVisibility(View.GONE);
            } else if (favoritesRecyclerView != null){
                favoritesRecyclerView.setVisibility(View.VISIBLE);
                emptyFavoritesTextView.setVisibility(View.GONE);
            }

        } else {
            Log.w(TAG,"Adapter or masterList is null during updateAdapterData.");
        }
    }

    private void showLoading(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoritesRef != null && favoritesListener != null) {
            favoritesRef.removeEventListener(favoritesListener);
            Log.d(TAG, "Favorites listener removed.");
        }
    }
}