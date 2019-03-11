package com.ludoscity.findmybikes.ui.table

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView.NO_POSITION
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.utils.Utils
import java.text.NumberFormat
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * ViewModel for handling StationMapFragment data prep for UI and business logic
 */

//TODO: try removing dependency on context by having a SettingsRepository
//Context is used to retrieve data from "dbHelper" in SharedPref file

//Model should prep and expose data for page to display

class TableFragmentViewModel(repo: FindMyBikesRepository, application: Application,
                             lookingforBike: Boolean, numFormat: NumberFormat) : AndroidViewModel(application) {


    private val DEBUG_outdated = false

    val tableItemDataList: LiveData<List<StationTableItemData>>
        get() = tableItemList
    private var selectedPos: Int = NO_POSITION
    //TODO: expose selected BikeStation data in activity model

    val isDataOutOfDate: LiveData<Boolean>
        get() = dataOutdated

    val sortedAvailabilityDataString: LiveData<String>
        get() = sortedAvailabilityDataStr

    val nearestAvailabilityLatLng: LiveData<LatLng>
        get() = nearestAvailabilityLatLong


    //val isLookingForBike: LiveData<Boolean>
    //    get() = lookingForBike

    private val tableItemList: MutableLiveData<List<StationTableItemData>>

    private var showProximity = false

    //private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val repository: FindMyBikesRepository = repo

    private lateinit var comparator: BaseBikeStationComparator

    var respondToClick = true

    private val sortedAvailabilityDataStr: MutableLiveData<String>
    private val nearestAvailabilityLatLong: MutableLiveData<LatLng>

    //TODO: move to activity model (map needs it too)
    private val dataOutdated: MutableLiveData<Boolean>
    //TODO: move to activity model (map needs it too)
    //private val lookingForBike: MutableLiveData<Boolean>

    companion object {
        val AVAILABILITY_POSTFIX_START_SEQUENCE = "_AVAILABILITY_"
        val AOK_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "AOK"
        val BAD_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "BAD"
        val CRITICAL_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "CRI"
        internal val LOCKED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "LCK"
        internal val OUTDATED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "OUT"
        internal val ERROR_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "ERR"

    }


    //TODO: come up with a design that doesn't require dynamic casting
    //Have a base comparator class that calculates proximity String
    abstract class BaseBikeStationComparator : Comparator<BikeStation> {
        abstract fun getProximityString(station: BikeStation, lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String
    }


    class DistanceComparator : BaseBikeStationComparator, Parcelable {
        override fun getProximityString(station: BikeStation, lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String {
            return if (lookingForBike) {
                Utils.getWalkingProximityString(
                        station.location, distanceRef, false, numFormat, ctx)
            } else {
                Utils.getBikingProximityString(
                        station.location, distanceRef, false, numFormat, ctx)
            }
        }

        internal var distanceRef: LatLng

        constructor(_fromLatLng: LatLng) {
            distanceRef = _fromLatLng
        }

        override fun compare(lhs: BikeStation, rhs: BikeStation): Int {
            return (lhs.getMeterFromLatLng(distanceRef) - rhs.getMeterFromLatLng(distanceRef)).toInt()
        }

        private constructor(`in`: Parcel) {

            val latitude = `in`.readDouble()
            val longitude = `in`.readDouble()
            distanceRef = LatLng(latitude, longitude)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeDouble(distanceRef.latitude)
            dest.writeDouble(distanceRef.longitude)
        }

        companion object CREATOR : Parcelable.Creator<DistanceComparator> {
            override fun createFromParcel(parcel: Parcel): DistanceComparator {
                return DistanceComparator(parcel)
            }

            override fun newArray(size: Int): Array<DistanceComparator?> {
                return arrayOfNulls(size)
            }
        }
    }

    class TotalTripTimeComparator : BaseBikeStationComparator, Parcelable {
        override fun getProximityString(station: BikeStation,
                                        lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String {
            val totalTime = calculateWalkTimeMinute(station) + calculateBikeTimeMinute(station)

            return Utils.durationToProximityString(totalTime, false, numFormat, ctx)
        }

        private val mStationALatLng: LatLng
        private val mDestinationLatLng: LatLng?
        private val mWalkingSpeedKmh: Float
        private val mBikingSpeedKmh: Float

        private val mTimeUserToAMinutes: Int

        constructor(_walkingSpeedKmh: Float, _bikingSpeedKmh: Float, _userLatLng: LatLng,
                    _stationALatLng: LatLng, _destinationLatLng: LatLng, numFormat: NumberFormat) {

            mWalkingSpeedKmh = _walkingSpeedKmh
            mBikingSpeedKmh = _bikingSpeedKmh

            mTimeUserToAMinutes = Utils.computeTimeBetweenInMinutes(_userLatLng, _stationALatLng, mWalkingSpeedKmh)

            mStationALatLng = _stationALatLng
            mDestinationLatLng = _destinationLatLng
        }

        private constructor(`in`: Parcel) {

            var latitude = `in`.readDouble()
            var longitude = `in`.readDouble()
            mStationALatLng = LatLng(latitude, longitude)

            latitude = `in`.readDouble()
            longitude = `in`.readDouble()
            mDestinationLatLng = LatLng(latitude, longitude)

            mWalkingSpeedKmh = `in`.readFloat()
            mBikingSpeedKmh = `in`.readFloat()
            mTimeUserToAMinutes = `in`.readInt()
        }

        internal fun getUpdatedComparatorFor(_userLatLng: LatLng, _stationALatLng: LatLng?,
                                             numFormat: NumberFormat): TotalTripTimeComparator {
            return TotalTripTimeComparator(mWalkingSpeedKmh, mBikingSpeedKmh, _userLatLng,
                    _stationALatLng ?: mStationALatLng, mDestinationLatLng!!, numFormat)
        }

        override fun compare(lhs: BikeStation, rhs: BikeStation): Int {

            val lhsWalkTime = calculateWalkTimeMinute(lhs)
            val rhsWalkTime = calculateWalkTimeMinute(rhs)

            val lhsBikeTime = calculateBikeTimeMinute(lhs)
            val rhsBikeTime = calculateBikeTimeMinute(rhs)

            val totalTimeDiff = lhsWalkTime + lhsBikeTime - (rhsWalkTime + rhsBikeTime)

            return if (totalTimeDiff != 0)
                totalTimeDiff
            else
                lhsWalkTime - rhsWalkTime
        }

        internal fun calculateWalkTimeMinute(_stationB: BikeStation): Int {

            var timeBtoDestMinutes = 0

            if (mDestinationLatLng != null)
                timeBtoDestMinutes = Utils.computeTimeBetweenInMinutes(_stationB.location,
                        mDestinationLatLng, mWalkingSpeedKmh)

            return mTimeUserToAMinutes + timeBtoDestMinutes

        }

        internal fun calculateBikeTimeMinute(_stationB: BikeStation): Int {

            return Utils.computeTimeBetweenInMinutes(mStationALatLng, _stationB.location,
                    mBikingSpeedKmh)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeDouble(mStationALatLng.latitude)
            dest.writeDouble(mStationALatLng.longitude)
            dest.writeDouble(mDestinationLatLng!!.latitude)
            dest.writeDouble(mDestinationLatLng.longitude)
            dest.writeFloat(mWalkingSpeedKmh)
            dest.writeFloat(mBikingSpeedKmh)
            dest.writeInt(mTimeUserToAMinutes)
        }

        companion object CREATOR : Parcelable.Creator<TotalTripTimeComparator> {
            override fun createFromParcel(parcel: Parcel): TotalTripTimeComparator {
                return TotalTripTimeComparator(parcel)
            }

            override fun newArray(size: Int): Array<TotalTripTimeComparator?> {
                return arrayOfNulls(size)
            }
        }
    }

    init {

        tableItemList = MutableLiveData()


        dataOutdated = MutableLiveData()
        //lookingForBike = MutableLiveData<Boolean>()

        sortedAvailabilityDataStr = MutableLiveData()
        nearestAvailabilityLatLong = MutableLiveData()

        repo.getBikeSystemStationData(getApplication()).observeForever { newData ->

            val sortedStationList = newData?.toMutableList() ?: emptyList<BikeStation>()

            Collections.sort(sortedStationList, comparator)

            val newDisplayData = ArrayList<StationTableItemData>()

            //Do that in background and post the resulting list for observers to consume
            sortedStationList.forEach {

                newDisplayData.add(StationTableItemData(
                        android.R.color.transparent, //TODO: replug background color - includes item selection tracking
                        if (showProximity) comparator.getProximityString(it, lookingforBike,
                                numFormat, getApplication()) else null,
                        1.0f, //TODO replug alpha - includes item selection tracking
                        it.name, //TODO: replug having favorite name if it is a favorite
                        1.0f, //TODO replug alpha - includes item selection tracking
                        when {
                            lookingforBike -> numFormat.format(it.freeBikes)
                            it.emptySlots == -1 -> "--"
                            else -> numFormat.format(it.emptySlots)
                        },
                        1.0f, //TODO replug alpha - includes item selection tracking
                        DEBUG_outdated, Typeface.DEFAULT_BOLD, it.locationHash
                ))
            }

            //////////////////////////
            val availabilityDataPostfixBuilder = StringBuilder()

            if (DEBUG_outdated) {
                availabilityDataPostfixBuilder.append(sortedStationList[0].locationHash).append(OUTDATED_AVAILABILITY_POSTFIX)
            } else {
                var badOrAOKStationCount = 0
                val minimumServiceCount = Math.round(sortedStationList.size * 0.1)   //10%

                for (stationItem in sortedStationList) { //SORTED BY DISTANCE
                    if (lookingforBike) {
                        if (!stationItem.isLocked) {

                            if (stationItem.freeBikes > DBHelper.getInstance().getCriticalAvailabilityMax(getApplication())) {

                                if (badOrAOKStationCount == 0) {
                                    if (stationItem.freeBikes <= DBHelper.getInstance().getBadAvailabilityMax(getApplication())) {

                                        availabilityDataPostfixBuilder.insert(0, stationItem.locationHash + BAD_AVAILABILITY_POSTFIX)
                                    } else {
                                        availabilityDataPostfixBuilder.insert(0, stationItem.locationHash + AOK_AVAILABILITY_POSTFIX)
                                    }
                                }

                                ++badOrAOKStationCount

                                if (badOrAOKStationCount >= minimumServiceCount)
                                    break
                            } else if (badOrAOKStationCount == 0) {
                                availabilityDataPostfixBuilder.append(stationItem.locationHash)
                                        .append(CRITICAL_AVAILABILITY_POSTFIX)
                            }
                        } else if (badOrAOKStationCount == 0) {
                            availabilityDataPostfixBuilder.append(stationItem.locationHash)
                                    .append(LOCKED_AVAILABILITY_POSTFIX)
                        }
                    } else {  //A locked station accepts bike returns

                        if (stationItem.emptySlots == -1 || stationItem.emptySlots > DBHelper.getInstance().getCriticalAvailabilityMax(getApplication())) {

                            if (badOrAOKStationCount == 0) {

                                if (stationItem.emptySlots != -1 && stationItem.emptySlots <= DBHelper.getInstance().getBadAvailabilityMax(getApplication())) {

                                    availabilityDataPostfixBuilder.insert(0, stationItem.locationHash + BAD_AVAILABILITY_POSTFIX)
                                } else {

                                    availabilityDataPostfixBuilder.insert(0, stationItem.locationHash + AOK_AVAILABILITY_POSTFIX)
                                }
                            }

                            ++badOrAOKStationCount

                            if (badOrAOKStationCount >= minimumServiceCount)
                                break
                        } else if (badOrAOKStationCount == 0) {
                            availabilityDataPostfixBuilder.append(stationItem.locationHash)
                                    .append(CRITICAL_AVAILABILITY_POSTFIX)
                        }
                    }
                }

                //failsafe if no bike could be found
                if (badOrAOKStationCount == 0 && !sortedStationList.isEmpty())
                    availabilityDataPostfixBuilder.append(sortedStationList[0].locationHash).append(LOCKED_AVAILABILITY_POSTFIX)
                else if (badOrAOKStationCount != 0 && badOrAOKStationCount < minimumServiceCount) {
                    //if less than 10% of the network could provide service but a bike could still be found, let's prevent tweeting from happening
                    val firstBadOrOkIdx = if (availabilityDataPostfixBuilder.indexOf(BAD_AVAILABILITY_POSTFIX) != -1)
                        availabilityDataPostfixBuilder.indexOf(BAD_AVAILABILITY_POSTFIX)
                    else
                        availabilityDataPostfixBuilder.indexOf(AOK_AVAILABILITY_POSTFIX)

                    availabilityDataPostfixBuilder.replace(firstBadOrOkIdx, firstBadOrOkIdx + ERROR_AVAILABILITY_POSTFIX.length, ERROR_AVAILABILITY_POSTFIX)
                }
            }

            val availabilityDataString = availabilityDataPostfixBuilder.toString()

            sortedAvailabilityDataStr.value = availabilityDataString

            val extractedStationId = Utils.extractNearestAvailableStationIdFromDataString(availabilityDataString)

            nearestAvailabilityLatLong.value =
                    sortedStationList.find { it.locationHash.equals(extractedStationId, ignoreCase = true) }?.location
        }

        //TODO: observe data being out of date, if it happens, rebuild tableItemDataList
        //TODO: observe new user location available, requiring resorting and reprocessing of sorted list
    }


    //////////////////////////
    //TODO: replug having favorite name if it is a favorite
    /*if (mFavoriteListViewModel.isFavorite(_station.locationHash))
        mName.text = mFavoriteListViewModel.getFavoriteEntityForId(_station.locationHash)!!.getSpannedDisplayName(mCtx, false)
    else
        mName.text = _station.name*/
    //TODO: replug selection tracking, background color and alpha
    /*private fun setColorAndTransparencyFeedback(selected: Boolean, availabilityValue: Int) {

        if (!mOutdatedAvailability) {

            if (availabilityValue != -1 && availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(mCtx)) {
                if (selected) {
                    itemView.setBackgroundResource(R.color.stationtable_item_selected_background_red)
                    mProximity.alpha = 1f
                    mName.alpha = 1f
                    mAvailability.alpha = 1f
                } else {
                    itemView.setBackgroundResource(R.color.stationtable_item_background_red)
                    val alpha = mCtx.resources.getFraction(R.fraction.station_item_critical_availability_alpha, 1, 1)
                    mProximity.alpha = alpha
                    mName.alpha = alpha
                    mAvailability.alpha = alpha
                }
            } else if (availabilityValue != -1 && availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(mCtx)) {
                if (selected) {
                    itemView.setBackgroundResource(R.color.stationtable_item_selected_background_yellow)
                    mProximity.alpha = 1f
                    mName.alpha = 1f
                    mAvailability.alpha = 1f
                } else {
                    itemView.setBackgroundResource(R.color.stationtable_item_background_yellow)
                    mName.alpha = mCtx.resources.getFraction(R.fraction.station_item_name_bad_availability_alpha, 1, 1)
                    mAvailability.alpha = mCtx.resources.getFraction(R.fraction.station_item_availability_bad_availability_alpha, 1, 1)
                    mProximity.alpha = 1f
                }
            } else {
                if (selected)
                    itemView.setBackgroundResource(R.color.stationtable_item_selected_background_green)
                else
                    itemView.setBackgroundResource(android.R.color.transparent)

                mName.alpha = 1f
                mAvailability.alpha = 1f
                mProximity.alpha = 1f
            }
        } else {
            if (selected)
                itemView.setBackgroundResource(R.color.theme_accent)
            else
                itemView.setBackgroundResource(android.R.color.transparent)

            mAvailability.alpha = mCtx.resources.getFraction(R.fraction.station_item_critical_availability_alpha, 1, 1)
            mProximity.alpha = 1f
            mName.alpha = 1f
        }
    }*/
    //TODO: replug list sorting keeps track of selection
    /*private fun sortStationList() {
        var selectedIdBefore: String? = null

        if (null != selected)
            selectedIdBefore = selected!!.locationHash

        if (sortComparator != null)
            Collections.sort(mStationList, sortComparator)

        if (selectedIdBefore != null)
            setSelection(selectedIdBefore, false)
    }
    val selected: BikeStation?
        get() {
            var toReturn: BikeStation? = null

            if (selectedPos != NO_POSITION && selectedPos < mStationList.size)
                toReturn = mStationList[selectedPos]

            return toReturn
        }
    */
    //TODO: replug selected item in recyclerview tracking
    /*fun setSelection(_stationId: String?, unselectOnTwice: Boolean): Int {

        return setSelectedPos(getStationItemPositionInList(_stationId), unselectOnTwice)
    }

    fun clearSelection() {
        val selectedBefore = selectedPos
        selectedPos = NO_POSITION

        if (selectedBefore != NO_POSITION)
            notifyItemChanged(selectedBefore)
    }
    fun setSelectedPos(pos: Int, unselectedOnTwice: Boolean): Int {

        var toReturn = NO_POSITION

        if (selectedPos == pos)
            if (unselectedOnTwice)
                clearSelection()
            else
                toReturn = selectedPos
        else {
            notifyItemChanged(selectedPos)
            selectedPos = pos
            notifyItemChanged(pos)
            toReturn = selectedPos
        }

        return toReturn
    }*/
}
