package com.ludoscity.findmybikes.citybik_es.model;

public class BikeStationExtra {
    private Boolean locked;
    private String name;

    public BikeStationExtra(boolean locked, String name) {
        this.locked = locked;
        this.name = name;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
