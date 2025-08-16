package com.cinecraze.free.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Background service for silent database updates
 * Runs in background without user interaction
 */
public class BackgroundUpdateService extends Service {
    
    private static final String TAG = "BackgroundUpdateService";
    private EnhancedUpdateManagerFixed updateManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        updateManager = new EnhancedUpdateManagerFixed(this);
        Log.i(TAG, "Background update service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Background update service started");
        
        // Start background update check
        checkForUpdatesInBackground();
        
        // Return START_NOT_STICKY so service doesn't restart if killed
        return START_NOT_STICKY;
    }
    
    private void checkForUpdatesInBackground() {
        updateManager.checkForUpdates(new EnhancedUpdateManagerFixed.UpdateCallback() {
            @Override
            public void onUpdateCheckStarted() {
                Log.i(TAG, "Background update check started");
            }
            
            @Override
            public void onUpdateAvailable(EnhancedUpdateManagerFixed.ManifestInfo manifestInfo) {
                Log.i(TAG, "Background update available: " + manifestInfo.version);
                
                // Download update silently
                updateManager.downloadUpdate(manifestInfo, this);
            }
            
            @Override
            public void onNoUpdateAvailable() {
                Log.i(TAG, "No background update needed");
                stopSelf();
            }
            
            @Override
            public void onUpdateCheckFailed(String error) {
                Log.e(TAG, "Background update check failed: " + error);
                stopSelf();
            }
            
            @Override
            public void onUpdateDownloadStarted() {
                Log.i(TAG, "Background download started");
            }
            
            @Override
            public void onUpdateDownloadProgress(int progress) {
                Log.i(TAG, "Background download progress: " + progress + "%");
            }
            
            @Override
            public void onUpdateDownloadCompleted() {
                Log.i(TAG, "Background download completed successfully");
                
                // Show a brief toast notification
                Toast.makeText(BackgroundUpdateService.this, 
                    "Database updated in background", Toast.LENGTH_SHORT).show();
                
                stopSelf();
            }
            
            @Override
            public void onUpdateDownloadFailed(String error) {
                Log.e(TAG, "Background download failed: " + error);
                stopSelf();
            }
        });
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Background update service destroyed");
    }
}