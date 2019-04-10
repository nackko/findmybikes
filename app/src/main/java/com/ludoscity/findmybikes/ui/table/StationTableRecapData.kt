package com.ludoscity.findmybikes.ui.table

import android.graphics.Typeface
import android.text.Spanned

data class StationTableRecapData(
        val name: Spanned,
        val availabilityText: String,
        val isAvailabilityPaintStrikeThru: Boolean,
        val availabilityPaintTypeface: Typeface,
        val availabilityTextColorResId: Int
)