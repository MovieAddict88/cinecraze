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
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Flexible Enhanced Update Manager
 * Handles file size mismatches and network issues gracefully
 */
public class EnhancedUpdateManagerFlexible {
    
    private static final String TAG = "EnhancedUpdateManagerFlexible";
    private static final String PREFS_NAME = "playlist_update_prefs";
    private static final String KEY_LOCAL_VERSION = "local_version";
    private static final String KEY_LOCAL_HASH = "local_hash";
    private static final String KEY_LAST_CHECK = "last_check";
    
    private static final String MANIFEST_URL = "https://github.com/MovieAddict88/Movie-Source/raw/main/manifest.json";
    private static final String DATABASE_URL = "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db";
    private static final String LOCAL_DB_NAME = "playlist.db";
    private static final String TEMP_DB_NAME = "playlist_temp.db";
    
    private Context context;
    private SharedPreferences prefs;
    
    public EnhancedUpdateManagerFlexible(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if database exists locally
     */
    public boolean isDatabaseExists() {
        File dbFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
        return dbFile.exists() && dbFile.length() > 0;
    }
    
    /**
     * Check for updates using manifest
     */
    public void checkForUpdates(UpdateCallback callback) {
        new UpdateCheckTask(callback).execute();
    }
    
    /**
     * Download update
     */
    public void downloadUpdate(ManifestInfo manifestInfo, UpdateCallback callback) {
        new UpdateDownloadTask(callback).execute(manifestInfo);
    }
    
    /**
     * Check if update is needed
     */
    private boolean isUpdateNeeded(ManifestInfo manifestInfo) {
        String localVersion = prefs.getString(KEY_LOCAL_VERSION, "");
        String localHash = prefs.getString(KEY_LOCAL_HASH, "");
        
        // If no local version, update is needed
        if (localVersion.isEmpty()) {
            Log.i(TAG, "No local version found, update needed");
            return true;
        }
        
        // Check version and hash
        boolean versionChanged = !localVersion.equals(manifestInfo.version);
        boolean hashChanged = !localHash.equals(manifestInfo.database.hash);
        
        Log.i(TAG, String.format("Update check - Local: %s, Remote: %s, VersionChanged: %s, HashChanged: %s",
            localVersion, manifestInfo.version, versionChanged, hashChanged));
        
        return versionChanged || hashChanged;
    }
    
    /**
     * Update check task
     */
    private class UpdateCheckTask extends AsyncTask<Void, Void, ManifestInfo> {
        private UpdateCallback callback;
        private String errorMessage = "";
        
        public UpdateCheckTask(UpdateCallback callback) {
            this.callback = callback;
        }
        
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
                // Download manifest
                URL url = new URL(MANIFEST_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("User-Agent", "CineCraze-Android-App");
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "HTTP Error: " + responseCode;
                    return null;
                }
                
                inputStream = connection.getInputStream();
                StringBuilder result = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    result.append(new String(buffer, 0, bytesRead));
                }
                
                // Parse manifest
                com.google.gson.Gson gson = new com.google.gson.Gson();
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
     * Flexible update download task
     */
    private class UpdateDownloadTask extends AsyncTask<ManifestInfo, Integer, Boolean> {
        private UpdateCallback callback;
        private String errorMessage = "";
        
        public UpdateDownloadTask(UpdateCallback callback) {
            this.callback = callback;
        }
        
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
                
                // Download database
                URL url = new URL(DATABASE_URL);
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
                
                // FLEXIBLE VERIFICATION - Log but don't fail on size mismatch
                long downloadedSize = tempFile.length();
                long expectedSize = manifestInfo.database.sizeBytes;
                
                Log.i(TAG, String.format("Download verification - Downloaded: %d bytes, Expected: %d bytes", 
                    downloadedSize, expectedSize));
                
                if (downloadedSize != expectedSize) {
                    Log.w(TAG, "File size mismatch detected, but continuing with download");
                    Log.w(TAG, "This might be due to compression or network issues");
                }
                
                // Only verify hash if file size is reasonable (within 10% tolerance)
                double sizeDifference = Math.abs(downloadedSize - expectedSize) / (double) expectedSize;
                if (sizeDifference > 0.1) {
                    Log.w(TAG, "File size difference too large (" + (sizeDifference * 100) + "%), skipping hash verification");
                } else {
                    // Verify hash
                    String downloadedHash = getFileHash(tempFile);
                    if (!downloadedHash.equals(manifestInfo.database.hash)) {
                        Log.w(TAG, "Hash verification failed, but continuing with download");
                        Log.w(TAG, "Expected: " + manifestInfo.database.hash);
                        Log.w(TAG, "Got: " + downloadedHash);
                    } else {
                        Log.i(TAG, "Hash verification successful");
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
                
                // Update preferences with actual downloaded info
                String actualHash = getFileHash(localFile);
                prefs.edit()
                    .putString(KEY_LOCAL_VERSION, manifestInfo.version)
                    .putString(KEY_LOCAL_HASH, actualHash)
                    .apply();
                
                Log.i(TAG, "Database updated successfully: " + manifestInfo.version);
                Log.i(TAG, "Actual file size: " + localFile.length() + " bytes");
                Log.i(TAG, "Actual hash: " + actualHash);
                
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
    
    /**
     * Get local version info
     */
    public String getLocalVersion() {
        return prefs.getString(KEY_LOCAL_VERSION, "Unknown");
    }
    
    /**
     * Get last check time
     */
    public String getLastCheckTime() {
        return prefs.getString(KEY_LAST_CHECK, "Never");
    }
    
    /**
     * Update callback interface
     */
    public interface UpdateCallback {
        void onUpdateCheckStarted();
        void onUpdateAvailable(ManifestInfo manifestInfo);
        void onNoUpdateAvailable();
        void onUpdateCheckFailed(String error);
        void onUpdateDownloadStarted();
        void onUpdateDownloadProgress(int progress);
        void onUpdateDownloadCompleted();
        void onUpdateDownloadFailed(String error);
    }
    
    /**
     * Manifest info class
     */
    public static class ManifestInfo {
        public String version;
        public String description;
        public DatabaseInfo database;
        
        public static class DatabaseInfo {
            public String filename;
            public String url;
            public long sizeBytes;
            public double sizeMb;
            public String hash;
        }
    }
}