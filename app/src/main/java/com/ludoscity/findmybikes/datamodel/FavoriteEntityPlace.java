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

    public FavoriteEntityPlace(String id, String defaultName, String bikeSystemId, LatLng location, String attributions){
        super(id, defaultName, -1, bikeSystemId);

        if (!getId().contains(PLACE_ID_PREFIX))
            setId(PLACE_ID_PREFIX + id);

        this.location = location;
        this.attributions = attributions;
    }


    @Override
    public String getAttributions() {
        return attributions;
    }

    @Override
    public LatLng getLocation() {
        return location;
    }

    //Room need those 2 accessors
    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setAttributions(String attributions) {
        this.attributions = attributions;
    }
}
