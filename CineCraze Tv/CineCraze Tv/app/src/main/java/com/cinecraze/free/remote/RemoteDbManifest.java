package com.cinecraze.free.remote;

public class RemoteDbManifest {
    public String version;
    public String dbUrl;
    public long sizeBytes;
    public String sha256; // checksum of the final unzipped DB file
    public boolean zipped;
}