package com.ludoscity.findmybikes.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository

class SettingsActivityModelFactory(private val repository: FindMyBikesRepository,
                                   private val application: Application) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        @Suppress("UNCHECKED_CAST")
        return SettingsActivityViewModel(
                repository,
                application
        ) as T
    }
}
