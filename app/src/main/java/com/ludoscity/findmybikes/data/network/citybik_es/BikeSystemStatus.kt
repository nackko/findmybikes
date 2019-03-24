package com.ludoscity.findmybikes.data.network.citybik_es

import com.google.gson.annotations.SerializedName
import com.ludoscity.findmybikes.data.database.BikeStation

class BikeSystemStatus(@SerializedName("stations") var bikeStationList: ArrayList<BikeStation>?,
                       var id: String?)
