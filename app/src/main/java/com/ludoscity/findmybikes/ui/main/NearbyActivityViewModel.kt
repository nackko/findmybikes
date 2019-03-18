package com.ludoscity.findmybikes.ui.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

//TODO: use AndroidViewModel instead of passing context
class NearbyActivityViewModel(repo: FindMyBikesRepository, app: Application) : AndroidViewModel(app) {
    private val favoriteFabShown = MutableLiveData<Boolean>()
    private val favoriteSheetShown = MutableLiveData<Boolean>()
    private val favoriteItemNameEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditFabShown = MutableLiveData<Boolean>()
    private val currentBikeSytemId = MutableLiveData<String>()

    private val nearestBikeAutoSelected = MutableLiveData<Boolean>()
    private val lastDataUpdateEpochTimestamp = MutableLiveData<Long>()
    private val currentUserLatLng = MutableLiveData<LatLng>()
    private val stationA = MutableLiveData<BikeStation>()
    private val stationB = MutableLiveData<BikeStation>()
    private val pickedFavorite = MutableLiveData<FavoriteEntityBase>()

    private val userLoc = MutableLiveData<LatLng>()
    private val finalDest = MutableLiveData<LatLng>()
    private val isFinalDestFav = MutableLiveData<Boolean>()

    private val appBarExpanded = MutableLiveData<Boolean>()

    //TODO: should that be maintained in repository or should repo update activity model ?
    private val dataOutOfDate = MutableLiveData<Boolean>()

    val userLocation: LiveData<LatLng>
        get() = userLoc

    val finalDestinationLatLng: LiveData<LatLng>
        get() = finalDest

    //TODO: Maybe use an enum instead. eSEARCH, eFAVORITE
    val isFinalDestinationFavorite: LiveData<Boolean>
        get() = isFinalDestFav

    private val lookingForBike = MutableLiveData<Boolean>()

    val isLookingForBike: LiveData<Boolean>
        get() = lookingForBike

    fun setSelectedTable(toSet: Boolean) {
        lookingForBike.value = toSet
    }

    private val isConnectivityAvailable = MutableLiveData<Boolean>()
    private val isLocationServiceAvailable = MutableLiveData<Boolean>()

    val isFavoriteFabShown: LiveData<Boolean>
        get() = favoriteFabShown
    val isFavoriteSheetShown: LiveData<Boolean>
        get() {

            return favoriteSheetShown
        }

    val isFavoriteSheetEditfabShown: LiveData<Boolean>
        get() {

            return favoriteSheetEditFabShown
        }

    val isFavoriteSheetItemNameEditInProgress: LiveData<Boolean>
        get() = favoriteItemNameEditInProgress

    val isFavoriteSheetEditInProgress: LiveData<Boolean>
        get() = favoriteSheetEditInProgress

    val isDataOutOfDate : LiveData<Boolean>
        get() = dataOutOfDate

    fun setDataOutOfDate(toSet: Boolean) {
        dataOutOfDate.value = toSet
    }

    fun setAppBarExpanded(toSet: Boolean) {
        appBarExpanded.value = toSet
    }

    fun setNearestBikeAutoselected(toSet: Boolean){
        nearestBikeAutoSelected.value = toSet
    }

    fun clearPickedFavorite(){
        pickedFavorite.value = null
    }

    fun postCurrentBikeSytemId(toSet: String) {
        currentBikeSytemId.postValue(toSet)
    }

    //TODO: revisit this to fix bug where you'd be asked to search for a destination after you switch network in a new city even though
    //you already have favorites
    fun setCurrentBikeSytemId(toSet: String) {
        currentBikeSytemId.value = toSet
    }


    fun getCurrentBikeSytemId(): LiveData<String> {
        return currentBikeSytemId
    }

    fun setStationA(toSet: BikeStation?) {
        stationA.value = toSet
    }

    fun getStationA(): LiveData<BikeStation> {
        return stationA
    }

    fun setStationB(toSet: BikeStation?) {
        stationB.value = toSet
    }
    fun getStationB(): LiveData<BikeStation>{
        return stationB
    }

    fun showFavoriteFab() {
        favoriteFabShown.value = true
    }

    fun showFavoriteSheet() {
        favoriteSheetShown.value = true
    }

    fun favoriteItemNameEditStart() {
        favoriteItemNameEditInProgress.value = true
    }

    fun favoriteSheetEditStart() {
        favoriteSheetEditInProgress.value = true
    }

    fun showFavoriteSheetEditFab() {
        favoriteSheetEditFabShown.value = true
    }

    fun hideFavoriteFab() {
        favoriteFabShown.value = false
    }

    fun hideFavoriteSheet() {
        favoriteSheetShown.value = false
    }

    fun favoriteItemNameEditStop() {
        favoriteItemNameEditInProgress.value = false
    }

    fun favoriteSheetEditStop() {
        favoriteSheetEditInProgress.value = false
    }

    fun hideFavoriteSheetEditFab() {
        favoriteSheetEditFabShown.value = false
    }

    fun isAppBarExpanded(): LiveData<Boolean> {
        return appBarExpanded
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //findMyBikesActivityViewModel
    private var locationCallback: LocationCallback
    private val repository : FindMyBikesRepository = repo
    val stationData: LiveData<List<BikeStation>>

    init {
        ///DEBUG
        //finalDest.value = LatLng(45.75725, 4.84974)//Lyon
        ///
        stationData = repo.getBikeSystemStationData(getApplication())

        //userLoc.value = LatLng(45.75725, 4.84974)//Lyon

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication() as Context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {

                var toEmit: LatLng? = null
                val userLat = locationResult?.locations?.getOrNull(0)?.latitude
                val userLng = locationResult?.locations?.getOrNull(0)?.longitude

                if (userLat != null && userLng != null) {
                    toEmit = LatLng(userLat, userLng)
                }

                Log.d(this::class.java.simpleName, "Emitting new Location !! : $toEmit")
                userLoc.value = toEmit
            }
        }

        val locationRequest = LocationRequest()
        locationRequest.interval = 2000 //TODO: make that flexible
        locationRequest.fastestInterval = 5000 //TODO: make that flexible
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, null)
        } catch (e: SecurityException) {
            Log.e(this::class.java.simpleName, "You need location permission !!")
        }

    }

    override fun onCleared() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication() as Context)

        //TODO: activity lifecycle methods should remove and re request to save battery
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onCleared()
    }

    //TODO: added for status display. Revisit when usage is clearer (splash screen and status bar)
    //TODO: add splashScreen back when clearer view of background processes by replacing all asynctasks
    /*enum class BackgroundState {
        STATE_IDLE_NOMINAL,
        STATE_IDLE_OFFLINE,
        STATE_IDLE_NETWORK_ERROR,
        STATE_NETWORK_DOWNLOAD, STATE_MAP_REFRESH
    }

    private val backgroundState = MutableLiveData<BackgroundState>()

    val currentBckState: LiveData<BackgroundState>
        get() = backgroundState*/

}
