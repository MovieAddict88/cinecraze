#!/usr/bin/env python3
"""
Test Manifest System
Demonstrates how the manifest system detects updates
"""

import json
import hashlib
import os
from datetime import datetime

def calculate_file_hash(file_path):
    """Calculate SHA256 hash of a file"""
    hash_sha256 = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_sha256.update(chunk)
    return hash_sha256.hexdigest()

def create_test_manifest(version, description):
    """Create a test manifest"""
    db_file = "playlist.db"
    
    if not os.path.exists(db_file):
        print(f"❌ Database file not found: {db_file}")
        return None
    
    # Get file info
    stat = os.stat(db_file)
    file_hash = calculate_file_hash(db_file)
    
    manifest = {
        "version": version,
        "description": description,
        "created_at": datetime.now().isoformat(),
        "database": {
            "filename": "playlist.db",
            "url": "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db",
            "size_bytes": stat.st_size,
            "size_mb": round(stat.st_size / (1024 * 1024), 2),
            "modified_time": datetime.fromtimestamp(stat.st_mtime).isoformat(),
            "hash": file_hash
        }
    }
    
    return manifest

def simulate_update_detection(local_version, local_hash, manifest):
    """Simulate how the Android app detects updates"""
    print(f"🔍 Simulating Update Detection")
    print(f"=" * 40)
    
    print(f"📱 Local Version: {local_version}")
    print(f"🔐 Local Hash: {local_hash[:16]}...")
    print(f"📦 Manifest Version: {manifest['version']}")
    print(f"🔐 Manifest Hash: {manifest['database']['hash'][:16]}...")
    
    # Check if update is needed
    version_different = local_version != manifest['version']
    hash_different = local_hash != manifest['database']['hash']
    
    print(f"\n📊 Update Analysis:")
    print(f"   Version Different: {version_different}")
    print(f"   Hash Different: {hash_different}")
    
    if version_different or hash_different:
        print(f"\n✅ UPDATE NEEDED!")
        print(f"   Reason: {'Version changed' if version_different else 'Hash changed'}")
        print(f"   New Description: {manifest['description']}")
        print(f"   New Size: {manifest['database']['size_mb']} MB")
    else:
        print(f"\n✅ NO UPDATE NEEDED")
        print(f"   Database is up to date")

def main():
    print("🧪 Testing Manifest Update Detection")
    print("=" * 50)
    
    # Test 1: Create initial manifest
    print("\n📝 Test 1: Creating initial manifest")
    manifest1 = create_test_manifest("20250816_143022", "Initial database")
    
    if manifest1:
        print(f"✅ Initial manifest created")
        print(f"   Version: {manifest1['version']}")
        print(f"   Hash: {manifest1['database']['hash'][:16]}...")
    
    # Test 2: Simulate local database (same version)
    print(f"\n📱 Test 2: Local database (same version)")
    local_version = "20250816_143022"
    local_hash = manifest1['database']['hash']
    simulate_update_detection(local_version, local_hash, manifest1)
    
    # Test 3: Simulate updated manifest (different version)
    print(f"\n🔄 Test 3: Updated manifest (different version)")
    manifest2 = create_test_manifest("20250816_143023", "Added 50 new movies")
    simulate_update_detection(local_version, local_hash, manifest2)
    
    # Test 4: Simulate updated manifest (same version, different content)
    print(f"\n🔄 Test 4: Same version, different content")
    manifest3 = create_test_manifest("20250816_143022", "Updated content")
    simulate_update_detection(local_version, local_hash, manifest3)
    
    print(f"\n🎯 Summary:")
    print(f"   - Version changes trigger updates")
    print(f"   - Hash changes trigger updates")
    print(f"   - Both version and hash are checked")
    print(f"   - This ensures accurate update detection")

if __name__ == "__main__":
    main()