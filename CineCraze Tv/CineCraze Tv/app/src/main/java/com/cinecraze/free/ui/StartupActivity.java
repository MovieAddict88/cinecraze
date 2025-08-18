package com.cinecraze.free.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.FragmentMainActivity;
import com.cinecraze.free.utils.EnhancedUpdateManagerFlexible;

/**
 * StartupActivity: single-responsibility launcher that decides the initial flow.
 * - If playlist database is missing: prompt and download first, then go to main
 * - If present: go straight to main
 * This prevents app-open update prompts from racing the initial download flow.
 */
public class StartupActivity extends AppCompatActivity {

    private static final String TAG = "StartupActivity";
    private EnhancedUpdateManagerFlexible updateManager;
    private EnhancedUpdateManagerFlexible.ManifestInfo initialManifestInfo;
    private AlertDialog downloadingDialog;
    private android.widget.TextView downloadingMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateManager = new EnhancedUpdateManagerFlexible(this);

        // Decide the flow as early as possible
        if (!updateManager.isDatabaseExists()) {
            Log.d(TAG, "Fresh install detected - database missing. Showing download prompt");
            showFreshInstallDialog();
        } else {
            Log.d(TAG, "Database exists - launching main activity");
            launchMain();
        }
    }

    private void showFreshInstallDialog() {
        // Try to fetch manifest to show accurate size; fallback to default
        updateManager.checkForUpdates(new EnhancedUpdateManagerFlexible.UpdateCallback() {
            @Override public void onUpdateCheckStarted() { }

            @Override
            public void onUpdateAvailable(EnhancedUpdateManagerFlexible.ManifestInfo manifestInfo) {
                initialManifestInfo = manifestInfo;
                long bytes = manifestInfo != null && manifestInfo.database != null ? manifestInfo.database.sizeBytes : -1L;
                runOnUiThread(() -> showDownloadPrompt(bytes));
            }

            @Override
            public void onNoUpdateAvailable() {
                EnhancedUpdateManagerFlexible.ManifestInfo fallback = new EnhancedUpdateManagerFlexible.ManifestInfo();
                fallback.version = "initial";
                fallback.description = "Initial database download";
                EnhancedUpdateManagerFlexible.ManifestInfo.DatabaseInfo db = new EnhancedUpdateManagerFlexible.ManifestInfo.DatabaseInfo();
                db.sizeBytes = 12 * 1024 * 1024L;
                db.sizeMb = 12;
                fallback.database = db;
                initialManifestInfo = fallback;
                runOnUiThread(() -> showDownloadPrompt(db.sizeBytes));
            }

            @Override public void onUpdateCheckFailed(String error) {
                runOnUiThread(() -> new AlertDialog.Builder(StartupActivity.this)
                    .setTitle("Network error")
                    .setMessage("Failed to check update: " + error)
                    .setPositiveButton("Retry", (d, w) -> showFreshInstallDialog())
                    .setNegativeButton("Exit", (d, w) -> finish())
                    .setCancelable(false)
                    .show());
            }

            @Override public void onUpdateDownloadStarted() { }
            @Override public void onUpdateDownloadProgress(int progress) { }
            @Override public void onUpdateDownloadCompleted() { }
            @Override public void onUpdateDownloadFailed(String error) { }
        });
    }

    private void showDownloadPrompt(long contentLengthBytes) {
        String sizeText;
        if (contentLengthBytes > 0) {
            double mb = contentLengthBytes / (1024.0 * 1024.0);
            sizeText = String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
        } else {
            sizeText = "unknown size";
        }

        new AlertDialog.Builder(this)
            .setTitle("Download required")
            .setMessage("Initial data needs to be downloaded (" + sizeText + "). Continue?")
            .setPositiveButton("Download", (dialog, which) -> startInitialDownload(contentLengthBytes))
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void startInitialDownload(long estimatedBytes) {
        showDownloadingDialog(estimatedBytes);
        if (initialManifestInfo == null) {
            if (downloadingDialog != null && downloadingDialog.isShowing()) downloadingDialog.dismiss();
            finish();
            return;
        }
        updateManager.downloadUpdate(initialManifestInfo, new EnhancedUpdateManagerFlexible.UpdateCallback() {
            @Override public void onUpdateCheckStarted() { }
            @Override public void onUpdateAvailable(EnhancedUpdateManagerFlexible.ManifestInfo manifestInfo) { }
            @Override public void onNoUpdateAvailable() { }
            @Override public void onUpdateCheckFailed(String error) { }

            @Override public void onUpdateDownloadStarted() { }

            @Override
            public void onUpdateDownloadProgress(int progress) {
                runOnUiThread(() -> {
                    if (downloadingMessageText != null) {
                        String sizeText;
                        if (estimatedBytes > 0) {
                            double mb = estimatedBytes / (1024.0 * 1024.0);
                            sizeText = String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
                        } else {
                            sizeText = "unknown size";
                        }
                        downloadingMessageText.setText("Downloading data (" + sizeText + ")...\n" + progress + "%");
                    }
                });
            }

            @Override
            public void onUpdateDownloadCompleted() {
                runOnUiThread(() -> {
                    if (downloadingDialog != null && downloadingDialog.isShowing()) downloadingDialog.dismiss();
                    // Mark this manifest version as handled so app-open update won't immediately trigger
                    try {
                        if (initialManifestInfo != null && initialManifestInfo.version != null && !initialManifestInfo.version.isEmpty()) {
                            android.content.SharedPreferences sp = getSharedPreferences("app_open_update_prefs", MODE_PRIVATE);
                            sp.edit().putString("last_handled_manifest_version", initialManifestInfo.version).apply();
                        }
                    } catch (Exception ignored) {}
                    launchMain();
                });
            }

            @Override
            public void onUpdateDownloadFailed(String error) {
                runOnUiThread(() -> {
                    if (downloadingDialog != null && downloadingDialog.isShowing()) downloadingDialog.dismiss();
                    new AlertDialog.Builder(StartupActivity.this)
                        .setTitle("Download failed")
                        .setMessage(error)
                        .setPositiveButton("Retry", (d, w) -> showFreshInstallDialog())
                        .setNegativeButton("Exit", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                });
            }
        });
    }

    private void showDownloadingDialog(long estimatedBytes) {
        String sizeText;
        if (estimatedBytes > 0) {
            double mb = estimatedBytes / (1024.0 * 1024.0);
            sizeText = String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
        } else {
            sizeText = "unknown size";
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        android.widget.LinearLayout.LayoutParams pbParams = new android.widget.LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        pbParams.setMargins(0, 0, padding, 0);
        progressBar.setLayoutParams(pbParams);

        downloadingMessageText = new android.widget.TextView(this);
        downloadingMessageText.setText("Downloading data (" + sizeText + ")...\nPlease wait, this may take a moment.");
        downloadingMessageText.setTextSize(14);

        container.addView(progressBar);
        container.addView(downloadingMessageText);

        downloadingDialog = new AlertDialog.Builder(this)
            .setTitle("Downloading")
            .setView(container)
            .setCancelable(false)
            .create();
        downloadingDialog.setCanceledOnTouchOutside(false);
        downloadingDialog.show();
    }

    private void launchMain() {
        Intent intent = new Intent(this, FragmentMainActivity.class);
        // Signal that initial setup is complete so app-open update logic can run later
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

