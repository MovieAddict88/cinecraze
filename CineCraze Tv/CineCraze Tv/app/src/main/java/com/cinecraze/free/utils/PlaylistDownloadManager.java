package com.cinecraze.free.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages downloading and updating the playlist database from GitHub
 */
public class PlaylistDownloadManager {
    
    private static final String TAG = "PlaylistDownloadManager";
    private static final String PREF_NAME = "playlist_download_prefs";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_DB_VERSION = "db_version";
    private static final String KEY_DOWNLOAD_URL = "download_url";
    
    // Remote download disabled in new plan
    private static final String GITHUB_JSON_URL = "";
    private static final String GITHUB_DB_URL = "";
    
    // Local file names
    private static final String LOCAL_DB_NAME = "playlist.db";
    private static final String TEMP_DB_NAME = "playlist_temp.db";
    
    private Context context;
    private SharedPreferences prefs;
    private DownloadCallback callback;
    
    public interface DownloadCallback {
        void onDownloadStarted();
        void onDownloadProgress(int progress);
        void onDownloadCompleted(File dbFile);
        void onDownloadFailed(String error);
        void onUpdateAvailable();
    }
    
    public PlaylistDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if database exists locally
     */
    public boolean isDatabaseExists() {
        File dbFile = getLocalDatabaseFile();
        return dbFile.exists() && dbFile.length() > 0;
    }
    
    /**
     * Get the local database file
     */
    public File getLocalDatabaseFile() {
        return new File(context.getFilesDir(), LOCAL_DB_NAME);
    }
    
    /**
     * Get the last update time
     */
    public String getLastUpdateTime() {
        return prefs.getString(KEY_LAST_UPDATE, "Never");
    }
    
    /**
     * Check if update is needed (simple time-based check)
     */
    public boolean isUpdateNeeded() {
        String lastUpdate = prefs.getString(KEY_LAST_UPDATE, "");
        if (lastUpdate.isEmpty()) {
            return true;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date lastUpdateDate = sdf.parse(lastUpdate);
            Date now = new Date();
            
            // Check if more than 24 hours have passed
            long diffInHours = (now.getTime() - lastUpdateDate.getTime()) / (1000 * 60 * 60);
            return diffInHours >= 24;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing last update time", e);
            return true;
        }
    }
    
    /**
     * Download database from GitHub
     */
    public void downloadDatabase(DownloadCallback callback) {
        if (callback != null) callback.onDownloadFailed("Download disabled (using bundled assets DB)");
    }
    
    /**
     * Download database in background
     */
    // Background downloader removed
    
    /**
     * Delete local database
     */
    public boolean deleteLocalDatabase() {
        File dbFile = getLocalDatabaseFile();
        if (dbFile.exists()) {
            boolean deleted = dbFile.delete();
            if (deleted) {
                prefs.edit().remove(KEY_LAST_UPDATE).apply();
            }
            return deleted;
        }
        return true;
    }
    
    /**
     * Get database file size in MB
     */
    public float getDatabaseSizeMB() {
        File dbFile = getLocalDatabaseFile();
        if (dbFile.exists()) {
            return dbFile.length() / (1024.0f * 1024.0f);
        }
        return 0;
    }
    
    /**
     * Check if database is corrupted
     */
    public boolean isDatabaseCorrupted() {
        File dbFile = getLocalDatabaseFile();
        if (!dbFile.exists()) {
            return true;
        }
        
        // Basic check: file should be at least 16KB (allow small bundled DBs)
        if (dbFile.length() < 16 * 1024) {
            return true;
        }
        
        // TODO: Add more sophisticated corruption checks
        return false;
    }
}