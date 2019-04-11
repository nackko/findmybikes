package com.ludoscity.findmybikes.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.ui.table.StationTableFragment
import com.ludoscity.findmybikes.ui.table.TableFragmentModelFactory
import com.ludoscity.findmybikes.ui.table.TableFragmentViewModel
import java.text.NumberFormat

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
class StationTablePagerAdapter(
        fm: FragmentManager,
        private val bikeTableFragmentModelFactory: TableFragmentModelFactory,
        private val dockTableFragmentModelFactory: TableFragmentModelFactory) : SmartFragmentPagerAdapter(fm) {

    val isHighlightedVisibleInRecyclerView: Boolean
        get() = false//retrieveListFragment(BIKE_STATIONS).isHighlightedVisibleInRecyclerView

    override fun getItem(position: Int): Fragment {
        val toReturn = StationTableFragment()

        val args = Bundle()
        if (position == BIKE_STATIONS) {
            args.putBoolean("isDockTable", bikeTableFragmentModelFactory.isDockTable)
        } else {
            args.putBoolean("isDockTable", dockTableFragmentModelFactory.isDockTable)
        }

        //args.putInt(StationTableFragment.STATION_LIST_ARG_BACKGROUND_RES_ID, R.drawable.ic_favorites_background);
        args.putSerializable("numFormat", NumberFormat.getInstance())

        toReturn.arguments = args

        return toReturn
    }

    override fun getCount(): Int {
        return NUM_ITEMS
    }

    fun setupUI(_pageID: Int, showProximity: Boolean,
                headerFromIconResId: Int?, headerToIconResId: Int?,
                stringIfEmpty: String) {

        retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
            var tableModel =
                    ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            if (_pageID == DOCK_STATIONS)
                retrieveListFragment(DOCK_STATIONS)?.let { dockFrag ->
                    tableModel = ViewModelProviders.of(dockFrag, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
                }

            tableModel.setShowProximity(showProximity)
            tableModel.setStringIfEmpty(stringIfEmpty)
        }
    }

    //TODO: this smell like business logic
    fun hideStationRecap() {
        retrieveListFragment(DOCK_STATIONS)?.let {
            val dockTableModel =
                    ViewModelProviders.of(it, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

            dockTableModel.setRecapVisibility(false)
        }
    }

    //TODO: this smell like business logic
    fun showStationRecap() {
        retrieveListFragment(DOCK_STATIONS)?.let {
            val dockTableModel =
                    ViewModelProviders.of(it, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

            dockTableModel.setRecapVisibility(true)
        }
    }

    fun setRefreshEnabledAll(toSet: Boolean) {
        retrieveListFragment(DOCK_STATIONS)?.let {
            val dockTableModel =
                    ViewModelProviders.of(it, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
                val bikeTableModel =
                        ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

                bikeTableModel.setRefreshEnabled(toSet)
                dockTableModel.setRefreshEnabled(toSet)
            }
        }
    }

    private fun retrieveListFragment(position: Int): StationTableFragment? {
        return getRegisteredFragment(position) as? StationTableFragment
    }

    fun getHighlightedStationForTable(position: Int): BikeStation? {
        //TODO: this is in activity model now, setStationA and setStationB
        return null
    }

    fun setCurrentUserLatLng(currentUserLatLng: LatLng) {

        //TODO: this should be sent to both TableFragment models so they can update

        if (isViewPagerReady) {
            //retrieveListFragment(BIKE_STATIONS).setSortComparatorAndSort(new StationTableRecyclerViewAdapter.DistanceComparator(currentUserLatLng));
            //retrieveListFragment(DOCK_STATIONS).updateTotalTripSortComparator(currentUserLatLng, null);
        }
    }

    fun notifyStationChangedAll(_stationId: String) {
        //TODO: change tracking is done by Table model now
        //retrieveListFragment(BIKE_STATIONS).notifyStationChanged(_stationId)
        //retrieveListFragment(DOCK_STATIONS).notifyStationChanged(_stationId)
    }

    fun retrieveClosestRawIdAndAvailability(_pageID: Int): String? {
        var toReturn: String? = null

        retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
            var tableModel =
                    ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            if (_pageID == DOCK_STATIONS)
                retrieveListFragment(DOCK_STATIONS)?.let { dockFrag ->
                    tableModel = ViewModelProviders.of(dockFrag, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
                }
            //TODO: someone should observe that instead of forcing value retrieval randomly
            toReturn = tableModel.sortedAvailabilityDataString.value
        }

        return toReturn
    }

    fun highlightStationforId(_lookingForBike: Boolean, _stationId: String) {

        //TODO: this is in activity model now, setStattionA or setStationB
        /*if (isViewPagerReady) {
            if (_lookingForBike)
                retrieveListFragment(BIKE_STATIONS).highlightStation(_stationId)
            else
                retrieveListFragment(DOCK_STATIONS).highlightStation(_stationId)
        }*/
    }

    fun isRecyclerViewReadyForItemSelection(tableID: Int): Boolean {
        //TODO: remove this
        return false//retrieveListFragment(tableID).isRecyclerViewReadyForItemSelection
    }

    fun highlightStationForTable(_stationId: String, _tableId: Int): Boolean {
        //TODO: this is in activity model now, setStationA and setStationB
        return true//retrieveListFragment(_tableId).highlightStation(_stationId)
    }

    fun removeStationHighlightForTable(tableId: Int) {
        //TODO: this is in activity model now, setStationA and setStationB
        //retrieveListFragment(tableId).removeStationHighlight()
    }

    fun setRefreshingAll(toSet: Boolean) {
        retrieveListFragment(DOCK_STATIONS)?.let {
            val dockTableModel =
                    ViewModelProviders.of(it, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
                val bikeTableModel =
                        ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

                dockTableModel.setRefreshLayoutVisible(toSet)
                bikeTableModel.setRefreshLayoutVisible(toSet)
            }
        }
    }

    fun notifyStationAUpdate(_newALatLng: LatLng, _userLatLng: LatLng) {
        retrieveListFragment(DOCK_STATIONS)?.updateTotalTripSortComparator(_userLatLng, _newALatLng)
    }

    fun smoothScrollHighlightedInViewForTable(_tableId: Int, _appBarExpanded: Boolean) {
        retrieveListFragment(BIKE_STATIONS)?.let { bikeFrag ->
            var tableModel =
                    ViewModelProviders.of(bikeFrag, bikeTableFragmentModelFactory).get(TableFragmentViewModel::class.java)
            if (_tableId == DOCK_STATIONS)
                retrieveListFragment(DOCK_STATIONS)?.let { dockFrag ->
                    tableModel = ViewModelProviders.of(dockFrag, dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

                    tableModel.smoothScrollSelectionInView()
                }
        }
    }

    fun setClickResponsivenessForTable(_tableId: Int, _toSet: Boolean) {
        //TODO: remove this
    }

    fun showFavoriteHeaderInBTab() {

        //val dockTableModel =
        //        ViewModelProviders.of(retrieveListFragment(DOCK_STATIONS), dockTableFragmentModelFactory).get(TableFragmentViewModel::class.java)

        //dockTableModel.setHeaderToIconResId((R.drawable.ic_pin_favorite_24dp_white))
    }

    companion object {

        var BIKE_STATIONS = 0
        var DOCK_STATIONS = 1

        private val NUM_ITEMS = 2


    }
}
