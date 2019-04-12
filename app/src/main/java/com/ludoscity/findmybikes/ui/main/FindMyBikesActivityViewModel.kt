package com.ludoscity.findmybikes.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.SharedPrefHelper
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityPlace
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityStation
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.concurrent.timer

/**
 * Created by F8Full on 2017-12-24. This file is part of #findmybikes
 * ViewModel for handling favoritelistFragment data prep for UI and business logic
 */

class FindMyBikesActivityViewModel(private val repo: FindMyBikesRepository, app: Application) : AndroidViewModel(app) {

    private val locationPermissionGranted = MutableLiveData<Boolean>()

    private val favoriteFabShown = MutableLiveData<Boolean>()
    private val tripDetailsWidgetShown = MutableLiveData<Boolean>()
    private val favoritePickerFabShown = MutableLiveData<Boolean>()
    private val searchFabShown = MutableLiveData<Boolean>()
    private val searchFabBckgTintListColorResId = MutableLiveData<Int>()
    private val directionsToStationAFabShown = MutableLiveData<Boolean>()
    private val clearBSelectionFabShown = MutableLiveData<Boolean>()
    private val favoriteSheetShown = MutableLiveData<Boolean>()
    private val favoriteItemNameEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditFabShown = MutableLiveData<Boolean>()

    private val myCurBikeSystem = MutableLiveData<BikeSystem>()
    private var lastBikeSystemId: String? = null

    val curBikeSystem: LiveData<BikeSystem>
        get() = myCurBikeSystem

    private val bikeSystemStatusAutoUpdate = MutableLiveData<Boolean>()

    private val nearestBikeAutoSelected = MutableLiveData<Boolean>()

    private val optimalDockStationId = MutableLiveData<String>()
    private val optimalBikeStationId = MutableLiveData<String>()

    private val favListClickedFavId = MutableLiveData<String>()

    private val lastStationStatusDataUpdateTimestampEpoch = MutableLiveData<Long>()
    private val userLoc = MutableLiveData<LatLng>()
    private val stationA = MutableLiveData<BikeStation>()
    private val statALatLng = MutableLiveData<LatLng>()
    private val stationB = MutableLiveData<BikeStation>()
    private val statBLatLng = MutableLiveData<LatLng>()

    private val finalDestLatLng = MutableLiveData<LatLng>()
    private val pickedFavorite = MutableLiveData<FavoriteEntityBase>()
    private val isFinalDestFav = MutableLiveData<Boolean>()

    private val appBarExpanded = MutableLiveData<Boolean>()

    //TODO: should that be maintained in repository or should repo update activity model ?
    //Tentative reply : that should be exposed by repo and channelled through model (like station availability data is)
    private val dataOutOfDate = MutableLiveData<Boolean>()

    val hasLocationPermission: LiveData<Boolean>
        get() = locationPermissionGranted

    fun setLocationPermissionGranted(toSet: Boolean) {
        locationPermissionGranted.value = toSet
    }

    private val bikeTableComp = MutableLiveData<BaseBikeStationComparator>()
    val bikeTableComparator: LiveData<BaseBikeStationComparator>
        get() = bikeTableComp

    private val dockTableComp = MutableLiveData<BaseBikeStationComparator>()
    val dockTableComparator: LiveData<BaseBikeStationComparator>
        get() = dockTableComp

    val userLocation: LiveData<LatLng>
        get() = userLoc

    val finalDestinationLatLng: LiveData<LatLng>
        get() = finalDestLatLng

    private val finalDestPlace = MutableLiveData<Place>()
    val finalDestinationPlace: LiveData<Place>
        get() = finalDestPlace

    private val finalDestFavorite = MutableLiveData<FavoriteEntityBase>()
    val finalDestinationFavorite: LiveData<FavoriteEntityBase>
        get() = finalDestFavorite

    val isFinalDestinationFavorite: LiveData<Boolean>
        get() = isFinalDestFav

    private val lookingForBike = MutableLiveData<Boolean>()

    val isLookingForBike: LiveData<Boolean>
        get() = lookingForBike

    fun setSelectedTable(toSet: Boolean) {
        lookingForBike.value = toSet
    }

    private val connectivityAvailable = MutableLiveData<Boolean>()
    //TODO: reimplement check for location services
    private val isLocationServiceAvailable = MutableLiveData<Boolean>()

    val isConnectivityAvailable: LiveData<Boolean>
        get() = connectivityAvailable

    val isSearchFabShown: LiveData<Boolean>
        get() = searchFabShown

    val searchFabBackgroundtintListColorResId: LiveData<Int>
        get() = searchFabBckgTintListColorResId

    val isDirectionsToStationAFabShown: LiveData<Boolean>
        get() = directionsToStationAFabShown

    val isClearBSelectionFabShown: LiveData<Boolean>
        get() = clearBSelectionFabShown

    val isTripDetailsFragmentShown: LiveData<Boolean>
        get() = tripDetailsWidgetShown

    val isFavoriteFabShown: LiveData<Boolean>
        get() = favoriteFabShown

