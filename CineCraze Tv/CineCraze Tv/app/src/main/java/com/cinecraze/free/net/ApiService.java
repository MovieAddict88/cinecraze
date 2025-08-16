package com.cinecraze.free.net;

import com.cinecraze.free.models.Playlist;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.HEAD;

public interface ApiService {
    @Headers({
        "User-Agent: Mozilla/5.0 (Android) CineCraze/1.0",
        "Accept: application/json"
    })
    @GET("playlist.json")
    Call<Playlist> getPlaylist();

    @Headers({
        "User-Agent: Mozilla/5.0 (Android) CineCraze/1.0",
        "Accept: application/json"
    })
    @HEAD("playlist.json")
    Call<Void> headPlaylist();
}