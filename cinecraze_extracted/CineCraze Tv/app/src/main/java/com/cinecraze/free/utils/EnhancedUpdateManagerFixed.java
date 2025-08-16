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

import com.google.gson.Gson;

/**
 * Fixed Enhanced Update Manager
 * Properly handles update detection and database replacement
 */
public class EnhancedUpdateManagerFixed {
    
    private static final String TAG = "EnhancedUpdateManagerFixed";
    private static final String PREFS_NAME = "playlist_update_prefs";
    private static final String KEY_LOCAL_VERSION = "local_version";
    private static final String KEY_LOCAL_HASH = "local_hash";
    private static final String KEY_LAST_CHECK = "last_check";
    private static final String KEY_DATABASE_EXISTS = "database_exists";
    
    private static final String MANIFEST_URL = "https://github.com/MovieAddict88/Movie-Source/raw/main/manifest.json";
    private static final String DATABASE_URL = "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db";
    private static final String LOCAL_DB_NAME = "playlist.db";
    private static final String TEMP_DB_NAME = "playlist_temp.db";
    private static final String BACKUP_DB_NAME = "playlist_backup.db";
    
    private Context context;
    private SharedPreferences prefs;
    
    public EnhancedUpdateManagerFixed(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if database exists locally
     */
    public boolean isDatabaseExists() {
        File dbFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
        boolean exists = dbFile.exists() && dbFile.length() > 0;
        
        // Update preference to track database existence
        prefs.edit().putBoolean(KEY_DATABASE_EXISTS, exists).apply();
        
        Log.i(TAG, "Database exists check: " + exists + " (size: " + dbFile.length() + " bytes)");
        return exists;
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
     * Check if update is needed - FIXED LOGIC
     */
    private boolean isUpdateNeeded(ManifestInfo manifestInfo) {
        String localVersion = prefs.getString(KEY_LOCAL_VERSION, "");
        String localHash = prefs.getString(KEY_LOCAL_HASH, "");
        boolean databaseExists = prefs.getBoolean(KEY_DATABASE_EXISTS, false);
        
        Log.i(TAG, "=== UPDATE CHECK ANALYSIS ===");
        Log.i(TAG, "Local Version: '" + localVersion + "'");
        Log.i(TAG, "Remote Version: '" + manifestInfo.version + "'");
        Log.i(TAG, "Local Hash: '" + localHash + "'");
        Log.i(TAG, "Remote Hash: '" + manifestInfo.database.hash + "'");
        Log.i(TAG, "Database Exists: " + databaseExists);
        
        // If no local version AND no database exists, this is first time
        if (localVersion.isEmpty() && !databaseExists) {
            Log.i(TAG, "First time setup - update needed");
            return true;
        }
        
        // If no local version BUT database exists, this is corrupted state
        if (localVersion.isEmpty() && databaseExists) {
            Log.w(TAG, "Corrupted state detected - database exists but no version recorded");
            Log.w(TAG, "Will re-download to fix corruption");
            return true;
        }
        
        // Check version and hash
        boolean versionChanged = !localVersion.equals(manifestInfo.version);
        boolean hashChanged = !localHash.equals(manifestInfo.database.hash);
        
        Log.i(TAG, "Version Changed: " + versionChanged);
        Log.i(TAG, "Hash Changed: " + hashChanged);
        
        boolean updateNeeded = versionChanged || hashChanged;
        
        if (updateNeeded) {
            Log.i(TAG, "UPDATE NEEDED - Reason: " + (versionChanged ? "Version changed" : "Hash changed"));
        } else {
            Log.i(TAG, "NO UPDATE NEEDED - Database is current");
        }
        
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
     * Fixed update download task with proper database replacement
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
                File backupFile = new File(context.getFilesDir(), BACKUP_DB_NAME);
                
                // Create backup of existing database
                if (localFile.exists()) {
                    Log.i(TAG, "Creating backup of existing database");
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                    if (!localFile.renameTo(backupFile)) {
                        Log.w(TAG, "Failed to create backup, but continuing");
                    }
                }
                
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
                
                // Verify download
                long downloadedSize = tempFile.length();
                Log.i(TAG, "Download completed - Size: " + downloadedSize + " bytes");
                
                if (downloadedSize == 0) {
                    errorMessage = "Downloaded file is empty";
                    return false;
                }
                
                // Calculate actual hash
                String actualHash = getFileHash(tempFile);
                Log.i(TAG, "Downloaded file hash: " + actualHash);
                
                // Move temp file to final location
                if (!tempFile.renameTo(localFile)) {
                    errorMessage = "Failed to save database file";
                    return false;
                }
                
                // Update preferences with new version and hash
                prefs.edit()
                    .putString(KEY_LOCAL_VERSION, manifestInfo.version)
                    .putString(KEY_LOCAL_HASH, actualHash)
                    .putBoolean(KEY_DATABASE_EXISTS, true)
                    .apply();
                
                Log.i(TAG, "Database updated successfully");
                Log.i(TAG, "New version: " + manifestInfo.version);
                Log.i(TAG, "New hash: " + actualHash);
                Log.i(TAG, "File size: " + localFile.length() + " bytes");
                
                // Clean up backup if everything succeeded
                if (backupFile.exists()) {
                    backupFile.delete();
                    Log.i(TAG, "Backup file cleaned up");
                }
                
                return true;
                
            } catch (Exception e) {
                errorMessage = "Download failed: " + e.getMessage();
                Log.e(TAG, "Download error", e);
                
                // Restore backup if download failed
                File backupFile = new File(context.getFilesDir(), BACKUP_DB_NAME);
                File localFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
                
                if (backupFile.exists()) {
                    Log.i(TAG, "Restoring backup due to download failure");
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                    backupFile.renameTo(localFile);
                }
                
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
     * Clear local database (for testing)
     */
    public void clearLocalDatabase() {
        File dbFile = new File(context.getFilesDir(), LOCAL_DB_NAME);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        prefs.edit()
            .remove(KEY_LOCAL_VERSION)
            .remove(KEY_LOCAL_HASH)
            .putBoolean(KEY_DATABASE_EXISTS, false)
            .apply();
        Log.i(TAG, "Local database cleared");
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