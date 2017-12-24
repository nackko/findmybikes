package com.ludoscity.findmybikes.datamodel;

import android.arch.persistence.room.Entity;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to thandle the concept of a Favorite place as searched through the Google widget
 */

@Entity
public class FavoriteEntityPlace extends FavoriteEntityBase {

    //To avoid collisions with citybik.es API ids
    public static final String PLACE_ID_PREFIX = "PLACE_";

    private LatLng location;
    private String attributions;

    public FavoriteEntityPlace(){
        super();
    }


    public FavoriteEntityPlace(String id, String name, LatLng location, String attributions){
        super(PLACE_ID_PREFIX + id,name);
        this.location = location;
        this.attributions = attributions;
    }


    @Override
    public String getAttributions() {
        return attributions;
    }

    @Override
    public boolean isDisplayNameDefault() {
        return false;
    }

    @Override
    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setAttributions(String attributions) {
        this.attributions = attributions;
    }
}
