package com.ludoscity.findmybikes.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener
import com.ludoscity.findmybikes.EditableMaterialSheetFab
import com.ludoscity.findmybikes.Fab
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.RootApplication
import com.ludoscity.findmybikes.activities.WebViewActivity
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.fragments.FavoriteListFragment
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.ui.main.StationTablePagerAdapter.Companion.BIKE_STATIONS
import com.ludoscity.findmybikes.ui.map.StationMapFragment
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel
import java.text.NumberFormat

class FindMyBikesActivity : AppCompatActivity(),
        FavoriteListFragment.OnFavoriteListFragmentInteractionListener,
        ViewPager.OnPageChangeListener,
        SwipeRefreshLayout.OnRefreshListener {
    //TODO: use this to do the animated bike feature
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        //Log.d(TAG, "onPageScrolled : position:$position, positionOffset:$positionOffset")
        if (positionOffset == 0.0f) {

            //TODO: wire this in so that it doesn't crash on screen rotation
            //Have smooth scroll request boolean live data in model that is set to false when no aciton is required
            //setting it to true would provoke correpsonding fragment to scroll when ready ?
            //an other way is to make code inside smothscroll... defensive
            //getTablePagerAdapter().smoothScrollHighlightedInViewForTable(position, true)
        }
    }

    override fun onPageSelected(position: Int) {
        nearbyActivityViewModel.setSelectedTable(stationTableViewPager.currentItem == BIKE_STATIONS)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onFavoriteItemEditDone(fsvoriteId: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFavoriteItemDeleted(favoriteId: String?, showUndo: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFavoriteListChanged(noFavorite: Boolean) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFavoriteListItemClicked(favoriteId: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var stationMapFragment: StationMapFragment

    private lateinit var circularRevealInterpolator: Interpolator

    //From activity layout
    private lateinit var stationTableViewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var placeAutocompleteLoadingProgressBar: ProgressBar
    //the following are not all visible at the same time (depends on app state)
    private lateinit var directionsLocToAFab: FloatingActionButton
    private lateinit var searchFAB: FloatingActionButton
    private lateinit var addFavoriteFAB: FloatingActionButton
    private lateinit var favoritesSheetFab: EditableMaterialSheetFab
    private lateinit var clearFAB: FloatingActionButton
    private lateinit var favoritePickerFAB: Fab
    private lateinit var autoSelectBikeFab: FloatingActionButton
    private lateinit var findBikesSnackbar: Snackbar

    //TODO: re add onboarding


    //TODO: Status bar fragment ?
    private lateinit var statusTextView: TextView
    private lateinit var statusBar: View

    private lateinit var splashScreen: View

    private val PLACE_AUTOCOMPLETE_REQUEST_CODE = 1

    //TODO: Trip details fragment
    private lateinit var tripDetailsFragment: View
    private lateinit var tripDetailsProximityTotal: TextView

    //models. TODO:Retrieve at construction time ?
    //private val favoriteListViewModel: FavoriteListViewModel
    private lateinit var nearbyActivityViewModel: NearbyActivityViewModel

    private val TABS_ICON_RES_ID = intArrayOf(R.drawable.ic_pin_a_36dp_white, R.drawable.ic_pin_b_36dp_white)

    //Places favorites stressed the previous design
    //TODO: explore refactoring with the following considerations
    //-stop relying on mapfragment markers visibility to branch code
    //private var mFavoritePicked = false    //True from the moment a favorite is picked until it's cleared //also set to true when a place is converted to a favorite
    //TODO: Refactoring step 1 : it is in model. the mere fact that a favorite been picked gives a non null liveData

    //already in model
    //private boolean mClosestBikeAutoSelected = false;

    private val DEBUG_FAKE_USER_CUR_LOC = LatLng(4.835659, 45.764043)

    companion object {
        private val TAG = FindMyBikesActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.FindMyBikesTheme) //https://developer.android.com/topic/performance/launch-time.html

        super.onCreate(savedInstanceState)

        val mapInit = MapsInitializer.initialize(applicationContext)

        if (mapInit != 0) {
            Log.e("NearbyActivity", "GooglePlayServicesNotAvailableException raised with error code :$mapInit")
        }

        //variables to retore state from bundle. TODO: Model will take care of that

        //var showcaseTripTotalPlaceName: String? = null

        /*if (savedInstanceState != null) {

            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos")
            mRequestingLocationUpdates = savedInstanceState.getBoolean("requesting_location_updates")
            mCurrentUserLatLng = savedInstanceState.getParcelable<LatLng>("user_location_latlng")
            mClosestBikeAutoSelected = savedInstanceState.getBoolean("closest_bike_auto_selected")
            //mFavoriteSheetVisible = savedInstanceState.getBoolean("favorite_sheet_visible");
            autoCompleteLoadingProgressBarVisible = savedInstanceState.getBoolean("place_autocomplete_loading")
            mRefreshTabs = savedInstanceState.getBoolean("refresh_tabs")
            mFavoritePicked = savedInstanceState.getBoolean("favorite_picked")
            mDataOutdated = savedInstanceState.getBoolean("data_outdated")
            showcaseTripTotalPlaceName = savedInstanceState.getString("onboarding_showcase_trip_total_place_name", null)
        }*/

        setContentView(R.layout.activity_findmybikes)
        setSupportActionBar(findViewById<View>(R.id.toolbar_main) as Toolbar)
        setupActionBarStrings()

        val modelFactory = InjectorUtils.provideMainActivityViewModelFactory(this.application)
        nearbyActivityViewModel = ViewModelProviders.of(this, modelFactory).get(NearbyActivityViewModel::class.java)

        nearbyActivityViewModel.stationData.observe(this, Observer { stationDataList ->
            Log.d(TAG, "New data has " + (stationDataList?.size ?: "") + " stations")

            stationDataList?.let {
                if (it.isNotEmpty()) {
                    Log.d(TAG, "un nom : " + it[0].name)
                }
            }
        })

        //nearbyActivityViewModel = ViewModelProviders.of(this).get(NearbyActivityViewModel::class.java)
        //TODO: retrieve favorites model
        FavoriteListViewModel.setNearbyActivityModel(nearbyActivityViewModel)
        //mFavoriteListViewModel = ViewModelProviders.of(this).get(FavoriteListViewModel::class.java)


        nearbyActivityViewModel.isFavoritePickerFabShown.observe(this, Observer {

            if (it == true)
                favoritesSheetFab.showFab()
            else
                favoritesSheetFab.hideSheetThenFab()
        })

        nearbyActivityViewModel.isFavoriteSheetShown.observe(this, Observer {
            if (it == true)
                favoritesSheetFab.showSheet()
            else
                favoritesSheetFab.hideSheet()

        })

        nearbyActivityViewModel.isFavoriteFabShown.observe(this, Observer {
            if (it == true) {
                addFavoriteFAB.show()
            } else {
                addFavoriteFAB.hide()
            }
        })

        nearbyActivityViewModel.isClearBSelectionFabShown.observe(this, Observer {
            if (it == true) {
                clearFAB.show()
            } else {
                clearFAB.hide()
            }
        })

        nearbyActivityViewModel.isSearchFabShown.observe(this, Observer {
            if (it == true)
                searchFAB.show()
            else
                searchFAB.hide()
        })

        nearbyActivityViewModel.isDirectionsToStationAFabShown.observe(this, Observer {
            if (it == true)
                directionsLocToAFab.show()
            else
                directionsLocToAFab.hide()
        })

        nearbyActivityViewModel.getCurrentBikeSytemId().observe(this, Observer<String> { newBikeSystemId ->
            setupFavoritePickerFab()

            //special case for test versions in firebase lab
            //full onboarding prevents meaningful coverage (robo test don't input anything in search autocomplete widget)
            /*if (getString(R.string.app_version_name).contains("test") || getString(R.string.app_version_name).contains("alpha")) {

                var addedCount = 4

                val networkStationList = RootApplication.getBikeNetworkStationList()
                for (station in networkStationList) {
                    if (!favoriteListViewModel.isFavorite(station.locationHash)) {

                        if (addedCount > 3)
                            break
                        else if (addedCount % 2 == 0) { //non default favorite name
                            val testFavToAdd = FavoriteEntityStation(station.locationHash, station.name, newBikeSystemId)
                            testFavToAdd.setCustomName(station.name + "-test")
                            favoriteListViewModel.addFavorite(testFavToAdd)
                        } else {   //default favorite name
                            favoriteListViewModel.addFavorite(FavoriteEntityStation(station.locationHash, station.name, newBikeSystemId))
                        }

                        ++addedCount
                    }
                }

                onboardingShowcaseView.hide()
            }*/
        })

        // Update Bar - TODO: Have fragment ?
        statusTextView = findViewById(R.id.status_textView)
        statusBar = findViewById<View>(R.id.app_status_bar)

        //TODO: this is "debug"
        nearbyActivityViewModel.setStationB(null)
        //TODO: the following crashes but shouldn't
        //nearbyActivityViewModel.setStationA(null)

        if (nearbyActivityViewModel.isDataOutOfDate.value == true)
            statusBar.setBackgroundColor(ContextCompat.getColor(this@FindMyBikesActivity, R.color.theme_accent))



        stationTableViewPager = findViewById(R.id.station_table_viewpager)
        stationTableViewPager.adapter = StationTablePagerAdapter(supportFragmentManager,
                InjectorUtils.provideTableFragmentViewModelFactory(application,
                        false,
                        nearbyActivityViewModel.isAppBarExpanded(),
                        nearbyActivityViewModel.isDataOutOfDate,
                        nearbyActivityViewModel.getStationA(),
                        nearbyActivityViewModel.getStationA(),
                        nearbyActivityViewModel.userLocation,
                        NumberFormat.getInstance()),
                InjectorUtils.provideTableFragmentViewModelFactory(application,
                        true,
                        nearbyActivityViewModel.isAppBarExpanded(),
                        nearbyActivityViewModel.isDataOutOfDate,
                        nearbyActivityViewModel.getStationA(),
                        nearbyActivityViewModel.getStationB(),
                        nearbyActivityViewModel.userLocation,
                        NumberFormat.getInstance()
                ))
        stationTableViewPager.addOnPageChangeListener(this)
        nearbyActivityViewModel.setSelectedTable(stationTableViewPager.currentItem == BIKE_STATIONS)

        // Give the TabLayout the ViewPager
        tabLayout = findViewById(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(stationTableViewPager)

        //TODO: what is this ?
        //Taking care of tabs icons here as pageradapter handles only title CharSequence for now
        var i = 0
        while (i < tabLayout.tabCount && i < TABS_ICON_RES_ID.size) {

            tabLayout.getTabAt(i)!!.setCustomView(R.layout.tab_custom_view)

            tabLayout.getTabAt(i)!!.icon = ContextCompat.getDrawable(this, TABS_ICON_RES_ID[i])
            ++i
        }

        appBarLayout = findViewById(R.id.action_toolbar_layout)

        appBarLayout.addOnOffsetChangedListener { appBarLayout: AppBarLayout, verticalOffset: Int ->

            if (Math.abs(verticalOffset) - appBarLayout.totalScrollRange == 0)
                nearbyActivityViewModel.setAppBarExpanded(false)
            else if (nearbyActivityViewModel.isAppBarExpanded().value != true)
                nearbyActivityViewModel.setAppBarExpanded(true)
        }



        coordinatorLayout = findViewById(R.id.snackbar_coordinator)

        //TODO: add splashscreen back
        //splashScreen = findViewById<View>(R.id.fragment_splash_screen)

        //TODO: trip details fragment
        tripDetailsFragment = findViewById<View>(R.id.trip_details_fragment)

        val layoutListener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                v?.removeOnLayoutChangeListener(this)
                nearbyActivityViewModel.isTripDetailsFragmentShown.observe(this@FindMyBikesActivity, Observer {
                    //We start hide animation on show status change. At the end of the hide anim, callback will launch
                    //show animation if show status is true then
                    hideTripDetailsFragment(nearbyActivityViewModel)
                })
            }
        }

        tripDetailsFragment.addOnLayoutChangeListener(layoutListener)

        searchFAB = findViewById(R.id.search_fab)
        addFavoriteFAB = findViewById(R.id.favorite_add_remove_fab)
        directionsLocToAFab = findViewById(R.id.directions_loc_to_a_fab)
        placeAutocompleteLoadingProgressBar = findViewById(R.id.place_autocomplete_loading)

        //TODO: add splashscreen back
        /*if (savedInstanceState == null)
            splashScreen.visibility = View.VISIBLE*/

        setupDirectionsLocToAFab()
        setupSearchFab()
        setupFavoritePickerFab()
        if (savedInstanceState?.getParcelable<Parcelable>("add_favorite_fab_data") != null) {
            //TODO: inspect this. It will have consequences when seting up the UI floating <3 button state
            //setupAddFavoriteFab((FavoriteItemPlace)savedInstanceState.getParcelable("add_favorite_fab_data"));
            addFavoriteFAB.show()
        }
        setupClearFab()
        setupAutoselectBikeFab()

        setStatusBarClickListener()

        getContentTablePagerAdapter().setCurrentUserLatLng(DEBUG_FAKE_USER_CUR_LOC)

        setupFavoriteSheet()

        //noinspection ConstantConditions
        findViewById<View>(R.id.trip_details_directions_loc_to_a).setOnClickListener {
            //TODO: this is wrong as userLoc and station values are captured at setup time
            nearbyActivityViewModel.stationALatLng.value?.let {
                launchGoogleMapsForDirections(nearbyActivityViewModel.userLocation.value, nearbyActivityViewModel.stationALatLng.value, true)
            }

        }
        //noinspection ConstantConditions
        findViewById<View>(R.id.trip_details_directions_a_to_b).setOnClickListener {
            //TODO: this is wrong as userLoc and station values are captured at setup time
            launchGoogleMapsForDirections(nearbyActivityViewModel.stationALatLng.value, nearbyActivityViewModel.stationBLatLng.value, false)
        }
        //noinspection ConstantConditions
        findViewById<View>(R.id.trip_details_directions_b_to_destination).setOnClickListener {
            //TODO: this is wrong as userLoc and station values are captured at setup time
            launchGoogleMapsForDirections(nearbyActivityViewModel.stationBLatLng.value, nearbyActivityViewModel.finalDestinationLatLng.value, true)
        }
        findViewById<View>(R.id.trip_details_share).setOnClickListener {
            //Je serai à la station Bixi Hutchison/beaubien dans ~15min ! Partagé via #findmybikes
            //I will be at the Bixi station Hutchison/beaubien in ~15min ! Shared via #findmybikes
            val message = String.format(resources.getString(R.string.trip_details_share_message_content),
                    DBHelper.getInstance().getBikeNetworkName(applicationContext), getContentTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.DOCK_STATIONS)!!.name,
                    tripDetailsProximityTotal.text.toString()) //TODO: total proximity is exposed in trip details fragment model

            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, message)
            sendIntent.type = "text/plain"
            startActivity(Intent.createChooser(sendIntent, getString(R.string.trip_details_share_title)))
        }

        circularRevealInterpolator = AnimationUtils.loadInterpolator(this, R.interpolator.msf_interpolator)

        //Not empty if RootApplication::onCreate got database data
        //TODO: this is clearly wrong : no copy of data should exist in root application
        //data should disseminate through repo observation
        //Observe some LiveData and detect when an initial setup is required
        if (RootApplication.bikeNetworkStationList.isEmpty()) {

            tryInitialSetup()
        }
    }

    //TODO, with repo and stuff, get rid of async tasks
    private fun tryInitialSetup() {

        /*if (Utils.Connectivity.isConnected(this)) {
            splashScreenTextTop.setText(getString(R.string.auto_bike_select_finding))

            if (DBHelper.getInstance().isBikeNetworkIdAvailable(this)) {

                nearbyActivityViewModel.setCurrentBikeSytemId(DBHelper.getBikeNetworkId(this))

                val downloadWebTask = NearbyActivity.DownloadWebTask()
                mDownloadWebTask.execute()

                Log.i("nearbyActivity", "No sortedStationList data in RootApplication but bike network id available in DBHelper- launching first download")
            } else {

                mFindNetworkTask = FindNetworkTask(DBHelper.getInstance().getBikeNetworkName(this))
                mFindNetworkTask.execute()
            }
        } else {
            Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(this@NearbyActivity, R.color.theme_primary_dark))
                    .setAction(R.string.retry) { tryInitialSetup() }.show()
        }*/
    }

    private fun setupFavoriteSheet() {
        //TODO: ??!!
    }

    private fun setStatusBarClickListener() {

        statusBar.setOnClickListener {
            if (Utils.Connectivity.isConnected(applicationContext)) {
                // use the android system webview
                val intent = Intent(this@FindMyBikesActivity, WebViewActivity::class.java)
                intent.putExtra(WebViewActivity.EXTRA_URL, "http://www.citybik.es")
                intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.hashtag_cities))
                intent.putExtra(WebViewActivity.EXTRA_JAVASCRIPT_ENABLED, true)
                startActivity(intent)
            }
        }
    }

    private fun setupAutoselectBikeFab() {
        autoSelectBikeFab = findViewById<FloatingActionButton>(R.id.autoselect_closest_bike)

        //Flipping this bool gives way to bike auto selection and found Snackbar animation
        //TODO : BonPlatDePates. Spaghetti monster must be contained.
        //In need an FSM of some kind. States being A selected Y/N B selected Y/N ....
        //TODO: Think about it more
        //DONE: use a viewmodel with following
        //isDataOutdated
        //isASelected/getSelectedA
        //isBSelected/getSelectedB
        //isConnectivityAvailable
        //isFavoritePicked/getPickedFavorite
        //currentSelectedTab (index or BIKELIST or STATIONLIST IDs ?)
        autoSelectBikeFab.setOnClickListener({ nearbyActivityViewModel.setNearestBikeAutoselected(false) })
    }

    private fun setupClearFab() {
        clearFAB = findViewById(R.id.clear_fab)

        clearFAB.setOnClickListener { nearbyActivityViewModel.setStationB(null) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PLACE_AUTOCOMPLETE_REQUEST_CODE -> {
                placeAutocompleteLoadingProgressBar.visibility = View.GONE
                searchFAB.backgroundTintList = ContextCompat.getColorStateList(this, R.color.theme_primary_dark)
                nearbyActivityViewModel.showFavoritePickerFab()
                //mFavoritesSheetFab.showFab();
                addFavoriteFAB.hide()
                nearbyActivityViewModel.showSearchFab()
            }
        }
    }

    //TODO: this business logic will go in Activity model
    //Maybe adapter should just observe activity model ?
    private fun clearBTab() {
        getContentTablePagerAdapter().removeStationHighlightForTable(StationTablePagerAdapter.DOCK_STATIONS)

        getContentTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS,
                false, null, null,
                getString(R.string.b_tab_question))

        //stationMapFragment.clearMarkerPickedPlace()
        //stationMapFragment.clearMarkerPickedFavorite()

        //A TAB
        getContentTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, false)

        if (nearbyActivityViewModel.isLookingForBike.value == null || nearbyActivityViewModel.isLookingForBike.value == false) {
            //stationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(stationMapFragment.markerALatLng, 13f))
            //mFavoritesSheetFab.showFab();
            nearbyActivityViewModel.showFavoritePickerFab()
            //nearbyActivityViewModel.showFavoriteFab()
            //mFavoriteListViewModel.showFab();
            if (Utils.Connectivity.isConnected(this@FindMyBikesActivity))
                nearbyActivityViewModel.showSearchFab()
            clearFAB.hide()
            addFavoriteFAB.hide()
        } else {
            val highlightedStation = getContentTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS)
            animateCameraToShowUserAndStation(highlightedStation)
        }
    }

    private fun animateCameraToShowUserAndStation(station: BikeStation?) {

        if (DEBUG_FAKE_USER_CUR_LOC != null) {
            if (tripDetailsFragment.visibility != View.VISIBLE)
            //Directions to A fab is visible
                animateCameraToShow(resources.getDimension(R.dimen.camera_fab_padding).toInt(), station!!.location, DEBUG_FAKE_USER_CUR_LOC, null)
            else
            //Map id padded on the left and interface is clear on the right
                animateCameraToShow(resources.getDimension(R.dimen.camera_ab_pin_padding).toInt(), station!!.location, DEBUG_FAKE_USER_CUR_LOC, null)

        } else {
            //stationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(station!!.location, 15f))
        }
    }

    //TODO: refactor this method such as
    //-passing only one valid LatLng leads to a regular animateCamera
    //-passing identical LatLng leads to a regular animateCamera, maybe with client code provided zoom level or a default one
    private fun animateCameraToShow(_cameraPaddingPx: Int, _latLng0: LatLng, _latLng1: LatLng, _latLng2: LatLng?) {
        val boundsBuilder = LatLngBounds.Builder()

        boundsBuilder.include(_latLng0).include(_latLng1)

        if (_latLng2 != null)
            boundsBuilder.include(_latLng2)

        //stationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), _cameraPaddingPx)) //Pin icon is 36 dp
    }

    private fun showTripDetailsFragment() {

        tripDetailsFragment.visibility = View.VISIBLE

        buildTripDetailsWidgetAnimators(true, ((resources.getInteger(R.integer.camera_animation_duration) / 3) * 2).toLong(), 0.23f)!!.start()
    }

    private fun hideTripDetailsFragment(model: NearbyActivityViewModel) {


        val hideAnimator = buildTripDetailsWidgetAnimators(false, (resources.getInteger(R.integer.camera_animation_duration) / 3).toLong(), 0.23f)
        // make the view invisible when the animation is done
        hideAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tripDetailsFragment.visibility = View.INVISIBLE

                if (model.isTripDetailsFragmentShown.value == true) {
                    showTripDetailsFragment()
                }
            }
        })
        hideAnimator.start()
    }

    //For reusable Animators (which most Animators are, apart from the one-shot animator produced by createCircularReveal()
    private fun buildTripDetailsWidgetAnimators(show: Boolean, duration: Long, _minRadiusMultiplier: Float): Animator? {

        val minRadiusMultiplier = Math.min(1f, _minRadiusMultiplier)

        val toReturn: Animator?

        // Native circular reveal uses coordinates relative to the view
        val revealStartX = 0
        val revealStartY = tripDetailsFragment.height

        val radiusMax = Math.hypot(tripDetailsFragment.height.toDouble(), tripDetailsFragment.width.toDouble()).toFloat()
        val radiusMin = radiusMax * minRadiusMultiplier

        toReturn = if (show) {
            ViewAnimationUtils.createCircularReveal(tripDetailsFragment, revealStartX,
                    revealStartY, radiusMin, radiusMax)
        } else {
            ViewAnimationUtils.createCircularReveal(tripDetailsFragment, revealStartX,
                    revealStartY, radiusMax, radiusMin)
        }

        toReturn!!.duration = duration
        toReturn.interpolator = circularRevealInterpolator

        return toReturn
    }

    private fun setupActionBarStrings() {

        supportActionBar!!.title = Utils.fromHtml(String.format(resources.getString(R.string.appbar_title_formatting),
                resources.getString(R.string.appbar_title_prefix),
                DBHelper.getInstance().getHashtaggableNetworkName(this),
                resources.getString(R.string.appbar_title_postfix)))
        //doesn't scale well, but just a little touch for my fellow Montréalers
        var cityHashtag = ""
        val bikeNetworkCity = DBHelper.getInstance().getBikeNetworkCity(this)
        if (bikeNetworkCity.contains("Montreal")) {
            cityHashtag = " @mtlvi"
        }
        val hastagedEnhancedBikeNetworkCity = bikeNetworkCity + cityHashtag
        supportActionBar!!.subtitle = Utils.fromHtml(String.format(resources.getString(R.string.appbar_subtitle_formatted), hastagedEnhancedBikeNetworkCity))
    }

    private fun setupSearchFab() {

        searchFAB.setOnClickListener(View.OnClickListener {
            if (placeAutocompleteLoadingProgressBar.visibility != View.GONE)
                return@OnClickListener

            try {

                val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                        .setBoundsBias(DBHelper.getInstance().getBikeNetworkBounds(this@FindMyBikesActivity,
                                Utils.getAverageBikingSpeedKmh(this@FindMyBikesActivity).toDouble()))
                        .build(this@FindMyBikesActivity)

                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)

                nearbyActivityViewModel.hideFavoritePickerFab()

                searchFAB.backgroundTintList = ContextCompat.getColorStateList(this@FindMyBikesActivity, R.color.light_gray)

                placeAutocompleteLoadingProgressBar.visibility = View.VISIBLE

                getContentTablePagerAdapter().hideStationRecap()

            } catch (e: GooglePlayServicesRepairableException) {
                Log.d("mPlacePickerFAB onClick", "oops", e)
            } catch (e: GooglePlayServicesNotAvailableException) {
                Log.d("mPlacePickerFAB onClick", "oops", e)
            }
        })
    }

    private fun setupDirectionsLocToAFab() {
        directionsLocToAFab.setOnClickListener {
            val curSelectedStation = getContentTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS)

            // Seen NullPointerException in crash report.
            if (null != curSelectedStation) {

                val tripLegOrigin = if (nearbyActivityViewModel.isLookingForBike.value == true) DEBUG_FAKE_USER_CUR_LOC else stationMapFragment.markerALatLng
                val tripLegDestination = curSelectedStation.location
                val walkMode = nearbyActivityViewModel.isLookingForBike.value

                if (walkMode != null) {
                    launchGoogleMapsForDirections(tripLegOrigin!!, tripLegDestination, walkMode)
                }
                else{
                    launchGoogleMapsForDirections(tripLegOrigin!!, tripLegDestination, false)
                }
            }
        }
    }

    private fun launchGoogleMapsForDirections(origin: LatLng?, destination: LatLng?, walking: Boolean) {

        origin?.let {
            destination?.let {

                val builder = StringBuilder("http://maps.google.com/maps?&saddr=")

                builder.append(origin.latitude).append(",").append(origin.longitude)

                builder.append("&daddr=").append(destination.latitude).append(",").append(destination.longitude).append("&dirflg=")//append("B"). Labeling doesn't work :'(

                if (walking)
                    builder.append("w")
                else
                    builder.append("b")

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(builder.toString()))
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
                if (packageManager.queryIntentActivities(intent, 0).size > 0) {
                    startActivity(intent) // launch the map activity
                } else {
                    Utils.Snackbar.makeStyled(coordinatorLayout, R.string.google_maps_not_installed,
                            Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                            .show()
                }
            }
        }
    }

    private fun getContentTablePagerAdapter(): StationTablePagerAdapter {
        return stationTableViewPager.adapter as StationTablePagerAdapter
    }

    //TODO: Onboarding fragment

    private fun setupFavoritePickerFab() {

        favoritePickerFAB = findViewById<Fab>(R.id.favorite_picker_fab)

        val sheetView = findViewById<View>(R.id.fab_sheet)
        //Sheet stays in nearby activity for now
        //but contains only a frame in which to do a fragment transaction

        val newFavListFragment = FavoriteListFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.favorite_list_fragment_holder, newFavListFragment)
        transaction.commit()

        ///////////////

        val overlay = findViewById<View>(R.id.overlay)
        val sheetColor = ContextCompat.getColor(this, R.color.cardview_light_background)
        val fabColor = ContextCompat.getColor(this, R.color.theme_primary_dark)


        //Caused by: java.lang.NullPointerException (sheetView)
        // Create material sheet FAB
        favoritesSheetFab = EditableMaterialSheetFab(
                this,
                nearbyActivityViewModel, favoritePickerFAB,
                sheetView, overlay, sheetColor, fabColor,
                newFavListFragment)


        favoritesSheetFab.setEventListener(object : MaterialSheetFabEventListener() {
            override fun onShowSheet() {

                nearbyActivityViewModel.hideSearchFab()
            }

            override fun onSheetHidden() {

                if (nearbyActivityViewModel.isLookingForBike.value == false &&
                        nearbyActivityViewModel.getStationB().value == null &&
                        nearbyActivityViewModel.isConnectivityAvailable.value == true)

                    nearbyActivityViewModel.showSearchFab()
            }
        })
    }

    override fun onRefresh() {
        val modelFactory = InjectorUtils.provideMainActivityViewModelFactory(this.application)
        nearbyActivityViewModel = ViewModelProviders.of(this, modelFactory).get(NearbyActivityViewModel::class.java)

        //this is debug
        nearbyActivityViewModel.setDataOutOfDate(!(nearbyActivityViewModel.isDataOutOfDate.value
                ?: true))

        //TODO: act on model ?
        getTablePagerAdapter().setRefreshingAll(false)

        //TODO: this is debug
        //getTablePagerAdapter().smoothScrollHighlightedInViewForTable(BIKE_STATIONS, true)

    }

    private fun getTablePagerAdapter(): StationTablePagerAdapter {
        return stationTableViewPager.adapter as StationTablePagerAdapter
    }
}