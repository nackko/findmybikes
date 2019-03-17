package com.ludoscity.findmybikes.ui.map

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * ViewModel for handling StationMapFragment data prep for UI and business logic
 */

class MapFragmentViewModel(repo: FindMyBikesRepository, application: Application,
                           val isLookingForBike: LiveData<Boolean>,
                           val isDataOutOfDate: LiveData<Boolean>,
                           private val userLoc: LiveData<LatLng>,
                           private val stationA: LiveData<BikeStation>,
                           private val stationB: LiveData<BikeStation>,
                           private val destinationLoc: LiveData<LatLng>
) : AndroidViewModel(application) {

    val cameraAnimationTarget: LiveData<CameraUpdate>
        get() = camAnimTarget

    val showMapItems: LiveData<Boolean>
        get() = mapItemsVisible

    fun showMapItems() {
        mapItemsVisible.value = true
    }

    private fun hideMapItems() {
        mapItemsVisible.value = false
    }

    val mapPaddingLeftPx: LiveData<Int>
        get() = mapPaddingLeft

    val mapPaddingRightPx: LiveData<Int>
        get() = mapPaddingRight

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val camAnimTarget: MutableLiveData<CameraUpdate> = MutableLiveData()

    private val mapItemsVisible: MutableLiveData<Boolean> = MutableLiveData()

    private val mapPaddingLeft: MutableLiveData<Int> = MutableLiveData()
    private val mapPaddingRight: MutableLiveData<Int> = MutableLiveData()

    private val repository: FindMyBikesRepository = repo
    private val mapGfxData: MutableLiveData<List<StationMapGfx>>

    val mapGfxLiveData: LiveData<List<StationMapGfx>>
        get() = mapGfxData //TODO: always prep marker data for A and B and switch visibility

    val lastClickedWhileLookingForBike: LiveData<BikeStation>
        get() = lastClickedForBikeMutable
    private val lastClickedForBikeMutable: MutableLiveData<BikeStation> = MutableLiveData()

    val lastClickedWhileLookingForDock: LiveData<BikeStation>
        get() = lastClickedForDockMutable
    private val lastClickedForDockMutable: MutableLiveData<BikeStation> = MutableLiveData()

    fun setLastClickedStationById(stationId: String?) {
        val stationList = bikeSystemAvailabilityDataSource.value ?: emptyList()

        if (isLookingForBike.value == true)
            lastClickedForBikeMutable.value = stationList.find {
                it.locationHash == stationId
            }
        else
            lastClickedForDockMutable.value = stationList.find {
                it.locationHash == stationId
            }

    }

    private val userLocationObserver: android.arch.lifecycle.Observer<LatLng>
    private val stationAObserver: android.arch.lifecycle.Observer<BikeStation>
    private val stationBObserver: android.arch.lifecycle.Observer<BikeStation>
    private val isDataOutOfDateObserver: android.arch.lifecycle.Observer<Boolean>
    private val isLookingForBikeObserver: Observer<Boolean>


    private val bikeSystemAvailabilityDataSource: LiveData<List<BikeStation>>
    private val bikeSystemAvailabilityDataObserver: android.arch.lifecycle.Observer<List<BikeStation>>

    init {

        mapGfxData = MutableLiveData<List<StationMapGfx>>()


        bikeSystemAvailabilityDataSource = repo.getBikeSystemStationData(getApplication())

        bikeSystemAvailabilityDataObserver = android.arch.lifecycle.Observer { newData ->

            //TODO: set background state as map refresh for UI display. Shall we have a common service for all background processing ?
            computeAndEmitMarkerData(newData, isDataOutOfDate.value == true, isLookingForBike.value == true)

        }
        bikeSystemAvailabilityDataSource.observeForever(bikeSystemAvailabilityDataObserver)

        isDataOutOfDateObserver = android.arch.lifecycle.Observer {

            coroutineScopeIO.launch {
                val mapMarkersGfxData = ArrayList<StationMapGfx>()

                //Do this in backgournd, then post result to LiveData so map fragment can refresh itself
                bikeSystemAvailabilityDataSource.value?.forEach { item ->
                    mapMarkersGfxData.add(StationMapGfx(it == true, item, isLookingForBike.value == true, getApplication()))
                }

                mapGfxData.postValue(mapMarkersGfxData)

            }
        }

        isDataOutOfDate.observeForever(isDataOutOfDateObserver)

        isLookingForBikeObserver = Observer {

            if (it == true) {
                //TODO: emit new camera update refactor in one method
                val userLocation = userLoc.value
                val stationALoc = stationA.value?.location

                val latLngBoundbuilder = LatLngBounds.builder()

                if (stationALoc != null) {

                    latLngBoundbuilder.include(LatLng(stationALoc.latitude, stationALoc.longitude))

                    hideMapItems()

                    if (userLocation != null) {
                        latLngBoundbuilder.include(userLocation)


                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(R.dimen.camera_fab_padding).toInt())
                    } else {
                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(LatLng(stationALoc.latitude, stationALoc.longitude), 15.0f)
                    }
                } else if (userLocation != null) {
                    hideMapItems()
                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f)
                } else {
                    //TODO: no user location
                }
            } else {
                //B table selected
                val stationB = stationB.value
                if (stationB != null)
                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                            LatLng(stationB.latitude, stationB.longitude), 15.0f)
                else {
                    val stationA = stationA.value
                    if (stationA != null)
                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                LatLng(stationA.latitude, stationA.longitude), 13.75f)
                }

            }



            coroutineScopeIO.launch {
                val mapMarkersGfxData = ArrayList<StationMapGfx>()

                //Do this in backgournd, then post result to LiveData so map fragment can refresh itself
                bikeSystemAvailabilityDataSource.value?.forEach { item ->
                    mapMarkersGfxData.add(StationMapGfx(isDataOutOfDate.value == true, item, it == true, getApplication()))
                }

                mapGfxData.postValue(mapMarkersGfxData)

            }
        }

        isLookingForBike.observeForever(isLookingForBikeObserver)

        userLocationObserver = android.arch.lifecycle.Observer {
            //it prepares an updated cameraUpdate for map animation

            val latLngBoundbuilder = LatLngBounds.builder()


            if (it != null) {
                if (isLookingForBike.value == true) {
                    val stationALoc = stationA.value?.location
                    if (stationALoc != null) {

                        latLngBoundbuilder.include(stationALoc).include(it)

                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(R.dimen.camera_fab_padding).toInt())
                    }
                }

            } else {
                //TODO: implement something when user loc is set to null
            }
        }

        userLoc.observeForever(userLocationObserver)

        stationAObserver = android.arch.lifecycle.Observer {

            //looking for bike
            if (isLookingForBike.value != false) {
                val userLocation = userLoc.value

                val latLngBoundbuilder = LatLngBounds.builder()

                if (it != null) {
                    latLngBoundbuilder.include(it.location)

                    hideMapItems()

                    if (userLocation != null) {
                        latLngBoundbuilder.include(userLocation)


                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(R.dimen.camera_fab_padding).toInt())
                    } else {
                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(it.location, 15.0f)
                    }
                } else if (userLocation != null) {
                    hideMapItems()
                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f)
                } else {
                    //TODO: no user location
                }
            }
        }

        stationA.observeForever(stationAObserver)

        stationBObserver = android.arch.lifecycle.Observer {

            if (it != null) {

                if (isLookingForBike.value == false) {
                    //looking for a dock, center zoom on B station
                    hideMapItems()

                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude), 15.0f)

                    mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                            R.dimen.trip_details_widget_width).toInt()
                    mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                            R.dimen.camera_fab_padding).toInt()
                } else {
                    mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                            R.dimen.trip_details_widget_width).toInt()
                    mapPaddingRight.value = 0

                }
            } else {
                if (isLookingForBike.value == false) {
                    mapPaddingLeft.value = 0
                    mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                            R.dimen.camera_fab_padding).toInt()

                } else {
                    mapPaddingLeft.value = 0
                    mapPaddingRight.value = 0

                }
            }

        }

        stationB.observeForever(stationBObserver)
    }

    private fun computeAndEmitMarkerData(toCompute: List<BikeStation>?,
                                         isDataOutOfDate: Boolean,
                                         isLookingForBike: Boolean) {
        coroutineScopeIO.launch {
            val mapMarkersGfxData = ArrayList<StationMapGfx>()

            //Do this in backgournd, then post result to LiveData so map fragment can refresh itself
            toCompute?.forEach { item ->
                mapMarkersGfxData.add(StationMapGfx(isDataOutOfDate, item, isLookingForBike, getApplication()))
            }

            mapGfxData.postValue(mapMarkersGfxData)
        }
    }

    override fun onCleared() {


        userLoc.removeObserver(userLocationObserver)
        stationA.removeObserver(stationAObserver)
        stationB.removeObserver(stationBObserver)
        isDataOutOfDate.removeObserver(isDataOutOfDateObserver)
        isLookingForBike.removeObserver(isLookingForBikeObserver)
        bikeSystemAvailabilityDataSource.removeObserver(bikeSystemAvailabilityDataObserver)

        super.onCleared()
    }
}
