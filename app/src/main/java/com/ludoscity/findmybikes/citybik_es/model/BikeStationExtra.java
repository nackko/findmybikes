package com.ludoscity.findmybikes.citybik_es.model;

import com.google.gson.annotations.SerializedName;

public class BikeStationExtra {
    private Boolean locked;
    @SerializedName("name")
    private String extraName;

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getExtraName() {
        return extraName;
    }

    public void setExtraName(String extraName) {
        this.extraName = extraName;
    }
}
