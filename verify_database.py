#!/usr/bin/env python3
"""
Database Verification Script
Verifies the playlist.db structure and tests queries
"""

import sqlite3
import json
import os

def verify_database():
    """Verify the database structure and test queries"""
    db_file = "playlist.db"
    
    if not os.path.exists(db_file):
        print(f"❌ Database file not found: {db_file}")
        return
    
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    print("🔍 Database Verification Report")
    print("=" * 50)
    
    # Check database size
    db_size = os.path.getsize(db_file)
    db_size_mb = db_size / (1024 * 1024)
    print(f"Database size: {db_size_mb:.2f} MB")
    
    # Get table schema
    print("\n📋 Database Schema:")
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = cursor.fetchall()
    
    for table in tables:
        table_name = table[0]
        print(f"\nTable: {table_name}")
        cursor.execute(f"PRAGMA table_info({table_name})")
        columns = cursor.fetchall()
        for col in columns:
            print(f"  - {col[1]} ({col[2]})")
    
    # Test queries
    print("\n📊 Data Statistics:")
    
    # Total entries
    cursor.execute("SELECT COUNT(*) FROM entries")
    total_entries = cursor.fetchone()[0]
    print(f"Total entries: {total_entries}")
    
    # Entries by category
    cursor.execute("""
        SELECT main_category, COUNT(*) as count 
        FROM entries 
        GROUP BY main_category 
        ORDER BY count DESC
    """)
    category_stats = cursor.fetchall()
    print("\nEntries by category:")
    for category, count in category_stats:
        print(f"  {category}: {count} entries")
    
    # Sample entries
    print("\n🎬 Sample Entries:")
    cursor.execute("""
        SELECT title, main_category, sub_category, year, rating 
        FROM entries 
        LIMIT 5
    """)
    samples = cursor.fetchall()
    for sample in samples:
        title, main_cat, sub_cat, year, rating = sample
        print(f"  - {title} ({year}) - {main_cat}/{sub_cat} - Rating: {rating}")
    
    # Check for series with seasons
    print("\n📺 Series with Seasons:")
    cursor.execute("""
        SELECT title, seasons_json 
        FROM entries 
        WHERE seasons_json != '' AND seasons_json != '[]'
        LIMIT 3
    """)
    series_with_seasons = cursor.fetchall()
    for series in series_with_seasons:
        title, seasons_json = series
        seasons = json.loads(seasons_json)
        print(f"  - {title}: {len(seasons)} seasons")
    
    # Check for entries with servers
    print("\n🔗 Entries with Servers:")
    cursor.execute("""
        SELECT COUNT(*) 
        FROM entries 
        WHERE servers_json != '' AND servers_json != '[]'
    """)
    entries_with_servers = cursor.fetchone()[0]
    print(f"  {entries_with_servers} entries have streaming servers")
    
    # Test search functionality
    print("\n🔍 Search Test:")
    cursor.execute("""
        SELECT title, main_category, year 
        FROM entries 
        WHERE title LIKE '%Jurassic%' OR title LIKE '%War%'
        LIMIT 3
    """)
    search_results = cursor.fetchall()
    for result in search_results:
        title, category, year = result
        print(f"  - {title} ({year}) - {category}")
    
    conn.close()
    print("\n✅ Database verification completed!")

if __name__ == "__main__":
    verify_database()