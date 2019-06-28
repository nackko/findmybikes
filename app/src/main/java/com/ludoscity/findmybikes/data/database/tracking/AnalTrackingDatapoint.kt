package com.ludoscity.findmybikes.data.database.tracking

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import com.jaredrummler.android.device.DeviceName
import com.ludoscity.findmybikes.BuildConfig
import java.util.*

/**
 * Created by F8Full on 2019-06-24. This file is part of #findmybikes
 * A data model class to handle analtracking datapoints, extends base with analytics data
 */
@Entity
data class AnalTrackingDatapoint(
        @SerializedName("timestamp_epoch")
        @ColumnInfo(name = "timestamp_epoch")
        var timestampEpoch: Long = System.currentTimeMillis(),
        var description: String,
        @Transient val ctx: Context? = null //Room ignores it because no ColumnInfo
        //Gson ignores it
) : BaseTrackingDatapoint(timestampEpoch, "_ANALYTICS") {
    @SerializedName("app_version")
    @ColumnInfo(name = "app_version")
    var appVersion: String =
            BuildConfig.VERSION_NAME
    @SerializedName("os_version")
    @ColumnInfo(name = "os_version")
    var osVersion: String =
            "android_api_level ${android.os.Build.VERSION.SDK_INT}"
    @SerializedName("device_model")
    @ColumnInfo(name = "device_model")
    var deviceModel: String =
            DeviceName.getDeviceName()
    var language: String = Locale.getDefault().language
    var country: String = Locale.getDefault().country
    @SerializedName("battery_level")
    @ColumnInfo(name = "battery_level")
    var batteryLevel: Int?

    //Required by Room
    constructor() : this(description = "usable constructor for Room", ctx = null)

    init {

        val batteryStatusIntent: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            ctx?.registerReceiver(null, it)
        }

        val batteryPercent: Float? = batteryStatusIntent?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }

        //null if no context was provided
        batteryLevel = (batteryPercent?.times(100))?.toInt()
    }
}