package com.example.mastermate.app;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.yandex.mapkit.MapKitFactory;

public class MyApplication extends Application {
    private static final String MAPKIT_API_KEY = "927db5ad-e653-4e4b-a215-26f823a56ae5";
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }



        try {
            Log.d(TAG, "Attempting to set Yandex MapKit API key...");
            MapKitFactory.setApiKey(MAPKIT_API_KEY);
            Log.d(TAG, "API key set. Attempting to initialize MapKit...");
            MapKitFactory.initialize(this);
            Log.i(TAG, "Yandex MapKit initialized successfully in Application class.");
        } catch (AssertionError e) {
            Log.w(TAG, "Yandex MapKit already initialized or error setting API key: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL error initializing Yandex MapKit in Application.", e);
        }

        // Твой StrictMode код, если он нужен
    }
}