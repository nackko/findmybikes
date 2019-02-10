package com.ludoscity.findmybikes.ui.main

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import com.ludoscity.findmybikes.data.FindMyBikesRepository

class FindMyBikesModelFactory(private val repository: FindMyBikesRepository,
                           private val ctx: Context) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        return NearbyActivityViewModel(repository, ctx) as T
    }
}