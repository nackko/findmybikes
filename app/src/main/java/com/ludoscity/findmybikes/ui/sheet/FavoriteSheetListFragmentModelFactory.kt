package com.ludoscity.findmybikes.ui.sheet

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem

class FavoriteSheetListFragmentModelFactory(private val repo: FindMyBikesRepository,
                                            private val app: Application,
                                            private val isSheetEditInProgress: LiveData<Boolean>,
                                            private val curBikeSystemDataSource: LiveData<BikeSystem>) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return FavoriteSheetListViewModel(repo, isSheetEditInProgress, curBikeSystemDataSource, app) as T
    }
}