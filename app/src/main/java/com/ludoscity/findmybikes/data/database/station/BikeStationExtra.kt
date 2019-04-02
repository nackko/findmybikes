package com.ludoscity.findmybikes.data.database.station

import com.google.gson.annotations.SerializedName

data class BikeStationExtra(val locked: Boolean?, val name: String?, val uid: String?, val renting: Int?,
                            val returning: Int?, val status: String?, @SerializedName("last_updated") val lastUpdated: Long)
