package com.cinecraze.free.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.cinecraze.free.R;
import com.cinecraze.free.utils.DatabaseLocationHelper;
import com.cinecraze.free.utils.PlaylistDownloadManager;

/**
 * Simple activity to display database location information
 * For debugging purposes only
 */
public class DatabaseInfoActivitySimple extends Activity {
    
    private TextView infoText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout programmatically
        infoText = new TextView(this);
        infoText.setPadding(32, 32, 32, 32);
        infoText.setTextColor(0xFFFFFFFF); // White text
        infoText.setBackgroundColor(0xFF000000); // Black background
        infoText.setTextSize(14);
        
        setContentView(infoText);
        
        // Show database info
        showDatabaseInfo();
    }
    
    private void showDatabaseInfo() {
        DatabaseLocationHelper locationHelper = new DatabaseLocationHelper(this);
        PlaylistDownloadManager downloadManager = new PlaylistDownloadManager(this);
        
        StringBuilder info = new StringBuilder();
        info.append("=== DATABASE LOCATION INFO ===\n\n");
        info.append("📁 App Files Directory:\n");
        info.append(locationHelper.getAppFilesDirectory()).append("\n\n");
        
        info.append("🗄️ Database File Path:\n");
        info.append(locationHelper.getDatabasePath()).append("\n\n");
        
        info.append("📱 Typical Android Path:\n");
        info.append(locationHelper.getTypicalDatabasePath()).append("\n\n");
        
        info.append("=== DATABASE STATUS ===\n\n");
        info.append("✅ Database Exists: ").append(locationHelper.isDatabaseExists()).append("\n");
        info.append("📖 Readable: ").append(locationHelper.isDatabaseReadable()).append("\n");
        info.append("✏️ Writable: ").append(locationHelper.isDatabaseWritable()).append("\n");
        info.append("🔐 Permissions: ").append(locationHelper.getDatabasePermissions()).append("\n");
        
        if (locationHelper.isDatabaseExists()) {
            info.append("📊 Size: ").append(String.format("%.2f", locationHelper.getDatabaseSizeMB())).append(" MB\n");
            info.append("📅 Last Modified: ").append(locationHelper.getDatabaseLastModified()).append("\n");
        }
        
        info.append("\n=== DOWNLOAD MANAGER INFO ===\n\n");
        info.append("🔄 Update Needed: ").append(downloadManager.isUpdateNeeded()).append("\n");
        info.append("⏰ Last Update: ").append(downloadManager.getLastUpdateTime()).append("\n");
        info.append("💾 Database Size: ").append(String.format("%.2f", downloadManager.getDatabaseSizeMB())).append(" MB\n");
        info.append("⚠️ Corrupted: ").append(downloadManager.isDatabaseCorrupted()).append("\n");
        
        info.append("\n=== HOW TO ACCESS ===\n\n");
        info.append("1. Use ADB (requires root):\n");
        info.append("   adb shell\n");
        info.append("   su\n");
        info.append("   cp ").append(locationHelper.getDatabasePath()).append(" /sdcard/\n\n");
        
        info.append("2. Use File Manager (requires root):\n");
        info.append("   Navigate to: ").append(locationHelper.getTypicalDataFolderPath()).append("\n\n");
        
        info.append("3. Use Android Studio:\n");
        info.append("   Device File Explorer → data → data → com.cinecraze.free → files\n\n");
        
        infoText.setText(info.toString());
        
        // Log to logcat for debugging
        locationHelper.logDatabaseInfo();
        
        Toast.makeText(this, "Database info displayed", Toast.LENGTH_SHORT).show();
    }
}