    val isFavoritePickerFabShown: LiveData<Boolean>
        get() = favoritePickerFabShown

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

    private val autocompleteLoadProgBarVis = MutableLiveData<Int>()
    val autocompleteLoadingProgressBarVisibility: LiveData<Int>
        get() = autocompleteLoadProgBarVis

    fun setAppBarExpanded(toSet: Boolean) {
        appBarExpanded.value = toSet
    }

    fun setNearestBikeAutoselected(toSet: Boolean){
        nearestBikeAutoSelected.value = toSet
    }

    fun clearPickedFavorite(){
        pickedFavorite.value = null
    }

    fun setStationA(toSet: BikeStation?) {
        stationA.value = toSet
    }

    private fun setStationAById(idToSet: String?) {
        idToSet?.let {
            coroutineScopeIO.launch {
                stationA.postValue(repo.getStationForId(idToSet))
            }
        }
    }

    fun getStationA(): LiveData<BikeStation> {
        return stationA
    }

    val stationALatLng: LiveData<LatLng>
        get() = statALatLng

    fun setStationB(toSet: BikeStation?) {
        stationB.value = toSet
    }

    private fun setStationBById(idToSet: String?) {
        idToSet?.let {
            coroutineScopeIO.launch {
                stationB.postValue(repo.getStationForId(idToSet))
                Log.d(TAG, "Selecting : ${repo.getStationForId(idToSet)}")
            }
        }
    }

    fun getStationB(): LiveData<BikeStation>{
        return stationB
    }

    fun setOptimalDockStationId(toSet: String?) {
        optimalDockStationId.value = toSet
    }

    fun setOptimalBikeStationId(toSet: String?) {
        optimalBikeStationId.value = toSet
    }

    fun setLastClickedFavoriteListItemFavoriteId(toSet: String?) {
        favListClickedFavId.value = toSet
    }

    val stationBLatLng: LiveData<LatLng>
        get() = statBLatLng

    fun addFavorite(toAdd: FavoriteEntityBase) {
        repository.addOrUpdateFavorite(toAdd)
    }

    fun removeFavoriteByFavoriteId(idToRemove: String) {
        repository.removeFavoriteByFavoriteId(idToRemove)
    }

    fun showFavoriteFab() {
        favoriteFabShown.value = true
    }

    fun showFavoriteFabPost() {
        favoriteFabShown.postValue(true)
    }

    fun showSearchFab() {
        searchFabShown.value = true
    }

    fun hideSearchFab() {
        searchFabShown.value = false
    }

    fun hideSearchFabPost() {
        searchFabShown.postValue(false)
    }

