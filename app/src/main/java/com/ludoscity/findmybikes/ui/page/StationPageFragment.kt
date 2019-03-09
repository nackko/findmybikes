package com.ludoscity.findmybikes.ui.page

import android.arch.lifecycle.ViewModelProviders
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.gms.maps.model.LatLng
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.utils.DividerItemDecoration
import com.ludoscity.findmybikes.utils.ScrollingLinearLayoutManager
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel
import java.util.*

class StationPageFragment : Fragment(), StationPageRecyclerViewAdapter.OnStationListItemClickListener {

    private var mStationRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerViewScrollingState = SCROLL_STATE_IDLE
    private var mEmptyListTextView: TextView? = null
    private var mProximityHeader: View? = null
    private var mStationRecap: View? = null
    private var mStationRecapName: TextView? = null
    private var mStationRecapAvailability: TextView? = null
    private var mProximityHeaderFromImageView: ImageView? = null
    private var mProximityHeaderToImageView: ImageView? = null
    private var mAvailabilityTextView: TextView? = null

    private var mFavoriteListModelView: FavoriteListViewModel? = null

    private var mListener: OnStationListFragmentInteractionListener? = null

    private val stationPageRecyclerViewAdapter: StationPageRecyclerViewAdapter
        get() = mStationRecyclerView!!.adapter as StationPageRecyclerViewAdapter

    val isRecyclerViewReadyForItemSelection: Boolean
        get() = mStationRecyclerView != null && stationPageRecyclerViewAdapter.sortComparator != null &&
                (mStationRecyclerView!!.layoutManager as ScrollingLinearLayoutManager).findFirstVisibleItemPosition() != NO_POSITION

    val highlightedFavoriteFabViewTarget: ViewTarget?
        get() = stationPageRecyclerViewAdapter.getSelectedItemFavoriteFabViewTarget(mStationRecyclerView)

    val highlightedStation: BikeStation?
        get() = stationPageRecyclerViewAdapter.selected

