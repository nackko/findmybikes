package com.ludoscity.findmybikes.ui.trip

import android.app.Application
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.utils.Utils
import com.ludoscity.findmybikes.utils.asLatLng
import java.text.NumberFormat

class TripFragmentViewModel(app: Application,
                            private val userLoc: LiveData<Location>,
                            private val stationALatLng: LiveData<LatLng>,
                            private val stationBLatLng: LiveData<LatLng>,
                            private val finalDestination: LiveData<LatLng>,
                            isFinalDestFavorite: LiveData<Boolean>,
                            numFormat: NumberFormat) : AndroidViewModel(app) {

    //convenience
    private val userLocToStationAWalkingDurationMin = MutableLiveData<Int>()
    private val stationAToStationBBikingDurationMin = MutableLiveData<Int>()
    private val stationBToFinalDestWalkingDurationMin = MutableLiveData<Int>()
    private val totalTripDurationMin = MutableLiveData<Int>()

    private val stationALatLngObserver: Observer<LatLng>
    private val stationBLatLngObserver: Observer<LatLng>
    private val userLocObserver: Observer<Location>
    private val finalDestObserver: Observer<LatLng>
    private val isFinalDestFavoriteObserver: Observer<Boolean>

    private val locToStationADurationString = MutableLiveData<String>()
    private val stationAToStationBDurationString = MutableLiveData<String>()
    private val stationBToFinalDestDurationString = MutableLiveData<String>()
    private val totalTripDurationString = MutableLiveData<String>()
    private val finalDestIconResId = MutableLiveData<Int>()
    private val lastRowVisible = MutableLiveData<Boolean>()

    private val locToAGoogleMapDirectionsIntent = MutableLiveData<Intent>()
    private val aToBGoogleMapDirectionsIntent = MutableLiveData<Intent>()
    private val bToDestGoogleMapDirectionsIntent = MutableLiveData<Intent>()

    private val lastStartActData = MutableLiveData<Intent>()
    val lastStartActivityIntent: LiveData<Intent>
        get() = lastStartActData

    fun requestStartActivity(intent: Intent) {
        lastStartActData.value = intent
    }

    fun clearLastStartActivityRequest() {
        lastStartActData.value = null
    }

    val locToStationAText: LiveData<String>
        get() = locToStationADurationString

    val stationAToStationBText: LiveData<String>
        get() = stationAToStationBDurationString

    val stationBToFinalDestText: LiveData<String>
        get() = stationBToFinalDestDurationString

    val totalTripText: LiveData<String>
        get() = totalTripDurationString

    val finalDestinationIconResId: LiveData<Int>
        get() = finalDestIconResId

    val isLastRowVisible: LiveData<Boolean>
        get() = lastRowVisible

    init {

        userLocObserver = Observer {

            val locToA = Utils.getWalkingDurationBetweenInMinutes(it.asLatLng(),
                    stationALatLng.value,
                    getApplication())

            userLocToStationAWalkingDurationMin.value = locToA

            val statAToStatB = stationAToStationBBikingDurationMin.value
            val statBToFinal = stationBToFinalDestWalkingDurationMin.value

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)

            locToAGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(it.asLatLng(),
                    stationALatLng.value,
                    true)
        }

        userLoc.observeForever(userLocObserver)

        stationALatLngObserver = Observer {

            val locToA = Utils.getWalkingDurationBetweenInMinutes(userLoc.value?.asLatLng(),
                    it,
                    getApplication())

            userLocToStationAWalkingDurationMin.value = locToA

            val statAToStatB = Utils.getBikingDurationBetweenInMinutes(it,
                    stationBLatLng.value,
                    getApplication())

            stationAToStationBBikingDurationMin.value = statAToStatB

            val statBToFinal = stationBToFinalDestWalkingDurationMin.value

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)

            locToAGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(userLoc.value?.asLatLng(),
                    it,
                    true)

            aToBGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(it,
                    stationBLatLng.value,
                    false)
        }

        stationALatLng.observeForever(stationALatLngObserver)

        stationBLatLngObserver = Observer {

            val locToA = userLocToStationAWalkingDurationMin.value

            val statAToStatB = Utils.getBikingDurationBetweenInMinutes(stationALatLng.value,
                    it,
                    getApplication())

            stationAToStationBBikingDurationMin.value = statAToStatB

            val statBToFinal = Utils.getWalkingDurationBetweenInMinutes(
                    stationBLatLng.value,
                    finalDestination.value,
                    getApplication())

            stationBToFinalDestWalkingDurationMin.value = statBToFinal

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)

            aToBGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(stationALatLng.value,
                    it,
                    false)

            bToDestGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(it,
                    finalDestination.value,
                    true)

        }

        stationBLatLng.observeForever(stationBLatLngObserver)

        finalDestObserver = Observer {

            lastRowVisible.value = it != null

            val locToA = userLocToStationAWalkingDurationMin.value
            val statAToStatB = stationAToStationBBikingDurationMin.value

            val statBToFinal = Utils.getWalkingDurationBetweenInMinutes(
                    stationBLatLng.value,
                    it,
                    getApplication())

            stationBToFinalDestWalkingDurationMin.value = statBToFinal

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)

            bToDestGoogleMapDirectionsIntent.value = prepareLaunchGoogleMapsForDirections(stationBLatLng.value,
                    it,
                    true)
        }

        finalDestination.observeForever(finalDestObserver)

        isFinalDestFavoriteObserver = Observer {
            if (it == true) {
                finalDestIconResId.value = R.drawable.ic_pin_favorite_24dp_black
            } else {
                finalDestIconResId.value = R.drawable.ic_pin_search_24dp_black
            }
        }

        isFinalDestFavorite.observeForever(isFinalDestFavoriteObserver)

        userLocToStationAWalkingDurationMin.observeForever {
            locToStationADurationString.value = Utils.durationToProximityString(it, true, numFormat, getApplication())
        }

        stationAToStationBBikingDurationMin.observeForever {
            stationAToStationBDurationString.value = Utils.durationToProximityString(it, true, numFormat, getApplication())
        }

        stationBToFinalDestWalkingDurationMin.observeForever {
            stationBToFinalDestDurationString.value =
                    Utils.durationToProximityString(it, true, numFormat, getApplication())
        }

        totalTripDurationMin.observeForever {
            totalTripDurationString.value =
                    Utils.durationToProximityString(it, false, numFormat, getApplication())
        }
    }

    fun locToStationADirectionsFabClick() {
        lastStartActData.value = locToAGoogleMapDirectionsIntent.value
    }

    fun stationAToStationBDirectionsFabClick() {
        lastStartActData.value = aToBGoogleMapDirectionsIntent.value
    }

    fun stationBTofinalDestinationDirectionsFabClick() {
        lastStartActData.value = bToDestGoogleMapDirectionsIntent.value
    }

    private fun recalculateTripTotal(statBToFinal: Int?, statAToStatB: Int?, locToA: Int?) {
        //Hopefully this does an addition or a null if any of the val are null
        val totalTripDuration = statBToFinal?.let { it1 -> statAToStatB?.let { itFirst -> locToA?.plus(itFirst) }?.plus(it1) }

        if (totalTripDuration != null) {
            totalTripDurationMin.value = totalTripDuration
            //TODO: show last row in fragment
        } else {
            totalTripDurationMin.value = statAToStatB?.let { it1 -> locToA?.plus(it1) }
        }
    }

    private fun prepareLaunchGoogleMapsForDirections(origin: LatLng?, destination: LatLng?, walking: Boolean): Intent? {

        var toReturn: Intent? = null
        origin?.let {
            destination?.let {

                val builder = StringBuilder("http://maps.google.com/maps?&saddr=")

                builder.append(origin.latitude).append(",").append(origin.longitude)

                builder.append("&daddr=").append(destination.latitude).append(",").append(destination.longitude).append("&dirflg=")//append("B"). Labeling doesn't work :'(

                if (walking)
                    builder.append("w")
                else
                    builder.append("b")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(builder.toString()))
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
                toReturn = intent
                //if (getApplication<Application>().packageManager.queryIntentActivities(intent, 0).size > 0) {
                //    getApplication<Application>().startActivity(intent) // launch the map activity
                //}
                /*else {
                    Utils.Snackbar.makeStyled(coordinatorLayout, R.string.google_maps_not_installed,
                            Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                            .show()
                }*/
            }
        }

        return toReturn
    }


    override fun onCleared() {
        userLoc.removeObserver(userLocObserver)
        stationALatLng.removeObserver(stationALatLngObserver)
        stationBLatLng.removeObserver(stationBLatLngObserver)
        finalDestination.removeObserver(finalDestObserver)
    }
}