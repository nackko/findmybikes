package com.ludoscity.findmybikes.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository

class FindMyBikesModelFactory(private val repository: FindMyBikesRepository,
                              private val app: Application) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        @Suppress("UNCHECKED_CAST")
        return FindMyBikesActivityViewModel(repository, app) as T
    }
}