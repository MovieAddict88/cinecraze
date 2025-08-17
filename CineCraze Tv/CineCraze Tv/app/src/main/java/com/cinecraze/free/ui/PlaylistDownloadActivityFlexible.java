package com.cinecraze.free.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cinecraze.free.R;
import com.cinecraze.free.FragmentMainActivity;
import com.cinecraze.free.utils.EnhancedUpdateManagerFlexible;

import java.io.File;
import android.util.Log;

/**
 * Flexible Activity for downloading the playlist database
 * Handles file size mismatches gracefully
 */
public class PlaylistDownloadActivityFlexible extends Activity implements EnhancedUpdateManagerFlexible.UpdateCallback {
    
    private static final String TAG = "PlaylistDownloadFlexible";
    
    private TextView statusText;
    private TextView progressText;
    private ProgressBar progressBar;
    private Button retryButton;
    private Button skipButton;
    private Button continueButton;
    
    private EnhancedUpdateManagerFlexible updateManager;
    private boolean downloadCompleted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_download);
        
        initViews();
        updateManager = new EnhancedUpdateManagerFlexible(this);
        
        // Check if this is a forced update
        boolean forceUpdate = getIntent().getBooleanExtra("force_update", false);
        String manifestVersion = getIntent().getStringExtra("manifest_version");
        
        if (forceUpdate && manifestVersion != null) {
            Log.i(TAG, "Forced update detected for version: " + manifestVersion);
            statusText.setText("Update available for version " + manifestVersion + "\nStarting download...");
            // Force immediate update check
            checkForUpdates();
        } else {
            // Normal flow
            if (updateManager.isDatabaseExists()) {
                // Check for updates using manifest
                checkForUpdates();
            } else {
                // No database exists, start download
                startDownload();
            }
        }
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        retryButton = findViewById(R.id.retry_button);
        skipButton = findViewById(R.id.skip_button);
        continueButton = findViewById(R.id.continue_button);
        
        // Set click listeners
        retryButton.setOnClickListener(v -> checkForUpdates());
        skipButton.setOnClickListener(v -> startMainActivity());
        continueButton.setOnClickListener(v -> startMainActivity());
        
        // Initially hide buttons
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
    }
    
    private void checkForUpdates() {
        // Reset UI
        statusText.setText("Checking for updates...");
        progressText.setText("");
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
        
        // Check for updates using manifest
        updateManager.checkForUpdates(this);
    }
    
    private void startDownload() {
        // Reset UI
        statusText.setText("Downloading playlist database...");
        progressText.setText("0%");
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
        
        // Check for updates (this will download if needed)
        updateManager.checkForUpdates(this);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(this, FragmentMainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onUpdateCheckStarted() {
        runOnUiThread(() -> {
            statusText.setText("Checking for updates...");
            progressText.setText("");
            progressBar.setVisibility(View.GONE);
        });
    }
    
    @Override
    public void onUpdateAvailable(EnhancedUpdateManagerFlexible.ManifestInfo manifestInfo) {
        runOnUiThread(() -> {
            String updateInfo = String.format("Update available!\nVersion: %s\nDescription: %s\nSize: %.2f MB",
                manifestInfo.version,
                manifestInfo.description,
                manifestInfo.database.sizeMb);
            
            statusText.setText(updateInfo);
            progressText.setText("Downloading...");
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            
            // Start downloading the update
            updateManager.downloadUpdate(manifestInfo, this);
        });
    }
    
    @Override
    public void onNoUpdateAvailable() {
        runOnUiThread(() -> {
            statusText.setText("Database is up to date!");
            progressText.setText("");
            progressBar.setVisibility(View.GONE);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Database is current", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadStarted() {
        runOnUiThread(() -> {
            statusText.setText("Downloading update...");
            progressText.setText("0%");
            progressBar.setProgress(0);
        });
    }
    
    @Override
    public void onUpdateDownloadProgress(int progress) {
        runOnUiThread(() -> {
            progressText.setText(progress + "%");
            progressBar.setProgress(progress);
        });
    }
    
    @Override
    public void onUpdateDownloadCompleted() {
        downloadCompleted = true;
        runOnUiThread(() -> {
            statusText.setText("Update completed successfully!");
            progressText.setText("100%");
            progressBar.setProgress(100);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Database updated successfully", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadFailed(String error) {
        runOnUiThread(() -> {
            statusText.setText("Download failed: " + error);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Download failed: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onUpdateCheckFailed(String error) {
        runOnUiThread(() -> {
            statusText.setText("Update check failed: " + error);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Update check failed: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing the activity during download
        if (!downloadCompleted) {
            Toast.makeText(this, "Please wait for download to complete", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}