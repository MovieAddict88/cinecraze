package com.cinecraze.free;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

/**
 * Application class for CineCraze Tv
 * Handles AdMob initialization and other app-wide configurations
 */
public class CineCrazeApplication extends Application {
    
    private static final String TAG = "CineCrazeApplication";
    private static CineCrazeApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Initialize AdMob
        initializeAdMob();
        
        // Reduce Glide memory footprint globally
        try {
            Glide.get(this).setMemoryCategory(MemoryCategory.LOW);
        } catch (Exception e) {
            Log.w(TAG, "Unable to set Glide MemoryCategory", e);
        }
        
        Log.d(TAG, "CineCraze Tv Application initialized successfully");
    }
    
    /**
     * Initialize AdMob SDK
     */
    private void initializeAdMob() {
        try {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    Log.d(TAG, "AdMob initialization completed successfully");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AdMob", e);
        }
    }
    
    /**
     * Get application instance
     */
    public static CineCrazeApplication getInstance() {
        return instance;
    }
}