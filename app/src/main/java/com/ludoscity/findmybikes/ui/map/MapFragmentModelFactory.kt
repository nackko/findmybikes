package com.ludoscity.findmybikes.ui.map

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * Factory class to retrieve model for station map
 */
class MapFragmentModelFactory(private val repository: FindMyBikesRepository,
                              private val application: Application) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        return MapFragmentViewModel(repository, application) as T
    }
}