    fun showFavoriteSheet() {
        searchFabShown.value = false
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

    fun showFavoritePickerFab() {
        favoritePickerFabShown.value = true
    }

    fun hideFavoritePickerFab() {
        favoritePickerFabShown.value = false
    }

    fun hideFavoriteFab() {
        favoriteFabShown.value = false
    }

    fun hideFavoriteFabPost() {
        favoriteFabShown.postValue(false)
    }

    fun hideFavoriteSheet() {
        favoriteSheetShown.value = false
    }

    fun favoriteItemNameEditStop() {
        favoriteItemNameEditInProgress.value = false
    }

    fun favoriteSheetEditDone() {
        refreshStationAAndB()
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
    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeMAIN = CoroutineScope(Dispatchers.Main)
    private var locationCallback: LocationCallback
    private var connectivityManagerNetworkCallback: ConnectivityManager.NetworkCallback
    private val repository : FindMyBikesRepository = repo
    val stationData: LiveData<List<BikeStation>>

    private val bikeTableProxColumnShown = MutableLiveData<Boolean>()
    private val dockTableProxColumnShown = MutableLiveData<Boolean>()

    val bikeTableProximityShown: LiveData<Boolean>
        get() = bikeTableProxColumnShown

    private val tableRefreshing = MutableLiveData<Boolean>()
    val allTableRefreshing: LiveData<Boolean>
        get() = tableRefreshing

    fun setAllTableRefreshing(toSet: Boolean) {
        tableRefreshing.value = toSet
    }

    fun setAllTableRefreshEnabled(toSet: Boolean) {
        tableRefreshEnabled.value = toSet
    }

    private val tableRefreshEnabled = MutableLiveData<Boolean>()
    val allTableRefreshEnabled: LiveData<Boolean>
        get() = tableRefreshEnabled

    val dockTableProximityShown: LiveData<Boolean>
        get() = dockTableProxColumnShown

    //TODO: have table model observe on finaldestPlace/FavEntityBase
    //and do the icons figuring out
    //could also use isfav like trip details fragment
    private val bikeTableProxHeaderFromResId = MutableLiveData<Int>()
    private val dockTableProxHeaderFromResId = MutableLiveData<Int>()
    private val bikeTableProxHeaderToResId = MutableLiveData<Int>()
    private val dockTableProxHeaderToResId = MutableLiveData<Int>()

    val bikeTableProximityHeaderFromResId: LiveData<Int>
        get() = bikeTableProxHeaderFromResId
    val dockTableProximityHeaderFromResId: LiveData<Int>
        get() = dockTableProxHeaderFromResId
    val bikeTableProximityHeaderToResId: LiveData<Int>
        get() = bikeTableProxHeaderToResId
    val dockTableProximityHeaderToResId: LiveData<Int>
        get() = dockTableProxHeaderToResId


    private lateinit var userLocObserverForInitialDownload: Observer<LatLng>
    private lateinit var userLocObserverForOutOfBounds: Observer<LatLng>
    private val userLocObserverForComparatorUpdate: Observer<LatLng>

    private val nowObserver: Observer<Long>

    private val optimalDockStationIdObserver: Observer<String>
    private val optimalBikeStationIdObserver: Observer<String>

    private val statusBarTxt = MutableLiveData<String>()
    private val statusBarBckColorResId = MutableLiveData<Int>()

    val statusBarText: LiveData<String>
        get() = statusBarTxt

    val statusBarBackgroundColorResId: LiveData<Int>
        get() = statusBarBckColorResId

    private val now = MutableLiveData<Long>()

    private val lastStartActForResultData = MutableLiveData<Pair<Intent, Int>>()

    val lastStartActivityForResultIntent: LiveData<Pair<Intent, Int>>
        get() = lastStartActForResultData

    fun requestStartActivityForResult(int: Intent, requestCode: Int) {
        if (requestCode == FindMyBikesActivity.PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            hideFavoritePickerFab()
            searchFabBckgTintListColorResId.value = R.color.light_gray
            autocompleteLoadProgBarVis.value = View.VISIBLE
        }

        lastStartActForResultData.value = Pair(int, requestCode)
    }

    fun clearLastStartActivityForResultRequest() {
        lastStartActForResultData.value = null
    }

    //TODO: have finalDesBikeStation the same way we have finalDestPlace and finalDestFavoriteEntityBase
    fun addFinalDestToFavoriteList() {
        coroutineScopeIO.launch {
            var done = false
            finalDestPlace.value?.let {
                val newFav = FavoriteEntityPlace(
                        id = it.id,
                        uiIndex = repo.getFavoriteCount(),
                        defaultName = it.name.toString(),
                        location = it.latLng,
                        attributions = it.attributions?.toString() ?: "",
                        bikeSystemId = curBikeSystem.value?.id ?: "[[[NO_ID]]]"
                )

                addFavorite(newFav)
                finalDestPlace.postValue(null)
                //TODO: have table model observe on finaldestPlace/FavEntityBase
                //and do the icons figuring out
                //could also use isfav like trip details fragment
                dockTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                dockTableProxHeaderToResId.postValue(R.drawable.ic_pin_favorite_24dp_white)
                bikeTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                bikeTableProxHeaderToResId.postValue(R.drawable.ic_pin_favorite_24dp_white)
                finalDestFavorite.postValue(newFav)

                done = true
            }

            if (!done) {
                val newFav = FavoriteEntityStation(
                        id = stationB.value?.locationHash ?: "[[[NO_HASH]]]",
                        uiIndex = repo.getFavoriteCount(),
                        defaultName = stationB.value?.name ?: "[[[NO_NAME]]]",
                        bikeSystemId = curBikeSystem.value?.id ?: "[[[NO_ID]]]"
                )
                addFavorite(newFav)
                finalDestFavorite.postValue(newFav)
            }
        }

        hideFavoriteFab()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FindMyBikesActivity.PLACE_AUTOCOMPLETE_REQUEST_CODE -> {
                searchFabBckgTintListColorResId.value = R.color.theme_primary_dark

                if (resultCode == RESULT_OK) {
                    coroutineScopeIO.launch {

                        val place = PlaceAutocomplete.getPlace(getApplication(), data)

                        //IDs are not guaranteed stable over long periods of time
                        //but searching for a place already in favorites is not a typical use case
                        //TODO: implement best practice of updating favorite places IDs once per month
                        //To that end, have a networkdatasource that fetch only if at least one month has passed
                        val existingFav = repo.getFavoriteEntityByFavoriteId(FavoriteEntityPlace.PLACE_ID_PREFIX + place.id)

                        if (existingFav == null) {

                            finalDestPlace.postValue(place)

                            //TODO: have table model observe on finaldestPlace/FavEntityBase
                            //and do the icons figuring out
                            //could also use isfav like trip details fragment
                            dockTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                            dockTableProxHeaderToResId.postValue(R.drawable.ic_pin_search_24dp_white)

                            bikeTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                            bikeTableProxHeaderToResId.postValue(R.drawable.ic_pin_search_24dp_white)

                            showFavoriteFabPost()
                        } else {
                            finalDestFavorite.postValue(existingFav)
                            dockTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                            dockTableProxHeaderToResId.postValue(R.drawable.ic_pin_favorite_24dp_white)
                            bikeTableProxHeaderFromResId.postValue(R.drawable.ic_destination_arrow_white_24dp)
                            bikeTableProxHeaderToResId.postValue(R.drawable.ic_pin_favorite_24dp_white)

                            hideFavoriteFabPost()
                        }

                        autocompleteLoadProgBarVis.postValue(View.INVISIBLE)
                        clearBSelectionFabShown.postValue(true)

                        //hideFavoritePickerFab()
                        hideSearchFabPost()
                    }
                } else {
                    showFavoritePickerFab()
                    showSearchFab()
                    autocompleteLoadProgBarVis.value = View.GONE
                    hideFavoriteFab()
                }
            }
            FindMyBikesActivity.SETTINGS_ACTIVITY_REQUEST_CODE -> {
                coroutineScopeIO.launch {
                    bikeSystemStatusAutoUpdate.postValue(SharedPrefHelper.getInstance().getAutoUpdate(getApplication()))
                }
            }
        }
    }

    init {

        //Initially we have no selection in B tab
        setStationB(null)

        coroutineScopeIO.launch {
            bikeSystemStatusAutoUpdate.postValue(SharedPrefHelper.getInstance().getAutoUpdate(getApplication()))
        }

        val nf = NumberFormat.getInstance()
        val pastStringBuilder = StringBuilder()
        val futureStringBuilder = StringBuilder()

        //TODO: stop and relaunch on activity callbacks ?
        timer(name = "uiRefresher", daemon = true, period = 1000) {
            if (curBikeSystem.value != null) {
                now.postValue(System.currentTimeMillis())
            }
        }

        nowObserver = Observer {
            it?.let { now ->

                coroutineScopeIO.launch {

                    val lastUpdateTimestamp = curBikeSystem.value?.lastStatusUpdateLocalTimestamp!!
                    val timeDeltaMillis = now - lastUpdateTimestamp

                    //first the past
                    if (timeDeltaMillis < DateUtils.MINUTE_IN_MILLIS)
                        pastStringBuilder.append(getApplication<Application>().getString(R.string.moments))
                    else
                        pastStringBuilder.append(getApplication<Application>().getString(R.string.il_y_a))
                                .append(nf.format(timeDeltaMillis / DateUtils.MINUTE_IN_MILLIS))
                                .append(" ").append(getApplication<Application>().getString(R.string.min_abbreviated))

                    //then the future
                    if (isConnectivityAvailable.value == true) {

                        if (bikeSystemStatusAutoUpdate.value != true) {
                            futureStringBuilder.append(getApplication<Application>().getString(R.string.pull_to_refresh))
                        } else {
                            //auto
                            val wishedUpdateTime = lastUpdateTimestamp +
                                    1 *
                                    getApplication<Application>().resources.getInteger(R.integer.update_auto_interval_minute) *
                                    1000 *
                                    60

                            //Model should keep time since last update
                            //someone should observe and request Bike system status refresh
                            if (now >= wishedUpdateTime)
                                requestCurrentBikeSystemStatusRefresh()
                            else {
                                futureStringBuilder.append(getApplication<Application>().getString(R.string.nextUpdate))
                                        .append(" ")
                                val deltaSeconds = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS

                                // formatted will be HH:MM:SS or MM:SS
                                futureStringBuilder.append(DateUtils.formatElapsedTime(deltaSeconds))
                            }
                        }
                    } else {

                        futureStringBuilder.append(getApplication<Application>().getString(R.string.no_connectivity))

                        //TODO: block refresh gesture in tables
                        //NO : table model observes connectivity
                    }

                    statusBarTxt.postValue(String.format(getApplication<Application>().getString(R.string.status_string),
                            pastStringBuilder.toString(), futureStringBuilder.toString()))

                    pastStringBuilder.clear()
                    futureStringBuilder.clear()

                    //Log.d("truc", "it.lastStatusUpdateLocalTimestamp : ${curBikeSystem.value?.lastStatusUpdateLocalTimestamp} \nlast update was ${(System.currentTimeMillis() - curBikeSystem.value?.lastStatusUpdateLocalTimestamp!!) / 1000L}s ago")
                    if (isConnectivityAvailable.value == false || now - curBikeSystem.value?.lastStatusUpdateLocalTimestamp!! >
                            getApplication<Application>().resources.getInteger(R.integer.outdated_data_time_minute) * 60 * 1000) {

                        if (dataOutOfDate.value != true) {

                            dataOutOfDate.postValue(true)
                            statusBarBckColorResId.postValue(R.color.theme_accent)
                        }
                    } else {

                        if (dataOutOfDate.value != false) {

                            dataOutOfDate.postValue(false)
                            statusBarBckColorResId.postValue(R.color.theme_primary_dark)
                        }
                    }
                }
            }
        }

        now.observeForever(nowObserver)

        repo.lastBikeNetworkStatusFetchErrored.observeForever {
            if (it == true) {
                now.observeForever(nowObserver)
            }
        }

        repo.getBikeSystemStationData(getApplication()).observeForever {
            //TODO deactivate refresh gesture in tables
            //TODO: bike and dock should be re selected based on new availability
            statusBarTxt.value = getApplication<Application>().getString(R.string.refreshing_map)
        }

        optimalDockStationIdObserver = Observer {
            if (finalDestLatLng.value != null && stationB.value == null && it != null) {
                Log.d(TAG, "Conditions met for B auto select, selecting")
                setStationBById(it)
            }
        }

        optimalDockStationId.observeForever(optimalDockStationIdObserver)

        optimalBikeStationIdObserver = Observer {
            if (it != null && userLoc.value != null && stationA.value == null) {
                Log.d(TAG, "Conditions met for A auto select, selecting")
                setStationAById(it)
            }
        }

        optimalBikeStationId.observeForever(optimalBikeStationIdObserver)

        finalDestPlace.observeForever {
            if (it != null) {
                finalDestLatLng.value = it.latLng
                isFinalDestFav.value = false
                bikeTableProxHeaderToResId.value = R.drawable.ic_pin_search_24dp_white
            } else if (finalDestFavorite.value == null) {
                finalDestLatLng.value = null
                isFinalDestFav.value = null

                //TODO: hide 'From' header
                //bikeTableProxHeaderFromResId.postValue(R.drawable.)
                bikeTableProxHeaderToResId.postValue(R.drawable.ic_walking_24dp_white)
            }
        }

        finalDestFavorite.observeForever {
            if (it != null) {
                coroutineScopeIO.launch {
                    finalDestLatLng.postValue(it.getLocation(getApplication()))
                    isFinalDestFav.postValue(true)
                    bikeTableProxHeaderToResId.postValue(R.drawable.ic_pin_favorite_24dp_white)
                }

            } else if (finalDestPlace.value == null) {
                finalDestLatLng.value = null
                isFinalDestFav.value = null
                //TODO: hide 'From' header
                bikeTableProxHeaderToResId.value = R.drawable.ic_walking_24dp_white
            }
        }

        locationPermissionGranted.value = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        stationData = repo.getBikeSystemStationData(getApplication())

        val bikeSystemListData = repo.getBikeSystemListData(getApplication())

        bikeSystemListData.observeForever {

            it?.let { newList ->
                if (userLoc.value != null) {
                    findNearestBikeSystemAndSetInRepo(newList, userLoc.value, repo)
                } else {
                    userLocObserverForInitialDownload = Observer { newUserLoc ->

                        newUserLoc?.let {
                            findNearestBikeSystemAndSetInRepo(newList, newUserLoc, repo)
                            userLoc.removeObserver(userLocObserverForInitialDownload)
                        }
                    }
                    userLoc.observeForever(userLocObserverForInitialDownload)
                }
            }
        }

        myCurBikeSystem.observeForever { newSystem ->
            Log.d(TAG, "$newSystem")

            if (lastBikeSystemId == null)
                lastBikeSystemId = newSystem?.id

            //so that closest bike selection (and tweeting) happens
            setStationA(null)
            statALatLng.value = null

            if (lastBikeSystemId != newSystem?.id) {
                //we switched systems since last time, reset everything
                setStationB(null)
                statBLatLng.value = null
                finalDestFavorite.value = null
                finalDestPlace.value = null
            }

            lastBikeSystemId = newSystem?.id

            if (newSystem == null) {
                bikeSystemListData.value?.let { bikeSystemList ->
                    findNearestBikeSystemAndSetInRepo(bikeSystemList, userLoc.value, repo)
                }
            } else {
                //We have bounds, start watching user location to trigger new attempt at finding a bike system
                //when getting out of bounds
                newSystem.boundingBoxNorthEastLatitude?.let { bbNELat ->

                    Log.d(TAG, "registering observer on userLoc for out of bounds detection for ${newSystem.id}")
                    val boundsBuilder = LatLngBounds.builder()
                    boundsBuilder.include(LatLng(bbNELat, newSystem.boundingBoxNorthEastLongitude!!))
                    boundsBuilder.include(LatLng(newSystem.boundingBoxSouthWestLatitude!!, newSystem.boundingBoxSouthWestLongitude!!))

                    val bounds = boundsBuilder.build()

                    userLocObserverForOutOfBounds = Observer { newUserLoc ->
                        newUserLoc?.let {
                            val lastUpdateTimestamp = newSystem.lastStatusUpdateLocalTimestamp
                            if (!bounds.contains(newUserLoc) && now.value ?: Int.MAX_VALUE - lastUpdateTimestamp >= getApplication<Application>().resources.getInteger(R.integer.outdated_data_time_minute) * 60 * 1000) {
                                //TODO: implement exponential backoff when out of bounds of any bike system
                                //TODO: just maintain out of bounds observable boolean in activity model. Observe where pertinent
                                Log.d(TAG, "out of bound dected, invalidating current bike system")
                                userLoc.removeObserver(userLocObserverForOutOfBounds)
                                statusBarTxt.value = getApplication<Application>().getString(R.string.searching_bike_network)
                                repo.invalidateCurrentBikeSystem(getApplication())
                            }
                        }
                    }

                    //TODO: implement exponential backoff
                    userLoc.observeForever(userLocObserverForOutOfBounds)
                }
            }
        }

        repo.getCurrentBikeSystem().observeForever {

            Log.d(TAG, "new data from repo regarding bikesystem")
            myCurBikeSystem.value = it
        }

        //connectivity
        connectivityManagerNetworkCallback = object : ConnectivityManager.NetworkCallback() {

            private var availabilityByNetworkToStringMap: HashMap<String, Boolean> = HashMap()

            init {
                //no connection by default
                connectivityAvailable.value = false
            }

            @Suppress("unused")
            fun getNetworkMap(): HashMap<String, Boolean> {
                return availabilityByNetworkToStringMap
            }

            override fun onAvailable(network: Network) {
                availabilityByNetworkToStringMap[network.toString()] = true
                updateNetworkAvailability()
            }

            override fun onLost(network: Network?) {
                availabilityByNetworkToStringMap[network.toString()] = false
                updateNetworkAvailability()
            }

            //Sets availability to true if at least one network is available, false otherwise
            private fun updateNetworkAvailability() {
                var atLeastOneAvailable = false

                for (networkAvailable in availabilityByNetworkToStringMap.values) {
                    atLeastOneAvailable = atLeastOneAvailable || networkAvailable
                }

                connectivityAvailable.postValue(atLeastOneAvailable)
            }
        }

        val builder = NetworkRequest.Builder()

        (getApplication<Application>().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).registerNetworkCallback(
                builder.build(),
                connectivityManagerNetworkCallback)

        connectivityAvailable.observeForever {
            if (it == true) {
                if (lookingForBike.value == false && stationB.value == null && favoriteSheetShown.value != true)
                    searchFabShown.value = true
            } else {
                searchFabShown.value = false
            }
        }


        //user location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication() as Context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {

                var toEmit: LatLng? = null
                val userLat = locationResult?.locations?.getOrNull(0)?.latitude
                val userLng = locationResult?.locations?.getOrNull(0)?.longitude

                if (userLat != null && userLng != null) {
                    toEmit = LatLng(userLat, userLng)
                }

                if (SphericalUtil.computeDistanceBetween(userLoc.value ?: LatLng(0.0, 0.0),
                                toEmit) >= 5.0) {
                    Log.d(TAG, "Emitting new Location !! : $toEmit")
                    userLoc.value = toEmit
                }
            }
        }

