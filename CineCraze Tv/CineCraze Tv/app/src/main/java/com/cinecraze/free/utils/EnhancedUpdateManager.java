package com.cinecraze.free.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Enhanced Update Manager that uses manifest.json for smart update detection
 */
public class EnhancedUpdateManager {
    
    private static final String TAG = "EnhancedUpdateManager";
    private static final String PREF_NAME = "enhanced_update_prefs";
    private static final String KEY_LOCAL_VERSION = "local_version";
    private static final String KEY_LOCAL_HASH = "local_hash";
    private static final String KEY_LAST_CHECK = "last_check";
    
    // URLs
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json";
    private static final String DATABASE_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db";
    
    // Local files
    private static final String LOCAL_DB_NAME = "playlist.db";
    private static final String TEMP_DB_NAME = "playlist_temp.db";
    
    private Context context;
    private SharedPreferences prefs;
    private UpdateCallback callback;
    
    public interface UpdateCallback {
        void onUpdateCheckStarted();
        void onUpdateAvailable(ManifestInfo manifestInfo);
        void onNoUpdateAvailable();
        void onUpdateDownloadStarted();
        void onUpdateDownloadProgress(int progress);
        void onUpdateDownloadCompleted();
        void onUpdateDownloadFailed(String error);
        void onUpdateCheckFailed(String error);
    }
    
    public static class ManifestInfo {
        public String version;
        public String description;
        public String createdAt;
        public DatabaseInfo database;
        public MetadataInfo metadata;
        public UpdateInfo updateInfo;
        
        public static class DatabaseInfo {
            public String filename;
            public String url;
            public long sizeBytes;
            public float sizeMb;
            public String modifiedTime;
            public String hash;
        }
        
        public static class MetadataInfo {
            public int totalEntries;
            public String lastUpdated;
        }
        
        public static class UpdateInfo {
            public boolean forceUpdate;
            public String minAppVersion;
            public boolean recommendedUpdate;
        }
    }
    
    public EnhancedUpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check for updates using manifest
     */
    public void checkForUpdates(UpdateCallback callback) {
        this.callback = callback;
        new UpdateCheckTask().execute();
    }
    
    /**
     * Download the latest database
     */
    public void downloadUpdate(ManifestInfo manifestInfo, UpdateCallback callback) {
        this.callback = callback;
        new UpdateDownloadTask().execute(manifestInfo);
    }
    
    /**
     * Get local database hash
     */
    private String getLocalDatabaseHash() {
        File dbFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
        if (!dbFile.exists()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(java.nio.file.Files.readAllBytes(dbFile.toPath()));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating hash", e);
            return "";
        }
    }
    
    /**
     * Check if update is needed based on manifest
     */
    private boolean isUpdateNeeded(ManifestInfo manifestInfo) {
        String localVersion = prefs.getString(KEY_LOCAL_VERSION, "");
        String localHash = prefs.getString(KEY_LOCAL_HASH, "");
        
        if (localVersion.isEmpty()) {
            return true;
        }
        if (!localVersion.equals(manifestInfo.version)) {
            return true;
        }
        if (!localHash.equals(manifestInfo.database.hash)) {
            return true;
        }
        return false;
    }
    
