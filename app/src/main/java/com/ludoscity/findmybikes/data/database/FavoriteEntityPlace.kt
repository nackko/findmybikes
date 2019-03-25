package com.ludoscity.findmybikes.data.database

import android.arch.persistence.room.Entity

import com.google.android.gms.maps.model.LatLng

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to thandle the concept of a Favorite place as searched through the Google widget
 */

@Entity
class FavoriteEntityPlace(id: String, defaultName: String, bikeSystemId: String, //Room need those 2 accessors
                          override var location: LatLng, override var attributions: String) : FavoriteEntityBase(id = id,
        defaultName = defaultName,
        uiIndex = -1,
        bikeSystemId = bikeSystemId) {

    init {

        //finalizing construction
        if (!this.id.contains(PLACE_ID_PREFIX))
            this.id = PLACE_ID_PREFIX + id
    }

    companion object {

        //To avoid collisions with citybik.es API ids
        const val PLACE_ID_PREFIX = "PLACE_"
    }
}
