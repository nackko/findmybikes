package com.ludoscity.findmybikes.data.network.citybik_es;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
public class BikeNetworkLocation {
    private Double latitude;
    private Double longitude;
    private String city;

    public LatLng getAsLatLng(){ return new LatLng(latitude, longitude); }

    public void setLatitude(Double toSet){
        this.latitude = toSet;
    }

    public void setLongitude(Double toSet){
        this.longitude = toSet;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
