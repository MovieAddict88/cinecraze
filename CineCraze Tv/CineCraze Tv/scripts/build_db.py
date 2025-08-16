#!/usr/bin/env python3
import argparse
import hashlib
import io
import json
import os
import sqlite3
import sys
import time
import urllib.request
import zipfile
from typing import Any, Dict, List

DEFAULT_URL = "https://raw.githubusercontent.com/MovieAddict88/Movie-Source/main/playlist.json"

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS entries (
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
);

CREATE TABLE IF NOT EXISTS cache_metadata (
    key TEXT PRIMARY KEY,
    last_updated INTEGER,
    data_version TEXT
);

-- Include all Room entities to satisfy schema validation
CREATE TABLE IF NOT EXISTS download_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    downloadManagerId INTEGER,
    title TEXT,
    url TEXT,
    localUri TEXT,
    totalBytes INTEGER,
    downloadedBytes INTEGER,
    status INTEGER,
    createdAt INTEGER,
    updatedAt INTEGER
);

CREATE INDEX IF NOT EXISTS idx_entries_title ON entries(title);
CREATE INDEX IF NOT EXISTS idx_entries_main_category ON entries(main_category);
CREATE INDEX IF NOT EXISTS idx_entries_sub_category ON entries(sub_category);
""".strip()


def fetch_json(url: str) -> Dict[str, Any]:
    with urllib.request.urlopen(url) as resp:
        data = resp.read()
    return json.loads(data.decode("utf-8", errors="replace"))


def normalize_str(v: Any, default: str = "") -> str:
    if v is None:
        return default
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, str):
        return v
    return default


def rating_to_str(v: Any) -> str:
    if v is None:
        return "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, str):
        return v
    return "0"


def year_to_str(v: Any) -> str:
    if v is None:
        return "0"
    if isinstance(v, (int, float)):
        return str(int(v))
    if isinstance(v, str):
        return v
    return "0"


def build_db(playlist: Dict[str, Any], out_path: str, version: str) -> None:
    if os.path.exists(out_path):
        os.remove(out_path)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    conn = sqlite3.connect(out_path)
    try:
        conn.executescript(SCHEMA_SQL)
        cur = conn.cursor()
        categories: List[Dict[str, Any]] = playlist.get("Categories", [])
        total = 0
        for cat in categories:
            main_category = normalize_str(cat.get("MainCategory"))
            entries = cat.get("Entries", []) or []
            for e in entries:
                title = normalize_str(e.get("Title"))
                sub_category = normalize_str(e.get("SubCategory"))
                country = normalize_str(e.get("Country"))
                description = normalize_str(e.get("Description"))
                poster = normalize_str(e.get("Poster"))
                thumbnail = normalize_str(e.get("Thumbnail"))
                rating = rating_to_str(e.get("Rating"))
                duration = normalize_str(e.get("Duration"))
                year = year_to_str(e.get("Year"))

                servers_json = json.dumps(e.get("Servers", []) or [], ensure_ascii=False)
                seasons_json = json.dumps(e.get("Seasons", []) or [], ensure_ascii=False)
                related_json = json.dumps(e.get("Related", []) or [], ensure_ascii=False)

                cur.execute(
                    """
                    INSERT INTO entries (
                        title, sub_category, country, description, poster, thumbnail,
                        rating, duration, year, main_category, servers_json, seasons_json, related_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        title, sub_category, country, description, poster, thumbnail,
                        rating, duration, year, main_category, servers_json, seasons_json, related_json,
                    ),
                )
                total += 1
        # Cache metadata
        cur.execute(
            "INSERT OR REPLACE INTO cache_metadata (key, last_updated, data_version) VALUES (?, ?, ?)",
            ("playlist_data", int(time.time() * 1000), version),
        )
        conn.commit()
        # VACUUM into the same file by using incremental trick: not needed for test; simple VACUUM
        cur.execute("VACUUM;")
        conn.commit()
        print(f"Inserted {total} entries into {out_path}")
    finally:
        conn.close()


def sha256_file(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def zip_db(db_path: str, zip_path: str) -> int:
    if os.path.exists(zip_path):
        os.remove(zip_path)
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(db_path, arcname=os.path.basename(db_path))
    return os.path.getsize(zip_path)


def write_manifest(manifest_path: str, version: str, db_url: str, db_sha256: str, zipped: bool, size_bytes: int) -> None:
    manifest = {
        "version": version,
        "dbUrl": db_url,
        "sizeBytes": size_bytes,
        "sha256": db_sha256,
        "zipped": zipped,
    }
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)


def main():
    parser = argparse.ArgumentParser(description="Build cinecraze SQLite DB from playlist.json")
    parser.add_argument("--url", default=DEFAULT_URL, help="URL to playlist.json (default: current GitHub)")
    parser.add_argument("--input", help="Local path to playlist.json (overrides --url)")
    parser.add_argument("--version", required=True, help="Data version to embed into DB and manifest")
    parser.add_argument("--output-dir", default="build", help="Output directory (default: build)")
    parser.add_argument("--db-url", default="", help="Public URL where the zipped DB will be hosted (for manifest)")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)
    json_data: Dict[str, Any]
    if args.input:
        with open(args.input, "r", encoding="utf-8") as f:
            json_data = json.load(f)
    else:
        print(f"Fetching JSON from: {args.url}")
        json_data = fetch_json(args.url)

    db_path = os.path.join(args.output_dir, "cinecraze.db")
    build_db(json_data, db_path, args.version)
    sha_unzipped = sha256_file(db_path)

    zip_path = os.path.join(args.output_dir, "cinecraze.db.zip")
    size_bytes = zip_db(db_path, zip_path)

    manifest_path = os.path.join(args.output_dir, "db-manifest.json")
    write_manifest(manifest_path, args.version, args.db_url, sha_unzipped, True, size_bytes)

    print("\nBuild complete:")
    print(f"  DB:           {db_path}")
    print(f"  DB sha256:    {sha_unzipped}")
    print(f"  Zipped:       {zip_path} ({size_bytes} bytes)")
    print(f"  Manifest:     {manifest_path}")
    if not args.db_url:
        print("\nNote: dbUrl in manifest is empty. After hosting cinecraze.db.zip, set dbUrl accordingly.")


if __name__ == "__main__":
    sys.exit(main())