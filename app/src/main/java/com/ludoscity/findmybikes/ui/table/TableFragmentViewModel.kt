package com.ludoscity.findmybikes.ui.table

import android.app.Application
import android.graphics.Typeface
import android.text.SpannableString
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.SharedPrefHelper
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                             val showProximityColumn: LiveData<Boolean>,
                             val isRefreshing: LiveData<Boolean>,
                             val isRefreshEnabled: LiveData<Boolean>,
                             val proximityHeaderFromResId: LiveData<Int>,
                             val proximityHeaderToResId: LiveData<Int>,
                             comparatorSource: LiveData<FindMyBikesActivityViewModel.BaseBikeStationComparator>,
                             numFormat: NumberFormat) : AndroidViewModel(app) {

    //TODO: header setup should happen by observing app state instead of being explicitly called on the model ?
    //at the same time, it is business logic directly related to the table

    private var comparator: FindMyBikesActivityViewModel.BaseBikeStationComparator? = null

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    val tableItemDataList: LiveData<List<StationTableItemData>>
        get() = tableItemList
    val tableRecapData: LiveData<StationTableRecapData>
        get() = tableRecapMutableData

    val nearestAvailabilityStationId: LiveData<String>
        get() = nearestAvailabilityStatId

    val stationRecapVisibility: LiveData<Boolean>
        get() = recapVisibility

    val stationListVisibility: LiveData<Boolean>
        get() = listVisibility

    val emptyTextVisibility: LiveData<Boolean>
        get() = emptyTxtVisibility

    val stringIfEmpty: LiveData<String>
        get() = stringOnEmpty

    val headerAvailabilityText: LiveData<String>
        get() = headerAvailText

    val recyclerViewAdapterPosToSmoothScollInView: LiveData<Int>
        get() = smoothScrollTargetIdx

    val lastClickedStation: LiveData<BikeStation>
        get() = lastClickedStationMutable
    private val lastClickedStationMutable: MutableLiveData<BikeStation> = MutableLiveData()
    fun setLastClickedStationById(stationId: String?) {
        coroutineScopeIO.launch {
            lastClickedStationMutable.postValue(repository.getStationForId(stationId ?: "nullId"))
        }
    }

    private val tableItemList: MutableLiveData<List<StationTableItemData>> = MutableLiveData()

    private val tableRecapMutableData: MutableLiveData<StationTableRecapData> = MutableLiveData()

    private val smoothScrollTargetIdx: MutableLiveData<Int> = MutableLiveData()

    private val repository: FindMyBikesRepository = repo

    private val nearestAvailabilityStatId: MutableLiveData<String> = MutableLiveData()
    private val recapVisibility: MutableLiveData<Boolean> = MutableLiveData()
    private val listVisibility: MutableLiveData<Boolean> = MutableLiveData()
    private val emptyTxtVisibility: MutableLiveData<Boolean> = MutableLiveData()

    private val stringOnEmpty: MutableLiveData<String> = MutableLiveData()
    private val headerAvailText: MutableLiveData<String> = MutableLiveData()

    private val bikeSystemAvailabilityDataSource: LiveData<List<BikeStation>>
    private val bikeSystemAvailabilityDataObserver: androidx.lifecycle.Observer<List<BikeStation>>

    private val comparatorObserver: androidx.lifecycle.Observer<FindMyBikesActivityViewModel.BaseBikeStationComparator>
    private val stationRecapDataSourceObserver: androidx.lifecycle.Observer<BikeStation>
    private val stationSelectionDataSourceObserver: androidx.lifecycle.Observer<BikeStation>
    private val dataOutdatedObserver: androidx.lifecycle.Observer<Boolean>

    companion object {
        private val TAG = TableFragmentViewModel::class.java.simpleName
        const val AVAILABILITY_POSTFIX_START_SEQUENCE = "_AVAILABILITY_"
        const val AOK_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "AOK"
        const val BAD_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "BAD"
        const val CRITICAL_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "CRI"
        internal const val LOCKED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "LCK"
        internal const val OUTDATED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "OUT"
        internal const val ERROR_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "ERR"
    }


    init {
        if (isDockTable) {
            headerAvailText.value = app.getString(R.string.docks)
        } else {
            headerAvailText.value = app.getString(R.string.bikes)
        }

        bikeSystemAvailabilityDataSource = repo.getBikeSystemStationData(getApplication())

        //recyclerView
        bikeSystemAvailabilityDataObserver = androidx.lifecycle.Observer { newData ->

            //TODO: have better Room database update strategy
            //this is to protect against drop table strategy (led to inconsistency crashes in recyclerView)
            if (newData?.isNotEmpty() != false) {
                computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, isDataOutOfDate.value != false)

                computeAndEmitTableDisplayData(newData, isDataOutOfDate.value != false,
                        numFormat, isDockTable)

                emitNearestAndTweet(repo, newData, !isDockTable)
            }
        }

        comparatorObserver = androidx.lifecycle.Observer {
            comparator = it

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value,
                    isDataOutOfDate.value != false, numFormat, isDockTable)

            emitNearestAndTweet(repo, bikeSystemAvailabilityDataSource.value, !isDockTable)
        }

        comparatorSource.observeForever(comparatorObserver)

        stationSelectionDataSourceObserver = androidx.lifecycle.Observer { newSelection ->
            recapVisibility.value = newSelection == null
            emptyTxtVisibility.value = newSelection == null
            listVisibility.value = newSelection != null

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value,
                    isDataOutOfDate.value != false, numFormat, isDockTable)
            smoothScrollSelectionInView()
        }

        stationSelectionDataSource.observeForever(stationSelectionDataSourceObserver)

        bikeSystemAvailabilityDataSource.observeForever(bikeSystemAvailabilityDataObserver)

        stationRecapDataSourceObserver = androidx.lifecycle.Observer {
            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, isDataOutOfDate.value != false)
        }

        stationRecapDataSource.observeForever(stationRecapDataSourceObserver)

        dataOutdatedObserver = androidx.lifecycle.Observer {


            computeAndEmitStationRecapDisplayData(stationRecapDataSource.value, it != false)

            computeAndEmitTableDisplayData(bikeSystemAvailabilityDataSource.value,
                    it != false,
                    numFormat, isDockTable)
        }

        isDataOutOfDate.observeForever(dataOutdatedObserver)
    }

    private fun emitNearestAndTweet(repo: FindMyBikesRepository, newData: List<BikeStation>?, alsoTweet: Boolean = false) {
        newData?.let {
            coroutineScopeIO.launch {
                val sortedStationList = newData.toMutableList()
                //if comparator is null, no need to extract nearest with availability
                comparator?.let { comp ->
                    Collections.sort(sortedStationList, comp)

                    val availabilityDataPostfixBuilder = StringBuilder()

                    if (isDataOutOfDate.value == true) {
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

                    if (alsoTweet && availabilityDataString.length > 32 + AOK_AVAILABILITY_POSTFIX.length &&
                            (availabilityDataString.contains(AOK_AVAILABILITY_POSTFIX) || availabilityDataString.contains(BAD_AVAILABILITY_POSTFIX)) &&
                            isDataOutOfDate.value != true)
                        repo.pushDataToTwitter(getApplication(), Utils.extractOrderedStationIdsFromProcessedString(availabilityDataString), comp.userLoc)
                    else
                        Log.i(TAG, "Conditions not met to publish on Twitter -- skipped")

                    nearestAvailabilityStatId.postValue(Utils.extractNearestAvailableStationIdFromDataString(availabilityDataString))
                }
            }
        }
    }

    override fun onCleared() {

        stationRecapDataSource.removeObserver(stationRecapDataSourceObserver)
        isDataOutOfDate.removeObserver(dataOutdatedObserver)
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

            //Can happen when current bike system is sitched while app is running
            //(unlikely with typical user)
            if (el != null)
                smoothScrollTargetIdx.value = displayData.indexOf(el)
        }

    }

    private fun computeAndEmitStationRecapDisplayData(stationToRecap: BikeStation?,
                                                      outdated: Boolean) {
        coroutineScopeIO.launch {
            tableRecapMutableData.postValue(StationTableRecapData(
                    if (stationToRecap != null && repository.isFavoriteId(stationToRecap.locationHash))
                        repository.getFavoriteEntityByFavoriteId(stationToRecap.locationHash)?.getSpannedDisplayName(getApplication(), true)
                                ?: SpannableString("[[[FAV_NAME]]]")
                    else
                        SpannableString(stationToRecap?.name ?: "[[[STATION_NAME]]]"),
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
                    }))
        }
    }

    private fun computeAndEmitTableDisplayData(availabilityData: List<BikeStation>?, oudated: Boolean, numFormat: NumberFormat, isDockTable: Boolean) {
        coroutineScopeIO.launch {

            val sortedStationList = availabilityData?.toMutableList() ?: emptyList<BikeStation>()

            if (comparator != null)
                Collections.sort(sortedStationList, comparator)

            val newDisplayData = ArrayList<StationTableItemData>()
            val allFavIdList = repository.getAllFavoriteStationIdList()

            sortedStationList.forEach { station ->

                val proximityText = if (showProximityColumn.value != false) comparator?.getProximityString(station, !isDockTable,
                        numFormat, getApplication()) ?: "" else ""


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
                        if (isDockTable && (comparator as? FindMyBikesActivityViewModel.TotalTripTimeComparatorForDockTable)?.hasFinalDest() == false) View.INVISIBLE else View.VISIBLE,
                        proximityText,
                        durationAlpha,
                        if (allFavIdList.contains(station.locationHash))
                            repository.getFavoriteEntityByFavoriteId(station.locationHash)?.getSpannedDisplayName(getApplication(), false)
                                    ?: SpannableString("[[[FAV_NAME]]]")
                        else
                            SpannableString(station.name ?: "[[[STATION_NAME]]]"),
                        nameAlpha,
                        if (availabilityValue != -1) numFormat.format(availabilityValue) else "--",
                        availabilityAlpha,
                        oudated,
                        if (oudated) Typeface.DEFAULT
                        else Typeface.DEFAULT_BOLD,
                        station.locationHash
                ))
            }

            tableItemList.postValue(newDisplayData)
        }
    }
}
