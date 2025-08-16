#!/usr/bin/env python3
"""
Simple Hash Calculator for playlist.db
Shows how to manually calculate SHA256 hash
"""

import hashlib
import os

def calculate_file_hash(file_path):
    """Calculate SHA256 hash of a file"""
    if not os.path.exists(file_path):
        print(f"❌ File not found: {file_path}")
        return None
    
    print(f"📁 Calculating hash for: {file_path}")
    print(f"📊 File size: {os.path.getsize(file_path) / (1024*1024):.2f} MB")
    
    # Calculate SHA256 hash
    hash_sha256 = hashlib.sha256()
    
    with open(file_path, "rb") as f:
        # Read file in chunks to handle large files
        for chunk in iter(lambda: f.read(4096), b""):
            hash_sha256.update(chunk)
    
    # Get the hash as hexadecimal string
    hash_hex = hash_sha256.hexdigest()
    
    print(f"🔐 SHA256 Hash: {hash_hex}")
    print(f"🔢 Hash length: {len(hash_hex)} characters")
    
    return hash_hex

def main():
    print("🔐 CineCraze Database Hash Calculator")
    print("=" * 40)
    
    # Calculate hash for playlist.db
    db_file = "playlist.db"
    hash_result = calculate_file_hash(db_file)
    
    if hash_result:
        print(f"\n✅ Hash calculation completed!")
        print(f"📋 Copy this hash to your manifest:")
        print(f"   \"hash\": \"{hash_result}\"")
        
        # Show first and last 8 characters for easy identification
        print(f"\n🔍 Hash preview:")
        print(f"   Start: {hash_result[:8]}...")
        print(f"   End: ...{hash_result[-8:]}")
        
        # Show how it's used in manifest
        print(f"\n📄 Example manifest entry:")
        print(f"   {{")
        print(f"     \"database\": {{")
        print(f"       \"filename\": \"playlist.db\",")
        print(f"       \"url\": \"https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db\",")
        print(f"       \"hash\": \"{hash_result}\"")
        print(f"     }}")
        print(f"   }}")

if __name__ == "__main__":
    main()