#!/usr/bin/env python3
"""
Test script to verify the fixed API Manager generates correct database structure
"""

import sqlite3
import json
import os

def test_database_structure(db_path):
    """Test the database structure to ensure TV series are properly grouped"""
    
    print(f"Testing database: {db_path}")
    
    if not os.path.exists(db_path):
        print(f"Error: Database file {db_path} not found")
        return False
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        # Check overall structure
        cursor.execute("SELECT main_category, COUNT(*) FROM entries GROUP BY main_category")
        categories = cursor.fetchall()
        print("\nCategories and counts:")
        for cat, count in categories:
            print(f"  {cat}: {count}")
        
        # Check TV Series structure
        cursor.execute("SELECT title, seasons_json FROM entries WHERE main_category = 'TV Series'")
        tv_series = cursor.fetchall()
        
        print(f"\nTV Series entries: {len(tv_series)}")
        
        for title, seasons_json in tv_series:
            print(f"\n{title}:")
            if seasons_json:
                try:
                    parsed = json.loads(seasons_json)
                    print(f"  Seasons: {len(parsed)}")
                    for season in parsed:
                        season_num = season.get('Season')
                        episodes = season.get('Episodes', [])
                        print(f"    Season {season_num}: {len(episodes)} episodes")
                        
                        # Check first episode structure
                        if episodes:
                            first_episode = episodes[0]
                            print(f"      First episode: {first_episode.get('Title')} (Episode {first_episode.get('Episode')})")
                            
                            # Check if episode has servers
                            servers = first_episode.get('Servers', [])
                            print(f"      Servers: {len(servers)}")
                            
                            # Check episode structure
                            required_fields = ['Episode', 'Title', 'Duration', 'Description', 'Thumbnail', 'Servers']
                            missing_fields = [field for field in required_fields if field not in first_episode]
                            if missing_fields:
                                print(f"      WARNING: Missing fields: {missing_fields}")
                            else:
                                print(f"      ✓ Episode structure is correct")
                except Exception as e:
                    print(f"  ERROR: Invalid JSON - {e}")
            else:
                print("  ERROR: No seasons data")
        
        # Check if there are any individual episode entries (should not exist)
        cursor.execute("SELECT COUNT(*) FROM entries WHERE title LIKE '%S%' AND title LIKE '%E%' AND main_category = 'TV Series'")
        individual_episodes = cursor.fetchone()[0]
        if individual_episodes > 0:
            print(f"\nWARNING: Found {individual_episodes} individual episode entries (should be grouped)")
        else:
            print(f"\n✓ No individual episode entries found (correct)")
        
        # Check series titles
        cursor.execute("SELECT DISTINCT title FROM entries WHERE main_category = 'TV Series'")
        series_titles = [row[0] for row in cursor.fetchall()]
        print(f"\nSeries titles: {series_titles}")
        
        # Verify no episode-specific titles in series entries
        episode_titles = [title for title in series_titles if 'S' in title and 'E' in title]
        if episode_titles:
            print(f"WARNING: Found episode-specific titles in series entries: {episode_titles}")
        else:
            print("✓ All series titles are clean (no episode info)")
        
        return True
        
    except Exception as e:
        print(f"Error testing database: {e}")
        return False
    finally:
        conn.close()

def compare_databases(original_db, fixed_db):
    """Compare original and fixed databases"""
    
    print("Comparing databases...")
    
    # Test original database
    print("\n" + "="*50)
    print("ORIGINAL DATABASE")
    print("="*50)
    test_database_structure(original_db)
    
    # Test fixed database
    print("\n" + "="*50)
    print("FIXED DATABASE")
    print("="*50)
    test_database_structure(fixed_db)

if __name__ == "__main__":
    original_db = "playlist.db"
    fixed_db = "playlist_fixed.db"
    
    if os.path.exists(original_db) and os.path.exists(fixed_db):
        compare_databases(original_db, fixed_db)
    elif os.path.exists(fixed_db):
        test_database_structure(fixed_db)
    else:
        print("No database files found to test")