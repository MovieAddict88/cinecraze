#!/usr/bin/env python3
"""
Test script to verify JSON parsing matches the expected structure
"""
import json
import requests

def test_json_parsing():
    """Test if the JSON structure matches what the app expects"""
    
    # Current manifest content
    manifest_json = '''{
  "version": "4",
  "description": "Playlist DB for CineCraze",
  "createdAt": "2025-01-01T00:00:00Z",
  "database": {
    "filename": "playlist.db",
    "url": "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.db",
    "sizeBytes": 12583912,
    "sizeMb": 12,
    "hash": ""
  },
  "metadata": {
    "totalEntries": 2,
    "lastUpdated": "2025-01-01T00:00:00Z"
  },
  "updateInfo": {
    "forceUpdate": false,
    "minAppVersion": "1.0.0",
    "recommendedUpdate": true
  }
}'''
    
    print("Testing JSON parsing...")
    print("JSON content:")
    print(manifest_json)
    print("\n" + "="*50)
    
    try:
        # Parse JSON
        data = json.loads(manifest_json)
        
        print("✅ JSON parsing successful!")
        print(f"Version: {data.get('version', 'N/A')}")
        print(f"Description: {data.get('description', 'N/A')}")
        
        if 'database' in data:
            db = data['database']
            print(f"Database filename: {db.get('filename', 'N/A')}")
            print(f"Database URL: {db.get('url', 'N/A')}")
            print(f"Database size: {db.get('sizeBytes', 'N/A')} bytes")
            print(f"Database size MB: {db.get('sizeMb', 'N/A')}")
            print(f"Database hash: '{db.get('hash', 'N/A')}'")
        
        if 'metadata' in data:
            meta = data['metadata']
            print(f"Total entries: {meta.get('totalEntries', 'N/A')}")
            print(f"Last updated: {meta.get('lastUpdated', 'N/A')}")
        
        if 'updateInfo' in data:
            update = data['updateInfo']
            print(f"Force update: {update.get('forceUpdate', 'N/A')}")
            print(f"Min app version: {update.get('minAppVersion', 'N/A')}")
            print(f"Recommended update: {update.get('recommendedUpdate', 'N/A')}")
        
        # Test the specific fields the app checks
        print("\n" + "="*50)
        print("Testing app-specific field checks:")
        
        version = data.get('version')
        if version:
            print(f"✅ Version field exists: '{version}'")
        else:
            print("❌ Version field missing or empty")
        
        database = data.get('database')
        if database:
            print(f"✅ Database object exists")
            size_bytes = database.get('sizeBytes')
            if size_bytes:
                print(f"✅ Size bytes: {size_bytes}")
            else:
                print("❌ Size bytes missing")
        else:
            print("❌ Database object missing")
        
        # Test version comparison logic
        print("\n" + "="*50)
        print("Testing version comparison logic:")
        
        test_versions = ["", "3", "4", "5"]
        current_version = "4"
        
        for test_version in test_versions:
            if not test_version or test_version != current_version:
                print(f"✅ Update needed: local='{test_version}' vs remote='{current_version}'")
            else:
                print(f"❌ No update needed: local='{test_version}' vs remote='{current_version}'")
        
    except json.JSONDecodeError as e:
        print(f"❌ JSON parsing failed: {e}")
    except Exception as e:
        print(f"❌ Error: {e}")

def test_remote_manifest():
    """Test the actual remote manifest"""
    print("\n" + "="*50)
    print("Testing remote manifest...")
    
    try:
        response = requests.get("https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json", timeout=10)
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Remote manifest accessible")
            print(f"Remote version: {data.get('version', 'N/A')}")
            print(f"Remote description: {data.get('description', 'N/A')}")
        else:
            print(f"❌ Remote manifest HTTP error: {response.status_code}")
    except Exception as e:
        print(f"❌ Remote manifest error: {e}")

if __name__ == "__main__":
    test_json_parsing()
    test_remote_manifest()