        val locationRequest = LocationRequest()
        locationRequest.interval = 2000 //TODO: make that flexible
        locationRequest.fastestInterval = 5000 //TODO: make that flexible
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        hasLocationPermission.observeForever {
            if (it == true) {
                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback, null)
            } else {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            }
        }

        //
        isLookingForBike.observeForever {
            if (it == true) {
                appBarExpanded.value = false
                searchFabShown.value = false
                favoritePickerFabShown.value = false
                favoriteFabShown.value = false
                clearBSelectionFabShown.value = false
                directionsToStationAFabShown.value = stationA.value != null && stationB.value == null

            } else {
                directionsToStationAFabShown.value = false
                if (stationB.value == null) {

                    if (favoriteSheetShown.value == true) {
                        searchFabShown.value = false
                    }
                    if (favoriteSheetShown.value == null || favoriteSheetShown.value == false) {
                        searchFabShown.value = connectivityAvailable.value == true
                        favoritePickerFabShown.value = true
                    }
                } else {
                    favoritePickerFabShown.value = false
                    searchFabShown.value = false
                    favoriteFabShown.value = true
                    clearBSelectionFabShown.value = true
                }
            }
        }

        userLocObserverForComparatorUpdate = Observer { newUserLoc ->

            var finalDest = finalDestinationLatLng.value

            if (finalDest == null)
                finalDest = stationBLatLng.value

            dockTableComp.value = TotalTripTimeComparatorForDockTable(
                    userLatLng = newUserLoc,
                    stationALatLng = stationALatLng.value,
                    destinationLatLng = finalDest
            )

            newUserLoc?.let {
                bikeTableComp.value = TotalTripTimeComparatorForBikeTable(
                        userLatLng = newUserLoc,
                        stationBLatLng = stationBLatLng.value,
                        destinationLatLng = finalDest
                )
            }
        }

        userLoc.observeForever(userLocObserverForComparatorUpdate)



        //TODO_OLD: have an observer and remove it on cleared
        //Nope, not when I observe one of my local member
        stationA.observeForever {
            if (it != null) {
                directionsToStationAFabShown.value = isLookingForBike.value == true && stationB.value == null

                statALatLng.value = it.location

                statBLatLng.value?.let {
                    tripDetailsWidgetShown.value = true
                }
            } else {
                directionsToStationAFabShown.value = false
                tripDetailsWidgetShown.value = false
                statALatLng.value = null
            }

            dockTableComp.value = TotalTripTimeComparatorForDockTable(
                    userLatLng = userLoc.value,
                    stationALatLng = it?.location,
                    destinationLatLng = finalDestinationLatLng.value
            )
        }

        stationB.observeForever {
            if (it != null) {
                dockTableProxColumnShown.value = finalDestLatLng.value != null

                statBLatLng.value = it.location
                tripDetailsWidgetShown.value = true
                if (lookingForBike.value != true) {
                    favoritePickerFabShown.value = false
                    searchFabShown.value = false
                    favoriteFabShown.value = true
                    coroutineScopeIO.launch {
                        if (finalDestFavorite.value != null || repo.isFavoriteId(it.locationHash)) {
                            favoriteFabShown.postValue(false)
                        }
                    }

                    clearBSelectionFabShown.value = true
                }
            } else {
                finalDestLatLng.value = null
                finalDestPlace.value = null
                finalDestFavorite.value = null
                statBLatLng.value = null
                tripDetailsWidgetShown.value = false
                favoritePickerFabShown.value = isLookingForBike.value != true
                searchFabShown.value = isLookingForBike.value != true && connectivityAvailable.value == true
                favoriteFabShown.value = false
                clearBSelectionFabShown.value = false
            }

            var finalDest = finalDestinationLatLng.value

            if (finalDest == null)
                finalDest = it?.location

            dockTableComp.value = TotalTripTimeComparatorForDockTable(
                    userLatLng = userLoc.value,
                    stationALatLng = stationALatLng.value,
                    destinationLatLng = finalDest
            )

            bikeTableComp.value = TotalTripTimeComparatorForBikeTable(
                    userLatLng = userLoc.value,
                    stationBLatLng = stationBLatLng.value,
                    destinationLatLng = finalDest
            )
        }

        finalDestinationLatLng.observeForever {
            dockTableComp.value = TotalTripTimeComparatorForDockTable(
                    userLatLng = userLoc.value,
                    stationALatLng = stationALatLng.value,
                    destinationLatLng = it
            )
        }

        favListClickedFavId.observeForever {
            it?.let {

                coroutineScopeIO.launch {
                    finalDestFavorite.postValue(repo.getFavoriteEntityByFavoriteId(it))

                    coroutineScopeMAIN.launch {
                        //TODO: have table model observe on finaldestPlace/FavEntityBase
                        //and do the icons figuring out
                        //could also use isfav like trip details fragment
                        dockTableProxHeaderFromResId.value = R.drawable.ic_destination_arrow_white_24dp
                        dockTableProxHeaderToResId.value = R.drawable.ic_pin_favorite_24dp_white

                        bikeTableProxHeaderFromResId.value = R.drawable.ic_destination_arrow_white_24dp
                        bikeTableProxHeaderToResId.value = R.drawable.ic_pin_favorite_24dp_white

                        hideSearchFab()
                        autocompleteLoadProgBarVis.value = View.INVISIBLE
                        clearBSelectionFabShown.value = true
                        showFavoriteFab()
                        //hideFavoritePickerFab()
                        hideSearchFab()
                    }
                }
            }
        }
    }

    fun markerRedrawEnd() {
        now.observeForever(nowObserver)
    }

    fun refreshStationAAndB() {
        //let's refresh everyone
        stationA.value = stationA.value
        stationB.value = stationB.value
    }

    fun requestCurrentBikeSystemStatusRefresh() {
        coroutineScopeMAIN.launch {
            statusBarTxt.value = getApplication<Application>().getString(R.string.downloading)
            now.removeObserver(nowObserver)
        }

        repo.invalidateBikeSystemStatus(getApplication(), curBikeSystem.value?.citybikDOTesUrl
                ?: "")
    }


    private fun findNearestBikeSystemAndSetInRepo(bikeSystemList: List<BikeSystem>, userLocation: LatLng?, repo: FindMyBikesRepository): Job {
        val sortedList = bikeSystemList.toMutableList()

        return coroutineScopeIO.launch {
            sortedList.sortWith(Comparator { bikeSystemLeft, bikeSystemRight ->
                (SphericalUtil.computeDistanceBetween(userLocation, LatLng(bikeSystemLeft.latitude, bikeSystemLeft.longitude)) -
                        SphericalUtil.computeDistanceBetween(userLocation, LatLng(bikeSystemRight.latitude, bikeSystemRight.longitude))).toInt()
            })

            var nearestBikeSystem = sortedList[0]

            if (nearestBikeSystem.id.equals(NEW_YORK_HUDSON_BIKESHARE_ID, ignoreCase = true)) {
                nearestBikeSystem = sortedList[1]
            }

            repo.setCurrentBikeSystem(getApplication(), nearestBikeSystem, alsoFetchStatus = true)
        }
    }

    override fun onCleared() {

        (getApplication<Application>().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .unregisterNetworkCallback(connectivityManagerNetworkCallback)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication() as Context)

        //TODO: activity lifecycle methods should remove and re request to save battery
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onCleared()
    }

    companion object {
        private val TAG = FindMyBikesActivityViewModel::class.java.simpleName
        private const val NEW_YORK_HUDSON_BIKESHARE_ID = "hudsonbikeshare-hoboken"
    }

    abstract class BaseBikeStationComparator : Comparator<BikeStation> {
        abstract fun getProximityString(station: BikeStation, lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String?

        abstract val userLoc: LatLng
    }

    inner class TotalTripTimeComparatorForDockTable(
            private val walkingSpeedKmh: Float = Utils.getAverageWalkingSpeedKmh(getApplication()),
            private val bikingSpeedKmh: Float = Utils.getAverageBikingSpeedKmh(getApplication()),
            private val userLatLng: LatLng?,
            private val stationALatLng: LatLng?,
            private val destinationLatLng: LatLng?) : BaseBikeStationComparator() {
        override fun getProximityString(station: BikeStation,
                                        lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String? {
            val totalTime = calculateWalkTimeMinute(station) + calculateBikeTimeMinute(station)

            return Utils.durationToProximityString(totalTime, false, numFormat, ctx)
        }

        fun hasFinalDest(): Boolean {
            return destinationLatLng != null
        }

        override val userLoc: LatLng
            get() = userLatLng ?: LatLng(0.0, 0.0)

        private val mTimeUserToAMinutes: Int = Utils.computeTimeBetweenInMinutes(userLatLng, stationALatLng, walkingSpeedKmh)

        /*internal fun getUpdatedComparatorFor(userLatLng: LatLng,
                                             stationALatLng: LatLng?): TotalTripTimeComparatorForDockTable {
            return TotalTripTimeComparatorForDockTable(walkingSpeedKmh, bikingSpeedKmh, userLatLng,
                    stationALatLng ?: this.stationALatLng, destinationLatLng)
        }*/

        override fun compare(lhs: BikeStation, rhs: BikeStation): Int {

            val lhsWalkTime = calculateWalkTimeMinute(lhs)
            val rhsWalkTime = calculateWalkTimeMinute(rhs)

            val lhsBikeTime = calculateBikeTimeMinute(lhs)
            val rhsBikeTime = calculateBikeTimeMinute(rhs)

            val totalTimeDiff = lhsWalkTime + lhsBikeTime - (rhsWalkTime + rhsBikeTime)

            return if (totalTimeDiff != 0)
                totalTimeDiff
            else
                lhsWalkTime - rhsWalkTime
        }

        private fun calculateWalkTimeMinute(_stationB: BikeStation): Int {
            return mTimeUserToAMinutes + Utils.computeTimeBetweenInMinutes(_stationB.location,
                    destinationLatLng, walkingSpeedKmh)

        }

        private fun calculateBikeTimeMinute(_stationB: BikeStation): Int {

            return Utils.computeTimeBetweenInMinutes(stationALatLng, _stationB.location,
                    bikingSpeedKmh)
        }
    }

    inner class TotalTripTimeComparatorForBikeTable(
            private val walkingSpeedKmh: Float = Utils.getAverageWalkingSpeedKmh(getApplication()),
            private val bikingSpeedKmh: Float = Utils.getAverageBikingSpeedKmh(getApplication()),
            private val userLatLng: LatLng?,
            private val stationBLatLng: LatLng?,
            private val destinationLatLng: LatLng?) : BaseBikeStationComparator() {
        override fun getProximityString(station: BikeStation,
                                        lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String? {
            val totalTime = calculateWalkTimeMinute(station) + calculateBikeTimeMinute(station)

            return Utils.durationToProximityString(totalTime, false, numFormat, ctx)
        }

        fun hasFinalDest(): Boolean {
            return destinationLatLng != null
        }

        override val userLoc: LatLng
            get() = userLatLng ?: LatLng(0.0, 0.0)

        private val mBToDestMinutes: Int = Utils.computeTimeBetweenInMinutes(stationBLatLng, destinationLatLng, walkingSpeedKmh)

        /*internal fun getUpdatedComparatorFor(userLatLng: LatLng,
                                             stationBLatLng: LatLng?): TotalTripTimeComparatorForDockTable {
            return TotalTripTimeComparatorForDockTable(walkingSpeedKmh, bikingSpeedKmh, userLatLng,
                    stationBLatLng ?: this.stationBLatLng, destinationLatLng)
        }*/

        override fun compare(lhs: BikeStation, rhs: BikeStation): Int {

            val lhsWalkTime = calculateWalkTimeMinute(lhs)
            val rhsWalkTime = calculateWalkTimeMinute(rhs)

            val lhsBikeTime = calculateBikeTimeMinute(lhs)
            val rhsBikeTime = calculateBikeTimeMinute(rhs)

            val totalTimeDiff = lhsWalkTime + lhsBikeTime - (rhsWalkTime + rhsBikeTime)

            return if (totalTimeDiff != 0)
                totalTimeDiff
            else
                lhsWalkTime - rhsWalkTime
        }

        private fun calculateWalkTimeMinute(stationA: BikeStation): Int {
            return mBToDestMinutes + Utils.computeTimeBetweenInMinutes(userLatLng,
                    stationA.location, walkingSpeedKmh)
        }

        private fun calculateBikeTimeMinute(stationA: BikeStation): Int {

            return Utils.computeTimeBetweenInMinutes(stationA.location, stationBLatLng,
                    bikingSpeedKmh)
        }
    }

}
