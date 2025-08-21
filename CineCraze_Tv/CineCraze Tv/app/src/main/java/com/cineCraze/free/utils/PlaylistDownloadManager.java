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
    
    // Remote download endpoint for the SQLite playlist database
    private static final String GITHUB_JSON_URL = ""; // not used
    private static final String GITHUB_DB_URL = "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db";
    
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
     * Download database from GitHub with progress callbacks.
     * The file is saved into the app's internal databases directory as playlist.db.
     */
    public void downloadDatabase(DownloadCallback callback) {
        this.callback = callback;

        final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        new Thread(() -> {
            try {
                if (callback != null) {
                    mainHandler.post(() -> callback.onDownloadStarted());
                }

                // Prepare target paths
                File dbDir = getLocalDatabaseFile().getParentFile();
                if (dbDir != null && !dbDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dbDir.mkdirs();
                }

                // Ensure old DB is removed to avoid any stale reads
                deleteLocalDatabase();

                File tempFile = new File(dbDir, TEMP_DB_NAME);
                if (tempFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }

                // Append cache-busting query to avoid CDN/proxy caches serving stale content
                String cacheBustedUrl = GITHUB_DB_URL + (GITHUB_DB_URL.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                java.net.URL url = new URL(cacheBustedUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.setUseCaches(false);
                connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                connection.addRequestProperty("Pragma", "no-cache");
                connection.addRequestProperty("Expires", "0");

                int responseCode = connection.getResponseCode();
                if (responseCode / 100 != 2) {
                    String error = "HTTP " + responseCode;
                    if (callback != null) mainHandler.post(() -> callback.onDownloadFailed("Failed to download: " + error));
                    connection.disconnect();
                    return;
                }

                int contentLength = connection.getContentLength();
                boolean hasKnownLength = contentLength > 0;

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(tempFile);

                byte[] buffer = new byte[16 * 1024];
                long totalRead = 0;
                int read;
                int lastProgress = -1;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalRead += read;
                    if (hasKnownLength) {
                        int progress = (int) Math.min(100, (totalRead * 100L) / contentLength);
                        if (progress != lastProgress && callback != null) {
                            int finalProgress = progress;
                            mainHandler.post(() -> callback.onDownloadProgress(finalProgress));
                            lastProgress = progress;
                        }
                    }
                }
                output.flush();
                output.close();
                input.close();
                connection.disconnect();

                // Optional: basic header check to ensure this is a SQLite file
                if (isDatabaseCorrupted(tempFile)) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                    if (callback != null) mainHandler.post(() -> callback.onDownloadFailed("Downloaded file is not a valid SQLite database"));
                    return;
                }

                // Move temp file into place atomically
                File finalFile = getLocalDatabaseFile();
                if (finalFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    finalFile.delete();
                }
                boolean renamed = tempFile.renameTo(finalFile);
                if (!renamed) {
                    // Fallback to copy
                    copyFile(tempFile, finalFile);
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }

                // Save last update timestamp
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                prefs.edit().putString(KEY_LAST_UPDATE, now).apply();

                if (callback != null) {
                    mainHandler.post(() -> callback.onDownloadCompleted(finalFile));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error downloading database", e);
                if (callback != null) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onDownloadFailed("Error: " + msg));
                }
            }
        }, "playlist-db-downloader").start();
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

        return isDatabaseCorrupted(dbFile);
    }

    private boolean isDatabaseCorrupted(File dbFile) {
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

    private static void copyFile(File source, File dest) throws IOException {
        java.io.FileInputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            in = new java.io.FileInputStream(source);
            out = new java.io.FileOutputStream(dest);
            byte[] buf = new byte[16 * 1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (out != null) try { out.close(); } catch (Exception ignored) {}
        }
    }
}