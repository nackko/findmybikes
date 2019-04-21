package com.ludoscity.findmybikes.ui.trip

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import java.text.NumberFormat

/**
 * Created by F8Full on 2019-03-20. This file is part of #findmybikes
 * Factory class to retrieve model for station map
 */
class TripFragmentModelFactory(private val application: Application,
                               private val userLoc: LiveData<LatLng>,
                               private val stationALatLng: LiveData<LatLng>,
                               private val stationBLatLng: LiveData<LatLng>,
                               private val finalDest: LiveData<LatLng>,
                               private val isFinalDestFavorite: LiveData<Boolean>,
                               private val numFormat: NumberFormat) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        @Suppress("UNCHECKED_CAST")
        return TripFragmentViewModel(
                application,
                userLoc,
                stationALatLng,
                stationBLatLng,
                finalDest,
                isFinalDestFavorite,
                numFormat) as T
    }
}