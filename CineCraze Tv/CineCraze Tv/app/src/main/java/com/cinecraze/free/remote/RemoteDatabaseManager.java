package com.cinecraze.free.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RemoteDatabaseManager {
    private static final String TAG = "RemoteDatabaseManager";

    public interface SetupCallback {
        void onReady();
        void onError(String error);
    }

    public static boolean isInstalled(Context context) {
        File dbFile = context.getDatabasePath(RemoteDbConfig.ROOM_DATABASE_NAME);
        return dbFile.exists() && dbFile.length() > 0;
    }

    public static void checkForUpdateAndPrompt(Context context) {
        new Thread(() -> {
            try {
                RemoteDbManifest manifest = fetchManifest();
                if (manifest == null || manifest.version == null) return;
                SharedPreferences prefs = context.getSharedPreferences(RemoteDbConfig.PREFS_NAME, Context.MODE_PRIVATE);
                String installed = prefs.getString(RemoteDbConfig.KEY_INSTALLED_VERSION, "");
                if (!installed.equals(manifest.version)) {
                    String sizeText = manifest.sizeBytes > 0 ? String.format(Locale.getDefault(), "%.1f MB", manifest.sizeBytes / (1024.0 * 1024.0)) : "unknown";
                    android.os.Handler main = new android.os.Handler(context.getMainLooper());
                    main.post(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Update available")
                            .setMessage("New resources available (" + sizeText + "). Update now?")
                            .setPositiveButton("Update", (d,w) -> stageUpdate(context, manifest))
                            .setNegativeButton("Later", null)
                            .show();
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private static void stageUpdate(Context context, RemoteDbManifest manifest) {
        new Thread(() -> {
            try {
                File cacheDir = context.getCacheDir();
                File tmp = new File(cacheDir, "cinecraze.db" + (manifest.zipped ? ".zip" : ".tmp"));
                downloadToFile(manifest.dbUrl, tmp);

                File finalDb;
                if (manifest.zipped) {
                    File unzipTarget = new File(cacheDir, "cinecraze_unzipped.db");
                    unzipSingle(tmp, unzipTarget);
                    finalDb = unzipTarget;
                } else {
                    finalDb = tmp;
                }

                if (manifest.sha256 != null && !manifest.sha256.isEmpty()) {
                    String actual = sha256Of(finalDb);
                    if (!manifest.sha256.equalsIgnoreCase(actual)) {
                        return;
                    }
                }

                // Stage file
                File staged = context.getDatabasePath(RemoteDbConfig.STAGED_DATABASE_NAME);
                staged.getParentFile().mkdirs();
                if (staged.exists()) staged.delete();
                copy(finalDb, staged);

                SharedPreferences prefs = context.getSharedPreferences(RemoteDbConfig.PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                    .putString(RemoteDbConfig.KEY_PENDING_VERSION, manifest.version)
                    .putString(RemoteDbConfig.KEY_PENDING_SHA256, manifest.sha256 != null ? manifest.sha256 : "")
                    .apply();

                android.os.Handler main = new android.os.Handler(context.getMainLooper());
                main.post(() -> new AlertDialog.Builder(context)
                    .setTitle("Restart required")
                    .setMessage("Resources will be updated next time you open the app.")
                    .setPositiveButton("OK", null)
                    .show());
            } catch (Exception e) {
                Log.e(TAG, "stageUpdate error: " + e.getMessage(), e);
            }
        }).start();
    }

    public static void activateStagedIfAny(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(RemoteDbConfig.PREFS_NAME, Context.MODE_PRIVATE);
        String pending = prefs.getString(RemoteDbConfig.KEY_PENDING_VERSION, "");
        if (pending == null || pending.isEmpty()) return;
        File staged = context.getDatabasePath(RemoteDbConfig.STAGED_DATABASE_NAME);
        if (!staged.exists()) return;
        File installed = context.getDatabasePath(RemoteDbConfig.ROOM_DATABASE_NAME);
        if (installed.exists()) installed.delete();
        staged.renameTo(installed);
        // refresh prepack copy
        try {
            File prepack = new File(context.getFilesDir(), RemoteDbConfig.PREPACK_FILE_NAME);
            if (prepack.exists()) prepack.delete();
            copy(installed, prepack);
        } catch (Exception ignored) {}
        prefs.edit()
            .putString(RemoteDbConfig.KEY_INSTALLED_VERSION, pending)
            .putString(RemoteDbConfig.KEY_PENDING_VERSION, "")
            .apply();
    }

    private static RemoteDbManifest fetchManifest() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(RemoteDbConfig.MANIFEST_URL).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "CineCraze/1.0");
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            byte[] bytes = readAll(in);
            String json = new String(bytes);
            return new Gson().fromJson(json, RemoteDbManifest.class);
        } finally {
            conn.disconnect();
        }
    }

    public static void ensureDatabase(Context context, boolean showPrompt, SetupCallback callback) {
        new Thread(() -> {
            try {
                activateStagedIfAny(context);
                if (isInstalled(context)) {
                    callback.onReady();
                    return;
                }
                RemoteDbManifest manifest = fetchManifest();
                if (manifest == null || manifest.dbUrl == null || manifest.dbUrl.isEmpty()) {
                    callback.onError("Manifest missing or invalid");
                    return;
                }
                String sizeText = manifest.sizeBytes > 0 ? String.format(Locale.getDefault(), "%.1f MB", manifest.sizeBytes / (1024.0 * 1024.0)) : "unknown";

                if (showPrompt) {
                    android.os.Handler main = new android.os.Handler(context.getMainLooper());
                    main.post(() -> {
                        new AlertDialog.Builder(new androidx.appcompat.view.ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog))
                                .setTitle("Download required")
                                .setMessage("Additional resources required (" + sizeText + "). Download now?")
                                .setCancelable(false)
                                .setPositiveButton("Download", (dialog, which) -> startDownload(context, manifest, callback))
                                .setNegativeButton("Exit", (dialog, which) -> callback.onError("User canceled"))
                                .show();
                    });
                } else {
                    startDownload(context, manifest, callback);
                }
            } catch (Exception e) {
                Log.e(TAG, "ensureDatabase error: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private static void startDownload(Context context, RemoteDbManifest manifest, SetupCallback callback) {
        new Thread(() -> {
            try {
                File cacheDir = context.getCacheDir();
                File tmp = new File(cacheDir, "cinecraze.db" + (manifest.zipped ? ".zip" : ".tmp"));
                downloadToFile(manifest.dbUrl, tmp);

                File finalDb;
                if (manifest.zipped) {
                    File unzipTarget = new File(cacheDir, "cinecraze_unzipped.db");
                    unzipSingle(tmp, unzipTarget);
                    finalDb = unzipTarget;
                } else {
                    finalDb = tmp;
                }

                if (manifest.sha256 != null && !manifest.sha256.isEmpty()) {
                    String actual = sha256Of(finalDb);
                    if (!manifest.sha256.equalsIgnoreCase(actual)) {
                        callback.onError("Checksum mismatch");
                        return;
                    }
                }

                File dbPath = context.getDatabasePath(RemoteDbConfig.ROOM_DATABASE_NAME);
                dbPath.getParentFile().mkdirs();
                if (dbPath.exists()) dbPath.delete();
                copy(finalDb, dbPath);

                // Also copy to prepack location for Room createFromFile bootstrap
                File prepack = new File(context.getFilesDir(), RemoteDbConfig.PREPACK_FILE_NAME);
                if (prepack.exists()) prepack.delete();
                copy(dbPath, prepack);

                SharedPreferences prefs = context.getSharedPreferences(RemoteDbConfig.PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString(RemoteDbConfig.KEY_INSTALLED_VERSION, manifest.version != null ? manifest.version : "")
                        .putString(RemoteDbConfig.KEY_INSTALLED_SHA256, manifest.sha256 != null ? manifest.sha256 : "")
                        .apply();

                callback.onReady();
            } catch (Exception e) {
                Log.e(TAG, "startDownload error: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private static void downloadToFile(String urlStr, File out) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "CineCraze/1.0");
        try (InputStream in = new BufferedInputStream(conn.getInputStream()); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void unzipSingle(File zip, File target) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                    }
                    break; // first file only
                }
            }
        }
    }

    private static void copy(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    private static String sha256Of(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                digest.update(buf, 0, len);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) baos.write(buf, 0, len);
        return baos.toByteArray();
    }
}