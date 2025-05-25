package com.example.mastermate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mastermate.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.SearchType;
import com.yandex.runtime.Error;
import com.yandex.runtime.Runtime;

public class SelectLocationActivity extends AppCompatActivity implements CameraListener {

    private static final String TAG = "SelectLocationActivity";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private MapView mapView;
    private ImageView centerMarkerImageView;
    private TextView selectedAddressTextView;
    private MaterialButton confirmButton;
    private FloatingActionButton fabCenterOnMe;

    private SearchManager searchManager;
    private Session searchSession;
    private Point currentSelectedPoint;
    private String currentSelectedAddress = "";
    private boolean isMapInteractionManual = true;

    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeMapKit();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        mapView = findViewById(R.id.mapView);
        centerMarkerImageView = findViewById(R.id.centerMarkerImageView);
        selectedAddressTextView = findViewById(R.id.selectedAddressTextView);
        confirmButton = findViewById(R.id.confirmButton);
        fabCenterOnMe = findViewById(R.id.fabCenterOnMe);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        setupPermissionLauncher();
        setupListeners();

        loadInitialLocation();
    }

    private void initializeMapKit() {
        try {
            MapKitFactory.setApiKey("927db5ad-e653-4e4b-a215-26f823a56ae5");
            MapKitFactory.initialize(this);
        } catch (AssertionError | Exception e) {
            Log.e(TAG, "Error initializing MapKit.", e);
            Toast.makeText(this, "Ошибка инициализации карты", Toast.LENGTH_LONG).show();
        }
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        fetchUserLocationAndMoveCamera();
                    } else {
                        Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupListeners() {
        mapView.getMap().addCameraListener(this);

        if (confirmButton != null) {
            confirmButton.setOnClickListener(v -> {
                if (currentSelectedPoint != null) {
                    sendLocationResult(currentSelectedPoint, currentSelectedAddress);
                } else {
                    Toast.makeText(this, "Пожалуйста, выберите точку на карте", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (fabCenterOnMe != null) {
            fabCenterOnMe.setOnClickListener(v -> requestUserLocationAndMoveCamera());
        }
    }

    private void loadInitialLocation() {
        if (userRef != null) {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Double savedLat = snapshot.child("latitude").getValue(Double.class);
                    Double savedLon = snapshot.child("longitude").getValue(Double.class);

                    if (savedLat != null && savedLon != null && (savedLat != 0 || savedLon != 0)) {
                        Point pointToMove = new Point(savedLat, savedLon);
                        Log.d(TAG, "Loaded last saved location: " + pointToMove.getLatitude() + ", " + pointToMove.getLongitude());
                        isMapInteractionManual = false;
                        moveCameraToLocation(new Point(savedLat, savedLon), 15.0f);
                    } else {
                        Log.d(TAG, "No valid saved location found, showing default.");
                        showDefaultLocation();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load last location", error.toException());
                    showDefaultLocation();
                }
            });
        } else {
            Log.d(TAG, "User not logged in or userRef is null, showing default location.");
            showDefaultLocation();
        }
    }

    private void requestUserLocationAndMoveCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndMoveCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchUserLocationAndMoveCamera() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Log.d(TAG, "Fetched user location: " + location.getLatitude() + ", " + location.getLongitude());
                            isMapInteractionManual = false;
                            moveCameraToLocation(new Point(location.getLatitude(), location.getLongitude()), 15.0f); // Передаем напрямую
                        } else {
                            Log.w(TAG, "Failed to get last known location (it's null).");
                            Toast.makeText(this, "Не удалось определить геолокацию", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "Error fetching last location", e);
                        Toast.makeText(this, "Ошибка получения геолокации", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission exception on fetch", e);
            Toast.makeText(this, "Ошибка прав доступа к геолокации", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCameraPositionChanged(@NonNull Map map, @NonNull CameraPosition cameraPosition, @NonNull CameraUpdateReason updateReason, boolean finished) {
        if (finished) {
            currentSelectedPoint = cameraPosition.getTarget();
            Log.d(TAG, "Camera finished moving to: " + currentSelectedPoint.getLatitude() + ", " + currentSelectedPoint.getLongitude());

            if (isMapInteractionManual) {
                getAddressFromCoordinates(currentSelectedPoint);
            }
            isMapInteractionManual = true;
        } else {

        }
    }

    private void getAddressFromCoordinates(Point point) {
        if (searchManager == null) return;
        Log.d(TAG, "Requesting address for point...");
        if(selectedAddressTextView != null) selectedAddressTextView.setVisibility(View.INVISIBLE);
        if (searchSession != null) {
            searchSession.cancel();
        }

        searchSession = searchManager.submit(point, 16, new SearchOptions().setSearchTypes(SearchType.GEO.value), new Session.SearchListener() {
            @Override
            public void onSearchResponse(@NonNull Response response) {
                String foundAddress = "Адрес не найден";
                if (!response.getCollection().getChildren().isEmpty() &&
                        response.getCollection().getChildren().get(0).getObj() != null) {
                    foundAddress = response.getCollection().getChildren().get(0).getObj().getName();
                    Log.d(TAG, "Address found: " + foundAddress);
                } else {
                    Log.w(TAG, "Geocoding response was empty or object was null.");
                }
                final String finalAddress = foundAddress;
                runOnUiThread(() -> {
                    currentSelectedAddress = finalAddress;
                    if (selectedAddressTextView != null) {
                        selectedAddressTextView.setText(currentSelectedAddress);
                        selectedAddressTextView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onSearchError(@NonNull Error error) {
                Log.e(TAG, "Reverse geocoding failed: " + error.toString());
                runOnUiThread(() -> {
                    currentSelectedAddress = "Ошибка получения адреса";
                    if (selectedAddressTextView != null) {
                        selectedAddressTextView.setText(currentSelectedAddress);
                        selectedAddressTextView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void sendLocationResult(Point point, String address) {
        if (point != null) {
            Log.i(TAG, "Confirming location: Lat=" + point.getLatitude() + ", Lon=" + point.getLongitude() + ", Address=" + address);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", point.getLatitude());
            resultIntent.putExtra("longitude", point.getLongitude());
            resultIntent.putExtra("address", address != null ? address : "Адрес не определен");
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Log.w(TAG, "Attempted to send result with null point.");
            Toast.makeText(this, "Точка не выбрана", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCameraToLocation(Point point, float zoom) {
        if (mapView != null) {
            mapView.getMap().move(
                    new CameraPosition(point, zoom, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 1),
                    null
            );
        }
    }

    private void showDefaultLocation() {
        Point defaultPoint = new Point(55.751244, 37.618423);
        isMapInteractionManual = false;
        moveCameraToLocation(defaultPoint, 10.0f);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            MapKitFactory.getInstance().onStart();
            if (mapView != null) {
                mapView.onStart();
            }
        } catch (Exception e) {
            Log.e(TAG, "MapKit onStart error", e);
        }
    }

    @Override
    protected void onStop() {
        try {
            if (mapView != null) {
                mapView.onStop();
            }
            MapKitFactory.getInstance().onStop();
        } catch (Exception e) {
            Log.e(TAG, "MapKit onStop error", e);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchSession != null) {
            searchSession.cancel();
        }
    }
}