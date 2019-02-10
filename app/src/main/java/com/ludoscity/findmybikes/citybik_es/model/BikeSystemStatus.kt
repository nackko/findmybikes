package com.ludoscity.findmybikes.citybik_es.model

import com.google.gson.annotations.SerializedName

class BikeSystemStatus(@SerializedName("stations") var bikeStationList: ArrayList<BikeStation>?,
                       var id: String?)
