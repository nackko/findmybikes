package com.ludoscity.findmybikes.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLngBounds
import com.ludoscity.findmybikes.data.database.BikeStation
import com.ludoscity.findmybikes.data.database.BikeStationDao
import com.ludoscity.findmybikes.data.database.BikeSystem
import com.ludoscity.findmybikes.data.database.BikeSystemDao
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
        private val bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
        private val bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource) {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private var mInitialized = false
    private val networkBikeSystemStatusData: LiveData<BikeSystemStatus> = bikeSystemStatusNetworkDataSource.data
    private val networkBikeSystemListAnswerRootData: LiveData<BikeSystemListAnswerRoot> = bikeSystemListNetworkDataSource.data

    //transient local list of all bike systems available. Model determine nearest one for DB saving with bounds of current bike system
    private val availableBikeSystemList: MutableLiveData<List<BikeSystem>> = MutableLiveData()

    init {

        networkBikeSystemStatusData.observeForever { newStatusDataFromNetwork ->

            newStatusDataFromNetwork?.let {

                coroutineScopeIO.launch {

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
                                it.name = it.extra?.name ?: "null"
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

    fun getBikeSystemStationData(ctx: Context): LiveData<List<BikeStation>>{
        initializeData(ctx)
        return stationDao.all
    }

    fun getBikeSystemListData(ctx: Context): LiveData<List<BikeSystem>> {
        initializeData(ctx)
        return availableBikeSystemList
    }

    fun getCurrentBikeSystem(): LiveData<BikeSystem> {
        return currentBikeSystemDao.single
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
                bikeSystemListNetworkDataSource: BikeSystemListNetworkDataSource,
                bikeSystemStatusNetworkDataSource: BikeSystemStatusNetworkDataSource): FindMyBikesRepository {
            //Log.d(TAG, "Getting the repository")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = FindMyBikesRepository(bikeSystemDao,
                            bikeStationDao,
                            bikeSystemListNetworkDataSource,
                            bikeSystemStatusNetworkDataSource)
                    Log.d(TAG, "Made new repository")
                }
            }
            return sInstance!!
        }
    }
}