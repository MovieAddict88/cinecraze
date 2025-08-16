# CineCraze Playlist Database System

This system converts your GitHub JSON playlist data to a SQLite database that can be downloaded and used locally in the Android app, avoiding the 10,000 entry limit and improving performance.

## 🎯 Overview

The system consists of:
1. **Python Converter**: Downloads JSON from GitHub and converts to SQLite
2. **Android Download Manager**: Downloads and manages the database locally
3. **Database Manager**: Handles local database operations
4. **Download Activity**: User-friendly download interface

## 📁 File Structure

```
├── github_json_to_sqlite_converter.py    # Python converter script
├── playlist.db                           # Generated SQLite database
├── cinecraze_extracted/
│   └── CineCraze Tv/
│       └── app/src/main/java/com/cinecraze/free/
│           ├── utils/
│           │   ├── PlaylistDownloadManager.java      # Download manager
│           │   └── DatabaseEntryConverter.java       # Cursor to Entry converter
│           ├── database/
│           │   └── PlaylistDatabaseManager.java      # Database operations
│           └── ui/
│               └── PlaylistDownloadActivity.java     # Download UI
│       └── app/src/main/res/layout/
│           └── activity_playlist_download.xml        # Download layout
└── CINECRAZE_PLAYLIST_SYSTEM_README.md   # This file
```

## 🚀 Quick Start

### 1. Convert JSON to SQLite Database

```bash
# Run the converter (downloads from your GitHub)
python3 github_json_to_sqlite_converter.py
```

This will:
- Download `playlist.json` from `https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.json`
- Convert it to `playlist.db` with the same schema as your Android app
- Create indexes for better performance
- Add metadata tracking

### 2. Upload Database to GitHub

Upload the generated `playlist.db` file to your GitHub repository:
```
https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db
```

### 3. Update Android App

The Android app will now:
- Check for local database on startup
- Download from GitHub if needed
- Use local database for all operations

## 📊 Database Schema

The SQLite database contains these tables:

### `entries` Table
- `id` - Primary key
- `title` - Movie/TV show title
- `sub_category` - Genre (Action, Comedy, etc.)
- `country` - Country of origin
- `description` - Plot description
- `poster` - Poster image URL
- `thumbnail` - Thumbnail image URL
- `rating` - Rating (number or string)
- `duration` - Runtime
- `year` - Release year
- `main_category` - Main category (Movies, TV Series, Live TV)
- `servers_json` - Streaming servers (JSON)
- `seasons_json` - TV seasons (JSON)
- `related_json` - Related content (JSON)

### `categories` Table
- `id` - Primary key
- `main_category` - Main category name
- `sub_categories` - Sub-categories (JSON array)

### `metadata` Table
- `id` - Primary key
- `last_updated` - Last update timestamp
- `source_url` - Source GitHub URL
- `total_entries` - Total number of entries
- `version` - Database version

## 🔧 Android Integration

### 1. Update AndroidManifest.xml

Add the download activity as the launcher:

```xml
<activity
    android:name=".ui.PlaylistDownloadActivity"
    android:exported="true"
    android:theme="@style/Theme.AppCompat.NoActionBar">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### 2. Add Internet Permission

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. Update Main Activity

Change your main activity to use the downloaded database:

```java
// In your main activity or fragment
PlaylistDatabaseManager dbManager = new PlaylistDatabaseManager(this);
if (dbManager.initializeDatabase()) {
    // Database is ready, load your content
    loadContentFromDatabase();
} else {
    // Handle database error
    showError("Database not available");
}
```

## 📱 User Experience

### First Launch
1. App shows download screen
2. Downloads playlist.db from GitHub
3. Shows progress with percentage
4. Continues to main app when complete

### Subsequent Launches
1. App checks if database exists locally
2. If valid, goes directly to main app
3. If corrupted/missing, shows download screen

### Update Check
- App checks for updates every 24 hours
- Can be configured to check more frequently
- Shows update notification when available

## 🔄 Update Process

### For Content Updates
1. Update your `playlist.json` on GitHub
2. Run the converter: `python3 github_json_to_sqlite_converter.py`
3. Upload the new `playlist.db` to GitHub
4. Users will get the update on next app launch

### For App Updates
1. Update the Android code as needed
2. Build and release new APK
3. Users will download new database if needed

## 📈 Performance Benefits

### Before (JSON)
- ❌ 10,000 entry limit
- ❌ Slow parsing of large JSON
- ❌ Memory issues with large datasets
- ❌ Network dependency for each query

### After (SQLite)
- ✅ No entry limit
- ✅ Fast SQL queries
- ✅ Efficient memory usage
- ✅ Offline access
- ✅ Indexed searches

## 🛠️ Customization

### Change GitHub URLs
Edit `PlaylistDownloadManager.java`:
```java
private static final String GITHUB_DB_URL = "https://github.com/YOUR_USERNAME/YOUR_REPO/raw/main/playlist.db";
```

### Change Update Frequency
Edit `PlaylistDownloadManager.java`:
```java
// Check if more than 24 hours have passed
long diffInHours = (now.getTime() - lastUpdateDate.getTime()) / (1000 * 60 * 60);
return diffInHours >= 24; // Change 24 to desired hours
```

### Add Custom Queries
Use `PlaylistDatabaseManager` methods:
```java
// Search by title
Cursor results = dbManager.searchEntries("Jurassic");

// Get by category
Cursor movies = dbManager.getEntriesByCategory("Movies");

// Get recent entries
Cursor recent = dbManager.getRecentEntries(20);
```

## 🐛 Troubleshooting

### Database Download Fails
- Check internet connection
- Verify GitHub URL is correct
- Check file permissions on device
- Ensure GitHub file is accessible

### Database Corruption
- App will detect corrupted database
- Will automatically re-download
- Check file size (should be > 1MB)

### Performance Issues
- Database is indexed for fast queries
- Use appropriate query methods
- Close cursors after use
- Consider pagination for large result sets

## 📝 Notes

- Database is stored in app's internal storage
- No additional permissions required
- Works offline after download
- Automatic update checking
- Graceful error handling
- User-friendly download interface

## 🔗 GitHub Integration

Your GitHub repository should have:
```
https://github.com/MovieAddict88/Movie-Source/
├── playlist.json    # Your source JSON data
└── playlist.db      # Generated SQLite database
```

The system will automatically download from these URLs and keep them in sync.

---

**Happy Streaming! 🎬📺**