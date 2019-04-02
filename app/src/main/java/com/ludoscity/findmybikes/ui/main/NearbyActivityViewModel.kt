package com.ludoscity.findmybikes.ui.main

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityBase
import com.ludoscity.findmybikes.data.database.station.BikeStation
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

class NearbyActivityViewModel(private val repo: FindMyBikesRepository, app: Application) : AndroidViewModel(app) {

    private val locationPermissionGranted = MutableLiveData<Boolean>()

    private val favoriteFabShown = MutableLiveData<Boolean>()
    private val tripDetailsWidgetShown = MutableLiveData<Boolean>()
    private val favoritePickerFabShown = MutableLiveData<Boolean>()
    private val searchFabShown = MutableLiveData<Boolean>()
    private val directionsToStationAFabShown = MutableLiveData<Boolean>()
    private val clearBSelectionFabShown = MutableLiveData<Boolean>()
    private val favoriteSheetShown = MutableLiveData<Boolean>()
    private val favoriteItemNameEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditInProgress = MutableLiveData<Boolean>()
    private val favoriteSheetEditFabShown = MutableLiveData<Boolean>()

    private val myCurBikeSystem = MutableLiveData<BikeSystem>()

    val curBikeSystem: LiveData<BikeSystem>
        get() = myCurBikeSystem


    private val nearestBikeAutoSelected = MutableLiveData<Boolean>()
    private val lastStationStatusDataUpdateTimestampEpoch = MutableLiveData<Long>()
    private val userLoc = MutableLiveData<LatLng>()
    private val stationA = MutableLiveData<BikeStation>()
    private val statALatLng = MutableLiveData<LatLng>()
    private val stationB = MutableLiveData<BikeStation>()
    private val statBLatLng = MutableLiveData<LatLng>()

    private val finalDest = MutableLiveData<LatLng>()
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

    private val connectivityAvailable = MutableLiveData<Boolean>()
    private val isLocationServiceAvailable = MutableLiveData<Boolean>()

    val isConnectivityAvailable: LiveData<Boolean>
        get() = connectivityAvailable

    val isSearchFabShown: LiveData<Boolean>
        get() = searchFabShown

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

    fun getStationA(): LiveData<BikeStation> {
        return stationA
    }

    val stationALatLng: LiveData<LatLng>
        get() = statALatLng

