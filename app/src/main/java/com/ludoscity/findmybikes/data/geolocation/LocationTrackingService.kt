package com.ludoscity.findmybikes.data.geolocation

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.google.android.gms.location.*
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.utils.InjectorUtils

/**
 * Created by F8Full on 2019-07-01. CANADA DAY !! This file is part of #findmybikes
 * A service class to feed location data when the main activity is in foreground
 */
class LocationTrackingService : Service() {

    private lateinit var repo: FindMyBikesRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var serviceHandler: Handler
    private val binder = LocalBinder()

    override fun onCreate() {
        repo = InjectorUtils.provideRepository(application)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {

                locationResult?.let {
                    //TODO: should that be a for each ?
                    repo.onNewLocation(it.locations.getOrNull(0))
                }
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {

        return binder
    }

    override fun onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * [SecurityException].
     */
    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")
        //Utils.setRequestingLocationUpdates(this, true)
        startService(Intent(applicationContext, LocationTrackingService::class.java))
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.myLooper())
        } catch (unlikely: SecurityException) {
            //Utils.setRequestingLocationUpdates(this, false)
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }


    /**
     * Removes location updates. Note that in this sample we merely log the
     * [SecurityException].
     */
    fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            //This is saved in repo (sharedPref) in the sample
            //TODO: in our case : it meas the user wants to stop the tracing mode ??
            //Utils.setRequestingLocationUpdates(this, false)
            stopSelf()
        } catch (unlikely: SecurityException) {
            //Utils.setRequestingLocationUpdates(this, true)
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }

    }

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result != null) {
                            repo.onNewLocation(task.result)
                        } else {
                            Log.w(TAG, "Failed to get location.")
                        }
                    }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: LocationTrackingService
            get() = this@LocationTrackingService
    }

    companion object {

        private val TAG = LocationTrackingService::class.java.simpleName

        private var UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private var SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000

        var updateIntervalMillis: Long
            set(value) {
                //TOOD: cancel current request and put a new one in ?
                UPDATE_INTERVAL_IN_MILLISECONDS = value
                SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS = value / 2
            }
            get() = UPDATE_INTERVAL_IN_MILLISECONDS
    }
}