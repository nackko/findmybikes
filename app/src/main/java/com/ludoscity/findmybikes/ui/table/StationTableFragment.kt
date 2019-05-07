package com.ludoscity.findmybikes.ui.table

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.ui.main.FindMyBikesActivityViewModel
import com.ludoscity.findmybikes.ui.sheet.DividerItemDecoration
import com.ludoscity.findmybikes.ui.sheet.ScrollingLinearLayoutManager
import com.ludoscity.findmybikes.utils.InjectorUtils
import java.text.NumberFormat

class StationTableFragment : Fragment() {

    //TODO: eurk !
    private var initialStatusLoadedCount = 0

    private lateinit var tableFragmentModel: TableFragmentViewModel
    private var mStationRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerViewScrollingState = SCROLL_STATE_IDLE //TODO: in model ??
    private var mEmptyListTextView: TextView? = null
    private var mProximityHeader: View? = null
    private var mStationRecap: View? = null
    private var mStationRecapName: TextView? = null
    private var mStationRecapAvailability: TextView? = null
    private var mProximityHeaderFromImageView: ImageView? = null
    private var mProximityHeaderToImageView: ImageView? = null
    private var headerAvailabilityTextView: TextView? = null

    private val stationTableRecyclerViewAdapter: StationTableRecyclerViewAdapter
        get() = mStationRecyclerView!!.adapter as StationTableRecyclerViewAdapter

