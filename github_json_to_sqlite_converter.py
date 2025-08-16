#!/usr/bin/env python3
"""
GitHub JSON to SQLite Converter for CineCraze
Downloads playlist.json from GitHub and converts to playlist.db
"""

import json
import sqlite3
import os
import urllib.request
import urllib.error
from typing import Dict, List, Any, Optional
from urllib.parse import urlparse

class GitHubJsonToSqliteConverter:
    def __init__(self, github_url: str, db_file_path: str):
        self.github_url = github_url
        self.db_file_path = db_file_path
        self.conn = None
        self.cursor = None
        
    def download_json_from_github(self) -> Dict[str, Any]:
        """Download JSON data from GitHub"""
        print(f"Downloading JSON from: {self.github_url}")
        
        try:
            # Create a request with headers to mimic a browser
            req = urllib.request.Request(
                self.github_url,
                headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                }
            )
            
            with urllib.request.urlopen(req, timeout=30) as response:
                content = response.read()
                data = json.loads(content.decode('utf-8'))
                
            print(f"✅ Successfully downloaded JSON data")
            print(f"📊 Data size: {len(content) / (1024*1024):.2f} MB")
            
            return data
            
        except urllib.error.URLError as e:
            print(f"❌ Error downloading from GitHub: {str(e)}")
            raise
        except json.JSONDecodeError as e:
            print(f"❌ Error parsing JSON: {str(e)}")
            raise
        except Exception as e:
            print(f"❌ Unexpected error: {str(e)}")
            raise
    
    def create_database(self):
        """Create the SQLite database with the same schema as the Android app"""
        self.conn = sqlite3.connect(self.db_file_path)
        self.cursor = self.conn.cursor()
        
        # Create entries table matching EntryEntity schema
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                sub_category TEXT,
                country TEXT,
                description TEXT,
                poster TEXT,
                thumbnail TEXT,
                rating TEXT,
                duration TEXT,
                year TEXT,
                main_category TEXT,
                servers_json TEXT,
                seasons_json TEXT,
                related_json TEXT
            )
        ''')
        
        # Create categories table for better organization
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                main_category TEXT NOT NULL,
                sub_categories TEXT
            )
        ''')
        
        # Create metadata table for tracking updates
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                last_updated TEXT,
                source_url TEXT,
                total_entries INTEGER,
                version TEXT
            )
        ''')
        
        # Create indexes for better performance
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_title ON entries(title)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_main_category ON entries(main_category)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_sub_category ON entries(sub_category)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_year ON entries(year)')
        
        self.conn.commit()
        print(f"Database created: {self.db_file_path}")
    
    def convert_object_to_string(self, obj: Any) -> str:
        """Convert any object to string, handling None values"""
        if obj is None:
            return ""
        return str(obj)
    
    def clear_existing_data(self):
        """Clear existing data before inserting new data"""
        print("Clearing existing data...")
        self.cursor.execute('DELETE FROM entries')
        self.cursor.execute('DELETE FROM categories')
        self.cursor.execute('DELETE FROM metadata')
        self.conn.commit()
        print("Existing data cleared")
    
    def insert_categories(self, data: Dict[str, Any]):
        """Insert categories into the database"""
        print("Inserting categories...")
        
        for category in data.get('Categories', []):
            main_category = category.get('MainCategory', '')
            sub_categories = category.get('SubCategories', [])
            
            # Convert sub_categories list to JSON string
            sub_categories_json = json.dumps(sub_categories) if sub_categories else ""
            
            self.cursor.execute('''
                INSERT INTO categories (main_category, sub_categories)
                VALUES (?, ?)
            ''', (main_category, sub_categories_json))
        
        self.conn.commit()
        print(f"Inserted {len(data.get('Categories', []))} categories")
    
    def insert_entries(self, data: Dict[str, Any]):
        """Insert all entries into the database"""
        print("Inserting entries...")
        
        total_entries = 0
        
        for category in data.get('Categories', []):
            main_category = category.get('MainCategory', '')
            entries = category.get('Entries', [])
            
            print(f"Processing {len(entries)} entries for category: {main_category}")
            
            for entry in entries:
                # Convert complex objects to JSON strings
                servers_json = json.dumps(entry.get('Servers', [])) if entry.get('Servers') else ""
                seasons_json = json.dumps(entry.get('Seasons', [])) if entry.get('Seasons') else ""
                related_json = json.dumps(entry.get('Related', [])) if entry.get('Related') else ""
                
                # Insert entry
                self.cursor.execute('''
                    INSERT INTO entries (
                        title, sub_category, country, description, poster, thumbnail,
                        rating, duration, year, main_category, servers_json, seasons_json, related_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    entry.get('Title', ''),
                    entry.get('SubCategory', ''),
                    entry.get('Country', ''),
                    entry.get('Description', ''),
                    entry.get('Poster', ''),
                    entry.get('Thumbnail', ''),
                    self.convert_object_to_string(entry.get('Rating')),
                    entry.get('Duration', ''),
                    self.convert_object_to_string(entry.get('Year')),
                    main_category,
                    servers_json,
                    seasons_json,
                    related_json
                ))
                
                total_entries += 1
                
                # Commit every 100 entries to avoid memory issues
                if total_entries % 100 == 0:
                    self.conn.commit()
                    print(f"Processed {total_entries} entries...")
        
        self.conn.commit()
        print(f"Total entries inserted: {total_entries}")
        return total_entries
    
    def insert_metadata(self, total_entries: int):
        """Insert metadata about the conversion"""
        from datetime import datetime
        
        self.cursor.execute('''
            INSERT INTO metadata (last_updated, source_url, total_entries, version)
            VALUES (?, ?, ?, ?)
        ''', (
            datetime.now().isoformat(),
            self.github_url,
            total_entries,
            "1.0"
        ))
        self.conn.commit()
    
    def create_statistics(self):
        """Create a statistics table for quick queries"""
        print("Creating statistics...")
        
        # Get counts by category
        self.cursor.execute('''
            SELECT main_category, COUNT(*) as count 
            FROM entries 
            GROUP BY main_category
        ''')
        
        category_stats = self.cursor.fetchall()
        print("\nEntries by category:")
        for category, count in category_stats:
            print(f"  {category}: {count} entries")
        
        # Get total count
        self.cursor.execute('SELECT COUNT(*) FROM entries')
        total_count = self.cursor.fetchone()[0]
        print(f"\nTotal entries in database: {total_count}")
        
        # Get database size
        db_size = os.path.getsize(self.db_file_path)
        db_size_mb = db_size / (1024 * 1024)
        print(f"Database file size: {db_size_mb:.2f} MB")
        
        # Get metadata
        self.cursor.execute('SELECT * FROM metadata ORDER BY id DESC LIMIT 1')
        metadata = self.cursor.fetchone()
        if metadata:
            print(f"Last updated: {metadata[1]}")
            print(f"Source: {metadata[2]}")
    
    def convert(self):
        """Main conversion method"""
        try:
            print("Starting GitHub JSON to SQLite conversion...")
            
            # Create database
            self.create_database()
            
            # Download JSON data from GitHub
            data = self.download_json_from_github()
            
            # Clear existing data
            self.clear_existing_data()
            
            # Insert categories
            self.insert_categories(data)
            
            # Insert entries
            total_entries = self.insert_entries(data)
            
            # Insert metadata
            self.insert_metadata(total_entries)
            
            # Create statistics
            self.create_statistics()
            
            print("\n✅ Conversion completed successfully!")
            print(f"Database saved as: {self.db_file_path}")
            
        except Exception as e:
            print(f"❌ Error during conversion: {str(e)}")
            raise
        finally:
            if self.conn:
                self.conn.close()

def main():
    # GitHub URL and output file
    github_url = "https://github.com/MovieAddict88/Movie-Source/raw/main/playlist.json"
    db_file = "playlist.db"
    
    # Create converter and run conversion
    converter = GitHubJsonToSqliteConverter(github_url, db_file)
    converter.convert()

if __name__ == "__main__":
    main()