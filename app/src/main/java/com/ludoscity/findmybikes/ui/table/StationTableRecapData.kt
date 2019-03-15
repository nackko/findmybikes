package com.ludoscity.findmybikes.ui.table

import android.graphics.Typeface

data class StationTableRecapData(
        val name: String,
        val availabilityText: String,
        val isAvailabilityPaintStrikeThru: Boolean,
        val availabilityPaintTypeface: Typeface,
        val availabilityTextColorResId: Int
)