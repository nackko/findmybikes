package com.ludoscity.findmybikes.ui.map

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * Factory class to retrieve model for station map
 */
class MapFragmentModelFactory(private val repository: FindMyBikesRepository,
                              private val application: Application,
                              private val isLookingForBike: LiveData<Boolean>,
                              private val isDataOutOfDate: LiveData<Boolean>,
                              private val userLoc: LiveData<LatLng>,
                              private val stationA: LiveData<BikeStation>,
                              private val stationB: LiveData<BikeStation>,
                              private val finalDestinationLoc: LiveData<LatLng>,
                              private val isFinalDestinationFavorite: LiveData<Boolean>
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        return MapFragmentViewModel(
                repository,
                application,
                isLookingForBike,
                isDataOutOfDate,
                userLoc,
                stationA,
                stationB,
                finalDestinationLoc,
                isFinalDestinationFavorite
        ) as T
    }
}