#!/usr/bin/env python3
"""
GitHub Upload Helper for CineCraze Playlist Database
Uploads playlist.db to your GitHub repository
"""

import os
import base64
import json
import urllib.request
import urllib.error
import urllib.parse

class GitHubUploader:
    def __init__(self, username, repo_name, token=None):
        self.username = username
        self.repo_name = repo_name
        self.token = token
        self.api_base = f"https://api.github.com/repos/{username}/{repo_name}"
        
    def upload_file(self, file_path, commit_message="Update playlist database"):
        """Upload a file to GitHub repository"""
        
        if not os.path.exists(file_path):
            print(f"❌ File not found: {file_path}")
            return False
            
        try:
            # Read file content
            with open(file_path, 'rb') as file:
                content = file.read()
            
            # Encode content
            content_b64 = base64.b64encode(content).decode('utf-8')
            
            # Prepare request data
            data = {
                "message": commit_message,
                "content": content_b64,
                "branch": "main"  # or "master" depending on your default branch
            }
            
            # Create request
            req = urllib.request.Request(
                f"{self.api_base}/contents/playlist.db",
                data=json.dumps(data).encode('utf-8'),
                headers={
                    'Content-Type': 'application/json',
                    'User-Agent': 'CineCraze-Uploader'
                }
            )
            
            # Add authorization if token provided
            if self.token:
                req.add_header('Authorization', f'token {self.token}')
            
            # Make request
            with urllib.request.urlopen(req) as response:
                result = json.loads(response.read().decode('utf-8'))
                
            print(f"✅ File uploaded successfully!")
            print(f"📁 File: {file_path}")
            print(f"🔗 URL: {result.get('content', {}).get('download_url', 'N/A')}")
            print(f"📝 Commit: {result.get('commit', {}).get('sha', 'N/A')}")
            
            return True
            
        except urllib.error.HTTPError as e:
            if e.code == 422:
                print("❌ File already exists. Use update_file() to update existing file.")
            else:
                print(f"❌ HTTP Error {e.code}: {e.reason}")
                print(f"Response: {e.read().decode('utf-8')}")
            return False
        except Exception as e:
            print(f"❌ Error uploading file: {str(e)}")
            return False
    
    def update_file(self, file_path, commit_message="Update playlist database"):
        """Update an existing file in GitHub repository"""
        
        if not os.path.exists(file_path):
            print(f"❌ File not found: {file_path}")
            return False
            
        try:
            # First, get the current file info
            req = urllib.request.Request(
                f"{self.api_base}/contents/playlist.db",
                headers={
                    'User-Agent': 'CineCraze-Uploader'
                }
            )
            
            if self.token:
                req.add_header('Authorization', f'token {self.token}')
            
            with urllib.request.urlopen(req) as response:
                current_file = json.loads(response.read().decode('utf-8'))
            
            # Read new file content
            with open(file_path, 'rb') as file:
                content = file.read()
            
            # Encode content
            content_b64 = base64.b64encode(content).decode('utf-8')
            
            # Prepare request data
            data = {
                "message": commit_message,
                "content": content_b64,
                "sha": current_file['sha'],
                "branch": "main"
            }
            
            # Create request
            req = urllib.request.Request(
                f"{self.api_base}/contents/playlist.db",
                data=json.dumps(data).encode('utf-8'),
                headers={
                    'Content-Type': 'application/json',
                    'User-Agent': 'CineCraze-Uploader'
                }
            )
            
            if self.token:
                req.add_header('Authorization', f'token {self.token}')
            
            # Make request
            with urllib.request.urlopen(req) as response:
                result = json.loads(response.read().decode('utf-8'))
                
            print(f"✅ File updated successfully!")
            print(f"📁 File: {file_path}")
            print(f"🔗 URL: {result.get('content', {}).get('download_url', 'N/A')}")
            print(f"📝 Commit: {result.get('commit', {}).get('sha', 'N/A')}")
            
            return True
            
        except urllib.error.HTTPError as e:
            print(f"❌ HTTP Error {e.code}: {e.reason}")
            print(f"Response: {e.read().decode('utf-8')}")
            return False
        except Exception as e:
            print(f"❌ Error updating file: {str(e)}")
            return False

def main():
    print("🚀 CineCraze GitHub Upload Helper")
    print("=" * 40)
    
    # Configuration
    username = "MovieAddict88"  # Your GitHub username
    repo_name = "Movie-Source"  # Your repository name
    file_path = "playlist.db"   # Database file to upload
    
    # Check if file exists
    if not os.path.exists(file_path):
        print(f"❌ Database file not found: {file_path}")
        print("Please run the converter first: python3 github_json_to_sqlite_converter.py")
        return
    
    # Get file size
    file_size = os.path.getsize(file_path) / (1024 * 1024)
    print(f"📁 File: {file_path}")
    print(f"📊 Size: {file_size:.2f} MB")
    
    # Ask for GitHub token (optional but recommended)
    token = input("🔑 GitHub Token (optional, press Enter to skip): ").strip()
    if not token:
        print("⚠️  No token provided. Upload may fail if repository is private.")
    
    # Create uploader
    uploader = GitHubUploader(username, repo_name, token if token else None)
    
    # Try to upload
    print("\n📤 Uploading to GitHub...")
    success = uploader.upload_file(file_path)
    
    if not success:
        print("\n🔄 Trying to update existing file...")
        success = uploader.update_file(file_path)
    
    if success:
        print(f"\n✅ Success! Your database is now available at:")
        print(f"https://github.com/{username}/{repo_name}/raw/main/playlist.db")
        print("\n🎉 Users can now download the database in your app!")
    else:
        print("\n❌ Upload failed. Please check your configuration and try again.")

if __name__ == "__main__":
    main()