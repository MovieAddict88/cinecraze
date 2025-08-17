# CineCraze TV - TV Series Display Fix

## Quick Fix for Users

If you're experiencing issues with TV series not showing properly in the CineCraze TV app, follow these steps:

### 1. Download the Fix Scripts
Download these files to your computer:
- `apply_tv_series_fix.py` - Main fix application script
- `fix_playlist_db.py` - Database conversion script
- `test_api_manager_fix.py` - Verification script

### 2. Place Scripts with Your Database
Put the scripts in the same folder as your `playlist.db` file.

### 3. Run the Fix
```bash
python3 apply_tv_series_fix.py
```

The script will:
- ✅ Check if your database needs fixing
- ✅ Create a backup of your original database
- ✅ Apply the TV series fix
- ✅ Verify the fix worked correctly
- ✅ Offer to replace your original database

### 4. Restart Your App
After the fix is applied, restart your CineCraze TV app and check the TV Series section.

## What This Fix Does

### Problem
- TV series episodes were stored as individual entries in the database
- Each episode appeared separately instead of being grouped by series
- The app couldn't properly display series with seasons and episodes

### Solution
- Groups individual episode entries into proper series entries
- Creates nested seasons and episodes structure
- Maintains all episode data (servers, descriptions, thumbnails)
- Preserves non-TV series content (movies, live TV)

### Before vs After

**Before Fix:**
```
Database: 40 individual episode entries
Display: Episodes scattered individually
Navigation: Difficult to find related episodes
```

**After Fix:**
```
Database: 2 series entries with nested episodes
Display: Properly grouped series with seasons
Navigation: Easy to browse seasons and episodes
```

## For Developers

### API Manager Fix
If you're using the API Manager to generate `playlist.db` files, update your `SQLiteExporter.java` with the fixed version provided in this repository.

### Database Structure
The fix converts from this structure:
```sql
-- BROKEN: Individual episodes
INSERT INTO entries (title, main_category, seasons_json) VALUES
('Wednesday S01E01', 'TV Series', '{"season": 1, "episode": 1, ...}'),
('Wednesday S01E02', 'TV Series', '{"season": 1, "episode": 2, ...}'),
```

To this structure:
```sql
-- FIXED: Grouped series
INSERT INTO entries (title, main_category, seasons_json) VALUES
('Wednesday', 'TV Series', '[
  {
    "Season": 1,
    "SeasonPoster": "...",
    "Episodes": [
      {
        "Episode": 1,
        "Title": "Wednesday S01E01",
        "Duration": "...",
        "Description": "...",
        "Thumbnail": "...",
        "Servers": [...]
      }
    ]
  }
]')
```

## Testing Your Fix

Run the verification script to check if the fix worked:
```bash
python3 test_api_manager_fix.py
```

You should see output like:
```
Fixed Database:
- 2 series entries (Wednesday, The Sandman)
- Proper seasons structure
- All episodes properly nested
```

## Troubleshooting

### Script Not Found
Make sure all script files are in the same directory as your `playlist.db` file.

### Permission Errors
On some systems, you may need to make the scripts executable:
```bash
chmod +x apply_tv_series_fix.py
```

### Python Not Found
Install Python 3 if not already installed:
- **Windows**: Download from python.org
- **Mac**: `brew install python3`
- **Linux**: `sudo apt-get install python3`

### Database Already Fixed
If the script says "Database already fixed", your database is already in the correct format.

### Backup Files
The script creates backup files with timestamps. You can safely delete old backup files if the fix works correctly.

## Files Included

- `apply_tv_series_fix.py` - User-friendly fix application script
- `fix_playlist_db.py` - Core database conversion logic
- `test_api_manager_fix.py` - Verification and testing script
- `TV_SERIES_FIX_SUMMARY.md` - Detailed technical documentation
- `README_TV_SERIES_FIX.md` - This user guide

## Support

If you encounter any issues:
1. Check that all script files are present
2. Ensure your `playlist.db` file is not corrupted
3. Try running the verification script to diagnose issues
4. Check the detailed technical documentation in `TV_SERIES_FIX_SUMMARY.md`

## Results

After applying this fix, your CineCraze TV app should:
- ✅ Display TV series properly grouped by series
- ✅ Show seasons and episodes in organized structure
- ✅ Allow proper navigation through series content
- ✅ Maintain all episode data and server information
- ✅ Keep movies and live TV working as before

The fix is safe, creates backups, and preserves all your content data.