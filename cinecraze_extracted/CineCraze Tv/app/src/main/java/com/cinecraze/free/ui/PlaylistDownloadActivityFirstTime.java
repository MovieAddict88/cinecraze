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
import com.cinecraze.free.utils.EnhancedUpdateManagerFixed;

import java.io.File;

/**
 * First-Time Optimized Activity for downloading the playlist database
 * Shows download directly on first install, update check only for existing users
 */
public class PlaylistDownloadActivityFirstTime extends Activity implements EnhancedUpdateManagerFixed.UpdateCallback {
    
    private static final String TAG = "PlaylistDownloadFirstTime";
    
    private TextView statusText;
    private TextView progressText;
    private ProgressBar progressBar;
    private Button retryButton;
    private Button skipButton;
    private Button continueButton;
    
    private EnhancedUpdateManagerFixed updateManager;
    private boolean downloadCompleted = false;
    private boolean isFirstTime = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_download);
        
        initViews();
        updateManager = new EnhancedUpdateManagerFixed(this);
        
        // Check if this is first time (no database exists)
        isFirstTime = !updateManager.isDatabaseExists();
        
        if (isFirstTime) {
            // First time - show download directly
            startFirstTimeDownload();
        } else {
            // Existing user - check for updates
            checkForUpdates();
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
        retryButton.setOnClickListener(v -> {
            if (isFirstTime) {
                startFirstTimeDownload();
            } else {
                checkForUpdates();
            }
        });
        skipButton.setOnClickListener(v -> startMainActivity());
        continueButton.setOnClickListener(v -> startMainActivity());
        
        // Initially hide buttons
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
    }
    
    private void startFirstTimeDownload() {
        // Reset UI for first-time download
        statusText.setText("Downloading playlist database...");
        progressText.setText("0%");
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
        
        // Start download directly (no update check)
        updateManager.checkForUpdates(this);
    }
    
    private void checkForUpdates() {
        // Reset UI for update check
        statusText.setText("Checking for updates...");
        progressText.setText("");
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
        
        // Check for updates using manifest
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
            if (isFirstTime) {
                // First time - show download message
                statusText.setText("Downloading playlist database...");
                progressText.setText("0%");
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                // Existing user - show update check message
                statusText.setText("Checking for updates...");
                progressText.setText("");
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    @Override
    public void onUpdateAvailable(EnhancedUpdateManagerFixed.ManifestInfo manifestInfo) {
        runOnUiThread(() -> {
            if (isFirstTime) {
                // First time - show simple download message
                statusText.setText("Downloading playlist database...");
                progressText.setText("0%");
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                // Existing user - show update details
                String updateInfo = String.format("Update available!\nVersion: %s\nDescription: %s\nSize: %.2f MB",
                    manifestInfo.version,
                    manifestInfo.description,
                    manifestInfo.database.sizeMb);
                
                statusText.setText(updateInfo);
                progressText.setText("Downloading...");
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }
            
            // Start downloading
            updateManager.downloadUpdate(manifestInfo, this);
        });
    }
    
    @Override
    public void onNoUpdateAvailable() {
        runOnUiThread(() -> {
            if (isFirstTime) {
                // First time - this shouldn't happen, but handle gracefully
                statusText.setText("Database downloaded successfully!");
                progressText.setText("100%");
                progressBar.setProgress(100);
            } else {
                // Existing user - show up to date message
                statusText.setText("Database is up to date!");
                progressText.setText("");
                progressBar.setVisibility(View.GONE);
            }
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Database ready", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadStarted() {
        runOnUiThread(() -> {
            if (isFirstTime) {
                statusText.setText("Downloading playlist database...");
            } else {
                statusText.setText("Downloading update...");
            }
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
            if (isFirstTime) {
                statusText.setText("Database downloaded successfully!");
            } else {
                statusText.setText("Update completed successfully!");
            }
            progressText.setText("100%");
            progressBar.setProgress(100);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Database ready", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadFailed(String error) {
        runOnUiThread(() -> {
            if (isFirstTime) {
                statusText.setText("Download failed: " + error);
            } else {
                statusText.setText("Update failed: " + error);
            }
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
            if (isFirstTime) {
                statusText.setText("Download failed: " + error);
            } else {
                statusText.setText("Update check failed: " + error);
            }
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Failed: " + error, Toast.LENGTH_LONG).show();
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