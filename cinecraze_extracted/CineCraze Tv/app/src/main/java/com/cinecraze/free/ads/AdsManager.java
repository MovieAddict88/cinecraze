package com.cinecraze.free.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

/**
 * Manages AdMob ads for CineCraze Tv
 * Based on CineMax ad implementation patterns
 */
public class AdsManager {
    
    private static final String TAG = "AdsManager";
    
    private Context context;
    private AdsConfig adsConfig;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private int interstitialClickCount = 0;
    
    public AdsManager(Context context) {
        this.context = context;
    }
    
    /**
     * Set ads configuration
     */
    public void setAdsConfig(AdsConfig config) {
        this.adsConfig = config;
    }
    
    /**
     * Load banner ad
     */
    public void loadBannerAd(String adUnitId, ViewGroup adContainer) {
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.w(TAG, "Banner ad unit ID is null or empty");
            return;
        }
        
        try {
            final AdView adView = new AdView(context);
            adView.setAdSize(AdSize.SMART_BANNER);
            adView.setAdUnitId(adUnitId);
            
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully");
                    adContainer.setVisibility(View.VISIBLE);
                }
                
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
                    adContainer.setVisibility(View.GONE);
                }
            });
            
            adContainer.removeAllViews();
            adContainer.addView(adView);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading banner ad", e);
            adContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * Load interstitial ad
     */
    public void loadInterstitialAd(String adUnitId) {
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.w(TAG, "Interstitial ad unit ID is null or empty");
            return;
        }
        
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            InterstitialAd.load(context, adUnitId, adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            interstitialAd = ad;
                            Log.d(TAG, "Interstitial ad loaded successfully");
                            
                            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    interstitialAd = null;
                                    Log.d(TAG, "Interstitial ad dismissed");
                                }
                                
                                @Override
                                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                    interstitialAd = null;
                                    Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                                }
                            });
                        }
                        
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            interstitialAd = null;
                            Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading interstitial ad", e);
        }
    }
    
    /**
     * Load rewarded ad
     */
    public void loadRewardedAd(String adUnitId) {
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.w(TAG, "Rewarded ad unit ID is null or empty");
            return;
        }
        
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            
            RewardedAd.load(context, adUnitId, adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                            Log.d(TAG, "Rewarded ad loaded successfully");
                        }
                        
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            rewardedAd = null;
                            Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading rewarded ad", e);
        }
    }
    
    /**
     * Check if interstitial ad is ready
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }
    
    /**
     * Check if rewarded ad is ready
     */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }
    
    /**
     * Show interstitial ad
     */
    public void showInterstitialAd(Activity activity) {
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial ad is not ready");
            return;
        }
        
        try {
            interstitialAd.show(activity);
            interstitialClickCount++;
            Log.d(TAG, "Interstitial ad shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing interstitial ad", e);
        }
    }
    
    /**
     * Show rewarded ad
     */
    public void showRewardedAd(Activity activity, OnUserEarnedRewardListener callback) {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded ad is not ready");
            return;
        }
        
        try {
            if (callback != null) {
                rewardedAd.show(activity, callback);
            } else {
                // Use default callback if none provided
                rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                        Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                    }
                });
            }
            Log.d(TAG, "Rewarded ad shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error showing rewarded ad", e);
        }
    }
    
    /**
     * Check if interstitial ad should be shown based on click count
     */
    public boolean shouldShowInterstitialAd() {
        if (adsConfig == null || adsConfig.getSettings() == null) {
            return false;
        }
        
        int requiredClicks = adsConfig.getSettings().getInterstitialClicks();
        return interstitialClickCount >= requiredClicks;
    }
    
    /**
     * Destroy ads and clean up resources
     */
    public void destroyAds() {
        if (interstitialAd != null) {
            interstitialAd = null;
        }
        if (rewardedAd != null) {
            rewardedAd = null;
        }
        Log.d(TAG, "Ads destroyed and resources cleaned up");
    }
}