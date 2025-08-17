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
import java.net.URLEncoder;

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
    
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json";
    private static final String DATABASE_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db";
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
        
        Log.d(TAG, "Checking if update needed - Local version: '" + localVersion + "', Remote version: '" + 
            (manifestInfo != null ? manifestInfo.version : "null") + "'");
        
        // If no local version, update is needed
        if (localVersion.isEmpty()) {
            Log.i(TAG, "No local version found, update needed");
            return true;
        }
        
        // Check version - be more flexible with version comparison
        boolean versionChanged = false;
        if (manifestInfo != null && manifestInfo.version != null && !manifestInfo.version.isEmpty()) {
            versionChanged = !localVersion.equals(manifestInfo.version);
            Log.d(TAG, "Version comparison: local='" + localVersion + "' vs remote='" + manifestInfo.version + "' = " + versionChanged);
        }
        
        // Optional: check size if provided in manifest
        boolean sizeChanged = false;
        try {
            if (manifestInfo != null && manifestInfo.database != null && manifestInfo.database.sizeBytes > 0) {
                File local = new File(context.getFilesDir(), LOCAL_DB_NAME);
                if (local.exists()) {
                    long localSize = local.length();
                    long remoteSize = manifestInfo.database.sizeBytes;
                    sizeChanged = localSize != remoteSize;
                    Log.d(TAG, "Size comparison: local=" + localSize + " bytes vs remote=" + remoteSize + " bytes = " + sizeChanged);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking size", e);
        }
        
        // Optional: check hash ONLY if manifest provides one
        boolean hashChanged = false;
        if (manifestInfo != null && manifestInfo.database != null && manifestInfo.database.hash != null && !manifestInfo.database.hash.isEmpty()) {
            hashChanged = !localHash.equals(manifestInfo.database.hash);
            Log.d(TAG, "Hash comparison: local='" + localHash + "' vs remote='" + manifestInfo.database.hash + "' = " + hashChanged);
        }
        
        boolean updateNeeded = versionChanged || sizeChanged || hashChanged;
        
        Log.i(TAG, String.format("Update check result - versionChanged=%s, sizeChanged=%s, hashChanged=%s, updateNeeded=%s",
            versionChanged, sizeChanged, hashChanged, updateNeeded));
        
        return updateNeeded;
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
                // Download manifest with cache-busting and no-cache headers
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
                
                // Download database using URL from manifest if present, with cache-busting and no-cache headers
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
                
                // Optional verification - log only if manifest provides expected values
                long downloadedSize = tempFile.length();
                long expectedSize = (manifestInfo != null && manifestInfo.database != null) ? manifestInfo.database.sizeBytes : 0L;
                if (expectedSize > 0) {
                    Log.i(TAG, String.format("Download verification - Downloaded: %d bytes, Expected: %d bytes", downloadedSize, expectedSize));
                    if (downloadedSize != expectedSize) {
                        Log.w(TAG, "File size mismatch detected (non-fatal)");
                    }
                }
                String downloadedHash = getFileHash(tempFile);
                String expectedHash = (manifestInfo != null && manifestInfo.database != null) ? manifestInfo.database.hash : null;
                if (expectedHash != null && !expectedHash.isEmpty()) {
                    if (!downloadedHash.equals(expectedHash)) {
                        Log.w(TAG, "Hash verification failed (non-fatal)");
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