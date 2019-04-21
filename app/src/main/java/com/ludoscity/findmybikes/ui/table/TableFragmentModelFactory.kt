package com.ludoscity.findmybikes.ui.table

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import java.text.NumberFormat

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * Factory class to retrieve model for station map
 */
class TableFragmentModelFactory(private val repository: FindMyBikesRepository,
                                private val application: Application,
                                val isDockTable: Boolean,
                                private val appBarExpanded: LiveData<Boolean>,
                                private val stationRecapDataSource: LiveData<BikeStation>,
                                private val stationSelectionDataSource: LiveData<BikeStation>,
                                private val dataOutOfDate: LiveData<Boolean>,
                                private val showProximityColumn: LiveData<Boolean>,
                                private val isRefreshing: LiveData<Boolean>,
                                private val isRefreshEnabled: LiveData<Boolean>,
                                private val proximityHeaderFromResId: LiveData<Int>,
                                private val proximityHeaderToResId: LiveData<Int>,
                                private val comparatorSource: LiveData<FindMyBikesActivityViewModel.BaseBikeStationComparator>,
                                private val numFormat: NumberFormat) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        @Suppress("UNCHECKED_CAST")
        return TableFragmentViewModel(repository,
                application,
                isDockTable,
                appBarExpanded,
                stationRecapDataSource,
                stationSelectionDataSource,
                dataOutOfDate,
                showProximityColumn,
                isRefreshing,
                isRefreshEnabled,
                proximityHeaderFromResId,
                proximityHeaderToResId,
                comparatorSource,
                numFormat) as T
    }
}