package com.ludoscity.findmybikes.data.database

import android.arch.persistence.room.Entity

import com.google.android.gms.maps.model.LatLng

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to handle the concept of a favorite station and save it using Room
 */
@Entity
class FavoriteEntityStation(id: String, defaultName: String, bikeSystemId: String) : FavoriteEntityBase(
        id = id,
        defaultName = defaultName,
        uiIndex = -1,
        bikeSystemId = bikeSystemId) {

    override val attributions: String
        get() = ""

    override//BikeStationRepository.getInstance().getStation(getId()).getLocation();
    val location: LatLng
        get() = LatLng(0.0, 0.0)
}
