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
        // Use the standard databases directory so SQLiteOpenHelper can open it
        return context.getDatabasePath(LOCAL_DB_NAME);
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
        // If file does not exist yet, do not mark as corrupted. The manager will
        // attempt to copy a valid database from bundled assets during initialization.
        if (!dbFile.exists()) {
            return false;
        }

        // Validate the SQLite header. A valid SQLite database starts with:
        // "SQLite format 3\0" (16 bytes)
        java.io.FileInputStream inputStream = null;
        try {
            inputStream = new java.io.FileInputStream(dbFile);
            byte[] header = new byte[16];
            int read = inputStream.read(header);
            if (read < 16) {
                // Too small to be a valid SQLite database
                return true;
            }

            // Compare first 15 characters to "SQLite format 3"
            char[] expected = "SQLite format 3".toCharArray();
            for (int i = 0; i < expected.length; i++) {
                if ((char) header[i] != expected[i]) {
                    return true;
                }
            }

            // Header matches; consider it not corrupted regardless of file size
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error reading database header for corruption check", e);
            return true;
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception ignored) {}
            }
        }
    }
}