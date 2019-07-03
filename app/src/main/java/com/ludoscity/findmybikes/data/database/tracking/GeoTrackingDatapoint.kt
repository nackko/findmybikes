package com.ludoscity.findmybikes.data.database.tracking

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

/**
 * Created by F8Full on 2019-06-24. This file is part of #findmybikes
 * A data model class to handle geotracking datapoints, extends base with geographic data
 */
@Entity
data class GeoTrackingDatapoint(
        @SerializedName("timestamp_epoch")
        @ColumnInfo(name = "timestamp_epoch")
        val timestampEpoch: Long = System.currentTimeMillis(),
        val altitude: Double?,
        @SerializedName("accuracy_horizontal_meters") @ColumnInfo(name = "accuracy_horizontal_meters")
        val accuracyHorizontalMeters: Float,
        @SerializedName("accuracy_vertical_meters") @ColumnInfo(name = "accuracy_vertical_meters")
        val accuracyVerticalMeters: Float?,
        val latitude: Double,
        val longitude: Double
) : BaseTrackingDatapoint(timestampEpoch, "_GEOLOCATION")