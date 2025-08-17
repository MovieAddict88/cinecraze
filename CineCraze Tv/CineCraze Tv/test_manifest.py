#!/usr/bin/env python3
"""
Test script to verify manifest.json accessibility and content
"""
import requests
import json
import time

def test_manifest_access():
    """Test if the manifest.json is accessible and check its content"""
    manifest_url = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json"
    
    print("Testing manifest.json accessibility...")
    print(f"URL: {manifest_url}")
    
    try:
        # Test with cache-busting
        timestamp = int(time.time() * 1000)
        test_url = f"{manifest_url}?_cb={timestamp}"
        
        print(f"Testing with cache-busting: {test_url}")
        
        response = requests.get(test_url, timeout=15)
        
        print(f"Response status: {response.status_code}")
        print(f"Response headers: {dict(response.headers)}")
        
        if response.status_code == 200:
            try:
                manifest_data = response.json()
                print("✅ Manifest.json is accessible!")
                print(f"Version: {manifest_data.get('version', 'N/A')}")
                print(f"Description: {manifest_data.get('description', 'N/A')}")
                
                if 'database' in manifest_data:
                    db_info = manifest_data['database']
                    print(f"Database size: {db_info.get('sizeBytes', 'N/A')} bytes")
                    print(f"Database URL: {db_info.get('url', 'N/A')}")
                
                print("\nFull manifest content:")
                print(json.dumps(manifest_data, indent=2))
                
            except json.JSONDecodeError as e:
                print(f"❌ Failed to parse JSON: {e}")
                print(f"Raw response: {response.text[:500]}...")
        else:
            print(f"❌ HTTP error: {response.status_code}")
            print(f"Response: {response.text}")
            
    except requests.exceptions.RequestException as e:
        print(f"❌ Network error: {e}")

def test_manifest_consistency():
    """Test if manifest content is consistent across multiple requests"""
    manifest_url = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/manifest.json"
    
    print("\nTesting manifest consistency...")
    
    responses = []
    for i in range(3):
        try:
            timestamp = int(time.time() * 1000)
            test_url = f"{manifest_url}?_cb={timestamp}"
            response = requests.get(test_url, timeout=15)
            
            if response.status_code == 200:
                manifest_data = response.json()
                responses.append(manifest_data)
                print(f"Request {i+1}: Version {manifest_data.get('version', 'N/A')}")
            else:
                print(f"Request {i+1}: HTTP {response.status_code}")
                
            time.sleep(1)  # Small delay between requests
            
        except Exception as e:
            print(f"Request {i+1}: Error {e}")
    
    if len(responses) >= 2:
        # Compare responses
        first_response = responses[0]
        for i, response in enumerate(responses[1:], 1):
            if first_response == response:
                print(f"✅ Response {i+1} matches first response")
            else:
                print(f"❌ Response {i+1} differs from first response")
                print(f"Differences: {set(first_response.items()) ^ set(response.items())}")

if __name__ == "__main__":
    test_manifest_access()
    test_manifest_consistency()