    //Minus 1 is for appbar
    val isHighlightedVisibleInRecyclerView: Boolean
        get() = stationPageRecyclerViewAdapter.selectedPos < (mStationRecyclerView!!.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() - 1 && stationPageRecyclerViewAdapter.selectedPos >= (mStationRecyclerView!!.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mFavoriteListModelView = ViewModelProviders.of(this).get(FavoriteListViewModel::class.java)

        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_station_page, container, false)
        mEmptyListTextView = inflatedView.findViewById(R.id.empty_page_text)
        mStationRecap = inflatedView.findViewById(R.id.station_recap)
        mStationRecapName = inflatedView.findViewById(R.id.station_recap_name)
        mStationRecapAvailability = inflatedView.findViewById(R.id.station_recap_availability)
        mStationRecyclerView = inflatedView.findViewById(R.id.station_page_recyclerview)
        mStationRecyclerView!!.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL_LIST))
        //mStationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mStationRecyclerView!!.layoutManager = ScrollingLinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false, 300)
        mStationRecyclerView!!.adapter = StationPageRecyclerViewAdapter(this, context, mFavoriteListModelView)
        mStationRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                mRecyclerViewScrollingState = newState

            }
        })

        mSwipeRefreshLayout = inflatedView.findViewById(R.id.station_page_swipe_refresh_layout)
        mSwipeRefreshLayout!!.setOnRefreshListener(activity as SwipeRefreshLayout.OnRefreshListener?)
        mSwipeRefreshLayout!!.setColorSchemeResources(R.color.stationlist_refresh_spinner_red,
                R.color.stationlist_refresh_spinner_yellow,
                R.color.stationlist_refresh_spinner_grey,
                R.color.stationlist_refresh_spinner_green)

        mAvailabilityTextView = inflatedView.findViewById(R.id.availability_header)
        mProximityHeader = inflatedView.findViewById(R.id.proximity_header)
        mProximityHeaderFromImageView = inflatedView.findViewById(R.id.proximity_header_from)
        mProximityHeaderToImageView = inflatedView.findViewById(R.id.proximity_header_to)

        val args = arguments
        if (args != null) {

            mStationRecyclerView!!.background = ContextCompat.getDrawable(this.context!!, args.getInt(STATION_LIST_ARG_BACKGROUND_RES_ID))
            mProximityHeader!!.visibility = View.GONE
        }

        return inflatedView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        try {
            mListener = activity as OnStationListFragmentInteractionListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement OnFragmentInteractionListener")
        }

    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("selected_pos", stationPageRecyclerViewAdapter.selectedPos)
        val comparator = stationPageRecyclerViewAdapter.sortComparator
        if (comparator is StationPageRecyclerViewAdapter.DistanceComparator)
            outState.putParcelable("sort_comparator", comparator)
        else
            outState.putParcelable("sort_comparator", comparator as StationPageRecyclerViewAdapter.TotalTripTimeComparator)
        outState.putString("string_if_empty", mEmptyListTextView!!.text.toString())
        outState.putString("station_recap_name", mStationRecapName!!.text.toString())
        outState.putString("station_recap_availability_string", mStationRecapAvailability!!.text.toString())
        outState.putInt("station_recap_availability_color", mStationRecapAvailability!!.currentTextColor)
        outState.putBoolean("station_recap_visible", mStationRecap!!.visibility == View.VISIBLE)
        outState.putBoolean("proximity_header_visible", mProximityHeader!!.visibility == View.VISIBLE)
        outState.putInt("proximity_header_from_icon_resid", if (mProximityHeaderFromImageView!!.tag == null) -1 else mProximityHeaderFromImageView!!.tag as Int)
        outState.putInt("proximity_header_to_icon_resid", if (mProximityHeaderToImageView!!.tag == null) -1 else mProximityHeaderToImageView!!.tag as Int)

        stationPageRecyclerViewAdapter.saveLookingForBike(outState)
        stationPageRecyclerViewAdapter.saveIsAvailabilityOutdated(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {

            val comparator: Comparator<BikeStation> = savedInstanceState.getParcelable<Parcelable>("sort_comparator") as Comparator<BikeStation>

            if (savedInstanceState.getBoolean("availability_outdated")) {
                stationPageRecyclerViewAdapter.setAvailabilityOutdated(true)
                mStationRecapAvailability!!.paint.isStrikeThruText = true
                mStationRecapAvailability!!.paint.typeface = Typeface.DEFAULT
            } else {
                stationPageRecyclerViewAdapter.setAvailabilityOutdated(false)
                mStationRecapAvailability!!.paint.isStrikeThruText = false
                mStationRecapAvailability!!.paint.typeface = Typeface.DEFAULT_BOLD
            }

            setupUI(RootApplication.bikeNetworkStationList, savedInstanceState.getBoolean("looking_for_bike"),
                    savedInstanceState.getBoolean("proximity_header_visible"),
                    if (savedInstanceState.getInt("proximity_header_from_icon_resid") == -1) null else savedInstanceState.getInt("proximity_header_from_icon_resid"),
                    if (savedInstanceState.getInt("proximity_header_to_icon_resid") == -1) null else savedInstanceState.getInt("proximity_header_to_icon_resid"),
                    savedInstanceState.getString("string_if_empty"),
                    comparator)

            val selectedPos = savedInstanceState.getInt("selected_pos")

            if (selectedPos != NO_POSITION)
                stationPageRecyclerViewAdapter.setSelectedPos(selectedPos, false)

            mStationRecapName!!.text = savedInstanceState.getString("station_recap_name")
            mStationRecapAvailability!!.text = savedInstanceState.getString("station_recap_availability_string")

            mStationRecapAvailability!!.setTextColor(savedInstanceState.getInt("station_recap_availability_color"))

            if (savedInstanceState.getBoolean("station_recap_visible")) {
                mStationRecyclerView!!.visibility = View.GONE
                mEmptyListTextView!!.visibility = View.VISIBLE
                mStationRecap!!.visibility = View.VISIBLE
            } else {
                mStationRecyclerView!!.visibility = View.VISIBLE
                mEmptyListTextView!!.visibility = View.GONE
                mStationRecap!!.visibility = View.GONE
            }
        }
    }

    fun setupUI(_stationsNetwork: List<BikeStation>, _lookingForBike: Boolean, _showProximity: Boolean,
                _headerFromIconResId: Int?, _headerToIconResId: Int?,
                _stringIfEmpty: String?,
                _sortComparator: Comparator<BikeStation>?) {

        //TODO: fix glitch when coming back from place widget (Note to past self : describe glitch)
        mEmptyListTextView!!.text = _stringIfEmpty
        if (!_stationsNetwork.isEmpty()) {
            mStationRecyclerView!!.visibility = View.VISIBLE
            mEmptyListTextView!!.visibility = View.GONE
            mStationRecap!!.visibility = View.GONE
        } else {
            mStationRecyclerView!!.visibility = View.GONE
            mEmptyListTextView!!.visibility = View.VISIBLE
            mStationRecap!!.visibility = View.VISIBLE
        }

        stationPageRecyclerViewAdapter.setupStationList(_stationsNetwork, _sortComparator)
        stationPageRecyclerViewAdapter.setShowProximity(_showProximity)
        setupHeaders(_lookingForBike, _showProximity, _headerFromIconResId, _headerToIconResId)
    }

    fun hideStationRecap() {
        mStationRecap!!.visibility = View.GONE
    }

    fun showStationRecap() {
        mStationRecap!!.visibility = View.VISIBLE
    }

    fun setupStationRecap(_station: BikeStation, _outdated: Boolean): Boolean {

        if (context == null)
            return false

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

            if (_station.freeBikes <= DBHelper.getInstance().getCriticalAvailabilityMax(context))
                mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_red))
            else if (_station.freeBikes <= DBHelper.getInstance().getBadAvailabilityMax(context))
                mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_yellow))
            else
                mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.station_recap_green))

        }

        return true
    }

    fun setSortComparatorAndSort(_toSet: Comparator<BikeStation>) {

        if (mRecyclerViewScrollingState == SCROLL_STATE_IDLE) {

            stationPageRecyclerViewAdapter.setStationSortComparatorAndSort(_toSet)
        }
    }

    //_stationALatLng can be null
    fun updateTotalTripSortComparator(_userLatLng: LatLng, _stationALatLng: LatLng) {
        stationPageRecyclerViewAdapter.updateTotalTripSortComparator(_userLatLng, _stationALatLng)
    }

    fun retrieveClosestRawIdAndAvailability(_lookingForBike: Boolean): String {

        return stationPageRecyclerViewAdapter.retrieveClosestRawIdAndAvailability(_lookingForBike)

    }

    fun getClosestAvailabilityLatLng(_lookingForBike: Boolean): LatLng? {
        return stationPageRecyclerViewAdapter.getClosestAvailabilityLatLng(_lookingForBike)
    }

    fun highlightStation(_stationId: String): Boolean {

        val selectedPos = stationPageRecyclerViewAdapter.setSelection(_stationId, false)

        (mStationRecyclerView!!.adapter as StationPageRecyclerViewAdapter).requestFabAnimation()

        return selectedPos != NO_POSITION
    }

    fun removeStationHighlight() {
        stationPageRecyclerViewAdapter.clearSelection()
    }

    fun setupHeaders(lookingForBike: Boolean, _showProximityHeader: Boolean, _headerFromIconResId: Int?, _headerToIconResId: Int?) {

        stationPageRecyclerViewAdapter.lookingForBikesNotify(lookingForBike)

        if (lookingForBike) {

            mAvailabilityTextView!!.text = getString(R.string.bikes)

            if (arguments != null) {
                mProximityHeader!!.visibility = View.GONE
            } else {

                mProximityHeaderFromImageView!!.visibility = View.GONE
                mProximityHeaderFromImageView!!.tag = -1
                mProximityHeaderToImageView!!.tag = _headerToIconResId
                mProximityHeaderToImageView!!.setImageResource(_headerToIconResId!!)

                mProximityHeader!!.visibility = View.VISIBLE
            }
        } else {

            mAvailabilityTextView!!.text = getString(R.string.docks)

            if (arguments != null || !_showProximityHeader) {
                mProximityHeader!!.visibility = View.INVISIBLE
            } else {

                mProximityHeaderFromImageView!!.visibility = View.VISIBLE
                mProximityHeaderFromImageView!!.setImageResource(_headerFromIconResId!!)
                mProximityHeaderToImageView!!.setImageResource(_headerToIconResId!!)
                mProximityHeaderFromImageView!!.tag = _headerFromIconResId
                mProximityHeaderToImageView!!.tag = _headerToIconResId

                mProximityHeader!!.visibility = View.VISIBLE
            }
        }
    }

    override fun onStationListItemClick(_path: String) {
        val builder = Uri.Builder()

        builder.appendPath(_path)

        if (mListener != null) {
            mListener!!.onStationListFragmentInteraction(builder.build())
        }
    }

    fun setRefreshing(toSet: Boolean) {
        if (toSet != mSwipeRefreshLayout!!.isRefreshing)
            mSwipeRefreshLayout!!.isRefreshing = toSet
    }

    fun setRefreshEnable(toSet: Boolean) {
        mSwipeRefreshLayout!!.isEnabled = toSet
    }

    fun smoothScrollSelectionInView(_appBarExpanded: Boolean) {
        //Not very proud of the defensive coding but some code path which are required do call this in invalid contexts
        if (stationPageRecyclerViewAdapter.selectedPos != NO_POSITION) {
            if (_appBarExpanded && stationPageRecyclerViewAdapter.selectedPos >= (mStationRecyclerView!!.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()) {
                mStationRecyclerView!!.smoothScrollToPosition(stationPageRecyclerViewAdapter.selectedPos + 1)
            } else
                mStationRecyclerView!!.smoothScrollToPosition(stationPageRecyclerViewAdapter.selectedPos)
        }
    }

    fun setResponsivenessToClick(_toSet: Boolean) {
        stationPageRecyclerViewAdapter.setClickResponsiveness(_toSet)
    }

    fun notifyStationChanged(_stationId: String) {
        stationPageRecyclerViewAdapter.notifyStationChanged(_stationId)
    }

    fun setOutdatedData(_availabilityOutdated: Boolean) {
        //TODO: refactor with MVC in mind. Outdated status is model
        if (_availabilityOutdated) {
            mStationRecapAvailability!!.paint.isStrikeThruText = true
            mStationRecapAvailability!!.paint.typeface = Typeface.DEFAULT
            mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(context!!, R.color.theme_accent))
        }
        stationPageRecyclerViewAdapter.setAvailabilityOutdated(_availabilityOutdated)
    }

    fun showFavoriteHeader() {

        mProximityHeaderToImageView!!.setImageResource(R.drawable.ic_pin_favorite_24dp_white)
    }

    interface OnStationListFragmentInteractionListener {

        fun onStationListFragmentInteraction(uri: Uri)
    }

    companion object {

        val STATION_LIST_ITEM_CLICK_PATH = "station_list_item_click"
        val STATION_LIST_INACTIVE_ITEM_CLICK_PATH = "station_list_inactive_item_click"
        val STATION_LIST_FAVORITE_FAB_CLICK_PATH = "station_list_fav_fab_click"
        val STATION_LIST_DIRECTIONS_FAB_CLICK_PATH = "station_list_dir_fab_click"
        val STATION_LIST_ARG_BACKGROUND_RES_ID = "station_list_arg_background_res_id"
    }

}
