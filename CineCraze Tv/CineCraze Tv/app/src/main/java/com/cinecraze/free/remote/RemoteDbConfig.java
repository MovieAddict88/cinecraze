package com.cinecraze.free.remote;

public class RemoteDbConfig {
    // URL to a small JSON manifest describing the current DB
    // Example manifest structure:
    // {
    //   "version": "2025.08.16",
    //   "dbUrl": "https://github.com/owner/repo/releases/download/v2025.08.16/cinecraze.db.zip",
    //   "sizeBytes": 123456789,
    //   "sha256": "...checksum of unzipped DB...",
    //   "zipped": true
    // }
    public static final String MANIFEST_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/db-manifest.json";

    // The final installed database file name (must match Room DB name)
    public static final String ROOM_DATABASE_NAME = "cinecraze_database";

    // SharedPreferences keys
    public static final String PREFS_NAME = "remote_db_prefs";
    public static final String KEY_INSTALLED_VERSION = "installed_db_version";
    public static final String KEY_INSTALLED_SHA256 = "installed_db_sha256";
}