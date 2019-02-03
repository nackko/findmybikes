package com.ludoscity.findmybikes.data.network

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.citybik_es.model.BikeSystemStatusAnswerRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.util.HashMap

class BikeSystemNetworkDataSource private constructor(){

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    // The number of days we want our API to return, set to 14 days or two weeks


    // Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
    // writing out a bunch of multiplication ourselves and risk making a silly mistake.


    private val downloadedBikeSystemStationData: MutableLiveData<Array<BikeStation>> = MutableLiveData()

    val stationData : LiveData<Array<BikeStation>>
        get() = downloadedBikeSystemStationData

    fun startFetchBikeSystemService(ctx: Context){
        val intentToFetch = Intent(ctx, FetchBikeSystemIntentService::class.java)
        intentToFetch.putExtra("system_href", "httpPROUT")

        FetchBikeSystemIntentService.enqueueWork(ctx, intentToFetch)

    }

    //TODO: non manual data refresh
    /*fun scheduleRecurringFetchBikeSystemSync(){
        //Work manager
    }*/

    fun fetchBikeSystem(citybik_esAPI : Citybik_esAPI, bikeSystemHRef: String){
        coroutineScopeIO.launch {
            val UrlParams = HashMap<String, String>()
            UrlParams["fields"] = "stations"

            val call = citybik_esAPI.getBikeNetworkStatus(bikeSystemHRef, UrlParams)

            val statusAnswer: Response<BikeSystemStatusAnswerRoot>

            try {
                statusAnswer = call.execute()

                //TODO: remove copy in Rootapplication
                val newBikeNetworkStationList = RootApplication.addAllToBikeNetworkStationList(statusAnswer.body()!!.network.bikeStationList!!)


                downloadedBikeSystemStationData.postValue(statusAnswer.body()!!.network.bikeStationList)
                //Calculate bounds - TODO: this should happen in some code observing data changes
                /*val boundsBuilder = LatLngBounds.Builder()

                for (station in statusAnswer.body()!!.network.bikeStationList) {
                    boundsBuilder.include(station.location)
                }

                mDownloadedBikeNetworkBounds = boundsBuilder.build()*/

            } catch (e: IOException) {

                //server level error, could not retrieve bike system data
                Log.w(TAG, "Exception raised trying to fetch -- Aborted")
            }


        }
    }


    companion object {
        private val TAG = BikeSystemNetworkDataSource::class.java.simpleName

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: BikeSystemNetworkDataSource? = null

        /**
         * Get the singleton for this class
         */
        fun getInstance(): BikeSystemNetworkDataSource {
            Log.d(TAG, "Getting thesingle bike system network data source")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = BikeSystemNetworkDataSource()
                    Log.d(TAG, "Made new single bike system network data source")
                }
            }
            return sInstance!!
        }
    }
}