    fun setStationB(toSet: BikeStation?) {
        stationB.value = toSet
    }
    fun getStationB(): LiveData<BikeStation>{
        return stationB
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

    fun showSearchFab() {
        searchFabShown.value = true
    }

    fun hideSearchFab() {
        searchFabShown.value = false
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

    fun hideFavoriteSheet() {
        favoriteSheetShown.value = false
    }

    fun favoriteItemNameEditStop() {
        favoriteItemNameEditInProgress.value = false
    }

    fun favoriteSheetEditDone() {
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
    private var locationCallback: LocationCallback
    private var connectivityManagerNetworkCallback: ConnectivityManager.NetworkCallback
    private val repository : FindMyBikesRepository = repo
    val stationData: LiveData<List<BikeStation>>

    private lateinit var userLocObserverForInitialDownload: Observer<LatLng>
    private lateinit var userLocObserverForOutOfBounds: Observer<LatLng>

    private val statusBarTxt = MutableLiveData<String>()

    val statusBarText: LiveData<String>
        get() = statusBarTxt

    private val now = MutableLiveData<Long>()

    init {

        val nf = NumberFormat.getInstance()
        val pastStringBuilder = StringBuilder()
        val futureStringBuilder = StringBuilder()

        //TODO: stop and relaunch on activity callbacks ?
        timer(name = "uiRefresher", daemon = true, period = 1000) {
            if (curBikeSystem.value != null) {
                now.postValue(System.currentTimeMillis())
            }
        }

        now.observeForever {
            it?.let { now ->

                val lastUpdatetimestamp = curBikeSystem.value?.lastStatusUpdateLocalTimestamp!!
                val timeDeltaMillis = now - lastUpdatetimestamp

                //first the past
                if (timeDeltaMillis < DateUtils.MINUTE_IN_MILLIS)
                    pastStringBuilder.append(getApplication<Application>().getString(R.string.moments))
                else
                    pastStringBuilder.append(getApplication<Application>().getString(R.string.il_y_a))
                            .append(nf.format(timeDeltaMillis / DateUtils.MINUTE_IN_MILLIS))
                            .append(" ").append(getApplication<Application>().getString(R.string.min_abbreviated))

                //then the future
                if (isConnectivityAvailable.value == true) {
                    searchFabShown.postValue(true)

                    //TODO: reimplement manual or auto refresh
                    //manual
                    //futureStringBuilder.append(getApplication<Application>().getString(R.string.pull_to_refresh))

                    //auto
                    val wishedUpdatetime = lastUpdatetimestamp +
                            1 *
                            //getApplication<Application>().resources.getInteger(R.integer.update_auto_interval_minute) *
                            1000 *
                            60

                    //Model should keep time since last update
                    //someone should observe and request Bike system status refresh
                    if (now >= wishedUpdatetime)
                        requestCurrentBikeSystemStatusRefresh()
                    else {
                        futureStringBuilder.append(getApplication<Application>().getString(R.string.nextUpdate))
                                .append(" ")
                        val deltaSeconds = (wishedUpdatetime - now) / DateUtils.SECOND_IN_MILLIS

                        // formatted will be HH:MM:SS or MM:SS
                        futureStringBuilder.append(DateUtils.formatElapsedTime(deltaSeconds))
                    }
                } else {

                    futureStringBuilder.append(getApplication<Application>().getString(R.string.no_connectivity))

                    //TODO: block refresh gesture in tables
                    //TODO: disable search Fab
                    searchFabShown.postValue(false)
                }

                statusBarTxt.value = String.format(getApplication<Application>().getString(R.string.status_string),
                        pastStringBuilder.toString(), futureStringBuilder.toString())

                pastStringBuilder.clear()
                futureStringBuilder.clear()

                //Log.d("truc", "it.lastStatusUpdateLocalTimestamp : ${curBikeSystem.value?.lastStatusUpdateLocalTimestamp} \nlast update was ${(System.currentTimeMillis() - curBikeSystem.value?.lastStatusUpdateLocalTimestamp!!) / 1000L}s ago")
                dataOutOfDate.value = System.currentTimeMillis() - curBikeSystem.value?.lastStatusUpdateLocalTimestamp!! > 30000

            }
        }

        locationPermissionGranted.value = ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        //finalDest.value = LatLng(45.75725, 4.84974)//Lyon
        /////userLoc.value = LatLng(45.75725, 4.84974)//Lyon
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

        myCurBikeSystem.observeForever {
            Log.d(TAG, "$it")

            if (it == null) {
                bikeSystemListData.value?.let { bikeSystemList ->
                    findNearestBikeSystemAndSetInRepo(bikeSystemList, userLoc.value, repo)
                }
            } else {
                //We have bounds, start watching user location to trigger new attempt at finding a bike system
                //when getting out of bounds
                it.boundingBoxNorthEastLatitude?.let { bbNELat ->

                    Log.d(TAG, "registering observer on userLoc for out of bounds detection for ${it.id}")
                    val boundsBuilder = LatLngBounds.builder()
                    boundsBuilder.include(LatLng(bbNELat, it.boundingBoxNorthEastLongitude!!))
                    boundsBuilder.include(LatLng(it.boundingBoxSouthWestLatitude!!, it.boundingBoxSouthWestLongitude!!))

                    val bounds = boundsBuilder.build()

                    userLocObserverForOutOfBounds = Observer { newUserLoc ->
                        newUserLoc?.let {
                            if (!bounds.contains(newUserLoc)) {
                                Log.d(TAG, "out of bound dected, invalidating current bike system")
                                userLoc.removeObserver(userLocObserverForOutOfBounds)
                                repo.invalidateCurrentBikeSystem(getApplication())
                            }
                        }
                    }

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

                //Log.d(TAG, "Emitting new Location !! : $toEmit")
                userLoc.value = toEmit
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
        }

        stationB.observeForever {
            if (it != null) {
                statBLatLng.value = it.location
                tripDetailsWidgetShown.value = true
                if (lookingForBike.value != true) {
                    favoritePickerFabShown.value = false
                    searchFabShown.value = false
                    favoriteFabShown.value = true
                    coroutineScopeIO.launch {
                        //TODO: add favorite fab icon resID depending if station B is a favorite (ask repo)
                        if (repo.isFavoriteId(it.locationHash)) {
                            favoriteFabShown.postValue(false)
                        }
                    }

                    clearBSelectionFabShown.value = true
                }
            } else {
                statBLatLng.value = null
                tripDetailsWidgetShown.value = false
                favoritePickerFabShown.value = true
                searchFabShown.value = connectivityAvailable.value == true
                favoriteFabShown.value = false
                clearBSelectionFabShown.value = false

            }
        }
    }

    fun requestCurrentBikeSystemStatusRefresh() {
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
        private val TAG = NearbyActivityViewModel::class.java.simpleName
        private const val NEW_YORK_HUDSON_BIKESHARE_ID = "hudsonbikeshare-hoboken"
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
