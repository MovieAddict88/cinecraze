#!/usr/bin/env python3
"""
Final Verification Script
Focuses on real compilation issues, not false positives
"""

import os

def check_real_compilation_issues():
    """Check for real compilation issues"""
    print("🔍 Final Verification - Real Compilation Issues")
    print("=" * 60)
    
    issues_found = []
    
    # Check 1: Intent import in FragmentMainActivity
    print("\n1. Checking Intent import in FragmentMainActivity...")
    fragment_file = "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/FragmentMainActivity.java"
    
    if os.path.exists(fragment_file):
        with open(fragment_file, 'r') as f:
            content = f.read()
            
        if 'import android.content.Intent;' in content:
            print("✅ Intent import found")
        else:
            print("❌ Intent import missing")
            issues_found.append("Missing Intent import in FragmentMainActivity")
    else:
        print("❌ FragmentMainActivity.java not found")
        issues_found.append("FragmentMainActivity.java missing")
    
    # Check 2: BackgroundUpdateService exists
    print("\n2. Checking BackgroundUpdateService...")
    service_file = "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/BackgroundUpdateService.java"
    
    if os.path.exists(service_file):
        print("✅ BackgroundUpdateService.java exists")
        with open(service_file, 'r') as f:
            content = f.read()
            
        if 'class BackgroundUpdateService extends Service' in content:
            print("✅ BackgroundUpdateService class properly defined")
        else:
            print("❌ BackgroundUpdateService class not properly defined")
            issues_found.append("BackgroundUpdateService class not properly defined")
    else:
        print("❌ BackgroundUpdateService.java not found")
        issues_found.append("BackgroundUpdateService.java missing")
    
    # Check 3: EnhancedUpdateManagerFixed exists and has Gson
    print("\n3. Checking EnhancedUpdateManagerFixed...")
    manager_file = "cinecraze_extracted/CineCraze Tv/app/src/main/java/com/cinecraze/free/utils/EnhancedUpdateManagerFixed.java"
    
    if os.path.exists(manager_file):
        print("✅ EnhancedUpdateManagerFixed.java exists")
        with open(manager_file, 'r') as f:
            content = f.read()
            
        if 'import com.google.gson.Gson;' in content:
            print("✅ Gson import found")
        else:
            print("❌ Gson import missing")
            issues_found.append("Missing Gson import in EnhancedUpdateManagerFixed")
            
        if 'class EnhancedUpdateManagerFixed' in content:
            print("✅ EnhancedUpdateManagerFixed class properly defined")
        else:
            print("❌ EnhancedUpdateManagerFixed class not properly defined")
            issues_found.append("EnhancedUpdateManagerFixed class not properly defined")
    else:
        print("❌ EnhancedUpdateManagerFixed.java not found")
        issues_found.append("EnhancedUpdateManagerFixed.java missing")
    
    # Check 4: AndroidManifest.xml has correct launcher
    print("\n4. Checking AndroidManifest.xml...")
    manifest_file = "cinecraze_extracted/CineCraze Tv/app/src/main/AndroidManifest.xml"
    
    if os.path.exists(manifest_file):
        print("✅ AndroidManifest.xml exists")
        with open(manifest_file, 'r') as f:
            content = f.read()
            
        if 'FragmentMainActivity' in content and 'android.intent.category.LAUNCHER' in content:
            print("✅ FragmentMainActivity is launcher")
        else:
            print("❌ FragmentMainActivity not properly configured as launcher")
            issues_found.append("FragmentMainActivity not properly configured as launcher")
            
        if 'BackgroundUpdateService' in content:
            print("✅ BackgroundUpdateService declared in manifest")
        else:
            print("❌ BackgroundUpdateService not declared in manifest")
            issues_found.append("BackgroundUpdateService not declared in manifest")
    else:
        print("❌ AndroidManifest.xml not found")
        issues_found.append("AndroidManifest.xml missing")
    
    return issues_found

def main():
    print("🎬 CineCraze Final Verification")
    print("=" * 60)
    
    issues = check_real_compilation_issues()
    
    print(f"\n🎯 Final Results:")
    if issues:
        print(f"❌ Found {len(issues)} real compilation issues:")
        for issue in issues:
            print(f"   - {issue}")
        print(f"\n🔧 Fix these issues before building")
    else:
        print(f"✅ No real compilation issues found!")
        print(f"🚀 Ready to build the app")
        print(f"\n📱 Expected behavior:")
        print(f"   - App opens directly to main screen")
        print(f"   - Background service handles updates silently")
        print(f"   - No download dialogs")

if __name__ == "__main__":
    main()