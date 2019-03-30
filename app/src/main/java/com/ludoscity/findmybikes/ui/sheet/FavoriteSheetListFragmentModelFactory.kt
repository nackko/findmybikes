package com.ludoscity.findmybikes.ui.sheet

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository

class FavoriteSheetListFragmentModelFactory(private val repo: FindMyBikesRepository,
                                            private val app: Application,
                                            private val isSheetEditInProgress: LiveData<Boolean>) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return FavoriteSheetListViewModel(repo, isSheetEditInProgress, app) as T
    }
}