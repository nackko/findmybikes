package com.ludoscity.findmybikes.data.geolocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.tracking.AnalTrackingDatapoint
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivity
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import com.ludoscity.findmybikes.utils.asLatLng
import com.ludoscity.findmybikes.utils.asString
import org.jetbrains.anko.intentFor

/**
 * Created by F8Full on 2019-07-02. This file is part of #findmybikes
 * A service class to monitor user activities transitions via activity transition recognition API
 * https://developer.android.com/guide/topics/location/transitions
 * As soon as it is started, it promotes itself to foreground. If repo indicates geotracking should happen
 * it subrscribes to the fusedlocaitonclient for updates every second. being a foreground service it can do that
 */
class TransitionRecognitionService : Service() {

    companion object {
        private val TAG = LocationTrackingService::class.java.simpleName
        var isTrackingActivityTransition = false
        private const val CHANNEL_ID = "fmb_channel_00"
        private const val PACKAGE_NAME =
                "com.ludoscity.findmybikes.data.geolocation"
        private const val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 696

        private var UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000
        private var SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000

    }

    private lateinit var repo: FindMyBikesRepository
    private lateinit var transitionRecognitionReq: ActivityTransitionRequest
    private lateinit var pendingIntent: PendingIntent
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var serviceHandler: Handler

    private lateinit var notifBuilder: NotificationCompat.Builder
    private lateinit var notifManager: NotificationManager
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

            notifBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
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
                notifBuilder.setChannelId(CHANNEL_ID)
            }

            return notifBuilder.build()
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

        repo.isTrackingGeolocation.observeForever {
            if (it == true) {
                Log.d(TAG, "Requesting location updates for tracing")
                //Utils.setRequestingLocationUpdates(this, true)
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest,
                            locationCallback, Looper.myLooper())
                    notifBuilder.setContentTitle("Tracing your bike trip")
                    notifManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notifBuilder.build())

                    repo.insertInDatabase(AnalTrackingDatapoint(
                            description = "TransitionRecognitionService--requestLocationUpdates-success"
                    ))
                } catch (unlikely: SecurityException) {
                    //Utils.setRequestingLocationUpdates(this, false)
                    Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
                }
            } else {
                Log.d(TAG, "Removing location updates for tracing")
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    //This is saved in repo (sharedPref) in the sample
                    //TODO: in our case : it meas the user wants to stop the tracing mode ??
                    //Utils.setRequestingLocationUpdates(this, false)
                    //stopSelf()
                    notifBuilder.setContentTitle("Waiting for next bike trip")
                    notifManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notifBuilder.build())
                    repo.insertInDatabase(AnalTrackingDatapoint(
                            description = "TransitionRecognitionService--removeLocationUpdates-success"
                    ))
                } catch (unlikely: SecurityException) {
                    //Utils.setRequestingLocationUpdates(this, true)
                    Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
                }
            }
        }

        val transitions = mutableListOf<ActivityTransition>()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        //.setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.RUNNING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.RUNNING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()

        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()

        transitionRecognitionReq = ActivityTransitionRequest(transitions)

        val intent = applicationContext.intentFor<TransitionRecognitionIntentService>()
        pendingIntent = PendingIntent.getService(applicationContext,
                999, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Set the Notification Channel for the Notification Manager.
            notifManager.createNotificationChannel(mChannel)
        }

        super.onCreate()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = SHORTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        goForeground()

        startTransitionUpdate()

        return START_STICKY
    }

    override fun onDestroy() {

        stopTransitionUpdates()

        super.onDestroy()
    }

    private fun goForeground() {
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startTransitionUpdate() {
        // Start updates
        val task = ActivityRecognition
                .getClient(this)
                .requestActivityTransitionUpdates(transitionRecognitionReq, pendingIntent)

        task.addOnSuccessListener {
            isTrackingActivityTransition = true
            repo.insertInDatabase(AnalTrackingDatapoint(
                    description = "TransitionRecognitionService--startTransitionUpdate-success"
            ))
        }

        task.addOnFailureListener { e ->
            repo.insertInDatabase(AnalTrackingDatapoint(
                    description = "TransitionRecognitionService--startTransitionUpdate-failure"
            ))
            //stopSelf()
        }
    }

    private fun stopTransitionUpdates() {
        Log.e("prout", "Stopping updates")
        // Stop updates
        val task = ActivityRecognition
                .getClient(this)
                .removeActivityTransitionUpdates(pendingIntent)

        task.addOnSuccessListener {
            pendingIntent.cancel()
            isTrackingActivityTransition = false
            repo.insertInDatabase(AnalTrackingDatapoint(
                    description = "TransitionRecognitionService--removeActivityTransitionUpdates-success"
            ))
        }

        task.addOnFailureListener { e ->
            repo.insertInDatabase(AnalTrackingDatapoint(
                    description = "TransitionRecognitionService--removeActivityTransitionUpdates-failure"
            ))
        }
    }
}