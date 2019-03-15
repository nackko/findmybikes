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
import com.ludoscity.findmybikes.R
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

class TableFragmentViewModel(repo: FindMyBikesRepository, app: Application,
                             private val isDockTable: Boolean,
                             val appBarExpanded: LiveData<Boolean>,
                             private val stationRecapDataSource: LiveData<BikeStation>,
                             private val stationSelectionDataSource: LiveData<BikeStation>,
                             private val dataOutdated: LiveData<Boolean>,
                             numFormat: NumberFormat) : AndroidViewModel(app) {

    //TODO: header setup should happen by observing app state instead of being explicitly called on the model ?
    //at the same time, it is business logic directly related to the table


    val tableItemDataList: LiveData<List<StationTableItemData>>
        get() = tableItemList
    val tableRecapData: LiveData<StationTableRecapData>
        get() = tableRecapMutableData

    private var selectedPos: Int = NO_POSITION
    //TODO: expose selected BikeStation data in activity model

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

    //val isLookingForBike: LiveData<Boolean>
    //    get() = lookingForBike

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
    //Have a base comparator class that calculates proximityText String
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

        comparator = DistanceComparator(LatLng(-73.567256, 45.5016889))



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

            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, dataOutdated.value != false)

            computeAndEmitTableDisplayData(newData, dataOutdated.value != false,
                    numFormat, isDockTable)

        }

        stationSelectionDataSourceObserver = android.arch.lifecycle.Observer {
            recapVisibility.value = stationSelectionDataSource.value == null

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value,
                    dataOutdated.value != false, numFormat, isDockTable)
        }

        stationSelectionDataSource.observeForever(stationSelectionDataSourceObserver)


        bikeSystemAvailabilityDataSource.observeForever(bikeSystemAvailabilityDataObserver)

        stationRecapDataSourceObserver = android.arch.lifecycle.Observer {

            computeAndEmitStationRecapDisplayData(it, dataOutdated.value != false)
            //TODO: re emit on new data available
            //TODO: replug favorite name
            /*if (mFavoriteListModelView!!.isFavorite(_station.locationHash)) {
                mStationRecapName!!.text = mFavoriteListModelView!!.getFavoriteEntityForId(_station.locationHash)!!.getSpannedDisplayName(context, true)
            } else {
                mStationRecapName!!.text = _station.name
            }*/
            /*tableRecapMutableData.value = StationTableRecapData(
                    it?.name ?: "[[[STATION_NAME]]]",
                    String.format(app.getString(R.string.station_recap_bikes), it?.freeBikes ?: -1),
                    dataOutdated.value != false,
                    if (dataOutdated.value != false) Typeface.DEFAULT
                    else Typeface.DEFAULT_BOLD,
                    when {
                        dataOutdated.value != false -> R.color.theme_accent
                        it?.freeBikes ?: -1 <= DBHelper.getInstance().getCriticalAvailabilityMax(app) -> R.color.station_recap_red
                        it?.freeBikes ?: -1 <= DBHelper.getInstance().getBadAvailabilityMax(app) -> R.color.station_recap_yellow
                        else -> R.color.station_recap_green
                    })*/
        }

        stationRecapDataSource.observeForever(stationRecapDataSourceObserver)

        dataOutdatedObserver = android.arch.lifecycle.Observer {


            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, it != false)

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value, it != false,
                    numFormat, isDockTable)

        }

        dataOutdated.observeForever(dataOutdatedObserver)

        //TODO: observe data being out of date, if it happens, rebuild tableItemDataList and StationTableRecapData
        //TODO: observe new user location available, requiring resorting and reprocessing of sorted list
    }

    override fun onCleared() {

        stationRecapDataSource.removeObserver(stationRecapDataSourceObserver)
        dataOutdated.removeObserver(dataOutdatedObserver)
        bikeSystemAvailabilityDataSource.removeObserver(bikeSystemAvailabilityDataObserver)
        stationSelectionDataSource.removeObserver(stationSelectionDataSourceObserver)

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

        //TODO: re emit when outdated status changes
        //TODO: re emit on new data available
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
                    stationToRecap?.freeBikes ?: -1 <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.station_recap_red
                    stationToRecap?.freeBikes ?: -1 <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.station_recap_yellow
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


            val alpha = when {
                oudated -> 1.0f
                false -> 1.0f //TODO: selection tracking - this is selected branch
                availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> getApplication<Application>().resources.getFraction(R.fraction.station_item_critical_availability_alpha, 1, 1)
                availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) -> getApplication<Application>().resources.getFraction(R.fraction.station_item_name_bad_availability_alpha, 1, 1)
                else -> 1.0f
            }

            val backgroundResId = if (station.locationHash == stationSelectionDataSource.value?.locationHash ?: false) {
                if (oudated) {
                    R.color.theme_accent
                } else {
                    when {
                        availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_red
                        availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_yellow
                        else -> R.color.stationtable_item_selected_background_green
                    }
                }
            } else {
                if (oudated)
                    android.R.color.transparent
                else {
                    when {
                        availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.stationtable_item_background_red
                        availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.stationtable_item_background_yellow
                        else -> android.R.color.transparent
                    }
                }
            }


            /*val backgroundResId = when{
                oudated -> when {
                    station.locationHash == stationSelectionDataSource.value?.locationHash?: false -> R.color.theme_accent
                    availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_red
                    availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) -> R.color.stationtable_item_selected_background_yellow
                    else -> android.R.color.transparent
                }
                availabilityValue <= DBHelper.getInstance().getCriticalAvailabilityMax(getApplication()) ->
                    R.color.stationtable_item_background_red
                availabilityValue <= DBHelper.getInstance().getBadAvailabilityMax(getApplication()) ->
                    R.color.stationtable_item_background_yellow
                else -> android.R.color.transparent
            }*/

            //TODO: emit a new list also when outdated data status changes
            //TODO: observe constructor parameter private val dataOutdated: LiveData<Boolean>
            newDisplayData.add(StationTableItemData(
                    backgroundResId, //TODO: replug background color - includes item selection tracking
                    proximityText,
                    alpha, //TODO replug alpha - includes item selection tracking
                    station.name, //TODO: replug having favorite name if it is a favorite
                    alpha, //TODO replug alpha - includes item selection tracking
                    if (availabilityValue != -1) numFormat.format(availabilityValue) else "--",
                    alpha, //TODO replug alpha - includes item selection tracking
                    oudated,
                    if (oudated) Typeface.DEFAULT
                    else Typeface.DEFAULT_BOLD,
                    station.locationHash
            ))
        }

        tableItemList.value = newDisplayData

        //////////////////////////
        //TODO: do this also when out of date value flips
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
    //TODO: replug outdated status makes station recap different, grab values to use in the following coe
    /*fun setupStationRecap(_station: BikeStation, _outdated: Boolean): Boolean {

        if (context == null)
            return false

        //TODO: replug favorite name from favorite model
        if (mFavoriteListModelView!!.isFavorite(_station.locationHash)) {
            mStationRecapName!!.text = mFavoriteListModelView!!.getFavoriteEntityForId(_station.locationHash)!!.getSpannedDisplayName(context, true)
        } else {
            mStationRecapName!!.text = _station.name
        }

        mStationRecapAvailability!!.text = String.format(resources.getString(R.string.station_recap_bikes), _station.freeBikes)

        if (_outdated) {
            mStationRecapAvailability!!.paint.isStrikeThruText = true
            mStationRecapAvailability!!.paint.typeface = Typeface.DEFAULT
            mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.theme_accent))
        } else {

            mStationRecapAvailability!!.paint.typeface = Typeface.DEFAULT_BOLD
            mStationRecapAvailability!!.paint.isStrikeThruText = false

            when {
                _station.freeBikes <= DBHelper.getInstance().getCriticalAvailabilityMax(context) -> mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_red))
                _station.freeBikes <= DBHelper.getInstance().getBadAvailabilityMax(context) -> mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_yellow))
                else -> mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_green))
            }

        }

        return true
    }*/
}
