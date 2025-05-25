package com.example.mastermate.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.mastermate.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.BusinessObjectMetadata;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.SearchType;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.ToponymObjectMetadata;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

public class SelectOrderLocationActivity extends AppCompatActivity implements CameraListener, UserLocationObjectListener {

    private static final String TAG = "SelectOrderLocation";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private MapView mapView;
    private TextView selectedAddressTextView;
    private MaterialButton confirmButton;
    private FloatingActionButton fabCenterOnMe;
    private ProgressBar addressGeocodingProgressBar;
    private Toolbar toolbar;

    private SearchManager searchManager;
    private Session geocodeSession;
    private Point currentCameraTargetPoint;
    private String currentResolvedAddress = "";
    private boolean isCameraMovingProgrammatically = false;

    private FusedLocationProviderClient fusedLocationClient;
    private UserLocationLayer userLocationLayer;

    private double initialLatitude = 0.0;
    private double initialLongitude = 0.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            MapKitFactory.setApiKey("927db5ad-e653-4e4b-a215-26f823a56ae5");
            MapKitFactory.initialize(this);
        } catch (AssertionError e) {
            Log.e(TAG, "MapKit already initialized or error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MapKit.", e);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_order_location);

        toolbar = findViewById(R.id.toolbar_select_location);
        mapView = findViewById(R.id.mapViewOrderLocation);
        selectedAddressTextView = findViewById(R.id.selectedOrderAddressTextView);
        confirmButton = findViewById(R.id.confirmOrderLocationButton);
        fabCenterOnMe = findViewById(R.id.fabCenterOnUserOrderLocation);
        addressGeocodingProgressBar = findViewById(R.id.addressGeocodingProgressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        if (mapView.getMapWindow().getMap() == null) {
            Log.e(TAG, "Map object is null after MapView initialization!");
            Toast.makeText(this, "Ошибка инициализации карты.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mapView.getMapWindow().getMap().addCameraListener(this);
        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.getMapWindow());
        userLocationLayer.setVisible(true);
        userLocationLayer.setHeadingEnabled(true);
        userLocationLayer.setObjectListener(this);


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("initialLatitude") && intent.hasExtra("initialLongitude")) {
            initialLatitude = intent.getDoubleExtra("initialLatitude", 0.0);
            initialLongitude = intent.getDoubleExtra("initialLongitude", 0.0);
        }


        setupPermissionLauncher();
        setupListeners();

        if (initialLatitude != 0.0 || initialLongitude != 0.0) {
            moveCameraTo(new Point(initialLatitude, initialLongitude), 17.0f, false);
        } else {
            requestUserLocationAndMoveCamera(true);
        }
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        requestUserLocationAndMoveCamera(true);
                    } else {
                        Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_LONG).show();
                        if (initialLatitude == 0.0 && initialLongitude == 0.0) {
                            moveCameraTo(new Point(55.751244, 37.618423), 10.0f, false);//Москва
                        }
                    }
                });
    }

    private void setupListeners() {
        confirmButton.setOnClickListener(v -> {
            if (currentCameraTargetPoint != null && !currentResolvedAddress.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", currentCameraTargetPoint.getLatitude());
                resultIntent.putExtra("longitude", currentCameraTargetPoint.getLongitude());
                resultIntent.putExtra("address", currentResolvedAddress);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Подождите, адрес определяется...", Toast.LENGTH_SHORT).show();
            }
        });

        fabCenterOnMe.setOnClickListener(v -> requestUserLocationAndMoveCamera(true));
    }

    private void requestUserLocationAndMoveCamera(boolean moveToUserLocation) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (moveToUserLocation) {
                Point lastKnownUserLocation = userLocationLayer.cameraPosition() != null ? userLocationLayer.cameraPosition().getTarget() : null;
                if(lastKnownUserLocation != null){
                    moveCameraTo(lastKnownUserLocation, 17.0f, false);
                } else {
                    try {
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, location -> {
                                    if (location != null) {
                                        moveCameraTo(new Point(location.getLatitude(), location.getLongitude()), 17.0f, false);
                                    } else {
                                        Toast.makeText(this, "Не удалось определить ваше местоположение.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } catch (SecurityException e) {Log.e(TAG, "Sec ex", e);}
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onCameraPositionChanged(@NonNull Map map, @NonNull CameraPosition cameraPosition, @NonNull CameraUpdateReason updateReason, boolean finished) {
        if (finished) {
            if (isCameraMovingProgrammatically) {
                isCameraMovingProgrammatically = false;
                currentCameraTargetPoint = cameraPosition.getTarget();
                reverseGeocode(currentCameraTargetPoint);
            } else {
                currentCameraTargetPoint = cameraPosition.getTarget();
                Log.d(TAG, "Camera moved by user to: " + currentCameraTargetPoint);
                reverseGeocode(currentCameraTargetPoint);
            }
        }
    }

    private void reverseGeocode(Point point) {
        if (searchManager == null || point == null) return;
        if (addressGeocodingProgressBar != null) addressGeocodingProgressBar.setVisibility(View.VISIBLE);
        selectedAddressTextView.setText("Определение адреса...");

        if (geocodeSession != null) {
            geocodeSession.cancel();
        }

        SearchOptions searchOptions = new SearchOptions().setSearchTypes(SearchType.GEO.value);

        geocodeSession = searchManager.submit(
                point,
                17,
                searchOptions,
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull Response response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (addressGeocodingProgressBar != null) addressGeocodingProgressBar.setVisibility(View.GONE);

                        String foundAddress = "Адрес не определен";

                        GeoObjectCollection resultCollection = response.getCollection();
                        if (resultCollection != null && !resultCollection.getChildren().isEmpty()) {
                            GeoObjectCollection.Item firstResultItem = resultCollection.getChildren().get(0);
                            GeoObject geoObject = firstResultItem.getObj();

                            if (geoObject != null) {
                                ToponymObjectMetadata toponymMetadata = geoObject.getMetadataContainer().getItem(ToponymObjectMetadata.class);
                                if (toponymMetadata != null && toponymMetadata.getAddress() != null &&
                                        toponymMetadata.getAddress().getFormattedAddress() != null &&
                                        !toponymMetadata.getAddress().getFormattedAddress().isEmpty()) {
                                    foundAddress = toponymMetadata.getAddress().getFormattedAddress();
                                    Log.d(TAG, "Geocoded (Toponym Formatted): " + foundAddress);
                                }
                                else {
                                    BusinessObjectMetadata businessMetadata = geoObject.getMetadataContainer().getItem(BusinessObjectMetadata.class);
                                    if (businessMetadata != null && businessMetadata.getAddress() != null &&
                                            businessMetadata.getAddress().getFormattedAddress() != null &&
                                            !businessMetadata.getAddress().getFormattedAddress().isEmpty()) {
                                        foundAddress = businessMetadata.getAddress().getFormattedAddress();
                                        Log.d(TAG, "Geocoded (Business Formatted): " + foundAddress);
                                    }
                                    else if (geoObject.getName() != null && !geoObject.getName().isEmpty()) {
                                        foundAddress = geoObject.getName();
                                        if (geoObject.getDescriptionText() != null && !geoObject.getDescriptionText().isEmpty()){
                                            foundAddress += ", " + geoObject.getDescriptionText();
                                        }
                                        Log.d(TAG, "Geocoded (Object Name/Description): " + foundAddress);
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "Geocoding response was empty or object was null.");
                        }

                        final String finalAddress = foundAddress;
                        currentResolvedAddress = finalAddress;
                        selectedAddressTextView.setText(currentResolvedAddress);
                        Log.d(TAG, "Final Resolved Address: " + currentResolvedAddress);
                    }

                    @Override
                    public void onSearchError(@NonNull Error error) {
                        if (isFinishing() || isDestroyed()) return;
                        if (addressGeocodingProgressBar != null) addressGeocodingProgressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Reverse geocoding error: " + error.toString());
                        currentResolvedAddress = "Ошибка определения адреса";
                        selectedAddressTextView.setText(currentResolvedAddress);
                    }
                }
        );
    }


    private void moveCameraTo(Point point, float zoom, boolean programmaticMove) {
        if (mapView != null && mapView.getMapWindow().getMap() != null) {
            isCameraMovingProgrammatically = programmaticMove;
            mapView.getMapWindow().getMap().move(
                    new CameraPosition(point, zoom, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 0.5f),
                    null
            );
            if (!programmaticMove) {
                currentCameraTargetPoint = point;
                reverseGeocode(point);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geocodeSession != null) {
            geocodeSession.cancel();
        }
        if (mapView != null && mapView.getMapWindow().getMap() != null) {
            mapView.getMapWindow().getMap().removeCameraListener(this);
        }
    }

    @Override
    public void onObjectAdded(@NonNull UserLocationView userLocationView) {
    }
    @Override
    public void onObjectRemoved(@NonNull UserLocationView userLocationView) {}
    @Override
    public void onObjectUpdated(@NonNull UserLocationView userLocationView, @NonNull ObjectEvent objectEvent) {}
}