    /**
     * Update check task
     */
    private class UpdateCheckTask extends AsyncTask<Void, Void, ManifestInfo> {
        private String errorMessage = "";
        
        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onUpdateCheckStarted();
            }
        }
        
        @Override
        protected ManifestInfo doInBackground(Void... params) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            
            try {
                // Download manifest with cache-busting
                String cb = String.valueOf(System.currentTimeMillis());
                URL url = new URL(MANIFEST_URL + (MANIFEST_URL.contains("?") ? "&" : "?") + "_cb=" + cb);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "CineCraze-Android-App");
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Expires", "0");
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP Error: " + responseCode;
                    return null;
                }
                
                // Read manifest
                inputStream = connection.getInputStream();
                byte[] buffer = new byte[8192];
                StringBuilder result = new StringBuilder();
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    result.append(new String(buffer, 0, bytesRead));
                }
                
                // Parse manifest
                Gson gson = new Gson();
                ManifestInfo manifestInfo = gson.fromJson(result.toString(), ManifestInfo.class);
                
                // Update last check time
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                prefs.edit().putString(KEY_LAST_CHECK, sdf.format(new Date())).apply();
                
                return manifestInfo;
                
            } catch (Exception e) {
                errorMessage = "Error checking for updates: " + e.getMessage();
                Log.e(TAG, "Update check error", e);
                return null;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams", e);
                }
            }
        }
        
        @Override
        protected void onPostExecute(ManifestInfo manifestInfo) {
            if (callback != null) {
                if (manifestInfo != null) {
                    if (isUpdateNeeded(manifestInfo)) {
                        callback.onUpdateAvailable(manifestInfo);
                    } else {
                        callback.onNoUpdateAvailable();
                    }
                } else {
                    callback.onUpdateCheckFailed(errorMessage);
                }
            }
        }
    }
    
    /**
     * Update download task
     */
    private class UpdateDownloadTask extends AsyncTask<ManifestInfo, Integer, Boolean> {
        private String errorMessage = "";
        
        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onUpdateDownloadStarted();
            }
        }
        
        @Override
        protected Boolean doInBackground(ManifestInfo... params) {
            if (params.length == 0) {
                errorMessage = "No manifest info provided";
                return false;
            }
            
            ManifestInfo manifestInfo = params[0];
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            
            try {
                // Create temp file
                File tempFile = new File(context.getFilesDir(), TEMP_DB_NAME);
                File localFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
                
                // Download database using URL from manifest if present, with cache busting
                String dbUrl = (manifestInfo != null && manifestInfo.database != null && manifestInfo.database.url != null && !manifestInfo.database.url.isEmpty())
                    ? manifestInfo.database.url
                    : DATABASE_URL;
                String cb = String.valueOf(System.currentTimeMillis());
                URL url = new URL(dbUrl + (dbUrl.contains("?") ? "&" : "?") + "_cb=" + cb);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("User-Agent", "CineCraze-Android-App");
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Expires", "0");
                
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
                
                // Verify file size and hash per manifest
                if (manifestInfo.database != null && manifestInfo.database.sizeBytes > 0 && tempFile.length() != manifestInfo.database.sizeBytes) {
                    Log.w(TAG, "File size mismatch: expected=" + manifestInfo.database.sizeBytes + ", got=" + tempFile.length());
                }
                String downloadedHash = getFileHash(tempFile);
                if (manifestInfo.database != null && manifestInfo.database.hash != null && !manifestInfo.database.hash.isEmpty()) {
                    if (!downloadedHash.equals(manifestInfo.database.hash)) {
                        Log.w(TAG, "Hash verification failed");
                    }
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
                prefs.edit()
                    .putString(KEY_LOCAL_VERSION, manifestInfo.version)
                    .putString(KEY_LOCAL_HASH, downloadedHash)
                    .apply();
                
                Log.i(TAG, "Database updated successfully: " + manifestInfo.version);
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
                callback.onUpdateDownloadProgress(values[0]);
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (callback != null) {
                if (success) {
                    callback.onUpdateDownloadCompleted();
                } else {
                    callback.onUpdateDownloadFailed(errorMessage);
                }
            }
        }
    }
    
    /**
     * Calculate file hash
     */
    private String getFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(java.nio.file.Files.readAllBytes(file.toPath()));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error calculating file hash", e);
            return "";
        }
    }
    
    public String getLocalVersion() {
        return prefs.getString(KEY_LOCAL_VERSION, "Unknown");
    }
    
    public String getLastCheckTime() {
        return prefs.getString(KEY_LAST_CHECK, "Never");
    }
    
    public boolean isDatabaseExists() {
        File dbFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
        return dbFile.exists() && dbFile.length() > 0;
    }
}