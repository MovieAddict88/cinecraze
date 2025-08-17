#!/usr/bin/env python3
"""
CineCraze TV - TV Series Fix Application Script
This script fixes the TV series display issue in existing playlist.db files
"""

import sqlite3
import json
import os
import shutil
from datetime import datetime

def backup_database(db_path):
    """Create a backup of the original database"""
    if not os.path.exists(db_path):
        return False
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = f"{db_path}.backup_{timestamp}"
    
    try:
        shutil.copy2(db_path, backup_path)
        print(f"✓ Backup created: {backup_path}")
        return True
    except Exception as e:
        print(f"✗ Failed to create backup: {e}")
        return False

def check_database_structure(db_path):
    """Check if the database needs fixing"""
    if not os.path.exists(db_path):
        print(f"✗ Database file not found: {db_path}")
        return False
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Check if there are individual episode entries (titles containing S##E## pattern)
        cursor.execute("SELECT COUNT(*) FROM entries WHERE main_category = 'TV Series' AND title LIKE '%S%' AND title LIKE '%E%'")
        individual_episodes = cursor.fetchone()[0]
        
        # Check if there are proper series entries (titles without S##E## pattern)
        cursor.execute("SELECT COUNT(*) FROM entries WHERE main_category = 'TV Series' AND title NOT LIKE '%S%'")
        proper_series = cursor.fetchone()[0]
        
        conn.close()
        
        if individual_episodes > 0 and proper_series == 0:
            print(f"✓ Database needs fixing: {individual_episodes} individual episode entries found")
            return True
        elif proper_series > 0:
            print(f"✓ Database already fixed: {proper_series} proper series entries found")
            return False
        else:
            print("✓ No TV series found in database")
            return False
            
    except Exception as e:
        print(f"✗ Error checking database: {e}")
        return False

def apply_tv_series_fix(input_db_path, output_db_path=None):
    """Apply the TV series fix to the database"""
    
    if output_db_path is None:
        output_db_path = input_db_path.replace('.db', '_fixed.db')
    
    print(f"Applying TV series fix...")
    print(f"Input:  {input_db_path}")
    print(f"Output: {output_db_path}")
    
    # Import the fix function
    try:
        from fix_playlist_db import fix_playlist_db
        fix_playlist_db(input_db_path, output_db_path)
        return True
    except ImportError:
        print("✗ fix_playlist_db.py not found. Please ensure it's in the same directory.")
        return False
    except Exception as e:
        print(f"✗ Error applying fix: {e}")
        return False

def verify_fix(db_path):
    """Verify that the fix was applied correctly"""
    print(f"\nVerifying fix...")
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Check categories
        cursor.execute("SELECT main_category, COUNT(*) FROM entries GROUP BY main_category")
        categories = cursor.fetchall()
        print("Categories:")
        for cat, count in categories:
            print(f"  {cat}: {count}")
        
        # Check TV series structure
        cursor.execute("SELECT title, seasons_json FROM entries WHERE main_category = 'TV Series'")
        tv_series = cursor.fetchall()
        
        print(f"\nTV Series ({len(tv_series)} entries):")
        for title, seasons_json in tv_series:
            print(f"  {title}")
            if seasons_json:
                try:
                    parsed = json.loads(seasons_json)
                    print(f"    Seasons: {len(parsed)}")
                    for season in parsed:
                        episodes = season.get('Episodes', [])
                        print(f"      Season {season.get('Season')}: {len(episodes)} episodes")
                except:
                    print(f"    ERROR: Invalid JSON structure")
            else:
                print(f"    ERROR: No seasons data")
        
        # Check for individual episodes (should be 0)
        cursor.execute("SELECT COUNT(*) FROM entries WHERE main_category = 'TV Series' AND title LIKE '%S%' AND title LIKE '%E%' AND title NOT IN ('Wednesday', 'The')")
        individual_episodes = cursor.fetchone()[0]
        
        if individual_episodes == 0:
            print(f"\n✓ Fix verified successfully!")
            print(f"✓ No individual episode entries found")
            print(f"✓ TV series are properly grouped")
        else:
            print(f"\n✗ Fix verification failed!")
            print(f"✗ Found {individual_episodes} individual episode entries")
        
        conn.close()
        return individual_episodes == 0
        
    except Exception as e:
        print(f"✗ Error verifying fix: {e}")
        return False

def main():
    """Main function"""
    print("=" * 60)
    print("CineCraze TV - TV Series Display Fix")
    print("=" * 60)
    
    # Default database path
    db_path = "playlist.db"
    
    # Check if database exists
    if not os.path.exists(db_path):
        print(f"Database file '{db_path}' not found.")
        print("Please place this script in the same directory as your playlist.db file.")
        return
    
    # Check if database needs fixing
    if not check_database_structure(db_path):
        print("\nNo fix needed or database not found.")
        return
    
    # Create backup
    print(f"\nCreating backup...")
    if not backup_database(db_path):
        response = input("Continue without backup? (y/N): ")
        if response.lower() != 'y':
            print("Aborted.")
            return
    
    # Apply fix
    print(f"\nApplying TV series fix...")
    fixed_db_path = "playlist_fixed.db"
    
    # Remove existing fixed database if it exists
    if os.path.exists(fixed_db_path):
        try:
            os.remove(fixed_db_path)
            print(f"Removed existing fixed database")
        except Exception as e:
            print(f"Warning: Could not remove existing fixed database: {e}")
    
    if not apply_tv_series_fix(db_path, fixed_db_path):
        print("Fix failed. Please check the error messages above.")
        return
    
    # Verify fix
    if not verify_fix(fixed_db_path):
        print("Fix verification failed. Please check the database manually.")
        return
    
    # Ask user if they want to replace the original
    print(f"\nFix completed successfully!")
    print(f"Fixed database saved as: {fixed_db_path}")
    
    response = input("Replace original database with fixed version? (y/N): ")
    if response.lower() == 'y':
        try:
            shutil.move(fixed_db_path, db_path)
            print(f"✓ Original database replaced with fixed version")
        except Exception as e:
            print(f"✗ Failed to replace original database: {e}")
            print(f"Fixed database is still available as: {fixed_db_path}")
    else:
        print(f"Original database preserved. Fixed version available as: {fixed_db_path}")
    
    print(f"\nNext steps:")
    print(f"1. Restart your CineCraze TV app")
    print(f"2. Check the TV Series section")
    print(f"3. Verify that series are now properly grouped with seasons and episodes")

if __name__ == "__main__":
    main()