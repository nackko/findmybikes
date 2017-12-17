package com.ludoscity.findmybikes.citybik_es.model;

import com.google.gson.annotations.SerializedName;

public class BikeStationExtra {
    private Boolean locked;
    @SerializedName("name")
    private String extra_name;

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getExtra_name() {
        return extra_name;
    }

    public void setExtra_name(String extra_name) {
        this.extra_name = extra_name;
    }
}
