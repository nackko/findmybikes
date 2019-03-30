package com.ludoscity.findmybikes.ui.sheet

data class FavoriteSheetItemData(
        val itemBackgroundResId: Int,
        val affordanceHandleVisible: Boolean,
        val editFabShown: Boolean,
        val deleteFabShown: Boolean,
        val infoWidthPercent: Float,
        val nameText: String,
        val nameTypefaceStyle: Int,
        val favoriteId: String
)