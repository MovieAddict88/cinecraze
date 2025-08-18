package com.cinecraze.free.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
    
    // GitHub URLs
    private static final String GITHUB_JSON_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json";
    private static final String GITHUB_DB_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db";
    
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
        this.callback = callback;
        new DownloadDatabaseTask().execute();
    }
    
    /**
     * Download database in background
     */
    private class DownloadDatabaseTask extends AsyncTask<Void, Integer, Boolean> {
        private String errorMessage = "";
        
        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onDownloadStarted();
            }
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            
            try {
                // Create temp file
                File tempFile = new File(context.getFilesDir(), TEMP_DB_NAME);
                File localFile = getLocalDatabaseFile();
                
                // Download from GitHub
                URL url = new URL(GITHUB_DB_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "CineCraze-Android-App");
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP Error: " + responseCode;
                    return false;
                }
                
                int fileLength = connection.getContentLength();
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                int totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (fileLength > 0) {
                        int progress = (int) ((totalBytesRead * 100) / fileLength);
                        publishProgress(progress);
                    }
                }
                
                outputStream.flush();
                
                // Verify file size
                if (tempFile.length() == 0) {
                    errorMessage = "Downloaded file is empty";
                    return false;
                }
                
                // Replace existing file
                if (localFile.exists()) {
                    localFile.delete();
                }
                
                if (!tempFile.renameTo(localFile)) {
                    errorMessage = "Failed to save database file";
                    return false;
                }
                
                // Update preferences
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                prefs.edit()
                    .putString(KEY_LAST_UPDATE, currentTime)
                    .putString(KEY_DOWNLOAD_URL, GITHUB_DB_URL)
                    .apply();
                
                Log.i(TAG, "Database downloaded successfully: " + localFile.getAbsolutePath());
                return true;
                
            } catch (Exception e) {
                errorMessage = "Download failed: " + e.getMessage();
                Log.e(TAG, "Download error", e);
                return false;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (outputStream != null) outputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams", e);
                }
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (callback != null && values.length > 0) {
                callback.onDownloadProgress(values[0]);
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (callback != null) {
                if (success) {
                    callback.onDownloadCompleted(getLocalDatabaseFile());
                } else {
                    callback.onDownloadFailed(errorMessage);
                }
            }
        }
    }
    
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
        
        // Basic check: file should be at least 100KB (more lenient)
        if (dbFile.length() < 100 * 1024) {
            return true;
        }
        
        // TODO: Add more sophisticated corruption checks
        return false;
    }
}