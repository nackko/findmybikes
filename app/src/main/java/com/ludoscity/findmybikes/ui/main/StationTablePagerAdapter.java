package com.ludoscity.findmybikes.ui.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.ui.table.StationTableFragment;
import com.ludoscity.findmybikes.ui.table.StationTableRecyclerViewAdapter;
import com.ludoscity.findmybikes.utils.SmartFragmentPagerAdapter;

import java.util.Comparator;
import java.util.List;

/**
 * Created by F8Full on 2015-10-19.
 * Adapter for view pager displaying station lists
 */
public class StationTablePagerAdapter extends SmartFragmentPagerAdapter {

    @SuppressWarnings("FieldCanBeLocal")
    private static int NUM_ITEMS = 2;

    public static int BIKE_STATIONS = 0;
    public static int DOCK_STATIONS = 1;

    public StationTablePagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        /*Fragment toReturn;
        if (position == BIKE_STATIONS)
            toReturn = new StationTableFragment();
        else {
            toReturn = new StationTableFragment();
            Bundle args = new Bundle();
            args.putInt(StationTableFragment.STATION_LIST_ARG_BACKGROUND_RES_ID, R.drawable.ic_favorites_background);
            toReturn.setArguments(args);
        }*/

        return new StationTableFragment();
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    public void setupUI(int _pageID, List<BikeStation> _stationsList, boolean _showProximity,
                        Integer _headerFromIconResId, Integer _headerToIconResId,
                        String _stringIfEmpty,
                        Comparator<BikeStation> _sortComparator){
        retrieveListFragment(_pageID).setupUI(_stationsList, _pageID == BIKE_STATIONS,
                _showProximity, _headerFromIconResId, _headerToIconResId,
                _stringIfEmpty, _sortComparator);
    }

    public void hideStationRecap(){
        retrieveListFragment(DOCK_STATIONS).hideStationRecap();
    }

    public void showStationRecap(){
        retrieveListFragment(DOCK_STATIONS).showStationRecap();
    }

    public void setRefreshEnableAll(boolean toSet) {
        retrieveListFragment(BIKE_STATIONS).setRefreshEnable(toSet);
        retrieveListFragment(DOCK_STATIONS).setRefreshEnable(toSet);
    }

    private StationTableFragment retrieveListFragment(int position) {
        return ((StationTableFragment) getRegisteredFragment(position));
    }

    public BikeStation getHighlightedStationForTable(int position) {
        BikeStation toReturn = null;

        if (isViewPagerReady())
            toReturn = retrieveListFragment(position).getHighlightedStation();

        return toReturn;
    }

    public LatLng getClosestBikeLatLng(){
        return retrieveListFragment(BIKE_STATIONS).getClosestAvailabilityLatLng(true);
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {
        if (isViewPagerReady()) {
            retrieveListFragment(BIKE_STATIONS).setSortComparatorAndSort(new StationTableRecyclerViewAdapter.DistanceComparator(currentUserLatLng));
            retrieveListFragment(DOCK_STATIONS).updateTotalTripSortComparator(currentUserLatLng, null);
        }
    }

    public void notifyStationChangedAll(String _stationId) {
        retrieveListFragment(BIKE_STATIONS).notifyStationChanged(_stationId);
        retrieveListFragment(DOCK_STATIONS).notifyStationChanged(_stationId);
    }

    public String retrieveClosestRawIdAndAvailability(boolean _lookingForBike){
        String toReturn = null;

        if (isViewPagerReady()){
            if (_lookingForBike)
                toReturn = retrieveListFragment(BIKE_STATIONS).retrieveClosestRawIdAndAvailability(true);
            else
                toReturn = retrieveListFragment(DOCK_STATIONS).retrieveClosestRawIdAndAvailability(false);
        }

        return toReturn;
    }

    public void highlightStationforId(boolean _lookingForBike, String _stationId){

        if (isViewPagerReady()){
            if (_lookingForBike)
                retrieveListFragment(BIKE_STATIONS).highlightStation(_stationId);
            else
                retrieveListFragment(DOCK_STATIONS).highlightStation(_stationId);
        }
    }

    public boolean isRecyclerViewReadyForItemSelection(int tableID) {
        return retrieveListFragment(tableID).isRecyclerViewReadyForItemSelection();
    }

    public boolean highlightStationForTable(String _stationId, int _tableId) {
        return retrieveListFragment(_tableId).highlightStation(_stationId);
    }

    public void removeStationHighlightForTable(int tableId) {
        retrieveListFragment(tableId).removeStationHighlight();
    }

    public void setRefreshingAll(boolean toSet) {
        retrieveListFragment(BIKE_STATIONS).setRefreshing(toSet);
        retrieveListFragment(DOCK_STATIONS).setRefreshing(toSet);
    }

    public void notifyStationAUpdate(LatLng _newALatLng, LatLng _userLatLng) {
        retrieveListFragment(DOCK_STATIONS).updateTotalTripSortComparator(_userLatLng, _newALatLng);
    }

    public void smoothScrollHighlightedInViewForTable(int _tableId, boolean _appBarExpanded) {
        retrieveListFragment(_tableId).smoothScrollSelectionInView(_appBarExpanded);
    }

    public boolean isHighlightedVisibleInRecyclerView() {
        return retrieveListFragment(BIKE_STATIONS).isHighlightedVisibleInRecyclerView();
    }

    public boolean setupBTabStationARecap(BikeStation _stationA, boolean _outdated) {
        return retrieveListFragment(DOCK_STATIONS).setupStationRecap(_stationA, _outdated);
    }

    public void setClickResponsivenessForTable(int _tableId, boolean _toSet) {
        retrieveListFragment(_tableId).setResponsivenessToClick(_toSet);
    }

    public void setOutdatedDataAll(boolean _isDataOutdated){
        retrieveListFragment(BIKE_STATIONS).setOutdatedData(_isDataOutdated);
        retrieveListFragment(DOCK_STATIONS).setOutdatedData(_isDataOutdated);
    }

    public void showFavoriteHeaderInBTab() {
        retrieveListFragment(DOCK_STATIONS).showFavoriteHeader();
    }
}