    val highlightedFavoriteFabViewTarget: ViewTarget?
        get() = null//stationTableRecyclerViewAdapter.getSelectedItemFavoriteFabViewTarget(mStationRecyclerView!!)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_station_table, container, false)
        mEmptyListTextView = inflatedView.findViewById(R.id.empty_table_text)
        mStationRecap = inflatedView.findViewById(R.id.station_recap)
        mStationRecapName = inflatedView.findViewById(R.id.station_recap_name)
        mStationRecapAvailability = inflatedView.findViewById(R.id.station_recap_availability)
        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)
        val nearbyActivityViewModel = ViewModelProviders.of(activity!!, activityModelFactory).get(FindMyBikesActivityViewModel::class.java)

        val isDockTable = arguments?.getBoolean("isDockTable") ?: true
        val modelFactory = InjectorUtils.provideTableFragmentViewModelFactory(activity!!.application,
                arguments?.getBoolean("isDockTable") ?: true,
                nearbyActivityViewModel.isAppBarExpanded(),
                nearbyActivityViewModel.isDataOutOfDate,
                if (isDockTable) nearbyActivityViewModel.dockTableProximityShown else nearbyActivityViewModel.bikeTableProximityShown,
                nearbyActivityViewModel.allTableRefreshing,
                nearbyActivityViewModel.allTableRefreshEnabled,
                if (isDockTable) nearbyActivityViewModel.dockTableProximityHeaderFromResId else nearbyActivityViewModel.bikeTableProximityHeaderFromResId,
                if (isDockTable) nearbyActivityViewModel.dockTableProximityHeaderToResId else nearbyActivityViewModel.bikeTableProximityHeaderToResId,
                nearbyActivityViewModel.getStationA(),
                if (!isDockTable) nearbyActivityViewModel.getStationA() else nearbyActivityViewModel.getStationB(),
                if (isDockTable) nearbyActivityViewModel.dockTableComparator else nearbyActivityViewModel.bikeTableComparator,
                arguments?.getSerializable("numFormat") as NumberFormat
        )

        tableFragmentModel = ViewModelProviders.of(this, modelFactory).get(TableFragmentViewModel::class.java)

        mStationRecyclerView = inflatedView.findViewById(R.id.station_table_recyclerview)
        mStationRecyclerView!!.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL_LIST))
        //mStationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mStationRecyclerView!!.layoutManager = ScrollingLinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false, 300)
        mStationRecyclerView!!.adapter = StationTableRecyclerViewAdapter(tableFragmentModel)
        mStationRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                mRecyclerViewScrollingState = newState
            }
        })

        mSwipeRefreshLayout = inflatedView.findViewById(R.id.station_table_swipe_refresh_layout)
        mSwipeRefreshLayout!!.setOnRefreshListener(activity as SwipeRefreshLayout.OnRefreshListener?)
        mSwipeRefreshLayout!!.setColorSchemeResources(R.color.stationlist_refresh_spinner_red,
                R.color.stationlist_refresh_spinner_yellow,
                R.color.stationlist_refresh_spinner_grey,
                R.color.stationlist_refresh_spinner_green)

        tableFragmentModel.isRefreshEnabled.observe(this, androidx.lifecycle.Observer {
            if (it == null)
                mSwipeRefreshLayout!!.isEnabled = true
            else mSwipeRefreshLayout!!.isEnabled = it == true
        })

        tableFragmentModel.isRefreshing.observe(this, androidx.lifecycle.Observer {
            if (it != mSwipeRefreshLayout!!.isRefreshing)
                mSwipeRefreshLayout!!.isRefreshing = it ?: false
        })

        headerAvailabilityTextView = inflatedView.findViewById(R.id.availability_header)
        mProximityHeader = inflatedView.findViewById(R.id.proximity_header)
        mProximityHeaderFromImageView = inflatedView.findViewById(R.id.proximity_header_from)
        mProximityHeaderToImageView = inflatedView.findViewById(R.id.proximity_header_to)

        return inflatedView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)
        val nearbyActivityViewModel = ViewModelProviders.of(activity!!, activityModelFactory).get(FindMyBikesActivityViewModel::class.java)

        val isDockTable = arguments?.getBoolean("isDockTable") ?: true

        tableFragmentModel.lastClickedStation.observe(this, androidx.lifecycle.Observer {

            if (isDockTable)
                nearbyActivityViewModel.setStationB(it)
            else
                nearbyActivityViewModel.setStationA(it)
        })

        tableFragmentModel.nearestAvailabilityStationId.observe(this, androidx.lifecycle.Observer {
            if (isDockTable)
                nearbyActivityViewModel.setOptimalDockStationId(it)
            else
                nearbyActivityViewModel.setOptimalBikeStationId(it)
        })

        tableFragmentModel.tableItemDataList.observe(this, androidx.lifecycle.Observer {
            stationTableRecyclerViewAdapter.loadItems(it ?: emptyList())

            tableFragmentModel.smoothScrollSelectionInView()

            //TODO: eurk !
            if (initialStatusLoadedCount < 2) {

                nearbyActivityViewModel.setSelectedTable(false)
                ++initialStatusLoadedCount
            }
        })

        tableFragmentModel.stationRecapVisibility.observe(this, androidx.lifecycle.Observer { visible ->

            if (visible == true)
                mStationRecap!!.visibility = View.VISIBLE
            else
                mStationRecap!!.visibility = View.GONE
        })

        tableFragmentModel.stationListVisibility.observe(this, androidx.lifecycle.Observer { visible ->
            if (visible == true)
                mStationRecyclerView!!.visibility = View.VISIBLE
            else
                mStationRecyclerView!!.visibility = View.GONE
        })

        tableFragmentModel.emptyTextVisibility.observe(this, androidx.lifecycle.Observer {
            if (it == true)
                mEmptyListTextView!!.visibility = View.VISIBLE
            else
                mEmptyListTextView!!.visibility = View.GONE
        })

        tableFragmentModel.stringIfEmpty.observe(this, androidx.lifecycle.Observer {
            mEmptyListTextView!!.text = it ?: ""
        })

        tableFragmentModel.headerAvailabilityText.observe(this, androidx.lifecycle.Observer {
            headerAvailabilityTextView!!.text = it ?: ""
        })

        tableFragmentModel.tableRecapData.observe(this, androidx.lifecycle.Observer {
            mStationRecapName!!.text = it?.name ?: "[[[STATION NAME]]]"
            mStationRecapAvailability!!.text = it?.availabilityText ?: "XX"
            mStationRecapAvailability!!.paint.isStrikeThruText = it?.isAvailabilityPaintStrikeThru
                    ?: false
            mStationRecapAvailability!!.paint.typeface = it?.availabilityPaintTypeface
                    ?: Typeface.DEFAULT
            mStationRecapAvailability!!.setTextColor(ContextCompat.getColor(activity!!, it?.availabilityTextColorResId
                    ?: android.R.color.transparent))

        })

        tableFragmentModel.showProximityColumn.observe(this, androidx.lifecycle.Observer {
            //TODO : column width adaptation
            if (it == true) {
                mProximityHeader!!.visibility = View.VISIBLE
            } else {
                mProximityHeader!!.visibility = View.INVISIBLE
            }
        })

        tableFragmentModel.proximityHeaderFromResId.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                mProximityHeaderFromImageView!!.visibility = View.VISIBLE
                mProximityHeaderFromImageView!!.setImageResource(it)
            } else {
                //TODO: have explicit visibility LiveData<Int>
                mProximityHeaderFromImageView!!.visibility = View.GONE
            }
        })

        tableFragmentModel.proximityHeaderToResId.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                mProximityHeaderToImageView!!.setImageResource(it)
            }
        })

        tableFragmentModel.recyclerViewAdapterPosToSmoothScollInView.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                val appBarExpanded = tableFragmentModel.appBarExpanded.value ?: true

                if (appBarExpanded && it >= (mStationRecyclerView!!.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()) {
                    mStationRecyclerView!!.smoothScrollToPosition(it + 1)
                } else
                    mStationRecyclerView!!.smoothScrollToPosition(it)
            }
        })
    }
}
