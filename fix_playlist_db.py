#!/usr/bin/env python3
"""
Fix playlist.db structure for CineCraze TV app
This script converts individual episode entries into proper TV series structure
"""

import sqlite3
import json
import os
from collections import defaultdict

def fix_playlist_db(input_db_path, output_db_path):
    """Fix the playlist.db structure for proper TV series handling"""
    
    print(f"Fixing playlist database: {input_db_path} -> {output_db_path}")
    
    # Connect to source database
    conn = sqlite3.connect(input_db_path)
    cursor = conn.cursor()
    
    # Get all TV Series entries
    cursor.execute("SELECT * FROM entries WHERE main_category = 'TV Series'")
    tv_series_entries = cursor.fetchall()
    
    print(f"Found {len(tv_series_entries)} TV series entries")
    
    # Group episodes by series title
    series_groups = defaultdict(list)
    
    for entry in tv_series_entries:
        # Parse the entry data
        entry_id, title, sub_category, country, description, poster, thumbnail, rating, duration, year, main_category, servers_json, seasons_json, related_json = entry
        
        # Extract series title from episode title (e.g., "Wednesday S01E01" -> "Wednesday")
        series_title = extract_series_title(title)
        
        # Parse seasons_json to get episode info
        try:
            episode_data = json.loads(seasons_json) if seasons_json else {}
            season_num = episode_data.get('season', 1)
            episode_num = episode_data.get('episode', 1)
            
            series_groups[series_title].append({
                'entry_id': entry_id,
                'title': title,
                'sub_category': sub_category,
                'country': country,
                'description': description,
                'poster': poster,
                'thumbnail': thumbnail,
                'rating': rating,
                'duration': duration,
                'year': year,
                'servers_json': servers_json,
                'season': season_num,
                'episode': episode_num,
                'episode_title': episode_data.get('title', title),
                'episode_description': episode_data.get('description', description),
                'episode_thumbnail': episode_data.get('thumbnail', thumbnail),
                'episode_duration': episode_data.get('duration', duration),
                'episode_servers': episode_data.get('servers', [])
            })
        except Exception as e:
            print(f"Error parsing entry {title}: {e}")
            continue
    
    print(f"Grouped into {len(series_groups)} series")
    
    # Create new database
    new_conn = sqlite3.connect(output_db_path)
    new_cursor = new_conn.cursor()
    
    # Create tables
    create_tables(new_cursor)
    
    # Copy non-TV series entries
    cursor.execute("SELECT * FROM entries WHERE main_category != 'TV Series'")
    other_entries = cursor.fetchall()
    
    for entry in other_entries:
        new_cursor.execute("""
            INSERT INTO entries (title, sub_category, country, description, poster, 
                               thumbnail, rating, duration, year, main_category, 
                               servers_json, seasons_json, related_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, entry[1:])  # Skip the id column
    
    print(f"Copied {len(other_entries)} non-TV series entries")
    
    # Create proper TV series entries
    for series_title, episodes in series_groups.items():
        if not episodes:
            continue
            
        # Sort episodes by season and episode number
        episodes.sort(key=lambda x: (x['season'], x['episode']))
        
        # Use the first episode's metadata for the series
        first_episode = episodes[0]
        
        # Create seasons structure
        seasons_structure = create_seasons_structure(episodes)
        
        # Insert series entry
        new_cursor.execute("""
            INSERT INTO entries (title, sub_category, country, description, poster, 
                               thumbnail, rating, duration, year, main_category, 
                               servers_json, seasons_json, related_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            series_title,  # Use series title instead of episode title
            first_episode['sub_category'],
            first_episode['country'],
            f"TV Series: {series_title}",  # Generic description
            first_episode['poster'],
            first_episode['thumbnail'],
            first_episode['rating'],
            "",  # Duration for series
            first_episode['year'],
            "TV Series",
            "[]",  # No servers at series level
            json.dumps(seasons_structure),
            "[]"  # No related entries
        ))
    
    print(f"Created {len(series_groups)} TV series entries")
    
    # Copy categories and metadata
    cursor.execute("SELECT * FROM categories")
    categories = cursor.fetchall()
    for category in categories:
        new_cursor.execute("INSERT INTO categories (main_category, sub_categories) VALUES (?, ?)", 
                          category[1:])
    
    cursor.execute("SELECT * FROM metadata")
    metadata = cursor.fetchall()
    for meta in metadata:
        new_cursor.execute("INSERT INTO metadata (last_updated, source_url, total_entries, version) VALUES (?, ?, ?, ?)", 
                          meta[1:])
    
    # Update metadata with new total count
    new_cursor.execute("SELECT COUNT(*) FROM entries")
    new_total = new_cursor.fetchone()[0]
    new_cursor.execute("UPDATE metadata SET total_entries = ? WHERE id = (SELECT MAX(id) FROM metadata)", (new_total,))
    
    # Commit and close
    new_conn.commit()
    new_conn.close()
    conn.close()
    
    print(f"Fixed database saved to: {output_db_path}")
    print(f"Total entries: {new_total}")

