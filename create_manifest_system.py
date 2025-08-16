#!/usr/bin/env python3
"""
Manifest System for CineCraze Database Updates
Creates a manifest.json file to track database versions and enable smart updates
"""

import json
import hashlib
import os
from datetime import datetime
from typing import Dict, Any

class DatabaseManifestCreator:
    def __init__(self, db_file_path: str, manifest_file_path: str = "manifest.json"):
        self.db_file_path = db_file_path
        self.manifest_file_path = manifest_file_path
        
    def calculate_file_hash(self, file_path: str) -> str:
        """Calculate SHA256 hash of the database file"""
        hash_sha256 = hashlib.sha256()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_sha256.update(chunk)
        return hash_sha256.hexdigest()
    
    def get_file_info(self, file_path: str) -> Dict[str, Any]:
        """Get file information including size, modification time, and hash"""
        if not os.path.exists(file_path):
            return None
            
        stat = os.stat(file_path)
        return {
            "size": stat.st_size,
            "size_mb": round(stat.st_size / (1024 * 1024), 2),
            "modified_time": datetime.fromtimestamp(stat.st_mtime).isoformat(),
            "hash": self.calculate_file_hash(file_path)
        }
    
    def create_manifest(self, version: str = None, description: str = None) -> Dict[str, Any]:
        """Create a manifest file with database information"""
        
        if not os.path.exists(self.db_file_path):
            print(f"❌ Database file not found: {self.db_file_path}")
            return None
        
        # Get database info
        db_info = self.get_file_info(self.db_file_path)
        if not db_info:
            return None
        
        # Create manifest
        manifest = {
            "version": version or datetime.now().strftime("%Y%m%d_%H%M%S"),
            "description": description or f"Database update - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
            "created_at": datetime.now().isoformat(),
            "database": {
                "filename": "playlist.db",
                "url": "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.db",
                "size_bytes": db_info["size"],
                "size_mb": db_info["size_mb"],
                "modified_time": db_info["modified_time"],
                "hash": db_info["hash"]
            },
            "metadata": {
                "total_entries": self.get_total_entries(),
                "categories": self.get_categories_info(),
                "last_updated": db_info["modified_time"]
            },
            "update_info": {
                "force_update": False,
                "min_app_version": "1.0.0",
                "recommended_update": True
            }
        }
        
        return manifest
    
    def get_total_entries(self) -> int:
        """Get total number of entries from database"""
        try:
            import sqlite3
            conn = sqlite3.connect(self.db_file_path)
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM entries")
            count = cursor.fetchone()[0]
            conn.close()
            return count
        except Exception as e:
            print(f"Warning: Could not get entry count: {e}")
            return 0
    
    def get_categories_info(self) -> Dict[str, int]:
        """Get categories and their entry counts"""
        try:
            import sqlite3
            conn = sqlite3.connect(self.db_file_path)
            cursor = conn.cursor()
            cursor.execute("SELECT main_category, COUNT(*) FROM entries GROUP BY main_category")
            categories = dict(cursor.fetchall())
            conn.close()
            return categories
        except Exception as e:
            print(f"Warning: Could not get categories info: {e}")
            return {}
    
    def save_manifest(self, manifest: Dict[str, Any]) -> bool:
        """Save manifest to file"""
        try:
            with open(self.manifest_file_path, 'w', encoding='utf-8') as f:
                json.dump(manifest, f, indent=2, ensure_ascii=False)
            
            print(f"✅ Manifest saved: {self.manifest_file_path}")
            return True
        except Exception as e:
            print(f"❌ Error saving manifest: {e}")
            return False
    
    def create_and_save_manifest(self, version: str = None, description: str = None) -> bool:
        """Create and save manifest file"""
        manifest = self.create_manifest(version, description)
        if manifest:
            return self.save_manifest(manifest)
        return False

def main():
    print("🔧 CineCraze Database Manifest Creator")
    print("=" * 40)
    
    # Configuration
    db_file = "playlist.db"
    manifest_file = "manifest.json"
    
    # Check if database exists
    if not os.path.exists(db_file):
        print(f"❌ Database file not found: {db_file}")
        print("Please run the converter first: python3 github_json_to_sqlite_converter.py")
        return
    
    # Get version and description
    version = input("📝 Version (press Enter for auto-generated): ").strip()
    if not version:
        version = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    description = input("📄 Description (press Enter for auto-generated): ").strip()
    if not description:
        description = f"Database update - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
    
    # Create manifest
    creator = DatabaseManifestCreator(db_file, manifest_file)
    success = creator.create_and_save_manifest(version, description)
    
    if success:
        print(f"\n✅ Manifest created successfully!")
        print(f"📁 File: {manifest_file}")
        print(f"🔢 Version: {version}")
        print(f"📄 Description: {description}")
        
        # Show manifest preview
        with open(manifest_file, 'r') as f:
            manifest = json.load(f)
        
        print(f"\n📊 Manifest Preview:")
        print(f"  Database Size: {manifest['database']['size_mb']} MB")
        print(f"  Total Entries: {manifest['metadata']['total_entries']}")
        print(f"  Hash: {manifest['database']['hash'][:16]}...")
        print(f"  Categories: {list(manifest['metadata']['categories'].keys())}")
        
        print(f"\n🚀 Upload both files to GitHub:")
        print(f"  - {db_file}")
        print(f"  - {manifest_file}")
        
        print(f"\n🔗 Manifest URL will be:")
        print(f"  https://github.com/MovieAddict88/Movie-Source/raw/main/manifest.json")
    else:
        print("❌ Failed to create manifest")

if __name__ == "__main__":
    main()