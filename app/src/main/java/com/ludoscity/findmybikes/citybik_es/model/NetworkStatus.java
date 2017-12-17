package com.ludoscity.findmybikes.citybik_es.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class NetworkStatus {
    @SerializedName("stations")
    private ArrayList<BikeStation> bikeStationList;

    public ArrayList<BikeStation> getBikeStationList() {
        return bikeStationList;
    }

    public void setBikeStationList(ArrayList<BikeStation> bikeStationList) {
        this.bikeStationList = bikeStationList;
    }
}
