package com.cinecraze.free.ads;

import com.google.gson.annotations.SerializedName;

/**
 * Data structure for parsing ads_config.json
 * Based on CineMax AdsResponse structure
 */
public class AdsConfig {
    
    @SerializedName("admob")
    private AdMobConfig admob;
    
    @SerializedName("facebook")
    private FacebookConfig facebook;
    
    @SerializedName("settings")
    private AdsSettings settings;
    
    public AdMobConfig getAdmob() {
        return admob;
    }
    
    public void setAdmob(AdMobConfig admob) {
        this.admob = admob;
    }
    
    public FacebookConfig getFacebook() {
        return facebook;
    }
    
    public void setFacebook(FacebookConfig facebook) {
        this.facebook = facebook;
    }
    
    public AdsSettings getSettings() {
        return settings;
    }
    
    public void setSettings(AdsSettings settings) {
        this.settings = settings;
    }
    
    public static class AdMobConfig {
        @SerializedName("banner_id")
        private String bannerId;
        
        @SerializedName("interstitial_id")
        private String interstitialId;
        
        @SerializedName("rewarded_id")
        private String rewardedId;
        
        @SerializedName("native_id")
        private String nativeId;
        
        @SerializedName("app_id")
        private String appId;
        
        public String getBannerId() {
            return bannerId;
        }
        
        public void setBannerId(String bannerId) {
            this.bannerId = bannerId;
        }
        
        public String getInterstitialId() {
            return interstitialId;
        }
        
        public void setInterstitialId(String interstitialId) {
            this.interstitialId = interstitialId;
        }
        
        public String getRewardedId() {
            return rewardedId;
        }
        
        public void setRewardedId(String rewardedId) {
            this.rewardedId = rewardedId;
        }
        
        public String getNativeId() {
            return nativeId;
        }
        
        public void setNativeId(String nativeId) {
            this.nativeId = nativeId;
        }
        
        public String getAppId() {
            return appId;
        }
        
        public void setAppId(String appId) {
            this.appId = appId;
        }
    }
    
    public static class FacebookConfig {
        @SerializedName("banner_id")
        private String bannerId;
        
        @SerializedName("interstitial_id")
        private String interstitialId;
        
        @SerializedName("rewarded_id")
        private String rewardedId;
        
        @SerializedName("native_id")
        private String nativeId;
        
        public String getBannerId() {
            return bannerId;
        }
        
        public void setBannerId(String bannerId) {
            this.bannerId = bannerId;
        }
        
        public String getInterstitialId() {
            return interstitialId;
        }
        
        public void setInterstitialId(String interstitialId) {
            this.interstitialId = interstitialId;
        }
        
        public String getRewardedId() {
            return rewardedId;
        }
        
        public void setRewardedId(String rewardedId) {
            this.rewardedId = rewardedId;
        }
        
        public String getNativeId() {
            return nativeId;
        }
        
        public void setNativeId(String nativeId) {
            this.nativeId = nativeId;
        }
    }
    
    public static class AdsSettings {
        @SerializedName("banner_enabled")
        private boolean bannerEnabled;
        
        @SerializedName("interstitial_enabled")
        private boolean interstitialEnabled;
        
        @SerializedName("rewarded_enabled")
        private boolean rewardedEnabled;
        
        @SerializedName("native_enabled")
        private boolean nativeEnabled;
        
        @SerializedName("interstitial_clicks")
        private int interstitialClicks;
        
        @SerializedName("native_lines")
        private int nativeLines;
        
        @SerializedName("banner_type")
        private String bannerType;
        
        @SerializedName("interstitial_type")
        private String interstitialType;
        
        @SerializedName("native_type")
        private String nativeType;
        
        public boolean isBannerEnabled() {
            return bannerEnabled;
        }
        
        public void setBannerEnabled(boolean bannerEnabled) {
            this.bannerEnabled = bannerEnabled;
        }
        
        public boolean isInterstitialEnabled() {
            return interstitialEnabled;
        }
        
        public void setInterstitialEnabled(boolean interstitialEnabled) {
            this.interstitialEnabled = interstitialEnabled;
        }
        
        public boolean isRewardedEnabled() {
            return rewardedEnabled;
        }
        
        public void setRewardedEnabled(boolean rewardedEnabled) {
            this.rewardedEnabled = rewardedEnabled;
        }
        
        public boolean isNativeEnabled() {
            return nativeEnabled;
        }
        
        public void setNativeEnabled(boolean nativeEnabled) {
            this.nativeEnabled = nativeEnabled;
        }
        
        public int getInterstitialClicks() {
            return interstitialClicks;
        }
        
        public void setInterstitialClicks(int interstitialClicks) {
            this.interstitialClicks = interstitialClicks;
        }
        
        public int getNativeLines() {
            return nativeLines;
        }
        
        public void setNativeLines(int nativeLines) {
            this.nativeLines = nativeLines;
        }
        
        public String getBannerType() {
            return bannerType;
        }
        
        public void setBannerType(String bannerType) {
            this.bannerType = bannerType;
        }
        
        public String getInterstitialType() {
            return interstitialType;
        }
        
        public void setInterstitialType(String interstitialType) {
            this.interstitialType = interstitialType;
        }
        
        public String getNativeType() {
            return nativeType;
        }
        
        public void setNativeType(String nativeType) {
            this.nativeType = nativeType;
        }
    }
}