package com.example.mastermate.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mastermate.R;
import com.example.mastermate.models.Master;
import com.example.mastermate.models.Order;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapObjectVisitor;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private MapView mapView;
    private FloatingActionButton fabMyLocation;
    private ProgressBar mapLoadingProgressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference rootRef;
    private DatabaseReference usersRef;
    private DatabaseReference ordersRef;
    private DatabaseReference currentUserDataRef;

    private FirebaseUser firebaseCurrentUser;
    private String currentUserRole;

    private PlacemarkMapObject userPlacemark;
    private MapObjectCollection dataPlacemarksCollection;

    private ValueEventListener roleListener;
    private ValueEventListener mainDataListener;
    private Map<String, ValueEventListener> orderDetailListeners = new HashMap<>();

    private static final float USER_ICON_SCALE = 0.15f;
    private static final float MASTER_ICON_SCALE = 0.12f;
    private static final float ORDER_ICON_SCALE = 0.10f;



    private final MapObjectTapListener masterTapListener = (mapObject, point) -> {
        if (mapObject instanceof PlacemarkMapObject && mapObject.getUserData() instanceof Master) {
            handleMasterTap((Master) mapObject.getUserData());
            return true;
        }
        return false;
    };

    private final MapObjectTapListener orderTapListener = (mapObject, point) -> {
        if (mapObject instanceof PlacemarkMapObject && mapObject.getUserData() instanceof Order) {
            handleOrderTap((Order) mapObject.getUserData());
            return true;
        }
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeUiComponents();
        setupFirebaseReferences();
        checkUserAuthentication();
    }

    private void initializeUiComponents() {
        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        mapLoadingProgressBar = findViewById(R.id.mapLoadingProgressBar);

        if (mapView.getMap() == null) {
            Log.e(TAG, "Yandex Map object is null! Check MapKit initialization in Application class.");
            Toast.makeText(this, "Ошибка инициализации карты.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        dataPlacemarksCollection = mapView.getMap().getMapObjects().addCollection();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupPermissionLauncher();
        setupFabListener();
    }

    private void setupFirebaseReferences() {
        rootRef = FirebaseDatabase.getInstance().getReference();
        usersRef = rootRef.child("users");
        ordersRef = rootRef.child("orders");
    }

    private void checkUserAuthentication() {
        firebaseCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseCurrentUser == null) {
            Log.w(TAG, "User not authenticated. Redirecting to LoginActivity.");
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } else {
            currentUserDataRef = usersRef.child(firebaseCurrentUser.getUid());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try { com.yandex.mapkit.MapKitFactory.getInstance().onStart(); if (mapView != null) mapView.onStart(); }
        catch (Exception e) { Log.e(TAG, "MapKit/MapView onStart error", e); }
        if (firebaseCurrentUser != null && currentUserDataRef != null) {
            Log.d(TAG, "onStart: Attaching role listener.");
            attachRoleListener();
        } else {
            Log.e(TAG, "onStart: FirebaseUser or currentUserDataRef is null, cannot attach role listener.");
            checkUserAuthentication();
        }
    }

    @Override
    protected void onStop() {
        try { if (mapView != null) mapView.onStop(); com.yandex.mapkit.MapKitFactory.getInstance().onStop(); }
        catch (Exception e) { Log.e(TAG, "MapKit/MapView onStop error", e); }
        Log.d(TAG, "onStop: Detaching Firebase listeners.");
        detachAllFirebaseListeners();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        detachAllFirebaseListeners();
        super.onDestroy();
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) getUserLocationAndCenterMap();
                    else { Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_LONG).show(); showDefaultLocation(); }
                });
    }

    private void setupFabListener() {
        if (fabMyLocation != null) fabMyLocation.setOnClickListener(v -> checkLocationPermissionAndLoadMap());
        else Log.e(TAG, "fabMyLocation is null!");
    }

    private void checkLocationPermissionAndLoadMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocationAndCenterMap();
        } else if (requestPermissionLauncher != null) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            Log.e(TAG, "requestPermissionLauncher is null. Requesting permission directly.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndCenterMap();
            } else {
                Toast.makeText(this, "Разрешение на геолокацию отклонено.", Toast.LENGTH_LONG).show();
                showDefaultLocation();
            }
        }
    }

    private void getUserLocationAndCenterMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "getUserLocationAndCenterMap called without permission.");
            return;
        }
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (!isFinishing() && !isDestroyed()) {
                            if (location != null) {
                                Point userPoint = new Point(location.getLatitude(), location.getLongitude());
                                updateUserPlacemark(userPoint);
                                moveCamera(userPoint, 14.0f);
                            } else {
                                Log.w(TAG, "FusedLocationProvider: getLastLocation returned null.");
                                showDefaultLocation();
                            }
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Log.e(TAG, "FusedLocationProvider: Error getting last location", e);
                            showDefaultLocation();
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "FusedLocationProvider: SecurityException in getUserLocation", e);
            if (!isFinishing() && !isDestroyed()) showDefaultLocation();
        }
    }

    private void showDefaultLocation() {
        if (!isFinishing() && !isDestroyed()) {
            moveCamera(new Point(55.751244, 37.618423), 10.0f); // Москва
        }
    }

    private void moveCamera(Point point, float zoom) {
        if (mapView != null && mapView.getMap() != null && !isFinishing() && !isDestroyed() && point != null) {
            mapView.getMap().move(
                    new CameraPosition(point, zoom, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 0.3f),
                    null);
        } else {
            Log.w(TAG, "Cannot move camera: map/point null or activity finishing.");
        }
    }

    private void showLoading(boolean isLoading) {
        if (mapLoadingProgressBar != null && !isFinishing() && !isDestroyed()) {
            mapLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void updateUserPlacemark(Point userPoint) {
        if (mapView == null || mapView.getMap() == null || mapView.getMap().getMapObjects() == null || isFinishing() || isDestroyed() || userPoint == null) return;
        ImageProvider userIcon = null;
        try { userIcon = ImageProvider.fromResource(this, R.drawable.ic_user_location); }
        catch (Exception e) { Log.e(TAG, "Error loading R.drawable.ic_user_location icon", e); return; }

        MapObjectCollection rootMapObjects = mapView.getMap().getMapObjects();
        if (userPlacemark != null && userPlacemark.isValid()) {
            userPlacemark.setGeometry(userPoint);
        } else {
            if (userPlacemark != null) rootMapObjects.remove(userPlacemark);
            userPlacemark = rootMapObjects.addPlacemark(userPoint);
            if (userPlacemark != null && userIcon != null) {
                userPlacemark.setIcon(userIcon);
                userPlacemark.setIconStyle(new IconStyle().setScale(USER_ICON_SCALE).setZIndex(100f).setFlat(true));
            } else if (userPlacemark == null) {
                Log.e(TAG, "Failed to add user placemark to root map objects.");
            }
        }
    }

    private void attachRoleListener() {
        showLoading(true);
        detachAllFirebaseListeners();

        if (currentUserDataRef == null) {
            Log.e(TAG, "currentUserDataRef is null in attachRoleListener.");
            showLoading(false); Toast.makeText(this, "Ошибка данных пользователя.", Toast.LENGTH_SHORT).show();
            return;
        }

        roleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                currentUserRole = snapshot.child("role").getValue(String.class);
                Log.d(TAG, "Role determined: " + currentUserRole);

                if (dataPlacemarksCollection != null) dataPlacemarksCollection.clear();
                detachMainDataListener();
                detachOrderDetailsListeners();

                if ("master".equals(currentUserRole)) {
                    Log.d(TAG, "Mode: MASTER. Attaching listener for master's order IDs.");
                    attachMainDataListener(currentUserDataRef.child("orders")); // Загружаем ID заказов мастера
                } else if ("client".equals(currentUserRole)) {
                    Log.d(TAG, "Mode: CLIENT. Attaching listener for all masters.");
                    attachMainDataListener(usersRef.orderByChild("role").equalTo("master")); // Загружаем всех мастеров
                } else {
                    showLoading(false);
                    Log.w(TAG, "Unknown user role: " + currentUserRole);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "RoleListener cancelled", error.toException());
                showLoading(false);
            }
        };
        currentUserDataRef.addValueEventListener(roleListener);
    }

    private void attachMainDataListener(Query query) {
        showLoading(true);
        mainDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (dataPlacemarksCollection != null) dataPlacemarksCollection.clear();
                detachOrderDetailsListeners();

                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "MainDataListener: No data found for the current query.");
                    showLoading(false);
                    return;
                }

                if ("client".equals(currentUserRole)) {
                    Log.d(TAG, "Client mode: Processing masters.");
                    int masterCount = 0;
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        Master master = userSnapshot.getValue(Master.class);
                        if (master != null) {
                            master.setId(userSnapshot.getKey());
                            if (isValidCoordinate(master.getLatitude(), master.getLongitude())) {
                                addMasterPlacemarkToCollection(master);
                                masterCount++;
                            }
                        }
                    }
                    Log.d(TAG, "Displayed " + masterCount + " masters for client.");
                    showLoading(false);
                } else if ("master".equals(currentUserRole)) {
                    Log.d(TAG, "Master mode: Processing order IDs.");
                    List<String> orderIds = new ArrayList<>();
                    for (DataSnapshot idSnap : dataSnapshot.getChildren()) {
                        if (idSnap.getKey() != null) orderIds.add(idSnap.getKey());
                    }
                    if (!orderIds.isEmpty()) {
                        loadAndDisplayOrderPlacemarksForMaster(orderIds);
                    } else {
                        Log.d(TAG, "Master has no order IDs.");
                        showLoading(false);
                    }
                } else {
                    showLoading(false);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "MainDataListener cancelled", databaseError.toException());
                showLoading(false);
            }
        };
        query.addValueEventListener(mainDataListener);
    }


    private void loadAndDisplayOrderPlacemarksForMaster(List<String> orderIds) {
        final AtomicInteger loadedCounter = new AtomicInteger(0);
        final int totalToLoad = orderIds.size();
        if (totalToLoad == 0) { showLoading(false); return; }

        for (String orderId : orderIds) {
            DatabaseReference singleOrderRef = ordersRef.child(orderId);
            ValueEventListener detailListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    removePlacemarkByUserDataIdFromCollection(snapshot.getKey(), Order.class);

                    Order order = snapshot.getValue(Order.class);
                    if (order != null && isValidCoordinate(order.getClientLatitude(), order.getClientLongitude())) {
                        order.setOrderId(snapshot.getKey());
                        if (shouldDisplayOrderOnMap(order.getStatus())) {
                            addOrderPlacemarkToCollection(order);
                        }
                    }
                    if (loadedCounter.incrementAndGet() >= totalToLoad) {
                        showLoading(false);
                        Log.d(TAG, "MASTER MODE: Initial load/update of all order details for map complete.");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "OrderDetailListener for " + orderId + " cancelled.", error.toException());
                    if (loadedCounter.incrementAndGet() >= totalToLoad) showLoading(false);
                }
            };
            singleOrderRef.addValueEventListener(detailListener);
            orderDetailListeners.put(orderId, detailListener);
        }
    }

    private boolean isValidCoordinate(Double lat, Double lon) { return lat != null && lon != null && (Math.abs(lat) > 0.0001 || Math.abs(lon) > 0.0001); } // Чуть строже проверка
    private boolean shouldDisplayOrderOnMap(String status) { if (status == null) return false; String s = status.toLowerCase(); return Order.STATUS_NEW.equals(s) || Order.STATUS_ACCEPTED.equals(s) || Order.STATUS_IN_PROGRESS.equals(s); }

    private void addMasterPlacemarkToCollection(Master master) {
        if (dataPlacemarksCollection == null || !isValidCoordinate(master.getLatitude(), master.getLongitude()) || isFinishing()) return;
        Point point = new Point(master.getLatitude(), master.getLongitude());

        ImageProvider icon = null;
        int iconResourceId = R.drawable.masteron1;
        List<String> specializations = master.getSpecializations();
        String primarySpecialization = null;
        if (specializations != null && !specializations.isEmpty()) {
            primarySpecialization = specializations.get(0).toLowerCase().trim();
        } else if (master.getSpecialization() != null && !master.getSpecialization().isEmpty()){
            primarySpecialization = master.getSpecialization().toLowerCase().trim();
        }


        if (primarySpecialization != null) {
            if (primarySpecialization.contains("сантехник")) {
                iconResourceId = R.drawable.santehnik;
            } else if (primarySpecialization.contains("электрик")) {
                iconResourceId = R.drawable.power_human;
            } else if (primarySpecialization.contains("плотник")) {
                iconResourceId = R.drawable.plotnik;
            }
        }

        try {
            icon = ImageProvider.fromResource(this, iconResourceId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading master icon resource ID: " + iconResourceId + " for master " + master.getId(), e);
            try {
                iconResourceId = R.drawable.masteron1;
                icon = ImageProvider.fromResource(this, iconResourceId);
            } catch (Exception e2) {
                Log.e(TAG, "Error loading R.drawable.ic_master_default icon.", e2);
                return;
            }
        }

        PlacemarkMapObject placemark = dataPlacemarksCollection.addPlacemark(point);
        if (placemark != null && icon != null) {
            placemark.setIcon(icon);
            placemark.setIconStyle(new IconStyle().setScale(MASTER_ICON_SCALE).setZIndex(50f).setFlat(true));
            placemark.setUserData(master);
            placemark.addTapListener(masterTapListener);
        } else if (placemark == null) {
            Log.e(TAG, "Failed to add placemark for master: " + master.getId());
        } else {
            Log.w(TAG, "Icon provider was null for master: " + master.getId() + " using icon resource ID: " + iconResourceId);
        }
    }



    private void addOrderPlacemarkToCollection(Order order) {
        if (dataPlacemarksCollection == null || !isValidCoordinate(order.getClientLatitude(), order.getClientLongitude()) || isFinishing()) return;
        Point point = new Point(order.getClientLatitude(), order.getClientLongitude());
        ImageProvider icon = getIconForOrderStatus(order.getStatus());
        if (icon == null) return;

        PlacemarkMapObject placemark = dataPlacemarksCollection.addPlacemark(point);
        if (placemark != null) {
            placemark.setIcon(icon);
            placemark.setIconStyle(new IconStyle().setScale(ORDER_ICON_SCALE).setZIndex(60f).setFlat(true));
            placemark.setUserData(order);
            placemark.addTapListener(orderTapListener);
        }
    }

    private ImageProvider getIconForOrderStatus(String status) {
        if (status == null) return null; int drawableId = 0;
        switch (status.toLowerCase()) {
            case Order.STATUS_NEW: drawableId = R.drawable.ic_marker_order_new; break;
            case Order.STATUS_ACCEPTED: case Order.STATUS_IN_PROGRESS: drawableId = R.drawable.ic_marker_order_accepted; break;
            default: return null;
        }
        try { return ImageProvider.fromResource(this, drawableId); }
        catch (Exception e) { Log.e(TAG, "Error ImageProvider for "+ getResources().getResourceEntryName(drawableId), e); return null; }
    }

    private String getDisplayableOrderStatus(String status) { if (status == null) return "Неизвестно"; switch (status.toLowerCase()) { case Order.STATUS_NEW: return "Новый"; case Order.STATUS_ACCEPTED: return "Принят"; case Order.STATUS_IN_PROGRESS: return "В работе"; default: return status; } }
    private String getShortDescription(String description) { if (description == null) return ""; return description.length() > 30 ? description.substring(0, 27) + "..." : description; }

    private void removePlacemarkByUserDataIdFromCollection(String idToRemove, Class<?> dataType) {
        if (dataPlacemarksCollection == null || idToRemove == null || dataType == null) return;
        final PlacemarkMapObject[] placemarkFoundHolder = {null};
        dataPlacemarksCollection.traverse(new MapObjectVisitor() {
            @Override public void onPlacemarkVisited(@NonNull PlacemarkMapObject placemark) {
                if (placemarkFoundHolder[0] != null) return;
                if (placemark.getUserData() != null && dataType.isInstance(placemark.getUserData())) {
                    String objectId = "";
                    if (dataType.equals(Order.class) && ((Order) placemark.getUserData()).getOrderId() != null) objectId = ((Order) placemark.getUserData()).getOrderId();
                    else if (dataType.equals(Master.class) && ((Master) placemark.getUserData()).getId() != null) objectId = ((Master) placemark.getUserData()).getId();
                    if (idToRemove.equals(objectId)) placemarkFoundHolder[0] = placemark;
                }
            }
            @Override public void onPolylineVisited(@NonNull PolylineMapObject polyline) {}
            @Override public void onPolygonVisited(@NonNull PolygonMapObject polygon) {}
            @Override public void onCircleVisited(@NonNull CircleMapObject circle) {}

            @Override
            public boolean onCollectionVisitStart(@NonNull MapObjectCollection mapObjectCollection) {
                return false;
            }

            @Override
            public void onCollectionVisitEnd(@NonNull MapObjectCollection mapObjectCollection) {

            }

            @Override
            public boolean onClusterizedCollectionVisitStart(@NonNull ClusterizedPlacemarkCollection clusterizedPlacemarkCollection) {
                return false;
            }

            @Override
            public void onClusterizedCollectionVisitEnd(@NonNull ClusterizedPlacemarkCollection clusterizedPlacemarkCollection) {

            }

            public void onCollectionVisited(@NonNull MapObjectCollection collection) { /* collection.traverse(this); // Если нужна рекурсия */ }
        });
        PlacemarkMapObject placemarkToRemove = placemarkFoundHolder[0];
        if (placemarkToRemove != null && placemarkToRemove.isValid()) dataPlacemarksCollection.remove(placemarkToRemove);
    }

    private void detachAllFirebaseListeners() {
        if (currentUserDataRef != null && roleListener != null) { currentUserDataRef.removeEventListener(roleListener); roleListener = null; }
        detachMainDataListener();
        detachOrderDetailsListeners();
        Log.d(TAG, "All Firebase listeners detached.");
    }
    private void detachMainDataListener() {
        if (mainDataListener != null) {
            if (currentUserRole != null) {
                if ("client".equals(currentUserRole) && usersRef != null) usersRef.orderByChild("role").equalTo("master").removeEventListener(mainDataListener);
                else if ("master".equals(currentUserRole) && currentUserDataRef != null) currentUserDataRef.child("orders").removeEventListener(mainDataListener);
            }
            mainDataListener = null; Log.d(TAG, "MainDataListener detached.");
        }
    }
    private void detachOrderDetailsListeners() {
        if (ordersRef != null && !orderDetailListeners.isEmpty()) {
            for (Map.Entry<String, ValueEventListener> entry : orderDetailListeners.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) ordersRef.child(entry.getKey()).removeEventListener(entry.getValue());
            }
            orderDetailListeners.clear(); Log.d(TAG, "OrderDetailsListeners detached.");
        }
    }

    private void handleMasterTap(Master master) {
        if (master == null || isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(master.getName())
                .setMessage("Специализации: " + master.getSpecializationsString() + "\nРейтинг: " + String.format("%.1f", master.getRating()))
                .setPositiveButton("Создать заявку", (d, w) -> startOrderCreation(master))
                .setNegativeButton("Профиль", (d, w) -> showMasterProfile(master))
                .setNeutralButton("Отмена", null).show();
    }
    private void handleOrderTap(Order order) {
        if (order == null || isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Заказ" + (order.getOrderId() != null ? " #" + order.getOrderId().substring(Math.max(0, order.getOrderId().length() - 6)) : ""))
                .setMessage("Описание: " + getShortDescription(order.getProblemDescription()) +
                        "\nКлиент: " + (order.getClientName() != null ? order.getClientName() : "-") +
                        "\nАдрес: " + (order.getClientAddress() != null ? order.getClientAddress() : "-") +
                        "\nСтатус: " + getDisplayableOrderStatus(order.getStatus()))
                .setPositiveButton("OK", null)
                .setNeutralButton("Подробнее", (d, w) -> showOrderDetailsScreen(order)).show();
    }
    private void startOrderCreation(Master master) { if(master==null || master.getId()==null) return; Intent intent = new Intent(this, CreateOrderActivity.class); intent.putExtra("masterId", master.getId()); startActivity(intent); }
    private void showMasterProfile(Master master) { if(master==null || master.getId()==null) return; Intent intent = new Intent(this, MasterProfileActivity.class); intent.putExtra("masterId", master.getId()); startActivity(intent); }
    private void showOrderDetailsScreen(Order order) {
        if (order == null || order.getOrderId() == null || order.getOrderId().isEmpty() || isFinishing() || isDestroyed()) return;
        Log.d(TAG, "Navigating to OrderDetailsActivity for orderId: " + order.getOrderId());
        Intent intent = new Intent(this, OrderDetailsActivity.class); // Убедись, что OrderDetailsActivity существует
        intent.putExtra("orderId", order.getOrderId());
        try { startActivity(intent); }
        catch (Exception e) { Log.e(TAG, "Error starting OrderDetailsActivity", e); Toast.makeText(this, "Не удалось открыть детали.", Toast.LENGTH_SHORT).show(); }
    }
}