package com.cinecraze.free.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cinecraze.free.R;
import com.cinecraze.free.utils.DatabaseLocationHelper;
import com.cinecraze.free.utils.PlaylistDownloadManager;

/**
 * Activity to display database location information
 * Useful for debugging and development
 */
public class DatabaseInfoActivity extends Activity {
    
    private TextView infoText;
    private Button downloadButton;
    private Button refreshButton;
    private Button backButton;
    
    private DatabaseLocationHelper locationHelper;
    private PlaylistDownloadManager downloadManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_info);
        
        locationHelper = new DatabaseLocationHelper(this);
        downloadManager = new PlaylistDownloadManager(this);
        
        initViews();
        refreshDatabaseInfo();
    }
    
    private void initViews() {
        infoText = findViewById(R.id.info_text);
        downloadButton = findViewById(R.id.download_button);
        refreshButton = findViewById(R.id.refresh_button);
        backButton = findViewById(R.id.back_button);
        
        downloadButton.setOnClickListener(v -> Toast.makeText(this, "Download disabled (using bundled DB)", Toast.LENGTH_SHORT).show());
        refreshButton.setOnClickListener(v -> refreshDatabaseInfo());
        backButton.setOnClickListener(v -> finish());
    }
    
    private void refreshDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        
        // Database location info
        info.append("=== DATABASE LOCATION INFO ===\n\n");
        info.append("📁 App Files Directory:\n");
        info.append(locationHelper.getAppFilesDirectory()).append("\n\n");
        
        info.append("🗄️ Database File Path:\n");
        info.append(locationHelper.getDatabasePath()).append("\n\n");
        
        info.append("📱 Typical Android Path:\n");
        info.append(locationHelper.getTypicalDatabasePath()).append("\n\n");
        
        // Database status
        info.append("=== DATABASE STATUS ===\n\n");
        info.append("✅ Database Exists: ").append(locationHelper.isDatabaseExists()).append("\n");
        info.append("📖 Readable: ").append(locationHelper.isDatabaseReadable()).append("\n");
        info.append("✏️ Writable: ").append(locationHelper.isDatabaseWritable()).append("\n");
        info.append("🔐 Permissions: ").append(locationHelper.getDatabasePermissions()).append("\n");
        
        if (locationHelper.isDatabaseExists()) {
            info.append("📊 Size: ").append(String.format("%.2f", locationHelper.getDatabaseSizeMB())).append(" MB\n");
            info.append("📅 Last Modified: ").append(locationHelper.getDatabaseLastModified()).append("\n");
        }
        
        // Bundled info
        info.append("\n=== BUNDLED DATABASE INFO ===\n\n");
        info.append("💾 Database Size: ").append(String.format("%.2f", downloadManager.getDatabaseSizeMB())).append(" MB\n");
        info.append("⚠️ Corrupted: ").append(downloadManager.isDatabaseCorrupted()).append("\n");
        
        // Instructions
        info.append("\n=== HOW TO ACCESS ===\n\n");
        info.append("1. Use ADB (requires root):\n");
        info.append("   adb shell\n");
        info.append("   su\n");
        info.append("   cp ").append(locationHelper.getDatabasePath()).append(" /sdcard/\n\n");
        
        info.append("2. Use File Manager (requires root):\n");
        info.append("   Navigate to: ").append(locationHelper.getTypicalDataFolderPath()).append("\n\n");
        
        info.append("3. Use Android Studio:\n");
        info.append("   Device File Explorer → data → data → com.cinecraze.free → files\n\n");
        
        info.append("4. Programmatically:\n");
        info.append("   File dbFile = new File(context.getFilesDir(), \"playlist.db\");\n");
        
        infoText.setText(info.toString());
        
        // Log to logcat for debugging
        locationHelper.logDatabaseInfo();
    }
    
    private void startDownload() { /* disabled */ }
}