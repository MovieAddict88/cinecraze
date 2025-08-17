# Update Detection Debugging Guide

## Overview
This document explains how the playlist.db app detects updates from the remote manifest.json file and how to troubleshoot issues.

## How Update Detection Works

### 1. Polling Mechanism
- **Background**: Checks every 1 minute when app is in background
- **Active**: Checks every 30 seconds when app is in foreground
- **On Resume**: Immediate check when app comes to foreground

### 2. Update Detection Logic
The app checks for updates by comparing:
- **Version**: Remote manifest version vs local stored version
- **Size**: Database file size vs manifest-reported size
- **Hash**: SHA-256 hash (if provided in manifest)

### 3. Update Triggers
An update is triggered when:
- No local version is stored (first run)
- Version string changes
- Database file size differs from manifest
- Hash differs from manifest (if hash is provided)

## Debugging Steps

### 1. Check Logs
Look for these log tags in Android logs:
```
FragmentMainActivity
EnhancedUpdateManagerFlexible
```

### 2. Manual Testing
```bash
# Test manifest accessibility
curl -s "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json?_cb=$(date +%s)"

# Expected response:
{
  "version": "3",
  "description": "Playlist DB for CineCraze",
  "database": {
    "sizeBytes": 12583912,
    "hash": ""
  }
}
```

### 3. Force Update Check
In the app, you can call:
```java
// Force immediate check
forceUpdateCheck();

// Reset version to force update prompt
resetUpdateVersion();
```

### 4. Common Issues

#### Issue: Updates not detected
**Possible Causes:**
- Network connectivity issues
- GitHub rate limiting
- Cache issues
- Version comparison logic

**Solutions:**
1. Check network connectivity
2. Verify manifest.json is accessible
3. Clear app cache
4. Reset update version preferences

#### Issue: False positive updates
**Possible Causes:**
- Manifest.json format changes
- Version string inconsistencies
- Hash calculation errors

**Solutions:**
1. Verify manifest.json format
2. Check version string consistency
3. Review hash calculation logic

## Configuration

### Polling Intervals
```java
// Background polling (1 minute)
MANIFEST_POLL_INTERVAL_MS = 1 * 60 * 1000;

// Active polling (30 seconds)
manifestHandler.postDelayed(manifestPoller, 30 * 1000);
```

### Cache Settings
```java
connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
connection.setRequestProperty("Pragma", "no-cache");
connection.setRequestProperty("Expires", "0");
```

### URLs
- **Manifest**: https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json
- **Database**: https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db

## Testing Checklist

- [ ] Manifest.json is accessible via curl
- [ ] Manifest.json returns valid JSON
- [ ] Version field is present and not empty
- [ ] Database size is accurate
- [ ] App can download database file
- [ ] Update detection triggers correctly
- [ ] Update download completes successfully
- [ ] Local database is updated correctly

## Troubleshooting Commands

```bash
# Test manifest access
curl -I "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json"

# Test database access
curl -I "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db"

# Check GitHub rate limits
curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/rate_limit
```