def extract_series_title(title):
    """Extract series title from episode title"""
    # Remove season/episode info (e.g., "Wednesday S01E01" -> "Wednesday")
    if " S" in title:
        return title.split(" S")[0].strip()
    return title

def create_seasons_structure(episodes):
    """Create proper seasons structure for TV series"""
    seasons = defaultdict(list)
    
    for episode in episodes:
        season_num = episode['season']
        episode_num = episode['episode']
        
        # Create episode object
        episode_obj = {
            "Episode": episode_num,
            "Title": episode['episode_title'],
            "Duration": episode['episode_duration'],
            "Description": episode['episode_description'],
            "Thumbnail": episode['episode_thumbnail'],
            "Servers": episode['episode_servers']
        }
        
        seasons[season_num].append(episode_obj)
    
    # Convert to list of season objects
    seasons_list = []
    for season_num in sorted(seasons.keys()):
        season_obj = {
            "Season": season_num,
            "SeasonPoster": episodes[0]['poster'],  # Use series poster
            "Episodes": sorted(seasons[season_num], key=lambda x: x['Episode'])
        }
        seasons_list.append(season_obj)
    
    return seasons_list

def create_tables(cursor):
    """Create database tables"""
    cursor.execute("""
        CREATE TABLE entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
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
    """)
    
    cursor.execute("""
        CREATE TABLE categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            main_category TEXT,
            sub_categories TEXT
        )
    """)
    
    cursor.execute("""
        CREATE TABLE metadata (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            last_updated TEXT,
            source_url TEXT,
            total_entries INTEGER,
            version TEXT
        )
    """)

if __name__ == "__main__":
    input_db = "playlist.db"
    output_db = "playlist_fixed.db"
    
    if not os.path.exists(input_db):
        print(f"Error: {input_db} not found")
        exit(1)
    
    fix_playlist_db(input_db, output_db)
    
    # Verify the fix
    print("\nVerifying fixed database...")
    conn = sqlite3.connect(output_db)
    cursor = conn.cursor()
    
    cursor.execute("SELECT main_category, COUNT(*) FROM entries GROUP BY main_category")
    categories = cursor.fetchall()
    print("Categories and counts:")
    for cat, count in categories:
        print(f"  {cat}: {count}")
    
    cursor.execute("SELECT title, seasons_json FROM entries WHERE main_category = 'TV Series' LIMIT 3")
    tv_series = cursor.fetchall()
    print("\nTV Series sample:")
    for title, seasons_json in tv_series:
        print(f"  {title}: {len(seasons_json)} characters of seasons data")
        if seasons_json:
            try:
                parsed = json.loads(seasons_json)
                print(f"    Seasons: {len(parsed)}")
                for season in parsed:
                    print(f"      Season {season.get('Season')}: {len(season.get('Episodes', []))} episodes")
            except:
                print(f"    Invalid JSON")
    
    conn.close()