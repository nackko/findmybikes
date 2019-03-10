package com.ludoscity.findmybikes.ui.table

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * ViewModel for handling StationMapFragment data prep for UI and business logic
 */

//TODO: try removing dependency on context by having a SettingsRepository
//Context is used to retrieve data from "dbHelper" in SharedPref file

//Model should prep and expose data for page to display

class TableFragmentViewModel(repo: FindMyBikesRepository, application: Application) : AndroidViewModel(application) {

    //private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val repository: FindMyBikesRepository = repo

    private lateinit var comparator: Comparator<BikeStation>

    //TODO: move to activity model (mpa needs it too)
    private val dataOutdated: MutableLiveData<Boolean>

    val isDataOutOfDate: LiveData<Boolean>
        get() = dataOutdated

    //val mapGfxData = LiveData<List<StationMapGfx>>


    init {

        dataOutdated = MutableLiveData<Boolean>()

        repo.getBikeSystemStationData(getApplication()).observeForever { newData ->


        }
    }
}
