#!/usr/bin/env python3
"""
Comprehensive Review Script
Checks all files systematically for compilation issues
"""

import os
import re

def check_imports(file_path):
    """Check if file has proper imports"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # Check for common missing imports
        missing_imports = []
        
        # Check if Intent is used but not imported
        if 'Intent' in content and 'import android.content.Intent;' not in content:
            missing_imports.append('android.content.Intent')
            
        # Check if Gson is used but not imported
        if 'Gson' in content and 'import com.google.gson.Gson;' not in content:
            missing_imports.append('com.google.gson.Gson')
            
        # Check if AsyncTask is used but not imported
        if 'AsyncTask' in content and 'import android.os.AsyncTask;' not in content:
            missing_imports.append('android.os.AsyncTask')
            
        return missing_imports
    except Exception as e:
        return [f"Error reading file: {str(e)}"]

def check_file_structure(file_path):
    """Check file structure for basic issues"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        issues = []
        
        # Check for missing closing braces
        open_braces = content.count('{')
        close_braces = content.count('}')
        if open_braces != close_braces:
            issues.append(f"Brace mismatch: {open_braces} open, {close_braces} close")
            
        # Check for missing semicolons in obvious places
        lines = content.split('\n')
        for i, line in enumerate(lines, 1):
            stripped = line.strip()
            if stripped and not stripped.endswith(';') and not stripped.endswith('{') and not stripped.endswith('}') and 'class' not in stripped and 'import' not in stripped and 'package' not in stripped:
                if any(keyword in stripped for keyword in ['new ', 'return ', 'Intent(', 'startService(']):
                    issues.append(f"Possible missing semicolon at line {i}: {stripped}")
                    
        return issues
    except Exception as e:
        return [f"Error checking file: {str(e)}"]

def review_all_files():
    """Review all relevant files"""
    print("🔍 Comprehensive Review of All Files")
    print("=" * 60)
    
    files_to_check = [
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/FragmentMainActivity.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/BackgroundUpdateService.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/EnhancedUpdateManagerFixed.java",
        "cinecraze_extracted/CineCraze Tv/app/src/main/AndroidManifest.xml"
    ]
    
    all_issues = []
    
    for file_path in files_to_check:
        print(f"\n📁 Checking: {file_path}")
        print("-" * 50)
        
        if not os.path.exists(file_path):
            print(f"❌ File does not exist!")
            all_issues.append(f"Missing file: {file_path}")
            continue
            
        # Check imports
        missing_imports = check_imports(file_path)
        if missing_imports:
            print(f"❌ Missing imports: {missing_imports}")
            all_issues.append(f"Missing imports in {file_path}: {missing_imports}")
        else:
            print("✅ Imports look good")
            
        # Check file structure
        structure_issues = check_file_structure(file_path)
        if structure_issues:
            print(f"❌ Structure issues: {structure_issues}")
            all_issues.append(f"Structure issues in {file_path}: {structure_issues}")
        else:
            print("✅ File structure looks good")
            
        # Check file size
        file_size = os.path.getsize(file_path)
        print(f"📊 File size: {file_size} bytes")
        
        if file_size == 0:
            print("❌ File is empty!")
            all_issues.append(f"Empty file: {file_path}")
    
    return all_issues

def check_android_manifest():
    """Check AndroidManifest.xml specifically"""
    print(f"\n📋 AndroidManifest.xml Review")
    print("=" * 50)
    
    manifest_path = "cinecraze_extracted/CineCraze Tv/app/src/main/AndroidManifest.xml"
    
    if not os.path.exists(manifest_path):
        print("❌ AndroidManifest.xml not found!")
        return ["Missing AndroidManifest.xml"]
        
    with open(manifest_path, 'r') as f:
        content = f.read()
        
    issues = []
    
    # Check for duplicate activities
    activities = re.findall(r'android:name="([^"]*)"', content)
    activity_counts = {}
    for activity in activities:
        activity_counts[activity] = activity_counts.get(activity, 0) + 1
        
    duplicates = [activity for activity, count in activity_counts.items() if count > 1]
    if duplicates:
        print(f"❌ Duplicate activities: {duplicates}")
        issues.append(f"Duplicate activities: {duplicates}")
    else:
        print("✅ No duplicate activities")
        
    # Check for launcher activity
    if 'FragmentMainActivity' in content and 'android.intent.category.LAUNCHER' in content:
        print("✅ Launcher activity properly configured")
    else:
        print("❌ Launcher activity not properly configured")
        issues.append("Launcher activity not properly configured")
        
    # Check for service declaration
    if 'BackgroundUpdateService' in content:
        print("✅ Background service declared")
    else:
        print("❌ Background service not declared")
        issues.append("Background service not declared")
        
    return issues

def provide_fixes(issues):
    """Provide fixes for identified issues"""
    print(f"\n🔧 Fixes for Identified Issues")
    print("=" * 50)
    
    if not issues:
        print("✅ No issues found! Ready to build.")
        return
        
    for issue in issues:
        print(f"❌ Issue: {issue}")
        
        if "Missing imports" in issue:
            print("   Fix: Add missing import statements")
        elif "Duplicate activities" in issue:
            print("   Fix: Remove duplicate activity declarations")
        elif "Missing file" in issue:
            print("   Fix: Create the missing file")
        elif "Empty file" in issue:
            print("   Fix: Add content to the file")
        elif "Brace mismatch" in issue:
            print("   Fix: Check and fix brace balance")
        else:
            print("   Fix: Review and correct the issue")
        print()

def main():
    print("🎬 CineCraze Comprehensive Review")
    print("=" * 60)
    
    # Review all files
    file_issues = review_all_files()
    
    # Check AndroidManifest.xml specifically
    manifest_issues = check_android_manifest()
    
    # Combine all issues
    all_issues = file_issues + manifest_issues
    
    # Provide fixes
    provide_fixes(all_issues)
    
    print(f"\n🎯 Summary:")
    if all_issues:
        print(f"   ❌ Found {len(all_issues)} issues that need fixing")
        print(f"   🔧 Apply the fixes above before building")
    else:
        print(f"   ✅ All files look good!")
        print(f"   🚀 Ready to build the app")

if __name__ == "__main__":
    main()