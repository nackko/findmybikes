package com.ludoscity.findmybikes.data.network.citybik_es

import com.google.gson.annotations.SerializedName
import com.ludoscity.findmybikes.data.database.station.BikeStation

class BikeSystemStatus(@SerializedName("stations") var bikeStationList: List<BikeStation>?,
                       var id: String)
