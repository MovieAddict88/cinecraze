#!/usr/bin/env python3
"""
Diagnose App Issues
Systematically checks what's happening with the app
"""

import urllib.request
import json
import os
import subprocess
from datetime import datetime

def check_manifest_status():
    """Check current manifest status"""
    print("📦 Checking Current Manifest Status")
    print("=" * 50)
    
    manifest_url = "https://github.com/MovieAddict88/Movie-Source/raw/main/manifest.json"
    try:
        req = urllib.request.Request(
            manifest_url,
            headers={'User-Agent': 'CineCraze-Android-App'}
        )
        
        with urllib.request.urlopen(req, timeout=10) as response:
            manifest_content = response.read()
            manifest = json.loads(manifest_content.decode('utf-8'))
            
        print(f"✅ Manifest accessible")
        print(f"   Version: {manifest['version']}")
        print(f"   Description: {manifest['description']}")
        print(f"   Database Size: {manifest['database']['size_mb']} MB")
        print(f"   Hash: {manifest['database']['hash'][:16]}...")
        
        # Check if manifest has created_at field
        if 'created_at' in manifest:
            print(f"   Created: {manifest['created_at']}")
        else:
            print(f"   ⚠️  No 'created_at' field in manifest")
        
        return manifest
        
    except Exception as e:
        print(f"❌ Error accessing manifest: {str(e)}")
        return None

def check_database_status():
    """Check current database status"""
    print("\n🗄️ Checking Database Status")
    print("=" * 50)
    
    db_url = "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db"
    try:
        req = urllib.request.Request(
            db_url,
            headers={'User-Agent': 'CineCraze-Android-App'}
        )
        
        with urllib.request.urlopen(req, timeout=30) as response:
            # Just check headers, don't download full file
            content_length = response.headers.get('Content-Length')
            last_modified = response.headers.get('Last-Modified')
            
        print(f"✅ Database accessible")
        print(f"   Size: {content_length} bytes ({int(content_length)/1024/1024:.2f} MB)")
        print(f"   Last Modified: {last_modified}")
        
        return True
        
    except Exception as e:
        print(f"❌ Error accessing database: {str(e)}")
        return False

def check_android_files():
    """Check Android project files"""
    print("\n📱 Checking Android Project Files")
    print("=" * 50)
    
    files_to_check = [
        "cinecraze_extracted/CineCraze Tv/app/src/main/AndroidManifest.xml",
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/ui/PlaylistDownloadActivityFirstTime.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/EnhancedUpdateManagerFixed.java"
    ]
    
    for file_path in files_to_check:
        if os.path.exists(file_path):
            print(f"✅ {file_path}")
            # Check file modification time
            mtime = os.path.getmtime(file_path)
            mtime_str = datetime.fromtimestamp(mtime).strftime('%Y-%m-%d %H:%M:%S')
            print(f"   Modified: {mtime_str}")
        else:
            print(f"❌ {file_path} - MISSING")

def check_manifest_content():
    """Check AndroidManifest.xml content"""
    print("\n📋 Checking AndroidManifest.xml Content")
    print("=" * 50)
    
    manifest_path = "cinecraze_extracted/CineCraze Tv/app/src/main/AndroidManifest.xml"
    if os.path.exists(manifest_path):
        with open(manifest_path, 'r') as f:
            content = f.read()
            
        # Check for launcher activity
        if "PlaylistDownloadActivityFirstTime" in content:
            print("✅ Launcher activity is PlaylistDownloadActivityFirstTime")
        else:
            print("❌ Launcher activity is NOT PlaylistDownloadActivityFirstTime")
            
        # Check for other activities
        activities = [
            "PlaylistDownloadActivityFirstTime",
            "PlaylistDownloadActivity",
            "DatabaseInfoActivity",
            "FragmentMainActivity"
        ]
        
        for activity in activities:
            if activity in content:
                print(f"✅ {activity} found in manifest")
            else:
                print(f"❌ {activity} NOT found in manifest")
    else:
        print("❌ AndroidManifest.xml not found")

def check_compilation_issues():
    """Check for potential compilation issues"""
    print("\n🔧 Checking for Compilation Issues")
    print("=" * 50)
    
    # Check if all required files exist
    required_files = [
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/ui/PlaylistDownloadActivityFirstTime.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/EnhancedUpdateManagerFixed.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/res/layout/activity_playlist_download.xml"
    ]
    
    missing_files = []
    for file_path in required_files:
        if not os.path.exists(file_path):
            missing_files.append(file_path)
    
    if missing_files:
        print("❌ Missing files that could cause compilation errors:")
        for file_path in missing_files:
            print(f"   - {file_path}")
    else:
        print("✅ All required files exist")

def check_import_issues():
    """Check for import issues in Java files"""
    print("\n📥 Checking Import Issues")
    print("=" * 50)
    
    java_file = "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/ui/PlaylistDownloadActivityFirstTime.java"
    if os.path.exists(java_file):
        with open(java_file, 'r') as f:
            content = f.read()
            
        # Check for required imports
        required_imports = [
            "import com.cinecraze.free.utils.EnhancedUpdateManagerFixed;",
            "import com.cinecraze.free.FragmentMainActivity;"
        ]
        
        for import_line in required_imports:
            if import_line in content:
                print(f"✅ {import_line}")
            else:
                print(f"❌ {import_line} - MISSING")
    else:
        print("❌ PlaylistDownloadActivityFirstTime.java not found")

def provide_solutions():
    """Provide solutions based on findings"""
    print("\n💡 Solutions")
    print("=" * 50)
    
    print("1. REBUILD THE APP:")
    print("   ./gradlew clean")
    print("   ./gradlew assembleDebug")
    print("   ./gradlew installDebug")
    
    print("\n2. CLEAR APP DATA:")
    print("   adb shell pm clear com.cinecraze.free")
    
    print("\n3. CHECK LOGS:")
    print("   adb logcat | grep -i 'playlist\|download\|update'")
    
    print("\n4. VERIFY INSTALLATION:")
    print("   adb shell pm list packages | grep cinecraze")
    
    print("\n5. FORCE STOP AND RESTART:")
    print("   adb shell am force-stop com.cinecraze.free")
    print("   # Then open app again")

def main():
    print("🔍 CineCraze App Issue Diagnosis")
    print("=" * 60)
    
    check_manifest_status()
    check_database_status()
    check_android_files()
    check_manifest_content()
    check_compilation_issues()
    check_import_issues()
    provide_solutions()
    
    print(f"\n🎯 Next Steps:")
    print(f"   1. Rebuild the app completely")
    print(f"   2. Clear app data")
    print(f"   3. Install fresh APK")
    print(f"   4. Test first-time experience")

if __name__ == "__main__":
    main()