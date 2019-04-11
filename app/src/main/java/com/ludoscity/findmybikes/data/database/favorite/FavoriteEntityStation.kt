package com.ludoscity.findmybikes.data.database.favorite

import android.arch.persistence.room.Entity
import android.content.Context

import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.utils.InjectorUtils

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 * A data model class to handle the concept of a favorite station and save it using Room
 */
@Entity
class FavoriteEntityStation(id: String, uiIndex: Int, defaultName: String, bikeSystemId: String) : FavoriteEntityBase(
        id = id,
        defaultName = defaultName,
        uiIndex = uiIndex,
        bikeSystemId = bikeSystemId) {

    override val attributions: String
        get() = ""

    override
    fun getLocation(ctx: Context): LatLng {
        return InjectorUtils.provideRepository(ctx).getStationForId(id).location
    }
}
