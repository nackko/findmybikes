package com.ludoscity.findmybikes.ui.table

import android.graphics.Typeface
import android.text.Spanned

data class StationTableItemData(
        val itemBackgroundResId: Int,
        val durationTextVisibility: Int,
        val durationText: String, val proximityAlpha: Float,
        val name: Spanned, val nameAlpha: Float,
        val availabilityText: String, val availabilityAlpha: Float,
        val isAvailabilityPaintStrikeThru: Boolean,
        val availabilityPaintTypeface: Typeface,
        //TODO: rework station identification
        val locationHash: String
)