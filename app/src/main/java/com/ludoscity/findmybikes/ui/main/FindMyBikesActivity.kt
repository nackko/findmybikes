package com.ludoscity.findmybikes.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.fondesa.kpermissions.extension.listeners
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.ui.main.StationTablePagerAdapter.Companion.BIKE_STATIONS
import com.ludoscity.findmybikes.ui.map.StationMapFragment
import com.ludoscity.findmybikes.ui.settings.SettingsActivity
import com.ludoscity.findmybikes.ui.sheet.EditableMaterialSheetFab
import com.ludoscity.findmybikes.ui.sheet.Fab
import com.ludoscity.findmybikes.ui.sheet.FavoriteListFragment
import com.ludoscity.findmybikes.ui.webview.WebViewActivity
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import de.psdev.licensesdialog.LicensesDialog
import java.text.NumberFormat

class FindMyBikesActivity : AppCompatActivity(),
        FavoriteListFragment.OnFavoriteListFragmentInteractionListener,
        ViewPager.OnPageChangeListener,
        SwipeRefreshLayout.OnRefreshListener {
    //TODO: use this to do the animated bike feature
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        //Log.d(TAG, "onPageScrolled : position:$position, positionOffset:$positionOffset")
        if (positionOffset == 0.0f) {

            getTablePagerAdapter().smoothScrollHighlightedInViewForTable(position)
        }
    }

    override fun onPageSelected(position: Int) {
        findMyBikesActivityViewModel.setSelectedTable(stationTableViewPager.currentItem == BIKE_STATIONS)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onFavoriteItemDeleted(favoriteId: String, showUndo: Boolean) {
        findMyBikesActivityViewModel.removeFavoriteByFavoriteId(favoriteId)
    }

    override fun onFavoriteListChanged(noFavorite: Boolean) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFavoriteListItemClicked(favoriteId: String) {
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

    private lateinit var tripDetailsFragment: View
    private lateinit var tripDetailsProximityTotal: TextView

    private lateinit var findMyBikesActivityViewModel: FindMyBikesActivityViewModel

    private val TABS_ICON_RES_ID = intArrayOf(R.drawable.ic_pin_a_36dp_white, R.drawable.ic_pin_b_36dp_white)

    private var searchAutocompleteIntent: Intent? = null

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

        const val PLACE_AUTOCOMPLETE_REQUEST_CODE: Int = 1
        const val SETTINGS_ACTIVITY_REQUEST_CODE: Int = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.FindMyBikesTheme) //https://developer.android.com/topic/performance/launch-time.html

        super.onCreate(savedInstanceState)

        val mapInit = MapsInitializer.initialize(applicationContext)

        if (mapInit != 0) {
            Log.e(TAG, "GooglePlayServicesNotAvailableException raised with error code :$mapInit")
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

        val modelFactory = InjectorUtils.provideMainActivityViewModelFactory(this.application)
        findMyBikesActivityViewModel = ViewModelProviders.of(this, modelFactory).get(FindMyBikesActivityViewModel::class.java)


        setContentView(R.layout.activity_findmybikes)
        setSupportActionBar(findViewById<View>(R.id.toolbar_main) as Toolbar)

        findMyBikesActivityViewModel.lastStartActivityForResultIntent.observe(this, Observer {
            it?.let { data ->
                findMyBikesActivityViewModel.clearLastStartActivityForResultRequest()
                startActivityForResult(data.first, data.second)
            }
        })

        //TODO: Model should prepare desired app bar expansion state. !!!appBarExpanded already in model tracks if it *is* expanded or not!!!
        findMyBikesActivityViewModel.isLookingForBike.observe(this, Observer {
            appBarLayout.setExpanded(it != true, true)
        })

        findMyBikesActivityViewModel.stationData.observe(this, Observer { stationDataList ->
            Log.d(TAG, "New data has " + (stationDataList?.size ?: "") + " stations")

            stationDataList?.let {
                if (it.isNotEmpty()) {
                    Log.d(TAG, "un nom : " + it[0].name)
                }
            }
        })

        findMyBikesActivityViewModel.isFavoritePickerFabShown.observe(this, Observer {

            if (it == true)
                favoritesSheetFab.showFab()
            else
                favoritesSheetFab.hideSheetThenFab()
        })

        findMyBikesActivityViewModel.isFavoriteSheetShown.observe(this, Observer {
            if (it == true)
                favoritesSheetFab.showSheet()
            else
                favoritesSheetFab.hideSheet()

        })

        findMyBikesActivityViewModel.isFavoriteFabShown.observe(this, Observer {
            if (it == true) {
                addFavoriteFAB.show()
            } else {
                addFavoriteFAB.hide()
            }
        })

        findMyBikesActivityViewModel.isClearBSelectionFabShown.observe(this, Observer {
            if (it == true) {
                clearFAB.show()
            } else {
                clearFAB.hide()
            }
        })

        findMyBikesActivityViewModel.isSearchFabShown.observe(this, Observer {
            if (it == true)
                searchFAB.show()
            else
                searchFAB.hide()
        })

        findMyBikesActivityViewModel.isDirectionsToStationAFabShown.observe(this, Observer {
            if (it == true)
                directionsLocToAFab.show()
            else
                directionsLocToAFab.hide()
        })

        InjectorUtils.provideRepository(this).lastBikeNetworkStatusFetchErrored.observeForever {
            if (it == true) {
                getTablePagerAdapter().setRefreshingAll(false)
            }
        }

        findMyBikesActivityViewModel.curBikeSystem.observe(this, Observer { newBikeSystem ->

            setupFavoritePickerFab()
            setupActionBarStrings(newBikeSystem)

            newBikeSystem?.let {
                searchAutocompleteIntent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)

                        .setBoundsBias(Utils.getBikeSpeedPaddedBounds(this,
                                LatLngBounds(LatLng(it.boundingBoxSouthWestLatitude ?: 0.0,
                                        it.boundingBoxSouthWestLongitude ?: 0.0),
                                        LatLng(it.boundingBoxNorthEastLatitude ?: 0.0,
                                                it.boundingBoxNorthEastLongitude ?: 0.0))))
                        .build(this)

                //TODO: act on model ?
                getTablePagerAdapter().setRefreshingAll(false)
            }


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

        findMyBikesActivityViewModel.curBikeSystem.observe(this, Observer { bikeSystem ->
            bikeSystem?.let {
                findViewById<TextView>(R.id.favorites_sheet_header_textview).text =
                        Utils.fromHtml(String.format(resources.getString(R.string.favorites_sheet_header),
                                it.name
                        ))
            }
        })

        findMyBikesActivityViewModel.isFavoriteSheetEditInProgress.observe(this, Observer {
            if (it == true) {
                favoritesSheetFab.hideEditFab()
                favoritesSheetFab.showEditDoneFab()
            } else {
                favoritesSheetFab.showEditFab()
                favoritesSheetFab.hideEditDoneFab()
            }
        })

        findMyBikesActivityViewModel.autocompleteLoadingProgressBarVisibility.observe(this, Observer {
            if (it != null) {
                placeAutocompleteLoadingProgressBar.visibility = it
            }
        })

        findMyBikesActivityViewModel.isConnectivityAvailable.observe(this, Observer {
            //TODO: doesn't work when launching while offline. Probably because table fragments don't exist just yet
            //TODO: refactor such as not going through adapter (pass LiveData<Boolean> to table model constructor). Manipulate it from activity model
            //Do it when refactoring distance comparator
            getTablePagerAdapter().setRefreshEnabledAll(it == true)
        })

        findMyBikesActivityViewModel.searchFabBackgroundtintListColorResId.observe(this, Observer {
            it?.let { colorResId ->
                searchFAB.backgroundTintList = ContextCompat.getColorStateList(this, colorResId)
            }
        })

        // Update Bar - TODO: Have fragment ?
        statusTextView = findViewById(R.id.status_textView)
        statusBar = findViewById<View>(R.id.app_status_bar)

        findMyBikesActivityViewModel.statusBarText.observe(this, Observer {
            statusTextView.text = it
        })

        findMyBikesActivityViewModel.statusBarBackgroundColorResId.observe(this, Observer { colorResId ->
            colorResId?.let {
                statusBar.setBackgroundColor(ContextCompat.getColor(this, it))
            }
        })

        stationTableViewPager = findViewById(R.id.station_table_viewpager)
        stationTableViewPager.adapter = StationTablePagerAdapter(supportFragmentManager,
                InjectorUtils.provideTableFragmentViewModelFactory(application,
                        false,
                        findMyBikesActivityViewModel.isAppBarExpanded(),
                        findMyBikesActivityViewModel.isDataOutOfDate,
                        findMyBikesActivityViewModel.bikeTableProximityShown,
                        findMyBikesActivityViewModel.bikeTableProximityHeaderFromResId,
                        findMyBikesActivityViewModel.bikeTableProximityHeaderToResId,
                        findMyBikesActivityViewModel.getStationA(),
                        findMyBikesActivityViewModel.getStationA(),
                        findMyBikesActivityViewModel.distanceToUserComparator,
                        findMyBikesActivityViewModel.totalTripTimeComparator,
                        NumberFormat.getInstance()),
                InjectorUtils.provideTableFragmentViewModelFactory(application,
                        true,
                        findMyBikesActivityViewModel.isAppBarExpanded(),
                        findMyBikesActivityViewModel.isDataOutOfDate,
                        findMyBikesActivityViewModel.dockTableProximityShown,
                        findMyBikesActivityViewModel.dockTableProximityHeaderFromResId,
                        findMyBikesActivityViewModel.dockTableProximityHeaderToResId,
                        findMyBikesActivityViewModel.getStationA(),
                        findMyBikesActivityViewModel.getStationB(),
                        findMyBikesActivityViewModel.distanceToUserComparator,
                        findMyBikesActivityViewModel.totalTripTimeComparator,
                        NumberFormat.getInstance()
                ))
        stationTableViewPager.addOnPageChangeListener(this)
        findMyBikesActivityViewModel.setSelectedTable(stationTableViewPager.currentItem == BIKE_STATIONS)

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
                findMyBikesActivityViewModel.setAppBarExpanded(false)
            else if (findMyBikesActivityViewModel.isAppBarExpanded().value != true)
                findMyBikesActivityViewModel.setAppBarExpanded(true)
        }



        coordinatorLayout = findViewById(R.id.snackbar_coordinator)

        //TODO: add splashscreen back
        //splashScreen = findViewById<View>(R.id.fragment_splash_screen)

        //TODO: trip details fragment
        tripDetailsFragment = findViewById<View>(R.id.trip_details_fragment)

        val layoutListener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                v?.removeOnLayoutChangeListener(this)
                findMyBikesActivityViewModel.isTripDetailsFragmentShown.observe(this@FindMyBikesActivity, Observer {
                    //We start hide animation on show status change. At the end of the hide anim, callback will launch
                    //show animation if show status is true then
                    hideTripDetailsFragment(findMyBikesActivityViewModel)
                })
            }
        }

        tripDetailsFragment.addOnLayoutChangeListener(layoutListener)

        searchFAB = findViewById(R.id.search_fab)
        addFavoriteFAB = findViewById(R.id.favorite_add_remove_fab)
        directionsLocToAFab = findViewById(R.id.directions_loc_to_a_fab)

        directionsLocToAFab.setOnClickListener {

            launchGoogleMapsForDirections(findMyBikesActivityViewModel.userLocation.value,
                    findMyBikesActivityViewModel.stationALatLng.value,
                    true)
        }
        placeAutocompleteLoadingProgressBar = findViewById(R.id.place_autocomplete_loading)

        addFavoriteFAB.setOnClickListener {
            findMyBikesActivityViewModel.addFinalDestToFavoriteList()
        }

        //TODO: add splashscreen back
        /*if (savedInstanceState == null)
            splashScreen.visibility = View.VISIBLE*/

        setupSearchFab()
        setupFavoritePickerFab()
        setupClearFab()
        setupAutoselectBikeFab()

        setStatusBarClickListener()

        setupFavoriteSheet()

        circularRevealInterpolator = AnimationUtils.loadInterpolator(this, R.interpolator.msf_interpolator)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_findmybikes, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_menu_item -> {
                findMyBikesActivityViewModel.requestStartActivityForResult(
                        Intent(this, SettingsActivity::class.java), SETTINGS_ACTIVITY_REQUEST_CODE)
                return true
            }
            R.id.about_menu_item -> {
                ContextCompat.getDrawable(this@FindMyBikesActivity, R.drawable.logo_48dp)?.let {
                    MaterialDialog.Builder(this)
                            .title("${getString(R.string.app_name)} - ${getString(R.string.app_version_name)} ©2015–2019     F8Full")//http://stackoverflow.com/questions/4471025/how-can-you-get-the-manifest-version-number-from-the-apps-layout-xml-variable-->
                            .items(R.array.about_dialog_items)
                            .icon(it)
                            .autoDismiss(false)
                            .itemsCallback { _: MaterialDialog, _: View, which: Int, text: CharSequence ->
                                when (which) {
                                    0 -> {
                                        val intent = Intent(this@FindMyBikesActivity, WebViewActivity::class.java)
                                        intent.putExtra(WebViewActivity.EXTRA_URL, "http://www.citybik.es")
                                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, text.toString())
                                        intent.putExtra(WebViewActivity.EXTRA_JAVASCRIPT_ENABLED, true)
                                        startActivity(intent)
                                    }
                                    1 -> {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = Uri.parse("market://details?id=com.ludoscity.findmybikes")
                                        if (intent.resolveActivity(packageManager) != null) {
                                            startActivity(intent)
                                        }
                                    }
                                    2 -> {
                                        val url = "https://www.facebook.com/findmybikes/"
                                        val uri: Uri
                                        try {
                                            packageManager.getPackageInfo("com.facebook.katana", 0)
                                            // http://stackoverflow.com/questions/24526882/open-facebook-page-from-android-app-in-facebook-version-v11
                                            uri = Uri.parse("fb://facewebmodal/f?href=$url")
                                            intent = Intent(Intent.ACTION_VIEW, uri)
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        }


                                        //Seen ActivityNotFoundException in firebase cloud lab (FB package found but can't be launched)
                                        if (intent.resolveActivity(packageManager) == null)
                                            intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

                                        startActivity(intent)
                                    }
                                    3 -> {
                                        intent = Intent(Intent.ACTION_SENDTO)
                                        intent.data = Uri.parse("mailto:") // only email apps should handle this
                                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("ludos+findmybikesfeedback" + getString(R.string.app_version_name) + "@ludoscity.com"))
                                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                                        if (intent.resolveActivity(packageManager) != null) {
                                            startActivity(intent)
                                        }
                                    }
                                    4 -> {
                                        LicensesDialog.Builder(this@FindMyBikesActivity)
                                                .setNotices(R.raw.notices)
                                                .build()
                                                .show()
                                    }
                                    5 -> {
                                        intent = Intent(this@FindMyBikesActivity, WebViewActivity::class.java)
                                        intent.putExtra(WebViewActivity.EXTRA_URL, "file:///android_res/raw/privacy_policy.html")
                                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.hashtag_privacy))
                                        startActivity(intent)
                                    }
                                    6 -> {
                                        try {
                                            // get the Twitter app if possible
                                            packageManager.getPackageInfo("com.twitter.android", 0)
                                            intent = Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=findmybikesdata"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            // no Twitter app, revert to browser
                                            intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/findmybikesdata"))
                                        }


                                        if (intent.resolveActivity(packageManager) == null)
                                            intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/findmybikesdata"))

                                        startActivity(intent)
                                    }
                                    7 -> {
                                        intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/f8full/findmybikes"))
                                        startActivity(intent)
                                    }
                                }
                            }
                            .show()
                }

                return true
            }

        }

        return super.onOptionsItemSelected(item)

    }

    override fun onResume() {

        if (findMyBikesActivityViewModel.hasLocationPermission.value != true) {
            val request = permissionsBuilder(android.Manifest.permission.ACCESS_FINE_LOCATION).build()

            Log.i(TAG, "Sending location permission request")
            request.send()

            request.listeners {
                onAccepted { Log.i(TAG, "Location permission granted");findMyBikesActivityViewModel.setLocationPermissionGranted(true) }
                onDenied { Log.i(TAG, "Location permission denied");findMyBikesActivityViewModel.setLocationPermissionGranted(false) }
                onPermanentlyDenied { Log.i(TAG, "Location permission permanently denied");findMyBikesActivityViewModel.setLocationPermissionGranted(false) }
                //onShouldShowRationale { perms, nonce ->
                //}
            }
        } else {
            Log.i(TAG, "Activity was resumed and already have location permission, carrying on...")
        }

        super.onResume()
    }

    private fun setupFavoriteSheet() {
        //TODO: ??!!
    }

    private fun setStatusBarClickListener() {

        statusBar.setOnClickListener {
            if (findMyBikesActivityViewModel.isConnectivityAvailable.value == true) {
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
        autoSelectBikeFab.setOnClickListener({ findMyBikesActivityViewModel.setNearestBikeAutoselected(false) })
    }

    private fun setupClearFab() {
        clearFAB = findViewById(R.id.clear_fab)

        clearFAB.setOnClickListener { findMyBikesActivityViewModel.setStationB(null) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        findMyBikesActivityViewModel.onActivityResult(requestCode, resultCode, data)

    }

    private fun showTripDetailsFragment() {

        tripDetailsFragment.visibility = View.VISIBLE

        buildTripDetailsWidgetAnimators(true, ((resources.getInteger(R.integer.camera_animation_duration) / 3) * 2).toLong(), 0.23f)!!.start()
    }

    private fun hideTripDetailsFragment(model: FindMyBikesActivityViewModel) {


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

    //TODO: this hole computation should happen in model
    private fun setupActionBarStrings(bs: BikeSystem?) {

        var hashtagableBikeSystemName: String = bs?.name ?: ""

        hashtagableBikeSystemName = hashtagableBikeSystemName.replace("\\s".toRegex(), "")
        hashtagableBikeSystemName = hashtagableBikeSystemName.replace("[^A-Za-z0-9 ]".toRegex(), "")
        @Suppress("UNUSED_VALUE")
        hashtagableBikeSystemName = hashtagableBikeSystemName.toLowerCase()

        supportActionBar!!.title = Utils.fromHtml(String.format(resources.getString(R.string.appbar_title_formatting),
                resources.getString(R.string.appbar_title_prefix),
                bs?.name ?: "",//hashtagableBikeSystemName,
                resources.getString(R.string.appbar_title_postfix)))
        //doesn't scale well, but just a little touch for my fellow Montréalers
        @Suppress("CanBeVal") var cityHashtag = ""
        val bikeNetworkCity = bs?.city ?: ""
        /*if (bikeNetworkCity.contains("Montréal")) {
            cityHashtag = " @mtlvi"
        }*/
        val hastagedEnhancedBikeNetworkCity = bikeNetworkCity + cityHashtag
        supportActionBar!!.subtitle = Utils.fromHtml(String.format(resources.getString(R.string.appbar_subtitle_formatted), hastagedEnhancedBikeNetworkCity))
    }

    private fun setupSearchFab() {

        searchFAB.setOnClickListener {
            if (searchAutocompleteIntent != null) {
                try {
                    findMyBikesActivityViewModel.requestStartActivityForResult(searchAutocompleteIntent!!, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                    //TODO: hise the recap and be able to show it back if user presses back from search
                    //getContentTablePagerAdapter().hideStationRecap()

                } catch (e: GooglePlayServicesRepairableException) {
                    Log.d(TAG, "oops", e)
                } catch (e: GooglePlayServicesNotAvailableException) {
                    Log.d(TAG, "oops", e)
                }
            }
        }
    }

    //intent prep in Utils (same code in TripFragmentviewModel::prepareLaunchGoogleMapsForDirections)
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
                findMyBikesActivityViewModel, favoritePickerFAB,
                sheetView, overlay, sheetColor, fabColor,
                newFavListFragment)


        favoritesSheetFab.setEventListener(object : MaterialSheetFabEventListener() {
            override fun onShowSheet() {

                findMyBikesActivityViewModel.hideSearchFab()
            }

            override fun onSheetHidden() {

                if (findMyBikesActivityViewModel.isLookingForBike.value == false &&
                        findMyBikesActivityViewModel.getStationB().value == null &&
                        findMyBikesActivityViewModel.isConnectivityAvailable.value == true)

                    findMyBikesActivityViewModel.showSearchFab()
            }
        })
    }

    override fun onRefresh() {
        //val modelFactory = InjectorUtils.provideMainActivityViewModelFactory(this.application)
        //findMyBikesActivityViewModel = ViewModelProviders.of(this, modelFactory).get(FindMyBikesActivityViewModel::class.java)

        //this is debug
        //findMyBikesActivityViewModel.setDataOutOfDate(!(findMyBikesActivityViewModel.isDataOutOfDate.value
        //        ?: true))
        findMyBikesActivityViewModel.requestCurrentBikeSystemStatusRefresh()

        //TODO: act on model ?
        getTablePagerAdapter().setRefreshingAll(true)

        //TODO: this is debug
        //getTablePagerAdapter().smoothScrollHighlightedInViewForTable(BIKE_STATIONS, true)

    }

    private fun getTablePagerAdapter(): StationTablePagerAdapter {
        return stationTableViewPager.adapter as StationTablePagerAdapter
    }
}