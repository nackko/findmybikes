package com.ludoscity.findmybikes.data.network

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.data.network.FetchCitybikDOTesDataIntentService.Companion.ACTION_FETCH_SYSTEM_STATUS
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemStatus
import com.ludoscity.findmybikes.data.network.citybik_es.BikeSystemStatusAnswerRoot
import com.ludoscity.findmybikes.data.network.citybik_es.Citybik_esAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.util.*

class BikeSystemStatusNetworkDataSource private constructor() {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    // The number of days we want our API to return, set to 14 days or two weeks


    // Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
    // writing out a bunch of multiplication ourselves and risk making a silly mistake.


    private val downloadedBikeSystemStatus: MutableLiveData<BikeSystemStatus> = MutableLiveData()

    val data: LiveData<BikeSystemStatus>
        get() = downloadedBikeSystemStatus

    fun startFetchBikeSystemService(ctx: Context){
        val intentToFetch = Intent(ctx, FetchCitybikDOTesDataIntentService::class.java)
        intentToFetch.action = ACTION_FETCH_SYSTEM_STATUS
        intentToFetch.putExtra("system_href", "/v2/networks/velov")

        FetchCitybikDOTesDataIntentService.enqueueWork(ctx, intentToFetch)
    }

    //TODO: non manual data refresh
    /*fun scheduleRecurringFetchBikeSystemSync(){
        //Work manager
    }*/

    fun fetchBikeSystemStatus(citybik_esAPI: Citybik_esAPI, bikeSystemHRef: String) {
        coroutineScopeIO.launch {
            val UrlParams = HashMap<String, String>()
            UrlParams["fields"] = "stations,id"

            val call = citybik_esAPI.getBikeNetworkStatus(bikeSystemHRef, UrlParams)

            val statusAnswer: Response<BikeSystemStatusAnswerRoot>

            try {
                statusAnswer = call.execute()

                //TODO: remove copy in Rootapplication
                //TODO: record a copy for working when API is down
                val newBikeNetworkStationList = RootApplication.addAllToBikeNetworkStationList(statusAnswer.body()!!.network.bikeStationList!!)


                downloadedBikeSystemStatus.postValue(statusAnswer.body()!!.network)

            } catch (e: IOException) {

                //server level error, could not retrieve bike system data
                Log.w(TAG, "Exception raised trying to fetch bike system status -- Aborted")
            }


        }
    }


    companion object {
        private val TAG = BikeSystemStatusNetworkDataSource::class.java.simpleName

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: BikeSystemStatusNetworkDataSource? = null

        /**
         * Get the singleton for this class
         */
        fun getInstance(): BikeSystemStatusNetworkDataSource {
            //Log.d(TAG, "Getting thesingle bike system network data source")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = BikeSystemStatusNetworkDataSource()
                    Log.d(TAG, "Made new single bike system network data source")
                }
            }
            return sInstance!!
        }
    }
}