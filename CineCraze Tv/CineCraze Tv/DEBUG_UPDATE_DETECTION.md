# Debugging Update Detection - Step by Step Guide

## Current Status
- Remote manifest version: **"4"**
- Local manifest version: **"test-002"** (from build/db-manifest.json)
- Update should be detected immediately

## Testing Steps

### 1. Build and Install the App
```bash
# Build the app with the latest changes
./gradlew assembleDebug

# Install on device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Monitor Logs
```bash
# Clear logs first
adb logcat -c

# Monitor logs for update detection
adb logcat | grep -E "(FragmentMainActivity|EnhancedUpdateManagerFlexible|UPDATE|MANIFEST)"
```

### 3. Expected Log Output
When the app starts, you should see:
```
I/FragmentMainActivity: === FORCING IMMEDIATE UPDATE CHECK ON APP START ===
I/FragmentMainActivity: === TESTING UPDATE DETECTION ===
I/FragmentMainActivity: Cleared all update preferences
I/FragmentMainActivity: Cleared EnhancedUpdateManagerFlexible preferences
D/FragmentMainActivity: === STARTING MANIFEST CHECK ===
D/FragmentMainActivity: Database exists: true/false
D/FragmentMainActivity: Starting manifest check for updates...
D/FragmentMainActivity: Fetching manifest from: https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json?_cb=...
D/FragmentMainActivity: Manifest HTTP response code: 200
D/FragmentMainActivity: Full manifest content: {"version":"4",...}
D/FragmentMainActivity: Remote manifest version: 4
D/FragmentMainActivity: Last handled version: 
D/FragmentMainActivity: Version comparison: '' vs '4'
I/FragmentMainActivity: *** UPDATE DETECTED *** New manifest version: 4 (was: )
I/FragmentMainActivity: *** LAUNCHING UPDATE ACTIVITY *** for version: 4
```

### 4. If Update is Not Detected

#### Option A: Force Update via Code
Add this to any button click or menu item:
```java
// In any activity or fragment
FragmentMainActivity activity = (FragmentMainActivity) getActivity();
if (activity != null) {
    activity.testUpdateDetection();
}
```

#### Option B: Clear App Data
```bash
# Clear app data to reset all preferences
adb shell pm clear com.cinecraze.free
```

#### Option C: Manual Testing
1. Open the app
2. Look for the toast message "Testing update detection..."
3. Check if the update activity launches
4. Monitor logs for any errors

### 5. Common Issues and Solutions

#### Issue: No logs appear
**Solution**: Check if the app is actually running the updated code
```bash
# Verify the app was installed
adb shell pm list packages | grep cinecraze
```

#### Issue: Logs show but no update detected
**Solution**: Check the version comparison logic
- Look for "Version comparison" in logs
- Verify the remote version is "4"
- Check if last handled version is empty or different

#### Issue: Update detected but no activity launched
**Solution**: Check if PlaylistDownloadActivityFlexible exists
```bash
# Check if the activity is declared in AndroidManifest.xml
grep -r "PlaylistDownloadActivityFlexible" app/src/main/
```

#### Issue: Network errors
**Solution**: Check network connectivity
```bash
# Test manifest access
curl -s "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json"
```

### 6. Force Update Detection

If the automatic detection still doesn't work, you can force it by:

1. **Clearing SharedPreferences**:
```java
// Add this to onCreate or any method
SharedPreferences sp = getSharedPreferences("app_open_update_prefs", MODE_PRIVATE);
sp.edit().clear().apply();
```

2. **Setting forceUpdate to true**:
In `checkManifestAndMaybeForceUpdate()`, the line:
```java
boolean forceUpdate = true; // This forces update detection
```

3. **Manual trigger**:
Call `testUpdateDetection()` from any UI element.

### 7. Verification Checklist

- [ ] App builds and installs successfully
- [ ] Logs show "Testing update detection..." toast
- [ ] Logs show manifest fetch attempt
- [ ] Logs show "UPDATE DETECTED" message
- [ ] Update activity launches
- [ ] Database downloads successfully

### 8. Next Steps

If the update detection works:
1. Remove the `forceUpdate = true` line
2. Remove the automatic `testUpdateDetection()` call from onCreate
3. Test normal update detection flow
4. Adjust polling intervals if needed

If it still doesn't work:
1. Check all logs for errors
2. Verify network connectivity
3. Test with different manifest versions
4. Check if the update activity exists and is properly declared