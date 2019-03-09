package com.ludoscity.findmybikes.ui.map

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.ludoscity.findmybikes.StationMapGfx
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * ViewModel for handling StationMapFragment data prep for UI and business logic
 */

class MapFragmentViewModel(repo: FindMyBikesRepository, application: Application) : AndroidViewModel(application) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val repository: FindMyBikesRepository = repo
    private val mapGfxData: MutableLiveData<List<StationMapGfx>>

    val mapGfxLiveData: LiveData<List<StationMapGfx>>
        get() = mapGfxData

    //val mapGfxData = LiveData<List<StationMapGfx>>


    init {

        mapGfxData = MutableLiveData<List<StationMapGfx>>()



        repo.getBikeSystemStationData(getApplication()).observeForever { newData ->

            //TODO: set background state as map refresh for UI display. Shall we have a common service for all background processing ?
            coroutineScopeIO.launch {
                val mapMarkersGfxData = ArrayList<StationMapGfx>()

                //Do this in backgournd, then post result to LiveData so map fragment can refresh itself
                newData?.forEach { item ->
                    mapMarkersGfxData.add(StationMapGfx(false, item, true, getApplication()))
                }

                mapGfxData.postValue(mapMarkersGfxData)
            }
        }
    }
}
