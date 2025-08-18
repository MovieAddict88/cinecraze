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
        
        // Get reference to main activity
        mainActivity = (FragmentMainActivity) getApplicationContext();
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
        if (mainActivity != null) {
            mainActivity.testUpdateDetection();
        } else {
            log("Main activity not available");
        }
    }
    
    private void forceFreshInstall() {
        log("Forcing fresh install experience...");
        if (mainActivity != null) {
            mainActivity.forceFreshInstall();
        } else {
            log("Main activity not available");
        }
    }
    
    private void clearDatabase() {
        log("Clearing database...");
        if (mainActivity != null) {
            mainActivity.forceClearDatabase();
        } else {
            log("Main activity not available");
        }
    }
    
    private void checkDatabaseStatus() {
        log("Checking database status...");
        if (mainActivity != null) {
            mainActivity.checkDatabaseStatus();
        } else {
            log("Main activity not available");
        }
    }
    
    private void resetUpdateVersion() {
        log("Resetting update version...");
        if (mainActivity != null) {
            mainActivity.resetUpdateVersion();
        } else {
            log("Main activity not available");
        }
    }
    
    private void forceUpdateCheck() {
        log("Forcing update check...");
        if (mainActivity != null) {
            mainActivity.forceUpdateCheck();
        } else {
            log("Main activity not available");
        }
    }
    
    private void clearAllPreferences() {
        log("Clearing all preferences...");
        if (mainActivity != null) {
            mainActivity.forceFreshUpdateCheck();
        } else {
            log("Main activity not available");
        }
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