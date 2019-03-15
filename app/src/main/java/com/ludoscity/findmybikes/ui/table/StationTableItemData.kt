package com.ludoscity.findmybikes.ui.table

import android.graphics.Typeface

data class StationTableItemData(
        val itemBackgroundResId: Int,
        val proximityText: String?, val proximityAlpha: Float,
        val name: String, val nameAlpha: Float,
        val availabilityText: String, val availabilityalpha: Float,
        val isAvailabilityPaintStrikeThru: Boolean,
        val availabilityPaintTypeface: Typeface,
        //TODO: rework station identification
        val locationHash: String
)