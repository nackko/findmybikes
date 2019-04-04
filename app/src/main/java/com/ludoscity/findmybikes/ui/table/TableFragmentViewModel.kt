package com.ludoscity.findmybikes.ui.table

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.SharedPrefHelper
import com.ludoscity.findmybikes.data.database.station.BikeStation
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

class TableFragmentViewModel(repo: FindMyBikesRepository, app: Application,
                             private val isDockTable: Boolean,
                             val appBarExpanded: LiveData<Boolean>,
                             private val stationRecapDataSource: LiveData<BikeStation>,
                             private val stationSelectionDataSource: LiveData<BikeStation>,
                             private val isDataOutOfDate: LiveData<Boolean>,
                             private val userloc: LiveData<LatLng>,//TODO: might be encapsulated in a comparator, we'll see
                             numFormat: NumberFormat) : AndroidViewModel(app) {

    //TODO: header setup should happen by observing app state instead of being explicitly called on the model ?
    //at the same time, it is business logic directly related to the table


    val tableItemDataList: LiveData<List<StationTableItemData>>
        get() = tableItemList
    val tableRecapData: LiveData<StationTableRecapData>
        get() = tableRecapMutableData

    val sortedAvailabilityDataString: LiveData<String>
        get() = sortedAvailabilityDataStr

    val nearestAvailabilityLatLng: LiveData<LatLng>
        get() = nearestAvailabilityLatLong

    val stationRecapVisibility: LiveData<Boolean>
        get() = recapVisibility

    fun setRecapVisibility(toSet: Boolean) {
        recapVisibility.value = toSet
    }

    val headerFromIconResId: LiveData<Int>
        get() = headerFromIconResourceId

    fun setHeaderFromIconResId(toSet: Int?) {
        headerFromIconResourceId.value = toSet
    }

    val headerToIconResId: LiveData<Int>
        get() = headerToIconResourceId

    fun setHeaderToIconResId(toSet: Int?) {
        headerToIconResourceId.value = toSet
    }

    val stringIfEmpty: LiveData<String>
        get() = stringOnEmpty

    fun setStringIfEmpty(toSet: String) {
        stringOnEmpty.value = toSet
    }

    val headerAvailabilityText: LiveData<String>
        get() = headerAvailText

    val showProximity: LiveData<Boolean>
        get() = showProx

    fun setShowProximity(toSet: Boolean) {
        showProx.value = toSet
    }

    val isRefreshEnabled: LiveData<Boolean>
        get() = refreshGestureAvailable

    fun setRefreshEnabled(toSet: Boolean?) {
        refreshGestureAvailable.value = toSet
    }

    val isRefreshLayoutVisible: LiveData<Boolean>
        get() = showRefreshLayout

    fun setRefreshLayoutVisible(toSet: Boolean?) {
        showRefreshLayout.value = toSet
    }

    val recyclerViewAdapterPosToSmoothScollInView: LiveData<Int>
        get() = smoothScrollTargetIdx

    val lastClickedStation: LiveData<BikeStation>
        get() = lastClickedStationMutable
    private val lastClickedStationMutable: MutableLiveData<BikeStation> = MutableLiveData()
    fun setLastClickedStationById(stationId: String?) {
        val stationList = bikeSystemAvailabilityDataSource.value ?: emptyList()

        lastClickedStationMutable.value = stationList.find {
            it.locationHash == stationId
        }
    }

    private val tableItemList: MutableLiveData<List<StationTableItemData>> = MutableLiveData()

    private val tableRecapMutableData: MutableLiveData<StationTableRecapData> = MutableLiveData()

    private val refreshGestureAvailable: MutableLiveData<Boolean> = MutableLiveData()
    private val showRefreshLayout: MutableLiveData<Boolean> = MutableLiveData()


    private val smoothScrollTargetIdx: MutableLiveData<Int> = MutableLiveData()

    //private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val repository: FindMyBikesRepository = repo

    private var comparator: BaseBikeStationComparator

    private val sortedAvailabilityDataStr: MutableLiveData<String>
    private val nearestAvailabilityLatLong: MutableLiveData<LatLng>
    private val recapVisibility: MutableLiveData<Boolean> = MutableLiveData()

    private val headerFromIconResourceId: MutableLiveData<Int> = MutableLiveData()
    private val headerToIconResourceId: MutableLiveData<Int> = MutableLiveData()

    private val stringOnEmpty: MutableLiveData<String> = MutableLiveData()
    private val headerAvailText: MutableLiveData<String> = MutableLiveData()

    private val showProx: MutableLiveData<Boolean> = MutableLiveData()

    private val stationRecapDataSourceObserver: android.arch.lifecycle.Observer<BikeStation>
    private val stationSelectionDataSourceObserver: android.arch.lifecycle.Observer<BikeStation>
    private val dataOutdatedObserver: android.arch.lifecycle.Observer<Boolean>
    private val userLocObserver: android.arch.lifecycle.Observer<LatLng>

    private val bikeSystemAvailabilityDataSource: LiveData<List<BikeStation>>
    private val bikeSystemAvailabilityDataObserver: android.arch.lifecycle.Observer<List<BikeStation>>

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
    //Have a base comparator class that calculates durationText String
    abstract class BaseBikeStationComparator : Comparator<BikeStation> {
        abstract fun getProximityString(station: BikeStation, lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String?
    }


    class DistanceComparator : BaseBikeStationComparator, Parcelable {
        override fun getProximityString(station: BikeStation, lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String? {
            return if (lookingForBike) {
                Utils.getWalkingProximityString(
                        station.location, distanceRef, false, numFormat, ctx)
            } else {
                //TODO: use LiveData from model
                Utils.getBikingProximityString(
                        station.location, distanceRef, false, numFormat, ctx)
            }
        }

        private var distanceRef: LatLng

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

    /*class TotalTripTimeComparator : BaseBikeStationComparator, Parcelable {
        override fun getProximityString(station: BikeStation,
                                        lookingForBike: Boolean,
                                        numFormat: NumberFormat,
                                        ctx: Context): String? {
            val totalTime = calculateWalkTimeMinute(station) + calculateBikeTimeMinute(station)

            return Utils.durationToProximityString(totalTime, false, numFormat, ctx)
        }

        private val mStationALatLng: LatLng
        private val mDestinationLatLng: LatLng?
        private val mWalkingSpeedKmh: Float
        private val mBikingSpeedKmh: Float

        private val mTimeUserToAMinutes: Int?

        constructor(_walkingSpeedKmh: Float, _bikingSpeedKmh: Float, _userLatLng: LatLng,
                    _stationALatLng: LatLng, _destinationLatLng: LatLng) {

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
                    _stationALatLng ?: mStationALatLng, mDestinationLatLng!!)
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

        internal fun calculateWalkTimeMinute(_stationB: BikeStation): Int? {

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
    }*/

    init {

        //TODO: add user location livedata as constructor parameter and observe it to update comparator
        //comparator = DistanceComparator(LatLng(-73.567256, 45.5016889)) //mtl
        //comparator = DistanceComparator(LatLng(45.76404, 4.83566)) //lyon
        comparator = DistanceComparator(userloc.value ?: LatLng(0.0, 0.0))



        sortedAvailabilityDataStr = MutableLiveData()
        nearestAvailabilityLatLong = MutableLiveData()

        if (isDockTable) {
            headerAvailText.value = app.getString(R.string.docks)
        } else {
            headerAvailText.value = app.getString(R.string.bikes)
        }

        bikeSystemAvailabilityDataSource = repo.getBikeSystemStationData(getApplication())

        //recyclerView
        bikeSystemAvailabilityDataObserver = android.arch.lifecycle.Observer { newData ->

            //TODO: have better Room database update strategy
            //this is to protect against drop table strategy (led to inconsistency crashes in recyclerView)
            if (newData?.isNotEmpty() != false) {
                computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, isDataOutOfDate.value != false)

                computeAndEmitTableDisplayData(newData, isDataOutOfDate.value != false,
                        numFormat, isDockTable)
            }

        }

        stationSelectionDataSourceObserver = android.arch.lifecycle.Observer {
            recapVisibility.value = it == null

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value,
                    isDataOutOfDate.value != false, numFormat, isDockTable)
            smoothScrollSelectionInView()
        }

        stationSelectionDataSource.observeForever(stationSelectionDataSourceObserver)


        bikeSystemAvailabilityDataSource.observeForever(bikeSystemAvailabilityDataObserver)

        stationRecapDataSourceObserver = android.arch.lifecycle.Observer {
            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, isDataOutOfDate.value != false)
        }

        stationRecapDataSource.observeForever(stationRecapDataSourceObserver)

        userLocObserver = android.arch.lifecycle.Observer {
            comparator = DistanceComparator(userloc.value ?: LatLng(0.0, 0.0))

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value, isDataOutOfDate.value == true, numFormat, isDockTable)
        }

        userloc.observeForever(userLocObserver)

        dataOutdatedObserver = android.arch.lifecycle.Observer {


            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, it != false)

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value, it != false,
                    numFormat, isDockTable)

        }

        isDataOutOfDate.observeForever(dataOutdatedObserver)
    }

    override fun onCleared() {

        stationRecapDataSource.removeObserver(stationRecapDataSourceObserver)
        isDataOutOfDate.removeObserver(dataOutdatedObserver)
        bikeSystemAvailabilityDataSource.removeObserver(bikeSystemAvailabilityDataObserver)
        stationSelectionDataSource.removeObserver(stationSelectionDataSourceObserver)
        userloc.removeObserver(userLocObserver)

        super.onCleared()
    }

    fun smoothScrollSelectionInView() {

        //TODO: find the right place to obseve and precompute required data bits
        val selectedStation = stationSelectionDataSource.value
        //first, retrieve display position of selection in recyclerview
        if (selectedStation != null) {
            val displayData = tableItemDataList.value?.toMutableList()
                    ?: emptyList<StationTableItemData>()

            val el = displayData.find {
                it.locationHash == selectedStation.locationHash
            }

            smoothScrollTargetIdx.value = displayData.indexOf(el)
        }

    }

    private fun computeAndEmitStationRecapDisplayData(stationToRecap: BikeStation?,
                                                      outdated: Boolean) {

        //TODO: replug favorite name
        /*if (mFavoriteListModelView!!.isFavorite(_station.locationHash)) {
            mStationRecapName!!.text = mFavoriteListModelView!!.getFavoriteEntityForId(_station.locationHash)!!.getSpannedDisplayName(context, true)
        } else {
            mStationRecapName!!.text = _station.name
        }*/
        tableRecapMutableData.value = StationTableRecapData(
                stationToRecap?.name ?: "[[[STATION_NAME]]]",
                String.format(getApplication<Application>().getString(R.string.station_recap_bikes), stationToRecap?.freeBikes
                        ?: -1),
                outdated,
                if (outdated) Typeface.DEFAULT
                else Typeface.DEFAULT_BOLD,
                when {
                    outdated -> R.color.theme_accent
                    stationToRecap?.freeBikes ?: -1 <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.station_recap_red
                    stationToRecap?.freeBikes ?: -1 <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.station_recap_yellow
                    else -> R.color.station_recap_green
                })

    }

    private fun computeAndEmitTableDisplayData(availabilityData: List<BikeStation>?,
                                               oudated: Boolean, numFormat: NumberFormat, isDockTable: Boolean) {

        val sortedStationList = availabilityData?.toMutableList() ?: emptyList<BikeStation>()

        Collections.sort(sortedStationList, comparator)

        val newDisplayData = ArrayList<StationTableItemData>()

        //Do that in background and post the resulting list for observers to consume
        sortedStationList.forEach { station ->


            val proximityText = if (showProximity.value != false) comparator.getProximityString(station, !isDockTable,
                    numFormat, getApplication()) else null


            val availabilityValue = when {
                isDockTable -> station.emptySlots
                else -> station.freeBikes
            }

            val backgroundResId = if (station.locationHash == stationSelectionDataSource.value?.locationHash ?: false) {
                if (oudated) {
                    R.color.theme_accent
                } else {
                    when {
                        availabilityValue <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_red
                        availabilityValue <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_yellow
                        else -> R.color.stationtable_item_selected_background_green
                    }
                }
            } else {
                if (oudated)
                    android.R.color.transparent
                else {
                    when {
                        availabilityValue <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.stationtable_item_background_red
                        availabilityValue <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.stationtable_item_background_yellow
                        else -> android.R.color.transparent
                    }
                }
            }

            val durationAlpha = when {
                isDataOutOfDate.value == true -> 1.0f
                station.locationHash == stationSelectionDataSource.value?.locationHash -> 1.0f
                availabilityValue <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) ->
                    getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_25percent, 1, 1)
                else -> 1.0f
            }
            val nameAlpha = when {
                isDataOutOfDate.value == true -> 1.0f
                station.locationHash == stationSelectionDataSource.value?.locationHash -> 1.0f
                availabilityValue <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) ->
                    getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_25percent, 1, 1)
                availabilityValue <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication()) ->
                    getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_50percent, 1, 1)
                else -> 1.0f
            }
            val availabilityAlpha = when {
                isDataOutOfDate.value == true -> getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_25percent, 1, 1)
                station.locationHash == stationSelectionDataSource.value?.locationHash -> 1.0f
                availabilityValue <= SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication()) ->
                    getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_25percent, 1, 1)
                availabilityValue <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication()) ->
                    getApplication<Application>().resources.getFraction(R.fraction.station_item_alpha_65percent, 1, 1)
                else -> 1.0f
            }


            newDisplayData.add(StationTableItemData(
                    backgroundResId,
                    proximityText,
                    durationAlpha,
                    station.name ?: "null", //TODO: replug having favorite name if it is a favorite
                    nameAlpha,
                    if (availabilityValue != -1) numFormat.format(availabilityValue) else "--",
                    availabilityAlpha,
                    oudated,
                    if (oudated) Typeface.DEFAULT
                    else Typeface.DEFAULT_BOLD,
                    station.locationHash
            ))
        }

        tableItemList.value = newDisplayData

        val availabilityDataPostfixBuilder = StringBuilder()

        if (oudated) {
            if (sortedStationList.isNotEmpty())
                availabilityDataPostfixBuilder.append(sortedStationList[0].locationHash).append(OUTDATED_AVAILABILITY_POSTFIX)
        } else {
            var badOrAOKStationCount = 0
            val minimumServiceCount = Math.round(sortedStationList.size * 0.1)   //10%

            for (stationItem in sortedStationList) { //SORTED BY DISTANCE
                if (!isDockTable) {
                    if (!stationItem.isLocked) {

                        if (stationItem.freeBikes > SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication())) {

                            if (badOrAOKStationCount == 0) {
                                if (stationItem.freeBikes <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication())) {

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

                    if (stationItem.emptySlots == -1 || stationItem.emptySlots > SharedPrefHelper.getInstance().getCriticalAvailabilityMax(getApplication())) {

                        if (badOrAOKStationCount == 0) {

                            if (stationItem.emptySlots != -1 && stationItem.emptySlots <= SharedPrefHelper.getInstance().getBadAvailabilityMax(getApplication())) {

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


    //////////////////////////
    //TODO: replug having favorite name if it is a favorite
    /*if (mFavoriteListViewModel.isFavorite(_station.locationHash))
        nameText.text = mFavoriteListViewModel.getFavoriteEntityForId(_station.locationHash)!!.getSpannedDisplayName(mCtx, false)
    else
        nameText.text = _station.name*/
}
