package com.ludoscity.findmybikes.ui.map

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.compat.Place
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.utils.asLatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * ViewModel for handling StationMapFragment data prep for UI and business logic
 */

class MapFragmentViewModel(repo: FindMyBikesRepository, application: Application,
                           val hasLocationPermission: LiveData<Boolean>,
                           val isLookingForBike: LiveData<Boolean>,
                           val isDataOutOfDate: LiveData<Boolean>,
                           private val userLoc: LiveData<Location>,
                           private val stationA: LiveData<BikeStation>,
                           private val stationB: LiveData<BikeStation>,
                           private val finalDestPlace: LiveData<Place>,
                           private val finalDestFavorite: LiveData<FavoriteEntityBase>)//,
    : AndroidViewModel(application) {

    private val finalDestLatLng = MutableLiveData<LatLng>()
    val finalDestinationLatLng: LiveData<LatLng>
        get() = finalDestLatLng

    private val finalDestTitle = MutableLiveData<String>()
    val finalDestinationTitle: LiveData<String>
        get() = finalDestTitle

    private val isFinalDestFavorite = MutableLiveData<Boolean>()
    val isFinalDestinationFavorite: LiveData<Boolean>
        get() = isFinalDestFavorite

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
    private val mapGfxData: MutableLiveData<List<StationMapGfx>> = MutableLiveData()

    private val scrollGesturesEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val mapGfxLiveData: LiveData<List<StationMapGfx>>
        get() = mapGfxData

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

    val isScrollGesturesEnabled: LiveData<Boolean>
        get() = scrollGesturesEnabled

    private val userLocationObserver: Observer<Location>
    private val stationAObserver: Observer<BikeStation>
    private val stationBObserver: Observer<BikeStation>
    private val isDataOutOfDateObserver: Observer<Boolean>
    private val isLookingForBikeObserver: Observer<Boolean>

    private val finalDestinationPlaceObserver: Observer<Place>
    private val finalDestinationFavoriteObserver: Observer<FavoriteEntityBase>


    private val bikeSystemAvailabilityDataSource: LiveData<List<BikeStation>> = repo.getBikeSystemStationData(getApplication())
    private val bikeSystemAvailabilityDataObserver: Observer<List<BikeStation>>

    init {

        bikeSystemAvailabilityDataObserver = Observer { newData ->

            //TODO: have better Room database update strategy
            //this is to protect against drop table strategy (led to inconsistency crashes in recyclerView)
            if (newData?.isNotEmpty() != false) {

                //TODO: set background state as map refresh for UI display. Shall we have a common service for all background processing ?
                coroutineScopeIO.launch {
                    val mapMarkersGfxData = ArrayList<StationMapGfx>()

                    //Do this in background, then post result to LiveData so map fragment can refresh itself
                    newData?.forEach { item ->
                        mapMarkersGfxData.add(StationMapGfx(isDataOutOfDate.value == true, item, isLookingForBike.value == true, getApplication()))
                    }

                    mapGfxData.postValue(mapMarkersGfxData)
                }
            }
        }
        bikeSystemAvailabilityDataSource.observeForever(bikeSystemAvailabilityDataObserver)

        isDataOutOfDateObserver = Observer {
        }

        isDataOutOfDate.observeForever(isDataOutOfDateObserver)

        isLookingForBikeObserver = Observer {

            scrollGesturesEnabled.value = it != true

            if (it == true) {
                //TODO: emit new camera update refactor in one method
                val userLocation = userLoc.value
                val stationALoc = stationA.value?.location

                val latLngBoundbuilder = LatLngBounds.builder()

                if (stationALoc != null) {

                    mapPaddingRight.value = 0

                    latLngBoundbuilder.include(LatLng(stationALoc.latitude, stationALoc.longitude))

                    hideMapItems()

                    if (userLocation != null) {
                        latLngBoundbuilder.include(userLocation.asLatLng())

                        val camPaddingResId = when {
                            finalDestPlace.value != null || finalDestFavorite.value != null -> R.dimen.camera_search_infowindow_padding
                            stationB.value == null -> R.dimen.camera_fab_padding
                            else -> R.dimen.camera_ab_pin_padding
                        }

                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(camPaddingResId).toInt())
                    } else {
                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(LatLng(stationALoc.latitude, stationALoc.longitude), 15.0f)
                    }
                } else if (userLocation != null) {
                    hideMapItems()
                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(userLocation.asLatLng(), 15.0f)
                } else {
                    //TODO: no user location
                }
            } else {
                //B table selected
                val stationB = stationB.value
                if (stationB != null) {
                    hideMapItems()

                    if (finalDestPlace.value == null && finalDestFavorite.value == null) {
                        mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                                R.dimen.map_fab_padding).toInt()

                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                LatLng(stationB.latitude, stationB.longitude), 15.0f)

                    } else {

                        mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                                R.dimen.trip_details_widget_width).toInt()
                        mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                                R.dimen.map_infowindow_padding).toInt()

                        coroutineScopeIO.launch {
                            val latLngBoundBuilder = LatLngBounds.builder()
                            var destIsStation = false

                            latLngBoundBuilder.include(stationB.location)

                            finalDestPlace.value?.let { place ->
                                latLngBoundBuilder.include(place.latLng)
                            }
                            finalDestFavorite.value?.let { favorite ->

                                latLngBoundBuilder.include(favorite.getLocation(getApplication()))

                                if (favorite.getLocation(getApplication()).latitude ==
                                        stationB.location.latitude &&
                                        favorite.getLocation(getApplication()).longitude ==
                                        stationB.location.longitude) {
                                    destIsStation = true
                                }
                            }

                            if (!destIsStation)
                                camAnimTarget.postValue(CameraUpdateFactory.newLatLngBounds(latLngBoundBuilder.build(),
                                        getApplication<Application>().resources.getDimension(R.dimen.camera_ab_pin_padding).toInt()))
                            else
                                camAnimTarget.postValue(CameraUpdateFactory.newLatLngZoom(
                                        LatLng(stationB.location.latitude, stationB.location.longitude), 15.0f))
                        }
                    }
                } else {
                    val stationA = stationA.value
                    if (stationA != null) {

                        hideMapItems()

                        mapPaddingLeft.value = 0
                        mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                                R.dimen.map_infowindow_padding).toInt()

                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                LatLng(stationA.latitude, stationA.longitude), 13.75f)
                    }
                }
            }
        }

        isLookingForBike.observeForever(isLookingForBikeObserver)

        userLocationObserver = Observer {
            //it prepares an updated cameraUpdate for map animation

            val latLngBoundbuilder = LatLngBounds.builder()


            if (it != null) {
                if (isLookingForBike.value == true) {
                    val stationALoc = stationA.value?.location
                    if (stationALoc != null) {

                        latLngBoundbuilder.include(stationALoc).include(it.asLatLng())
                        val camPaddingResId = when {
                            finalDestPlace.value != null || finalDestFavorite.value != null -> R.dimen.camera_search_infowindow_padding
                            stationB.value == null -> R.dimen.camera_fab_padding
                            else -> R.dimen.camera_ab_pin_padding
                        }

                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(camPaddingResId).toInt())
                    } else {
                        hideMapItems()
                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(it.asLatLng(), 15.0f)
                    }
                }

            } else {
                //TODO: implement something when user loc is set to null
            }
        }

        userLoc.observeForever(userLocationObserver)

        stationAObserver = Observer { stationA ->

            //looking for bike
            if (isLookingForBike.value != false) {
                val userLocation = userLoc.value

                val latLngBoundbuilder = LatLngBounds.builder()

                if (stationA != null) {
                    latLngBoundbuilder.include(stationA.location)

                    hideMapItems()

                    if (userLocation != null) {
                        latLngBoundbuilder.include(userLocation.asLatLng())

                        val camPaddingResId = when {
                            finalDestPlace.value != null || finalDestFavorite.value != null -> R.dimen.camera_search_infowindow_padding
                            stationB.value == null -> R.dimen.camera_fab_padding
                            else -> R.dimen.camera_ab_pin_padding
                        }

                        camAnimTarget.value = CameraUpdateFactory.newLatLngBounds(latLngBoundbuilder.build(),
                                getApplication<Application>().resources.getDimension(camPaddingResId).toInt())
                    } else {
                        mapPaddingRight.value = 0
                        mapPaddingLeft.value = 0

                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(stationA.location, 15.0f)
                    }
                } else if (userLocation != null) {
                    hideMapItems()
                    mapPaddingRight.value = 0
                    mapPaddingLeft.value = 0
                    camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(userLocation.asLatLng(), 15.0f)
                } else {
                    //TODO: no user location. Go to Montréal ?
                }
            }
        }

        stationA.observeForever(stationAObserver)

        stationBObserver = Observer {

            if (it != null) {

                if (isLookingForBike.value == false) {
                    //looking for a dock, center zoom on B station
                    hideMapItems()

                    mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                            R.dimen.trip_details_widget_width).toInt()

                    //TODO: this block is same code as one block in is isLookingForBikeObserver
                    //if (finalDestinationLatLng.value == null) { <-- look for that in isLookingForBikeObserver
                    if (finalDestPlace.value == null && finalDestFavorite.value == null) {

                        Log.d(MapFragmentViewModel::class.java.simpleName, "no final destination, zooming on B")
                        mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                                R.dimen.trip_details_widget_width).toInt()
                        mapPaddingRight.value = getApplication<Application>().resources.getDimension(
                                R.dimen.map_fab_padding).toInt()

                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude), 15.0f)

                    } else {

                        coroutineScopeIO.launch {
                            val latLngBoundBuilder = LatLngBounds.builder()
                            var destIsStation = false

                            latLngBoundBuilder.include(it.location)
                            finalDestPlace.value?.let { place ->
                                latLngBoundBuilder.include(place.latLng)
                            }
                            finalDestFavorite.value?.let { favorite ->

                                latLngBoundBuilder.include(favorite.getLocation(getApplication()))

                                if (favorite.getLocation(getApplication()).latitude ==
                                        stationB.value?.location?.latitude &&
                                        favorite.getLocation(getApplication()).longitude ==
                                        stationB.value?.location?.longitude) {
                                    destIsStation = true
                                }

                            }

                            mapPaddingRight.postValue(getApplication<Application>().resources.getDimension(
                                    R.dimen.map_infowindow_padding).toInt())

                            if (!destIsStation)
                                camAnimTarget.postValue(CameraUpdateFactory.newLatLngBounds(latLngBoundBuilder.build(),
                                        getApplication<Application>().resources.getDimension(R.dimen.camera_search_infowindow_padding).toInt()))
                            else
                                camAnimTarget.postValue(CameraUpdateFactory.newLatLngZoom(
                                        LatLng(stationB.value?.location?.latitude
                                                ?: 0.0, stationB.value?.location?.longitude
                                                ?: 0.0), 15.0f))
                        }
                    }

                } else {
                    mapPaddingLeft.value = getApplication<Application>().resources.getDimension(
                            R.dimen.trip_details_widget_width).toInt()
                    mapPaddingRight.value = 0

                }
            } else {
                if (isLookingForBike.value == false) {
                    mapPaddingLeft.value = 0
                    mapPaddingRight.value = 0
                    val statA = stationA.value
                    if (statA != null) {
                        hideMapItems()

                        camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                statA.location, 13.0f)
                    } else {
                        val usLoc = userLoc.value
                        if (usLoc != null) {

                            hideMapItems()
                            camAnimTarget.value = CameraUpdateFactory.newLatLngZoom(
                                    usLoc.asLatLng(), 13.0f)
                        }
                    }
                } else {
                    mapPaddingLeft.value = 0
                    mapPaddingRight.value = 0

                }
            }

        }

        stationB.observeForever(stationBObserver)

        finalDestinationPlaceObserver = Observer {
            if (it != null) {
                isFinalDestFavorite.value = false
                finalDestLatLng.value = it.latLng
                finalDestTitle.value = it.name.toString()
            } else if (finalDestFavorite.value == null) {
                //TODO: have explicit visibility flag final dest pin visibility
                finalDestLatLng.value = null
            }
        }

        finalDestPlace.observeForever(finalDestinationPlaceObserver)

        finalDestinationFavoriteObserver = Observer {
            if (it != null) {
                coroutineScopeIO.launch {
                    isFinalDestFavorite.postValue(true)
                    finalDestLatLng.postValue(it.getLocation(getApplication()))
                    finalDestTitle.postValue(it.displayName)
                }
            } else {
                //TODO: have explicit visibility flag final dest pin visibility
                finalDestLatLng.value = null
            }
        }

        finalDestFavorite.observeForever(finalDestinationFavoriteObserver)
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
