package com.ludoscity.findmybikes.citybik_es.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@Entity
public class BikeStation {

    @SerializedName("id")
    @PrimaryKey
    @NonNull
    private String location_hash; //used as a uid

    @ColumnInfo(name = "empty_slots")
    private Integer empty_slots;
    @Embedded
    private BikeStationExtra extra;
    @ColumnInfo(name = "free_bikes")
    private Integer free_bikes;
    @ColumnInfo(name = "latitude")
    private Double latitude;
    @ColumnInfo(name = "longitude")
    private Double longitude;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "timestamp")
    private String timestamp;

    @NonNull
    public String getLocation_hash() {
        return location_hash;
    }

    public void setLocation_hash(String id) {
        this.location_hash = id;
    }

    public Integer getEmpty_slots() {
        return empty_slots;
    }

    public void setEmpty_slots(Integer empty_slots) {
        this.empty_slots = empty_slots;
    }

    public BikeStationExtra getExtra() {
        return extra;
    }

    public void setExtra(BikeStationExtra bikeStationExtra) {
        this.extra = bikeStationExtra;
    }

    public Integer getFree_bikes() {
        return free_bikes;
    }

    public void setFree_bikes(Integer free_bikes) {
        this.free_bikes = free_bikes;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
