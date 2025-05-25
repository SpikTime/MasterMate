package com.example.mastermate.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mastermate.R;
import com.example.mastermate.adapters.MasterAdapter;
import com.example.mastermate.models.Master;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView mastersRecyclerView;
    private MasterAdapter masterAdapter;
    private List<Master> masterList;
    private final List<Master> fullMasterList = new ArrayList<>();
    private DatabaseReference usersRef;
    private ValueEventListener masterLoadListener;
    private ProgressBar loadingProgressBar;
    private ChipGroup specializationChipGroup;
    private SearchView searchView;
    private ImageButton menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        masterList = new ArrayList<>();

        setupRecyclerView();
        setupSearchView();
        setupChipGroup();
        setupMenuButton();

        loadMasters();
    }

    private void initializeViews() {
        mastersRecyclerView = findViewById(R.id.mastersRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        specializationChipGroup = findViewById(R.id.specializationChipGroup);
        searchView = findViewById(R.id.searchView);
        menuButton = findViewById(R.id.menuButton);
    }

    private void setupRecyclerView() {
        if (mastersRecyclerView == null) {
            Log.e(TAG, "RecyclerView not found!");
            return;
        }
        mastersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mastersRecyclerView.setHasFixedSize(true);

        masterAdapter = new MasterAdapter(this, masterList, master -> {
            if (master != null && master.getId() != null) {
                // Показываем диалог выбора действия при клике на мастера
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(master.getName())
                        .setMessage("Выберите действие")
                        .setPositiveButton("Создать заявку", (dialog, which) -> {
                            Intent intent = new Intent(MainActivity.this, CreateOrderActivity.class);
                            intent.putExtra("masterId", master.getId());
                            startActivity(intent);
                        })
                        .setNegativeButton("Просмотреть профиль", (dialog, which) -> {
                            Intent intent = new Intent(MainActivity.this, MasterProfileActivity.class);
                            intent.putExtra("masterId", master.getId());
                            startActivity(intent);
                        })
                        .show();
            } else {
                Log.e(TAG, "Cannot open profile/order: master or masterId is null");
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Не удалось выполнить действие", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mastersRecyclerView.setAdapter(masterAdapter);
    }

    private void setupSearchView() {
        if (searchView == null) {
            Log.e(TAG, "SearchView not found in layout!");
            return;
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                filterMasters(query, getSelectedSpecialization());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMasters(newText, getSelectedSpecialization());
                return true;
            }
        });

        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                searchView.setQuery("", false);
                searchView.setIconified(false);
                searchView.clearFocus();
                filterMasters("", getSelectedSpecialization());
            });
        } else {
            Log.w(TAG,"SearchView close button not found.");
        }
    }

    private void setupChipGroup() {
        if (specializationChipGroup == null) {
            Log.e(TAG, "ChipGroup not found in layout!");
            return;
        }
        specializationChipGroup.removeAllViews();

        String[] specializations;
        try {
            specializations = getResources().getStringArray(R.array.specializations_array);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Specializations array not found in resources!", e);
            Toast.makeText(this, "Ошибка загрузки фильтров", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String specialization : specializations) {
            Chip chip = null;
            try {
                chip = (Chip) inflater.inflate(R.layout.item_chip_filter, specializationChipGroup, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to inflate item_chip_filter, creating Chip programmatically.", e);
                chip = new Chip(this);
                chip.setCheckable(true);
            }

            if (chip != null) {
                chip.setText(specialization);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setId(View.generateViewId());
                chip.setCloseIconVisible(false);

                if (specialization.equals("Все")) {
                    chip.setChecked(true);
                }
                specializationChipGroup.addView(chip);
            }
        }

        specializationChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterMasters(searchView != null ? searchView.getQuery().toString() : "", getSelectedSpecialization());
        });
    }

    private String getSelectedSpecialization() {
        if (specializationChipGroup != null) {
            int checkedChipId = specializationChipGroup.getCheckedChipId();
            if (checkedChipId != View.NO_ID) {
                Chip selectedChip = specializationChipGroup.findViewById(checkedChipId);
                if (selectedChip != null) {
                    return selectedChip.getText().toString();
                }
            }
        }
        return "Все";
    }

    private void setupMenuButton() {
        if (menuButton != null) {
            menuButton.setOnClickListener(this::showPopupMenu);
        } else {
            Log.w(TAG, "Menu ImageButton (menuButton) not found in layout.");
        }
    }

    private void loadMasters() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.VISIBLE);
        if (mastersRecyclerView != null) mastersRecyclerView.setVisibility(View.GONE);

        if (masterLoadListener != null && usersRef != null) {
            Query query = usersRef.orderByChild("role").equalTo("master");
            query.removeEventListener(masterLoadListener);
            Log.d(TAG, "Removed previous master load listener.");
        }

        masterLoadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;

                fullMasterList.clear();
                Log.d(TAG, "Loading masters from Firebase...");

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    // Роль уже проверена Query в Firebase, но оставим проверку на всякий случай
                    if ("master".equals(userSnapshot.child("role").getValue(String.class))) {
                        Master master = userSnapshot.getValue(Master.class);
                        if (master != null) {
                            if (master.getId() == null || master.getId().isEmpty()) {
                                master.setId(userSnapshot.getKey());
                            }
                            fullMasterList.add(master);
                        } else {
                            Log.w(TAG, "Failed to parse Master object for key: " + userSnapshot.getKey());
                        }
                    }
                }
                Log.i(TAG, "Finished loading masters. Total masters found: " + fullMasterList.size());

                filterMasters(searchView != null ? searchView.getQuery().toString() : "", getSelectedSpecialization());

                if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE);
                if (mastersRecyclerView != null) mastersRecyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isFinishing() || isDestroyed()) return;

                Log.e(TAG, "Error loading master data", databaseError.toException());
                if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE);
                if (mastersRecyclerView != null) mastersRecyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Ошибка загрузки данных: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();

                fullMasterList.clear();
                updateAdapter(Collections.emptyList());
            }
        };

        Query query = usersRef.orderByChild("role").equalTo("master");
        query.addValueEventListener(masterLoadListener);

        Log.d(TAG, "Attached master load listener to users node with role query.");
    }

    private void filterMasters(String query, String selectedSpecializationFilter) {
        List<Master> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault()).trim();
        boolean filterBySpecialization = !selectedSpecializationFilter.isEmpty() && !selectedSpecializationFilter.equals("Все");

        Log.d(TAG, "Filtering masters. Query: '" + query + "', Specialization: '" + selectedSpecializationFilter + "'");

        for (Master master : fullMasterList) {
            if (master == null) continue;

            boolean nameMatches = lowerCaseQuery.isEmpty() ||
                    (master.getName() != null &&
                            master.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery));

            boolean specializationMatches = true;
            if (filterBySpecialization) {
                List<String> masterSpecializations = master.getSpecializations();
                specializationMatches = false;
                if (masterSpecializations != null && !masterSpecializations.isEmpty()) {
                    for (String masterSpec : masterSpecializations) {
                        if (masterSpec != null && masterSpec.equalsIgnoreCase(selectedSpecializationFilter)) {
                            specializationMatches = true;
                            break;
                        }
                    }
                }
            }

            if (nameMatches && specializationMatches) {
                filteredList.add(master);
            }
        }
        Log.d(TAG, "Filtering complete. Filtered list size: " + filteredList.size());
        updateAdapter(filteredList);
    }

    private void updateAdapter(List<Master> list) {
        if (masterList != null && masterAdapter != null) {
            masterList.clear();
            masterList.addAll(list);
            masterAdapter.notifyDataSetChanged();
            Log.d(TAG, "Adapter updated with " + masterList.size() + " items.");
            if (mastersRecyclerView != null && !list.isEmpty()) {
            }
        } else {
            Log.e(TAG, "Cannot update adapter, masterList or masterAdapter is null");
        }
    }
    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_logout) {
                logoutUser();
                return true;
            } else if (itemId == R.id.action_profile) {
                openProfile();
                return true;
            } else if (itemId == R.id.action_map) {
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                return true;
            } else if (itemId == R.id.action_favorites) {
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.action_my_orders) { // ДОБАВЛЕНО
                openMyOrders(); // Вызываем метод из BaseActivity
                return true;
            } else if (itemId == R.id.action_list) { // ДОБАВЛЕНО
                Toast.makeText(MainActivity.this, "Вы уже в списке мастеров", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (masterLoadListener != null && usersRef != null) {
            Query query = usersRef.orderByChild("role").equalTo("master");
            query.removeEventListener(masterLoadListener);
            Log.d(TAG, "Removed master load listener in onDestroy.");
        }
    }
}