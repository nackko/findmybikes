package com.ludoscity.findmybikes.data.geolocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivity
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import com.ludoscity.findmybikes.utils.asLatLng
import com.ludoscity.findmybikes.utils.asString

class LocationTrackingService : Service {

    constructor()

    private lateinit var repo: FindMyBikesRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val binder = LocalBinder()

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private var changingConfiguration = false

    private lateinit var notificationManager: NotificationManager

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    private lateinit var serviceHandler: Handler

    /**
     * Returns the [NotificationCompat] used as part of the foreground service.
     */
    private// Extra to help us figure out if we arrived in onStartCommand via the notification or not.
    // The PendingIntent that leads to a call to onStartCommand() in this service.
    // The PendingIntent to launch activity.
    // Set the Channel ID for Android O.
    // Channel ID
    val notification: Notification
        get() {
            val intent = Intent(this, LocationTrackingService::class.java)

            val text = repo.userLocation.value?.asLatLng()?.asString()
            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
            val servicePendingIntent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            val activityPendingIntent = PendingIntent.getActivity(this, 0,
                    Intent(this, FindMyBikesActivity::class.java), 0)

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .addAction(R.drawable.ic_launch_black_24dp, getString(R.string.open_app),
                            activityPendingIntent)
                    .addAction(R.drawable.ic_cancel_black_24dp, getString(R.string.cancel_tracing),
                            servicePendingIntent)
                    .setContentText(text)
                    .setContentTitle(Utils.getTracingNotificationTitle(this))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(text)
                    .setWhen(System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID)
            }

            return builder.build()
        }

    override fun onCreate() {
        repo = InjectorUtils.provideRepository(application)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {

                locationResult?.let {
                    repo.onNewLocation(it.locations.getOrNull(0))
                }
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        serviceHandler = Handler(handlerThread.looper)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            // Set the Notification Channel for the Notification Manager.
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        val startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false)

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            //removeLocationUpdates()
            //stopSelf() //This is done twice, I get ot avoid the case a security exception occurs in removeLocationUpdates()
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()")
        //stopForeground(true)
        changingConfiguration = false
        return binder
    }

    override fun onRebind(intent: Intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()")
        //stopForeground(true)
        changingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i(TAG, "Last client unbound from service")

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!changingConfiguration) {
            Log.i(TAG, "Starting foreground service")

            //startForeground(NOTIFICATION_ID, notification)
        }
        return true // Ensures onRebind() is called when a client re-binds.
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
        private const val PACKAGE_NAME =
                "com.google.android.gms.location.sample.locationupdatesforegroundservice"

        private val TAG = LocationTrackingService::class.java.simpleName

        private const val CHANNEL_ID = "fmb_channel_00"

        internal const val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.location"
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        //private const val FIND_BIKES_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        //private const val TRACE_MOBILITY_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000

        private var UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
        private var SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2500

        var updateIntervalMillis: Long
            set(value) {
                //TOOD: cancel current request and put a new one in ?
                UPDATE_INTERVAL_IN_MILLISECONDS = value
                SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS = value / 2
            }
            get() = UPDATE_INTERVAL_IN_MILLISECONDS


        /**
         * The identifier for the notification displayed for the foreground service.
         */
        private const val NOTIFICATION_ID = 30062019


    }
}