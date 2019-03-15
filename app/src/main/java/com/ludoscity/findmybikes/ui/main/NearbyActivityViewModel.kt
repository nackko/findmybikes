package com.ludoscity.findmybikes.ui.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context

import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

//TODO: use AndroidViewModel instead of passing context
class NearbyActivityViewModel(repo: FindMyBikesRepository, ctx: Context) : ViewModel() {
    private val favoriteFabShown = MutableLiveData<Boolean>()
    private val favoriteSheetShown = MutableLiveData<Boolean>()
    private val favoriteItemNameEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditFabShown = MutableLiveData<Boolean>()
    private val currentBikeSytemId = MutableLiveData<String>()

    private val lookingForBikes = MutableLiveData<Boolean>()
    private val nearestBikeAutoSelected = MutableLiveData<Boolean>()
    private val lastDataUpdateEpochTimestamp = MutableLiveData<Long>()
    private val currentUserLatLng = MutableLiveData<LatLng>()
    private val stationA = MutableLiveData<BikeStation>()
    private val stationB = MutableLiveData<BikeStation>()
    private val pickedFavorite = MutableLiveData<FavoriteEntityBase>()

    private val appBarExpanded = MutableLiveData<Boolean>()

    //TODO: should that be maintained in repository or should repo update activity model ?
    private val dataOutOfDate = MutableLiveData<Boolean>()


    //TODO: declare enum type
    private val selectedTab = MutableLiveData<Int>()

    private val isConnectivityAvailable = MutableLiveData<Boolean>()
    private val isLocationServiceAvailable = MutableLiveData<Boolean>()

    val isFavoriteFabShown: LiveData<Boolean>
        get() = favoriteFabShown
    val isFavoriteSheetShown: LiveData<Boolean>
        get() {

            return favoriteSheetShown
        }

    val isLookingForBikes: LiveData<Boolean>
        get() = lookingForBikes

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
    private val repository : FindMyBikesRepository = repo
    val stationData: LiveData<List<BikeStation>>

    init {
        stationData = repo.getBikeSystemStationData(ctx)
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
