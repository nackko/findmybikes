package com.ludoscity.findmybikes.datamodel;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to handle the concept of Favorite and save it using Room
 */

@Entity
public abstract class FavoriteEntityBase {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "display_name")
    private String displayName;

    FavoriteEntityBase(){
        id = "NOT_VALID";
    }


    FavoriteEntityBase(@NonNull String id, String displayName)
    {
        this.id = id;
        this.displayName = displayName;
    }

    public abstract CharSequence getAttributions();
    public abstract boolean isDisplayNameDefault();
    public abstract LatLng getLocation();

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
