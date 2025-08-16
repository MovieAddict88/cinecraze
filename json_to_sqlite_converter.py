#!/usr/bin/env python3
"""
CineCraze JSON to SQLite Converter
Converts playlist.json to playlist.db for Android app usage
"""

import json
import sqlite3
import os
from typing import Dict, List, Any, Optional

class CineCrazeJsonToSqliteConverter:
    def __init__(self, json_file_path: str, db_file_path: str):
        self.json_file_path = json_file_path
        self.db_file_path = db_file_path
        self.conn = None
        self.cursor = None
        
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
        
        # Create indexes for better performance
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_title ON entries(title)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_main_category ON entries(main_category)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_sub_category ON entries(sub_category)')
        self.cursor.execute('CREATE INDEX IF NOT EXISTS idx_year ON entries(year)')
        
        self.conn.commit()
        print(f"Database created: {self.db_file_path}")
    
    def load_json_data(self) -> Dict[str, Any]:
        """Load and parse the JSON file"""
        print(f"Loading JSON data from: {self.json_file_path}")
        with open(self.json_file_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
        print(f"JSON data loaded successfully")
        return data
    
    def convert_object_to_string(self, obj: Any) -> str:
        """Convert any object to string, handling None values"""
        if obj is None:
            return ""
        return str(obj)
    
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
    
    def convert(self):
        """Main conversion method"""
        try:
            print("Starting CineCraze JSON to SQLite conversion...")
            
            # Create database
            self.create_database()
            
            # Load JSON data
            data = self.load_json_data()
            
            # Insert categories
            self.insert_categories(data)
            
            # Insert entries
            self.insert_entries(data)
            
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
    # File paths
    json_file = "cinecraze_extracted/CineCraze Tv/playlist.json"
    db_file = "playlist.db"
    
    # Check if JSON file exists
    if not os.path.exists(json_file):
        print(f"❌ JSON file not found: {json_file}")
        return
    
    # Create converter and run conversion
    converter = CineCrazeJsonToSqliteConverter(json_file, db_file)
    converter.convert()

if __name__ == "__main__":
    main()