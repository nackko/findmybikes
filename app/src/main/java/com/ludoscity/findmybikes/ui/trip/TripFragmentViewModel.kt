package com.ludoscity.findmybikes.ui.trip

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.utils.Utils
import java.text.NumberFormat

class TripFragmentViewModel(app: Application,
                            private val userLoc: LiveData<LatLng>,
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

    private val stationALatLngObserver: android.arch.lifecycle.Observer<LatLng>
    private val stationBLatLngObserver: Observer<LatLng>
    private val userLocObserver: Observer<LatLng>
    private val finalDestObserver: Observer<LatLng>
    private val isFinalDestFavoriteObserver: Observer<Boolean>

    private val locToStationADurationString = MutableLiveData<String>()
    private val stationAToStationBDurationString = MutableLiveData<String>()
    private val stationBToFinalDestDurationString = MutableLiveData<String>()
    private val totalTripDurationString = MutableLiveData<String>()
    private val finalDestIconResId = MutableLiveData<Int>()
    private val lastRowVisible = MutableLiveData<Boolean>()

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

            val locToA = Utils.getWalkingDurationBetweenInMinutes(it,
                    stationALatLng.value,
                    getApplication())

            userLocToStationAWalkingDurationMin.value = locToA

            val statAToStatB = stationAToStationBBikingDurationMin.value
            val statBToFinal = stationBToFinalDestWalkingDurationMin.value

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)

        }

        userLoc.observeForever(userLocObserver)

        stationALatLngObserver = Observer {

            val locToA = Utils.getWalkingDurationBetweenInMinutes(userLoc.value,
                    it,
                    getApplication())

            userLocToStationAWalkingDurationMin.value = locToA

            val statAToStatB = Utils.getBikingDurationBetweenInMinutes(it,
                    stationBLatLng.value,
                    getApplication())

            stationAToStationBBikingDurationMin.value = statAToStatB

            val statBToFinal = stationBToFinalDestWalkingDurationMin.value

            recalculateTripTotal(statBToFinal, statAToStatB, locToA)
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

    override fun onCleared() {
        userLoc.removeObserver(userLocObserver)
        stationALatLng.removeObserver(stationALatLngObserver)
        stationBLatLng.removeObserver(stationBLatLngObserver)
        finalDestination.removeObserver(finalDestObserver)
    }
}