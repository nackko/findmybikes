package com.ludoscity.findmybikes.data.network.citybik_es

import com.google.android.gms.maps.model.LatLng

/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
data class BikeNetworkLocation(val latitude: Double,
                               val longitude: Double,
                               val city: String) {
    val asLatLng: LatLng
        get() = LatLng(latitude, longitude)
}
