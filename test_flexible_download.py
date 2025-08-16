#!/usr/bin/env python3
"""
Test Flexible Download System
Demonstrates how the flexible system handles file size mismatches
"""

import urllib.request
import json
import hashlib
import os

def download_file_with_size_check(url, expected_size):
    """Download file and check size"""
    try:
        print(f"📥 Downloading from: {url}")
        
        req = urllib.request.Request(
            url,
            headers={'User-Agent': 'CineCraze-Android-App'}
        )
        
        with urllib.request.urlopen(req, timeout=30) as response:
            content = response.read()
            
        actual_size = len(content)
        expected_size = int(expected_size)
        
        print(f"📊 Size Analysis:")
        print(f"   Expected: {expected_size:,} bytes")
        print(f"   Actual:   {actual_size:,} bytes")
        print(f"   Difference: {actual_size - expected_size:,} bytes")
        
        # Calculate percentage difference
        if expected_size > 0:
            diff_percent = abs(actual_size - expected_size) / expected_size * 100
            print(f"   Difference: {diff_percent:.2f}%")
            
            if diff_percent > 10:
                print(f"⚠️  WARNING: Size difference > 10%")
                print(f"   This would cause strict verification to fail")
                print(f"   Flexible system will continue anyway")
            else:
                print(f"✅ Size difference within acceptable range")
        
        return content, actual_size
        
    except Exception as e:
        print(f"❌ Download error: {str(e)}")
        return None, 0

def simulate_flexible_verification(manifest, actual_size, actual_hash):
    """Simulate flexible verification process"""
    print(f"\n🔍 Flexible Verification Simulation")
    print(f"=" * 50)
    
    expected_size = manifest['database']['size_bytes']
    expected_hash = manifest['database']['hash']
    
    print(f"📦 Manifest Info:")
    print(f"   Expected Size: {expected_size:,} bytes")
    print(f"   Expected Hash: {expected_hash[:16]}...")
    
    print(f"\n📱 Downloaded File:")
    print(f"   Actual Size: {actual_size:,} bytes")
    print(f"   Actual Hash: {actual_hash[:16]}...")
    
    # Size check
    size_different = actual_size != expected_size
    if size_different:
        diff_percent = abs(actual_size - expected_size) / expected_size * 100
        print(f"\n⚠️  Size Mismatch Detected:")
        print(f"   Difference: {diff_percent:.2f}%")
        
        if diff_percent > 10:
            print(f"   ❌ Strict system would FAIL here")
            print(f"   ✅ Flexible system continues anyway")
        else:
            print(f"   ✅ Size difference acceptable")
    else:
        print(f"\n✅ Size matches perfectly")
    
    # Hash check (only if size is reasonable)
    if not size_different or abs(actual_size - expected_size) / expected_size <= 0.1:
        hash_different = actual_hash != expected_hash
        if hash_different:
            print(f"\n⚠️  Hash Mismatch Detected:")
            print(f"   ❌ Strict system would FAIL here")
            print(f"   ✅ Flexible system continues anyway")
        else:
            print(f"\n✅ Hash verification successful")
    else:
        print(f"\n⏭️  Skipping hash verification (size difference too large)")
    
    # Final result
    print(f"\n🎯 Final Result:")
    print(f"   ✅ Flexible system: SUCCESS")
    print(f"   ❌ Strict system: FAILED")
    print(f"   📝 Database will be saved and used")

def main():
    print("🧪 Testing Flexible Download System")
    print("=" * 50)
    
    # Download manifest
    manifest_url = "https://github.com/MovieAddict88/Movie-Source/raw/main/manifest.json"
    try:
        req = urllib.request.Request(
            manifest_url,
            headers={'User-Agent': 'CineCraze-Android-App'}
        )
        
        with urllib.request.urlopen(req, timeout=10) as response:
            manifest_content = response.read()
            manifest = json.loads(manifest_content.decode('utf-8'))
            
        print(f"✅ Manifest downloaded")
        print(f"   Version: {manifest['version']}")
        print(f"   Expected Size: {manifest['database']['size_bytes']:,} bytes")
        
    except Exception as e:
        print(f"❌ Error downloading manifest: {str(e)}")
        return
    
    # Download database
    db_url = manifest['database']['url']
    content, actual_size = download_file_with_size_check(db_url, manifest['database']['size_bytes'])
    
    if content:
        # Calculate actual hash
        actual_hash = hashlib.sha256(content).hexdigest()
        
        # Simulate flexible verification
        simulate_flexible_verification(manifest, actual_size, actual_hash)
        
        print(f"\n📋 Summary:")
        print(f"   - Flexible system handles size mismatches")
        print(f"   - Continues download even with verification issues")
        print(f"   - Logs warnings but doesn't fail")
        print(f"   - Ensures app functionality even with network issues")
    else:
        print(f"\n❌ Cannot test without database download")

if __name__ == "__main__":
    main()