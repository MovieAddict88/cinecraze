package com.cinecraze.free.database.entities;

import androidx.room.ColumnInfo;

public class EntryLight {
    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "sub_category")
    private String subCategory;

    @ColumnInfo(name = "country")
    private String country;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "poster")
    private String poster;

    @ColumnInfo(name = "thumbnail")
    private String thumbnail;

    @ColumnInfo(name = "rating")
    private String rating;

    @ColumnInfo(name = "duration")
    private String duration;

    @ColumnInfo(name = "year")
    private String year;

    @ColumnInfo(name = "main_category")
    private String mainCategory;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }
}