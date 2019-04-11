package com.ludoscity.findmybikes.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLngBounds
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystemDao
import com.ludoscity.findmybikes.data.database.favorite.*
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.data.database.station.BikeStationDao
import com.ludoscity.findmybikes.data.network.BikeSystemListNetworkDataSource
import com.ludoscity.findmybikes.data.network.BikeSystemStatusNetworkDataSource
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemListAnswerRoot
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FindMyBikesRepository private constructor(
        private val currentBikeSystemDao: BikeSystemDao,
        private val stationDao: BikeStationDao,
        private val favoriteEntityPlaceDao: FavoriteEntityPlaceDao,
        private val favoriteEntityStationDao: FavoriteEntityStationDao,
        private val bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
        private val bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private var mInitialized = false
    private val networkBikeSystemStatusData: LiveData<BikeSystemStatus> = bikeSystemStatusNetworkDataSource.data
    private val networkBikeSystemListAnswerRootData: LiveData<BikeSystemListAnswerRoot> = bikeSystemListNetworkDataSource.data

    //transient local list of all bike systems available. Model determine nearest one for DB saving with bounds of current bike system
    private val availableBikeSystemList: MutableLiveData<List<BikeSystem>> = MutableLiveData()

    //For some reason, those copies are required for LiveData from Room to correctly trigger
    //when db is updated.
    //COULD NOT RETURN THEM DIRECTLY FROM ACCESSOR
    private val curBikeSystemData: LiveData<BikeSystem> = currentBikeSystemDao.single
    private val curBikeSystemStatusData: LiveData<List<BikeStation>> = stationDao.all

    private val statusFetchErrored = MutableLiveData<Boolean>()
    val lastBikeNetworkStatusFetchErrored: LiveData<Boolean>
        get () = statusFetchErrored


    init {

        networkBikeSystemStatusData.observeForever { newStatusDataFromNetwork ->

            if (newStatusDataFromNetwork == null) {
                statusFetchErrored.value = true
            }

            newStatusDataFromNetwork?.let {

                coroutineScopeIO.launch {

                    currentBikeSystemDao.updateLastUpdateTimestamp(it.id, System.currentTimeMillis())

                    Log.d(TAG, "New data from network, begin processing of " +
                            "${newStatusDataFromNetwork.bikeStationList?.size} stations for saving in Room")

                    val boundsBuilder = LatLngBounds.Builder()

                    newStatusDataFromNetwork.bikeStationList?.forEach {

                        boundsBuilder.include(it.location)

                        it.uid = it.locationHash

                        if (it.extra != null) {
                            if (it.extra.uid != null) {
                                it.uid = "${newStatusDataFromNetwork.id}:${it.extra.uid}"
                            }

                            if (it.name == null) {
                                it.name = it.extra.name ?: "null"
                            }
                        }
                    }

                    Log.d(TAG, "Calculating bounds")
                    val bounds = boundsBuilder.build()

                    Log.d(TAG, "Saving bounds")
                    currentBikeSystemDao.updateBounds(it.id,
                            bounds.northeast.latitude,
                            bounds.northeast.longitude,
                            bounds.southwest.latitude,
                            bounds.southwest.longitude)

                    Log.d(TAG, "Processing done")
                    //TODO: smarter update algorithm that purges obsolete stations from DB without dropping the whole table
                    //deleteOldData
                    //TODO: in relation to previous : do that only when switching from a bike network to an other one
                    //BUT, even when updating in place, some strategy should be in place to detect 'orphans' and purge them
                    //especially if they are a user's favourite.
                    //For now, live with the visual glitch on launch where the UI table empties itself and refills
                    stationDao.deleteAllBikeStation()
                    Log.d(TAG, "Old station data deleted")
                    //Insert new data into database
                    stationDao.insertBikeStationList(newStatusDataFromNetwork.bikeStationList!!)
                    Log.d(TAG, "New values inserted with replace strategy")
                }
            }
        }
        networkBikeSystemListAnswerRootData.observeForever { newBikeSystemListData ->

            newBikeSystemListData?.let { newBikeSystemListFromNetwork ->
                Log.d(TAG, "New complete list of bike system available, size: ${newBikeSystemListFromNetwork.networks?.size}")

                coroutineScopeIO.launch {
                    Log.d(TAG, "Backgorund thread, turning BikeSystemListanswerRoot into List<BikeSystem>")

                    val bikeSystemList = ArrayList<BikeSystem>()
                    //TODO: behave if networks is null (that should be a test)
                    newBikeSystemListData.networks!!.forEach {

                        bikeSystemList.add(BikeSystem(
                                it.id,
                                System.currentTimeMillis(),
                                it.href,
                                it.name,
                                it.location.latitude,
                                it.location.longitude,
                                it.location.city,
                                //TODO: also have country. Bounds will be calculated when network status is available
                                "to add country here",
                                null,
                                null,
                                null,
                                null
                        ))

                    }

                    //act model observes transient full list and then update repo for current bike system
                    //(it knows user location)
                    availableBikeSystemList.postValue(bikeSystemList)
                }
            }
        }
    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     */
    @Synchronized
    private fun initializeData(ctx: Context) {

        // Only perform initialization once per app lifetime. If initialization has already been
        // performed, we have nothing to do in this method.
        if (mInitialized) return
        mInitialized = true

        // This method call triggers Findmybikes to create its task to synchronize station data
        // periodically.
        //TODO: non manual refresh
        //bikeSystemStatusNetworkDataSource.scheduleRecurringFetchBikeSystemSync()

        coroutineScopeIO.launch {
            //TODO: implement real isFetchNeeded. Shall we have on for status and one for list ?
            //or should the thing cascade ?
            if (isFetchNeeded()){
                //startFetchBikeSystemStatusDataService(ctx)
                startFetchBikeSystemListDataService(ctx)
            }
        }
    }

    fun getStationForId(targetId: String): BikeStation {
        return stationDao.getStation(targetId)
    }

    fun getFavoriteStationList(): LiveData<List<FavoriteEntityStation>> {
        return favoriteEntityStationDao.getAllForBikeSystemId(currentBikeSystemDao.singleIdSynchronous)
    }

    fun getFavoritePlaceList(): LiveData<List<FavoriteEntityPlace>> {
        return favoriteEntityPlaceDao.getAllForBikeSystemId(currentBikeSystemDao.singleIdSynchronous)
    }

    fun addOrUpdateFavorite(toAddOrUpdate: FavoriteEntityBase) {
        coroutineScopeIO.launch {
            when (toAddOrUpdate) {
                is FavoriteEntityPlace -> favoriteEntityPlaceDao.insertOne(toAddOrUpdate)
                is FavoriteEntityStation -> favoriteEntityStationDao.insertOne(toAddOrUpdate)
            }

            Log.d(TAG, "Favorite added or updated, default name : ${toAddOrUpdate.defaultName}")
        }
    }

    fun updateFavoriteCustomNameByFavoriteId(idToUpdate: String, newCustomName: String) {

        coroutineScopeIO.launch {
            if (idToUpdate.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.updateCustomNameByFavoriteId(idToUpdate, newCustomName)
            } else {
                favoriteEntityStationDao.updateCustomNameByFavoriteId(idToUpdate, newCustomName)
            }
        }
    }

    fun updateFavoriteUiIndexByFavoriteId(idToUpdate: String, newUiIndex: Int) {
        coroutineScopeIO.launch {
            if (idToUpdate.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.updateUiIndexByFavoriteId(idToUpdate, newUiIndex)
            } else {
                favoriteEntityStationDao.updateUiIndexByFavoriteId(idToUpdate, newUiIndex)
            }
        }
    }

    fun removeFavoriteByFavoriteId(idToRemove: String) {
        coroutineScopeIO.launch {
            if (idToRemove.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
                favoriteEntityPlaceDao.deleteOne(idToRemove)
            } else {
                favoriteEntityStationDao.deleteOne(idToRemove)
            }
        }
    }

    fun getFavoriteEntityByFavoriteId(favoriteId: String?): FavoriteEntityBase? {
        if (favoriteId == null)
            return null

        return if (favoriteId.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX))
            getFavoritePlaceByFavoriteId(favoriteId)
        else
            getFavoriteStationByFavoriteId(favoriteId)
    }

    private fun getFavoriteStationByFavoriteId(favoriteId: String): FavoriteEntityStation? {
        return favoriteEntityStationDao.getForId(favoriteId)
    }

    private fun getFavoritePlaceByFavoriteId(favoriteId: String): FavoriteEntityPlace? {
        return favoriteEntityPlaceDao.getForId(favoriteId)
    }

    fun getFavoriteCount(): Int {
        return favoriteEntityPlaceDao.count + favoriteEntityStationDao.count
    }

    fun getAllFavoriteStationIdList(): List<String> {
        return favoriteEntityStationDao.allId
    }

    fun isFavoriteId(id: String?): Boolean {
        return id == null || (favoriteEntityPlaceDao.isFavoriteId(id) != 0 || favoriteEntityStationDao.isFavoriteId(id) != 0)
    }

    fun hasAtLeastNValidStationFavorites(nearestStationId: String, n: Int): Boolean {
        return favoriteEntityStationDao.validFavoriteCount(nearestStationId) >= n
    }

    fun getBikeSystemStationData(ctx: Context): LiveData<List<BikeStation>>{
        initializeData(ctx)
        return curBikeSystemStatusData
    }

    fun getBikeSystemListData(ctx: Context): LiveData<List<BikeSystem>> {
        initializeData(ctx)
        return availableBikeSystemList
    }

    fun getCurrentBikeSystem(): LiveData<BikeSystem> {
        return curBikeSystemData
    }

    fun setCurrentBikeSystem(ctx: Context, toSet: BikeSystem, alsoFetchStatus: Boolean = false) {
        Log.d(TAG, "setting current bike system to : $toSet")
        currentBikeSystemDao.deleteCurrentBikeSystem()
        currentBikeSystemDao.insertCurrentBikeSystem(toSet)
        if (alsoFetchStatus)
            startFetchBikeSystemStatusDataService(ctx, toSet.citybikDOTesUrl)
    }

    fun invalidateCurrentBikeSystem(ctx: Context) {
        startFetchBikeSystemListDataService(ctx)
    }

    fun invalidateBikeSystemStatus(ctx: Context, systemHRef: String) {
        startFetchBikeSystemStatusDataService(ctx, systemHRef)
    }

    private fun isFetchNeeded(): Boolean {
        return 0 == currentBikeSystemDao.countCurrentBikeSystem()
    }

    private fun isBikeSystemStatusFetchNeeded(): Boolean {
        return false
    }

    private fun startFetchBikeSystemStatusDataService(ctx: Context, systemHRef: String) {
        bikeSystemStatusNetworkDataSource.startFetchBikeSystemStatusService(ctx, systemHRef)
    }

    private fun startFetchBikeSystemListDataService(ctx: Context) {
        bikeSystemListNetworkDataSource.startFetchingBikeSystemListService(ctx)
    }

    companion object {
        private val TAG = FindMyBikesRepository::class.java.simpleName

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: FindMyBikesRepository? = null

        @Synchronized
        fun getInstance(
                bikeSystemDao: BikeSystemDao,
                bikeStationDao: BikeStationDao,
                favoriteEntityPlaceDao: FavoriteEntityPlaceDao,
                favoriteEntityStationDao: FavoriteEntityStationDao,
                bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
                bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource): FindMyBikesRepository {
            //Log.d(TAG, "Getting the repository")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = FindMyBikesRepository(bikeSystemDao,
                            bikeStationDao,
                            favoriteEntityPlaceDao,
                            favoriteEntityStationDao,
                            bikeSystemListNetworkDataSource,
                            bikeSystemStatusNetworkDataSource)
                    Log.d(TAG, "Made new repository")
                }
            }
            return sInstance!!
        }
    }
}