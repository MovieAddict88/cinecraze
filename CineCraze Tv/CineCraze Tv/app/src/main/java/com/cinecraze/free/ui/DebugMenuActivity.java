package com.cinecraze.free.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.cinecraze.free.FragmentMainActivity;
import com.cinecraze.free.R;

/**
 * Debug menu for testing update detection and database management
 */
public class DebugMenuActivity extends AppCompatActivity {

    private TextView logTextView;
    private FragmentMainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout programmatically
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);
        
        // Title
        TextView titleText = new TextView(this);
        titleText.setText("Debug Menu - Update System");
        titleText.setTextSize(20);
        titleText.setPadding(0, 0, 0, 32);
        container.addView(titleText);
        
        // Log display
        logTextView = new TextView(this);
        logTextView.setText("Debug logs will appear here...");
        logTextView.setPadding(0, 0, 0, 32);
        container.addView(logTextView);
        
        // Buttons
        Button[] buttons = {
            createButton("Test Update Detection", v -> testUpdateDetection()),
            createButton("Force Fresh Install", v -> forceFreshInstall()),
            createButton("Clear Database", v -> clearDatabase()),
            createButton("Check Database Status", v -> checkDatabaseStatus()),
            createButton("Reset Update Version", v -> resetUpdateVersion()),
            createButton("Force Update Check", v -> forceUpdateCheck()),
            createButton("Clear All Preferences", v -> clearAllPreferences()),
            createButton("Refresh Logs", v -> refreshLogs())
        };
        
        for (Button button : buttons) {
            container.addView(button);
        }
        
        scrollView.addView(container);
        setContentView(scrollView);
        
        // Avoid casting application context to an activity (crash source)
        mainActivity = null;
    }
    
    private Button createButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setPadding(0, 16, 0, 16);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        button.setOnClickListener(listener);
        return button;
    }
    
    private void testUpdateDetection() {
        log("Testing update detection...");
        log("Launching update activity for testing...");
        try {
            android.content.Intent intent = new android.content.Intent(this, com.cinecraze.free.ui.PlaylistDownloadActivityFlexible.class);
            intent.putExtra("force_update", true);
            startActivity(intent);
        } catch (Exception e) {
            log("Failed to launch update activity: " + e.getMessage());
        }
    }
    
    private void forceFreshInstall() {
        log("Forcing fresh install experience...");
        log("Clearing DB and prefs, then restarting startup flow...");
        clearDbAndPrefs();
        restartToStartup();
    }
    
    private void clearDatabase() {
        log("Clearing database...");
        log("Clearing only database, then restarting startup...");
        clearOnlyDb();
        restartToStartup();
    }
    
    private void checkDatabaseStatus() {
        log("Checking database status...");
        log("Opening DatabaseInfoActivity...");
        try {
            startActivity(new android.content.Intent(this, com.cinecraze.free.ui.DatabaseInfoActivity.class));
        } catch (Exception e) {
            log("Failed to open database info: " + e.getMessage());
        }
    }
    
    private void resetUpdateVersion() {
        log("Resetting update version...");
        log("Resetting update version flag...");
        android.content.SharedPreferences sp = getSharedPreferences("app_open_update_prefs", MODE_PRIVATE);
        sp.edit().remove("last_handled_manifest_version").apply();
    }
    
    private void forceUpdateCheck() {
        log("Forcing update check...");
        log("Triggering manifest check in background from main screen (if visible)...");
        try {
            android.content.Intent i = new android.content.Intent(this, com.cinecraze.free.FragmentMainActivity.class);
            i.putExtra("trigger_manifest_check", true);
            startActivity(i);
        } catch (Exception e) {
            log("Failed to trigger: " + e.getMessage());
        }
    }
    
    private void clearAllPreferences() {
        log("Clearing all preferences...");
        log("Clearing all preferences...");
        getSharedPreferences("app_open_update_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("playlist_update_prefs", MODE_PRIVATE).edit().clear().apply();
        log("Done.");

    }

    private void clearDbAndPrefs() {
        clearOnlyDb();
        getSharedPreferences("app_open_update_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("playlist_update_prefs", MODE_PRIVATE).edit().clear().apply();
    }

    private void clearOnlyDb() {
        java.io.File dbFile = new java.io.File(getFilesDir(), "playlist.db");
        if (dbFile.exists()) dbFile.delete();
    }

    private void restartToStartup() {
        android.content.Intent intent = new android.content.Intent(this, StartupActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void refreshLogs() {
        log("Refreshing logs...");
        // This would ideally fetch logs from the main activity
        log("Debug menu refreshed at " + System.currentTimeMillis());
    }
    
    private void log(String message) {
        String timestamp = java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
        String logMessage = timestamp + ": " + message + "\n";
        logTextView.append(logMessage);
        
        // Scroll to bottom
        final ScrollView scrollView = (ScrollView) findViewById(android.R.id.content);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}