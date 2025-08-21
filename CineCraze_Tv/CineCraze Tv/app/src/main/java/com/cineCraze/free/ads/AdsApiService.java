package com.cinecraze.free.ads;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service for fetching ads configuration from remote JSON
 * Based on CineMax API implementation patterns
 */
public class AdsApiService {
    
    private static final String TAG = "AdsApiService";
    private static final String ADS_CONFIG_URL = "https://raw.githubusercontent.com/MovieAddict88/movie-api/main/ads_config.json";
    
    private OkHttpClient httpClient;
    private Gson gson;
    
    public AdsApiService() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Fetch ads configuration from remote URL
     */
    public void fetchAdsConfig(AdsConfigCallback callback) {
        Request request = new Request.Builder()
                .url(ADS_CONFIG_URL)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch ads config", e);
                if (callback != null) {
                    callback.onFailure("Network error: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorMsg = "HTTP error: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onFailure(errorMsg);
                    }
                    return;
                }
                
                try {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Ads config response: " + jsonResponse);
                    
                    AdsConfig adsConfig = gson.fromJson(jsonResponse, AdsConfig.class);
                    if (adsConfig != null) {
                        if (callback != null) {
                            callback.onSuccess(adsConfig);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFailure("Failed to parse ads config");
                        }
                    }
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "Failed to parse JSON response", e);
                    if (callback != null) {
                        callback.onFailure("JSON parsing error: " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error processing response", e);
                    if (callback != null) {
                        callback.onFailure("Processing error: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Callback interface for ads config fetch operations
     */
    public interface AdsConfigCallback {
        void onSuccess(AdsConfig config);
        void onFailure(String error);
    }
}