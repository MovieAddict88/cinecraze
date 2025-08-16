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
 * Professional App Open Update Screen
 * Shows update screen but removes annoying download dialogs
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
        
        // Always show the professional update screen
        showProfessionalUpdateScreen();
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
    
    private void showProfessionalUpdateScreen() {
        // Show professional update screen
        statusText.setText("CineCraze - Movie & TV Streaming");
        progressText.setText("Checking for updates...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        
        // Start background update check
        checkForUpdates();
    }
    
    private void checkForUpdates() {
        // Reset UI for update check
        statusText.setText("CineCraze - Movie & TV Streaming");
        progressText.setText("Checking for updates...");
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
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
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Checking for updates...");
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
        });
    }
    
    @Override
    public void onUpdateAvailable(EnhancedUpdateManagerFixed.ManifestInfo manifestInfo) {
        runOnUiThread(() -> {
            // Show professional update message
            String updateInfo = String.format("Update Available\nVersion: %s\n%s", 
                manifestInfo.version,
                manifestInfo.description);
            
            statusText.setText(updateInfo);
            progressText.setText("Downloading...");
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            
            // Start downloading the update
            updateManager.downloadUpdate(manifestInfo, this);
        });
    }
    
    @Override
    public void onNoUpdateAvailable() {
        runOnUiThread(() -> {
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Ready to stream!");
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Ready to stream!", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadStarted() {
        runOnUiThread(() -> {
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Downloading update...");
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
        });
    }
    
    @Override
    public void onUpdateDownloadProgress(int progress) {
        runOnUiThread(() -> {
            progressText.setText("Downloading... " + progress + "%");
            progressBar.setProgress(progress);
        });
    }
    
    @Override
    public void onUpdateDownloadCompleted() {
        downloadCompleted = true;
        runOnUiThread(() -> {
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Update completed!");
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
            
            // Show continue button
            continueButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            skipButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Update completed successfully!", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onUpdateDownloadFailed(String error) {
        runOnUiThread(() -> {
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Update failed");
            progressBar.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Update failed: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onUpdateCheckFailed(String error) {
        runOnUiThread(() -> {
            statusText.setText("CineCraze - Movie & TV Streaming");
            progressText.setText("Check failed");
            progressBar.setVisibility(View.GONE);
            
            // Show retry and skip buttons
            retryButton.setVisibility(View.VISIBLE);
            skipButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.GONE);
            
            Toast.makeText(this, "Check failed: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from closing the activity during download
        if (!downloadCompleted) {
            Toast.makeText(this, "Please wait for update to complete", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}