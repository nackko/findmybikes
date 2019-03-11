package com.ludoscity.findmybikes.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.ludoscity.findmybikes.EditableMaterialSheetFab;
import com.ludoscity.findmybikes.Fab;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.RootApplication;
import com.ludoscity.findmybikes.citybik_es.Citybik_esAPI;
import com.ludoscity.findmybikes.citybik_es.model.BikeNetworkDesc;
import com.ludoscity.findmybikes.citybik_es.model.BikeNetworkListAnswerRoot;
import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.citybik_es.model.BikeSystemStatusAnswerRoot;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityBase;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;
import com.ludoscity.findmybikes.fragments.FavoriteListFragment;
import com.ludoscity.findmybikes.helpers.BikeStationRepository;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.ui.main.NearbyActivityViewModel;
import com.ludoscity.findmybikes.ui.main.StationTablePagerAdapter;
import com.ludoscity.findmybikes.ui.map.StationMapFragment;
import com.ludoscity.findmybikes.ui.table.StationTableFragment;
import com.ludoscity.findmybikes.ui.table.StationTableRecyclerViewAdapter;
import com.ludoscity.findmybikes.utils.Utils;
import com.ludoscity.findmybikes.viewmodels.FavoriteListViewModel;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.psdev.licensesdialog.LicensesDialog;
import retrofit2.Call;
import retrofit2.Response;
import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by F8Full on 2015-07-26.
 * Activity used to display the nearby section
 */
public class NearbyActivity extends AppCompatActivity
        implements StationMapFragment.OnStationMapFragmentInteractionListener,
        StationTableFragment.OnStationListFragmentInteractionListener,
        FavoriteListFragment.OnFavoriteListFragmentInteractionListener,
        SwipeRefreshLayout.OnRefreshListener,
        ViewPager.OnPageChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private StationMapFragment mStationMapFragment = null;

    private Handler mUpdateRefreshHandler = null;
    private Runnable mUpdateRefreshRunnableCode = null;

    private Interpolator mCircularRevealInterpolator;

    private DownloadWebTask mDownloadWebTask = null;
    private RedrawMarkersTask mRedrawMarkersTask = null;
    private FindNetworkTask mFindNetworkTask = null;
    private UpdateTwitterStatusTask mUpdateTwitterTask = null;
    private SaveNetworkToDatabaseTask mSaveNetworkToDatabaseTask = null;

    private LatLng mCurrentUserLatLng = null;

    private TextView mStatusTextView;
    private View mStatusBar;
    private ViewPager mStationTableViewPager;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBarLayout;
    private CoordinatorLayout mCoordinatorLayout;
    private ProgressBar mPlaceAutocompleteLoadingProgressBar;
    private View mTripDetailsWidget;
    private TextView mTripDetailsProximityA;
    private TextView mTripDetailsProximityB;
    private TextView mTripDetailsProximitySearch;
    private TextView mTripDetailsProximityTotal;
    private FrameLayout mTripDetailsSumSeparator;
    private View mTripDetailsBToDestinationRow;
    private View mTripDetailsPinSearch;
    private View mTripDetailsPinFavorite;
    private View mSplashScreen;
    private TextView mSplashScreenTextTop;
    private TextView mSplashScreenTextBottom;

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    private static final int CHECK_GPS_REQUEST_CODE = 3;
    private static final int CHECK_PERMISSION_REQUEST_CODE = 4;
    private FloatingActionButton mDirectionsLocToAFab;
    private FloatingActionButton mSearchFAB;
    private FloatingActionButton mAddFavoriteFAB;
    private EditableMaterialSheetFab mFavoritesSheetFab;
    private FavoriteListViewModel mFavoriteListViewModel;
    private NearbyActivityViewModel mNearbyActivityViewModel;
    //private boolean mFavoriteSheetVisible = false;
    private FloatingActionButton mClearFAB;
    private Fab mFavoritePickerFAB;
    private FloatingActionButton mAutoSelectBikeFab;

    /////////////////////////////////////////////////////////////////////////////////////////////

    private boolean mRefreshMarkers = true;
    private boolean mRefreshTabs = true;

    private CameraPosition mSavedInstanceCameraPosition;

    private static final int[] TABS_ICON_RES_ID = new int[]{
            R.drawable.ic_pin_a_36dp_white,
            R.drawable.ic_pin_b_36dp_white
    };

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = false;

    private boolean mClosestBikeAutoSelected = false;

    private Snackbar mFindBikesSnackbar;

    private ShowcaseView mOnboardingShowcaseView = null;
    private Snackbar mOnboardingSnackBar = null;    //Used to display hints

    //Places favorites stressed the previous design
    //TODO: explore refactoring with the following considerations
    //-stop relying on mapfragment markers visibility to branch code
    private boolean mFavoritePicked = false;    //True from the moment a favorite is picked until it's cleared
    //also set to true when a place is converted to a favorite
    //-consider moving it to some central location (pager adapter also has a copy)
    private boolean mDataOutdated = false;

    @Override
    public void onStart() {

        mGoogleApiClient.connect();

        checkAndAskLocationPermission();

        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public void onResume() {

        super.onResume();

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        mUpdateRefreshHandler = new Handler();

        mUpdateRefreshRunnableCode = createUpdateRefreshRunnableCode();

        mUpdateRefreshHandler.post(mUpdateRefreshRunnableCode);
    }

    private void checkAndAskLocationPermission(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            mSplashScreenTextTop.setText(R.string.location_please);
            mSplashScreenTextBottom.setText("");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRequestingLocationUpdates = true;
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        cancelDownloadWebTask();
        stopUIRefresh();

        if (mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //So that Utils::getBitmapDescriptor works on API < 21
        //when doing Drawable vectorDrawable = ResourcesCompat.getDrawable(ctx.getResources(), id, null);
        //see https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88#.i8luinewc
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setTheme(R.style.FindMyBikesTheme); //https://developer.android.com/topic/performance/launch-time.html
        super.onCreate(savedInstanceState);

        int mapInit = MapsInitializer.initialize(getApplicationContext());

        if ( mapInit != 0){
            Log.e("NearbyActivity", "GooglePlayServicesNotAvailableException raised with error code :" + mapInit);
        }

        boolean autoCompleteLoadingProgressBarVisible = false;
        String showcaseTripTotalPlaceName = null;

        if (savedInstanceState != null) {

            mSavedInstanceCameraPosition = savedInstanceState.getParcelable("saved_camera_pos");
            mRequestingLocationUpdates = savedInstanceState.getBoolean("requesting_location_updates");
            mCurrentUserLatLng = savedInstanceState.getParcelable("user_location_latlng");
            mClosestBikeAutoSelected = savedInstanceState.getBoolean("closest_bike_auto_selected");
            //mFavoriteSheetVisible = savedInstanceState.getBoolean("favorite_sheet_visible");
            autoCompleteLoadingProgressBarVisible = savedInstanceState.getBoolean("place_autocomplete_loading");
            mRefreshTabs = savedInstanceState.getBoolean("refresh_tabs");
            mFavoritePicked = savedInstanceState.getBoolean("favorite_picked");
            mDataOutdated = savedInstanceState.getBoolean("data_outdated");
            showcaseTripTotalPlaceName = savedInstanceState.getString("onboarding_showcase_trip_total_place_name", null);
        }

        setContentView(R.layout.activity_nearby);
        setSupportActionBar(findViewById(R.id.toolbar_main));
        setupActionBarStrings();

        mNearbyActivityViewModel = ViewModelProviders.of(this).get(NearbyActivityViewModel.class);
        FavoriteListViewModel.setNearbyActivityModel(mNearbyActivityViewModel);
        mFavoriteListViewModel = ViewModelProviders.of(this).get(FavoriteListViewModel.class);

        mNearbyActivityViewModel.isFavoriteFabShown().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if(aBoolean == null || aBoolean)
                    mFavoritesSheetFab.showFab();
                else
                    mFavoritesSheetFab.hideSheetThenFab();
            }
        });

        mNearbyActivityViewModel.getCurrentBikeSytemId().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newBikeSystemId) {

                setupFavoritePickerFab();

                //special case for test versions in firebase lab
                //full onboarding prevents meaningful coverage (robo test don't input anything in search autocomplete widget)
                if (getString(R.string.app_version_name).contains("test") || getString(R.string.app_version_name).contains("alpha")){

                    int addedCount = 4;

                    List<BikeStation> networkStationList = RootApplication.Companion.getBikeNetworkStationList();
                    for(BikeStation station : networkStationList) {
                        if (!mFavoriteListViewModel.isFavorite(station.getLocationHash())) {

                            if (addedCount > 3)
                                break;

                            else if (addedCount % 2 == 0) { //non default favorite name
                                FavoriteEntityStation testFavToAdd = new FavoriteEntityStation(station.getLocationHash(), station.getName(), newBikeSystemId);
                                testFavToAdd.setCustomName(station.getName() + "-test");
                                mFavoriteListViewModel.addFavorite(testFavToAdd);
                            }
                            else{   //default favorite name
                                mFavoriteListViewModel.addFavorite(new FavoriteEntityStation(station.getLocationHash(), station.getName(), newBikeSystemId));
                            }

                            ++addedCount;
                        }
                    }

                    if (mOnboardingShowcaseView != null)
                        mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }

            }
        });


        // Update Bar
        mStatusTextView = findViewById(R.id.status_textView);
        mStatusBar = findViewById(R.id.app_status_bar);

        if(mDataOutdated)
            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

        mStationTableViewPager = findViewById(R.id.station_table_viewpager);
        mStationTableViewPager.setAdapter(new StationTablePagerAdapter(getSupportFragmentManager()));
        mStationTableViewPager.addOnPageChangeListener(this);

        // Give the TabLayout the ViewPager
        mTabLayout = findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mStationTableViewPager);

        //Taking care of tabs icons here as pageradapter handles only title CharSequence for now
        for (int i=0; i<mTabLayout.getTabCount() && i<TABS_ICON_RES_ID.length; ++i)
        {
            //noinspection ConstantConditions
            mTabLayout.getTabAt(i).setCustomView(R.layout.tab_custom_view);
            //noinspection ConstantConditions
            mTabLayout.getTabAt(i).setIcon(ContextCompat.getDrawable(this,TABS_ICON_RES_ID[i]));
        }

        mAppBarLayout = findViewById(R.id.action_toolbar_layout);

        mCoordinatorLayout = findViewById(R.id.snackbar_coordinator);
        mSplashScreen = findViewById(R.id.splashscreen);
        mSplashScreenTextTop = findViewById(R.id.splashscreen_text_top);
        mSplashScreenTextBottom = findViewById(R.id.splashscreen_text_bottom);

        mTripDetailsWidget = findViewById(R.id.trip_details);
        mTripDetailsProximityA = findViewById(R.id.trip_details_proximity_a);
        mTripDetailsProximityB = findViewById(R.id.trip_details_proximity_b);
        mTripDetailsProximitySearch = findViewById(R.id.trip_details_proximity_search);
        mTripDetailsProximityTotal = findViewById(R.id.trip_details_proximity_total);
        mTripDetailsSumSeparator = findViewById(R.id.trip_details_sum_separator);
        mTripDetailsBToDestinationRow = findViewById(R.id.trip_details_b_to_search);
        mTripDetailsPinSearch = findViewById(R.id.trip_details_to_search);
        mTripDetailsPinFavorite = findViewById(R.id.trip_details_to_favorite);

        mSearchFAB = findViewById(R.id.search_fab);
        mAddFavoriteFAB = findViewById(R.id.favorite_add_remove_fab);
        mDirectionsLocToAFab = findViewById(R.id.directions_loc_to_a_fab);
        mPlaceAutocompleteLoadingProgressBar = findViewById(R.id.place_autocomplete_loading);
        if (autoCompleteLoadingProgressBarVisible)
            mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

        if (showcaseTripTotalPlaceName != null){
            setupShowcaseTripTotal();
            mOnboardingShowcaseView.setContentText(String.format(getString(R.string.onboarding_showcase_total_time_text),
                    DBHelper.getInstance().getBikeNetworkName(this), showcaseTripTotalPlaceName));
            mOnboardingShowcaseView.setTag(showcaseTripTotalPlaceName);
        }

        if(savedInstanceState == null)
            mSplashScreen.setVisibility(View.VISIBLE);

        setupDirectionsLocToAFab();
        setupSearchFab();
        setupFavoritePickerFab();
        if (savedInstanceState != null && savedInstanceState.getParcelable("add_favorite_fab_data") != null){
            //TODO: inspect this. It will have consequences when seting up the UI floating <3 button state
            //setupAddFavoriteFab((FavoriteItemPlace)savedInstanceState.getParcelable("add_favorite_fab_data"));
            mAddFavoriteFAB.show();
        }
        setupClearFab();
        setupAutoselectBikeFab();

        setStatusBarClickListener();

        getTablePagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        setupFavoriteSheet();

        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_loc_to_a).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                launchGoogleMapsForDirections(mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), true);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_a_to_b).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                launchGoogleMapsForDirections(mStationMapFragment.getMarkerALatLng(), mStationMapFragment.getMarkerBVisibleLatLng(), false);
            }
        });
        //noinspection ConstantConditions
        findViewById(R.id.trip_details_directions_b_to_destination).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnboardingShowcaseView != null){
                    mOnboardingShowcaseView.hide();
                    mOnboardingShowcaseView = null;
                }
                if (mStationMapFragment.isPickedPlaceMarkerVisible())
                    launchGoogleMapsForDirections(mStationMapFragment.getMarkerBVisibleLatLng(), mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(), true);
                else //Either Place marker or Favorite marker is visible, but not both at once
                    launchGoogleMapsForDirections(mStationMapFragment.getMarkerBVisibleLatLng(), mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng(), true);
            }
        });
        findViewById(R.id.trip_details_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Je serai à la station Bixi Hutchison/beaubien dans ~15min ! Partagé via #findmybikes
                //I will be at the Bixi station Hutchison/beaubien in ~15min ! Shared via #findmybikes
                String message = String.format(getResources().getString(R.string.trip_details_share_message_content),
                        DBHelper.getInstance().getBikeNetworkName(getApplicationContext()), getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.DOCK_STATIONS).getName(),
                        mTripDetailsProximityTotal.getText().toString());

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.trip_details_share_title)));
            }
        });

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        setupLocationRequest();

        mCircularRevealInterpolator = AnimationUtils.loadInterpolator(this, R.interpolator.msf_interpolator);

        //Not empty if RootApplication::onCreate got database data
        if(RootApplication.Companion.getBikeNetworkStationList().isEmpty()){

            tryInitialSetup();
        }
    }

    private void tryInitialSetup(){

        if (Utils.Connectivity.isConnected(this)) {
            mSplashScreenTextTop.setText(getString(R.string.auto_bike_select_finding));

            if (DBHelper.getInstance().isBikeNetworkIdAvailable(this)) {

                mNearbyActivityViewModel.setCurrentBikeSytemId(DBHelper.getBikeNetworkId(this));

                mDownloadWebTask = new DownloadWebTask();
                mDownloadWebTask.execute();

                Log.i("nearbyActivity", "No sortedStationList data in RootApplication but bike network id available in DBHelper- launching first download");
            }
            else{

                mFindNetworkTask = new FindNetworkTask(DBHelper.getInstance().getBikeNetworkName(this));
                mFindNetworkTask.execute();
            }
        }
        else{
            Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                    .setAction(R.string.retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            tryInitialSetup();
                        }
                    }).show();
        }
    }

    private void setupActionBarStrings() {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.appbar_title_formatting),
                getResources().getString(R.string.appbar_title_prefix),
                DBHelper.getInstance().getHashtaggableNetworkName(this),
                getResources().getString(R.string.appbar_title_postfix))));
        //doesn't scale well, but just a little touch for my fellow Montréalers
        String city_hashtag = "";
        String bikeNetworkCity = DBHelper.getInstance().getBikeNetworkCity(this);
        if (bikeNetworkCity.contains("Montreal")){
            city_hashtag = " @mtlvi";
        }
        String hastagedEnhanced_bikeNetworkCity = bikeNetworkCity + city_hashtag;
        getSupportActionBar().setSubtitle(Utils.fromHtml(String.format(getResources().getString(R.string.appbar_subtitle_formatted), hastagedEnhanced_bikeNetworkCity)));
    }

    private void setupLocationRequest(){

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                if(status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(
                                NearbyActivity.this,
                                CHECK_GPS_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                }

            }
        });

    }

    private void setupAutoselectBikeFab() {
        mAutoSelectBikeFab = findViewById(R.id.autoselect_closest_bike);

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
        mAutoSelectBikeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mClosestBikeAutoSelected = false;
            }
        });
    }

    private void setupDirectionsLocToAFab() {
        mDirectionsLocToAFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BikeStation curSelectedStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);

                // Seen NullPointerException in crash report.
                if (null != curSelectedStation) {

                    LatLng tripLegOrigin = isLookingForBike() ? mCurrentUserLatLng : mStationMapFragment.getMarkerALatLng();
                    LatLng tripLegDestination = curSelectedStation.getLocation();
                    boolean walkMode = isLookingForBike();

                    launchGoogleMapsForDirections(tripLegOrigin, tripLegDestination, walkMode);
                }
            }
        });
    }

    //returns true if _toAdd.getLocation_hash is NOT already in favorites
    private boolean setupAddFavoriteFab(final FavoriteEntityBase _toAdd){
        //This is a full add/remove code

        mAddFavoriteFAB.setTag(_toAdd);

        //boolean alreadyFavorite = DBHelper.getFavoriteEntityForId(_toAdd.getId()) != null;
        //boolean alreadyFavorite =

        if (!mFavoriteListViewModel.isFavorite(_toAdd.getId()))
            mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
        else{
            mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_24dp);
            return false;
        }

        mAddFavoriteFAB.setOnClickListener(new View.OnClickListener() {

            boolean mIsFavorite = false;
            @Override
            public void onClick(View view) {

                if (mOnboardingShowcaseView != null){
                    if (checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_CHECKONLY))
                        animateShowcaseToItinerary(); //TODO: have those elevated to steps and have animate* methods called from checkOnboarding
                    else{
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }
                }

                if (_toAdd instanceof FavoriteEntityPlace) {
                    getTablePagerAdapter().showFavoriteHeaderInBTab();
                    mStationMapFragment.clearMarkerPickedPlace();
                    mFavoritePicked = true;
                    hideSetupShowTripDetailsWidget();
                    mStationMapFragment.setPinForPickedFavorite(_toAdd.getDisplayName(),
                            _toAdd.getLocation(),
                            _toAdd.getAttributions());
                }
                else{   //_toAdd instanceof FavoriteItemStation == true

                    mStationMapFragment.setPinForPickedFavorite(_toAdd.getDisplayName(),
                            getLatLngForStation(_toAdd.getId()),
                            null);
                }

                if (!mIsFavorite)
                {
                    mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_24dp);
                    addFavorite(_toAdd, false);
                    //TODO: Investigation how that should happen
                    //mFavoritesSheetFab.scrollToTop();
                    //should be provided by favorite list fragment ?

                    mAddFavoriteFAB.hide();
                }
                else{
                    mAddFavoriteFAB.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
                    removeFavorite(_toAdd.getId(), false);
                }

                mIsFavorite = !mIsFavorite;
            }
        });

        return true;
    }

    private void setupSearchFab() {

        mSearchFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mPlaceAutocompleteLoadingProgressBar.getVisibility() != View.GONE)
                    return;

                try {

                    Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .setBoundsBias(DBHelper.getInstance().getBikeNetworkBounds(NearbyActivity.this,
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this)))
                            .build(NearbyActivity.this);

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

                    mNearbyActivityViewModel.hideFavoriteFab();

                    mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.light_gray));

                    mPlaceAutocompleteLoadingProgressBar.setVisibility(View.VISIBLE);

                    getTablePagerAdapter().hideStationRecap();

                    //onboarding is in progress, showcasing search action button, give way to search
                    //(google provided autocompletewidget)
                    if (mOnboardingShowcaseView != null){
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }

                    if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_HINT))
                        dismissOnboardingHint();

                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.d("mPlacePickerFAB onClick", "oops", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){

            mPlaceAutocompleteLoadingProgressBar.setVisibility(View.GONE);
            mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(NearbyActivity.this, R.color.theme_primary_dark));

            if (resultCode == RESULT_OK){

                final Place place = PlaceAutocomplete.getPlace(this, data);

                mSearchFAB.hide();

                //IDs are not guaranteed stable over long periods of time
                //but searching for a place already in favorites is not a typical use case
                //TODO: implement best practice of updating favorite places IDs once per month
                CharSequence attr = place.getAttributions();
                String attrString = "";
                if (attr != null)
                    attrString = attr.toString();

                FavoriteEntityPlace newFavForPlace = new FavoriteEntityPlace(place.getId(),
                        place.getName().toString(),
                        mNearbyActivityViewModel.getCurrentBikeSytemId().getValue(),
                        place.getLatLng(),
                        attrString);

                final FavoriteEntityBase existingFavForPlace = mFavoriteListViewModel.getFavoriteEntityForId(newFavForPlace.getId());

                if ( existingFavForPlace == null) {

                    if(setupAddFavoriteFab(newFavForPlace))
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();

                    //User selected a search result, onboarding showcases total trip time and favorite action button
                    if (checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE))
                    //Because destination name is available here and can't be passed down checkOnboarding
                    {
                        mOnboardingShowcaseView.setContentText(String.format(getString(R.string.onboarding_showcase_total_time_text),
                                DBHelper.getInstance().getBikeNetworkName(this), place.getName()));

                        mOnboardingShowcaseView.setTag(place.getName());
                    }

                    final Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (NearbyActivity.this.getTablePagerAdapter().isViewPagerReady()) {
                                setupBTabSelectionClosestDock(place);
                            } else
                                handler.postDelayed(this, 10);
                        }
                    }, 50);
                }
                else{   //Place was already a favorite
                    final Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (NearbyActivity.this.getTablePagerAdapter().isViewPagerReady()) {
                                setupBTabSelectionClosestDock(existingFavForPlace.getId());
                            } else
                                handler.postDelayed(this, 10);
                        }
                    }, 50);
                }
            } else { //user pressed back, there's no search result available

                mNearbyActivityViewModel.showFavoriteFab();
                //mFavoritesSheetFab.showFab();
                mAddFavoriteFAB.hide();
                mSearchFAB.show();

                getTablePagerAdapter().showStationRecap();

                //in case of full onboarding, setup search showcase (user cancelled previous showcased search)
                //... check if full onboarding should happen
                if( !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) ) {

                    //... if it doesn't, display hint if required
                    checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
                }
            }
        } else if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE){

            DBHelper.getInstance().resumeAutoUpdate();    //Does NOT start it if user didn't activated it in Setting activity
            mClosestBikeAutoSelected = false;
            mRefreshMarkers = true;
            refreshMap();
        } else if (requestCode == CHECK_PERMISSION_REQUEST_CODE){

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                // permission was granted, yay! Do the thing
                mSplashScreenTextTop.setText(R.string.auto_bike_select_finding);
                mStationMapFragment.enableMyLocationCheckingPermission();
            }
            else
                checkAndAskLocationPermission();
        }
        else if(requestCode == CHECK_GPS_REQUEST_CODE){
            //getting here when GPS been activated through system settings dialog
            if (resultCode != RESULT_OK){

                if (mSplashScreen.isShown()){
                    mSplashScreenTextTop.setText(R.string.sad_emoji);
                    mSplashScreenTextBottom.setText("");

                    Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_turn_on, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    setupLocationRequest();
                                }
                            }).show();
                }
            }
            else{
                mSplashScreenTextTop.setText(R.string.auto_bike_select_finding);
            }
        }
    }

    private void setupFavoriteSheet() {


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("saved_camera_pos", mStationMapFragment.getCameraPosition());
        outState.putBoolean("requesting_location_updates", mRequestingLocationUpdates);
        outState.putParcelable("user_location_latlng", mCurrentUserLatLng);
        outState.putBoolean("closest_bike_auto_selected", mClosestBikeAutoSelected);
        //outState.putBoolean("favorite_sheet_visible", mFavoriteSheetVisible);
        outState.putBoolean("place_autocomplete_loading", mPlaceAutocompleteLoadingProgressBar.getVisibility() == View.VISIBLE);
        outState.putBoolean("refresh_tabs", mRefreshTabs);
        outState.putBoolean("favorite_picked", mFavoritePicked);
        outState.putBoolean("data_outdated", mDataOutdated);

        if (mOnboardingShowcaseView != null && mOnboardingShowcaseView.getTag() != null){
            outState.putString("onboarding_showcase_trip_total_place_name", (String)mOnboardingShowcaseView.getTag());
        }

        if (mAddFavoriteFAB.isShown() && mAddFavoriteFAB.getTag() instanceof FavoriteEntityPlace){
            outState.putParcelable("add_favorite_fab_data", null);
            //TODO: inspect this, should Entities be made parcelable as well ?
            //outState.putParcelable("add_favorite_fab_data", (FavoriteItemPlace) mAddFavoriteFAB.getTag());
        } else{
            outState.putParcelable("add_favorite_fab_data", null);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mStationMapFragment = (StationMapFragment)getSupportFragmentManager().findFragmentById(
                R.id.station_map_fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the thing
            mSplashScreenTextTop.setText(R.string.auto_bike_select_finding);
            mStationMapFragment.enableMyLocationCheckingPermission();

        }else {

            mSplashScreenTextTop.setText(R.string.sad_emoji);
            mSplashScreenTextBottom.setText("");

            if (ActivityCompat.shouldShowRequestPermissionRationale(NearbyActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                checkAndAskLocationPermission();
                            }
                        }).show();
            }
            else{
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.location_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, CHECK_PERMISSION_REQUEST_CODE);
                            }
                        }).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {

            case R.id.settings_menu_item:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, SETTINGS_ACTIVITY_REQUEST_CODE);
                return true;

            case R.id.about_menu_item:
                new MaterialDialog.Builder(this)
                        .title(getString(R.string.app_name) + " – " + getString(R.string.app_version_name) + " ©2015–2017     F8Full") //http://stackoverflow.com/questions/4471025/how-can-you-get-the-manifest-version-number-from-the-apps-layout-xml-variable-->
                        .items(R.array.about_dialog_items)
                        .icon(ContextCompat.getDrawable(NearbyActivity.this,R.drawable.logo_48dp))
                        .autoDismiss(false)
                        .itemsCallback(new MaterialDialog.ListCallback() {

                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                                Intent intent;

                                switch (which){
                                    case 0:
                                        startActivity(Utils.getWebIntent(NearbyActivity.this, "http://www.citybik.es", true, text.toString()));
                                        break;
                                    case 1:
                                        intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("market://details?id=com.ludoscity.findmybikes"));
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                        break;
                                    case 2:
                                        String url = "https://www.facebook.com/findmybikes/";
                                        Uri uri;
                                        try {
                                            getPackageManager().getPackageInfo("com.facebook.katana", 0);
                                            // http://stackoverflow.com/questions/24526882/open-facebook-page-from-android-app-in-facebook-version-v11
                                            uri = Uri.parse("fb://facewebmodal/f?href=" + url);
                                            intent = new Intent(Intent.ACTION_VIEW, uri);
                                        } catch (PackageManager.NameNotFoundException e) {
                                            intent = Utils.getWebIntent(NearbyActivity.this, url, true, text.toString());
                                        }

                                        //Seen ActivityNotFoundException in firebase cloud lab (FB package found but can't be launched)
                                        if(intent.resolveActivity(getPackageManager()) == null)
                                            intent = Utils.getWebIntent(NearbyActivity.this, url, true, text.toString());

                                        startActivity(intent);

                                        break;
                                    case 3:
                                        intent = new Intent(Intent.ACTION_SENDTO);
                                        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ludos+findmybikesfeedback" + getString(R.string.app_version_name) + "@ludoscity.com"});
                                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                        break;
                                    case 4:
                                        new LicensesDialog.Builder(NearbyActivity.this)
                                                .setNotices(R.raw.notices)
                                                .build()
                                                .show();
                                        break;
                                    case 5:
                                        intent = new Intent(NearbyActivity.this, WebViewActivity.class);
                                        intent.putExtra(WebViewActivity.EXTRA_URL, "file:///android_res/raw/privacy_policy.html");
                                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.hashtag_privacy));
                                        startActivity(intent);
                                        break;
                                    case 6:
                                        try {
                                            // get the Twitter app if possible
                                            getPackageManager().getPackageInfo("com.twitter.android", 0);
                                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=findmybikesdata"));
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        } catch (PackageManager.NameNotFoundException e) {
                                            // no Twitter app, revert to browser
                                            intent = Utils.getWebIntent(NearbyActivity.this, "https://twitter.com/findmybikesdata", true, text.toString());
                                        }

                                        if(intent.resolveActivity(getPackageManager()) == null)
                                            intent = Utils.getWebIntent(NearbyActivity.this, "https://twitter.com/findmybikesdata", true, text.toString());

                                        startActivity(intent);

                                        break;
                                    case 7:
                                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/f8full/ludOScity/tree/master/FindMyBikes"));
                                        startActivity(intent);
                                        break;

                                }

                            }
                        })
                        .show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeFavorite(final String _favIdToRemove, boolean showUndo) {

        /*0.*/onFavoriteItemDeleted(_favIdToRemove, showUndo);
        //Ordering matters : undo setup retrieves favorite data from model
        /*1*/mFavoriteListViewModel.removeFavorite(_favIdToRemove);
    }

    private void addFavorite(final FavoriteEntityBase _toAdd, boolean showUndo) {

        mFavoriteListViewModel.addFavorite(_toAdd);

        //To setup correct name
        final BikeStation closestBikeStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
        getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

        if (_toAdd instanceof FavoriteEntityStation)
            getTablePagerAdapter().notifyStationChangedAll(_toAdd.getId());

        if (!showUndo) {
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();
        } else {
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_added, Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            removeFavorite(_toAdd.getId(), false);
                            getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
                        }
                    }).show();
        }
    }

    private void setupFavoritePickerFab() {

        mFavoritePickerFAB = findViewById(R.id.favorite_picker_fab);

        View sheetView = findViewById(R.id.fab_sheet);
        //Sheet stays in nearby activity for now
        //but contains only a frame in which to do a fragment transaction

        FavoriteListFragment newFavListFragment = new FavoriteListFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.favorite_list_fragment_holder, newFavListFragment);
        transaction.commit();

        ///////////////

        View overlay = findViewById(R.id.overlay);
        int sheetColor = ContextCompat.getColor(this, R.color.cardview_light_background);
        int fabColor = ContextCompat.getColor(this, R.color.theme_primary_dark);


        //Caused by: java.lang.NullPointerException (sheetView)
        // Create material sheet FAB
        mFavoritesSheetFab = new EditableMaterialSheetFab(this, mNearbyActivityViewModel, mFavoritePickerFAB, sheetView, overlay, sheetColor, fabColor, newFavListFragment);




        mFavoritesSheetFab.setEventListener(new MaterialSheetFabEventListener() {
            @Override
            public void onShowSheet() {

                mSearchFAB.hide();
                //mFavoriteSheetVisible = true;   //This is tracked in viewmodel

                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_ULTRA_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_TAP_FAV_NAME_HINT))
                    dismissOnboardingHint();
            }

            @Override
            public void onSheetHidden() {
                if (!isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    //B tab with no selection
                    if (Utils.Connectivity.isConnected(NearbyActivity.this))
                        mSearchFAB.show();

                    if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) &&
                            !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT))
                    {
                        dismissOnboardingHint();
                    }
                }

                //mFavoriteSheetVisible = false;
            }
        });
    }

    private void setupClearFab() {
        mClearFAB = findViewById(R.id.clear_fab);

        mClearFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clearBSelection();
            }
        });
    }

    private void clearBSelection() {
        mFavoritePicked = false;
        mStationMapFragment.setMapPaddingLeft(0);
        mStationMapFragment.setMapPaddingRight(0);
        hideTripDetailsWidget();
        clearBTab();

        if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)) {
            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
        }
    }

    @Override
    public void onFavoriteItemEditDone(String favoriteId) {

        //TODO: handle this through activity ViewModel
        BikeStation closestBikeStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
        getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
        getTablePagerAdapter().notifyStationChangedAll(favoriteId);
    }

    @Override
    public void onFavoriteItemDeleted(final String favoriteId, boolean showUndo) {

        //To setup correct name
        final BikeStation closestBikeStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
        getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

        //TODO: this should be done by observing the list of favorite stations
        //if (_toRemove instanceof FavoriteEntityStation)
        getTablePagerAdapter().notifyStationChangedAll(favoriteId);

        if(!showUndo){

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();
        }
        else {
            final FavoriteEntityBase toReAdd = mFavoriteListViewModel.getFavoriteEntityForId(favoriteId);
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.favorite_removed,
                    Snackbar.LENGTH_LONG, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            addFavorite(toReAdd, false);
                            //mFavoritesSheetFab.scrollToTop();
                            getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
                        }
                    }).show();
        }
    }

    @Override
    public void onFavoriteListChanged(boolean noFavorite) {
         if (noFavorite){
            ((TextView)findViewById(R.id.favorites_sheet_header_textview)).setText(
                    Utils.fromHtml(String.format(getResources().getString(R.string.no_favorite), DBHelper.getInstance().getBikeNetworkName(this))));
            findViewById(R.id.favorite_sheet_edit_fab).setVisibility(View.INVISIBLE);
            findViewById(R.id.favorite_sheet_edit_done_fab).setVisibility(View.INVISIBLE);

            mNearbyActivityViewModel.favoriteSheetEditStop();
        }
        else{
            ((TextView)findViewById(R.id.favorites_sheet_header_textview)).setText(
                    Utils.fromHtml(String.format(getResources().getString(R.string.favorites_sheet_header), DBHelper.getInstance().getBikeNetworkName(this))));

            ((FloatingActionButton)findViewById(R.id.favorite_sheet_edit_fab)).show();
        }
    }

    @Override
    public void onFavoriteListItemClicked(String favoriteId) {
        BikeStation stationA = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);

        if (stationA.getLocationHash().equalsIgnoreCase(favoriteId)) {

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.such_short_trip, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();

        } else {
            mFavoritePicked = true;
            setupBTabSelectionClosestDock(favoriteId);
        }
    }

    private enum eONBOARDING_STEP { ONBOARDING_STEP_CHECKONLY, ONBOARDING_STEP_SEARCH_SHOWCASE, ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE,
        ONBOARDING_STEP_MAIN_CHOICE_HINT, ONBOARDING_STEP_TAP_FAV_NAME_HINT, ONBOARDING_STEP_SEARCH_HINT }

    private enum eONBOARDING_LEVEL{ONBOARDING_LEVEL_FULL, ONBOARDING_LEVEL_LIGHT, ONBOARDING_LEVEL_ULTRA_LIGHT}

    //returns true if conditions satisfied (onboarding is showed)
    private boolean checkOnboarding(eONBOARDING_LEVEL _level, eONBOARDING_STEP _step){

        boolean toReturn = false;

        int minValidFavorites = -1;

        if (_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_light_min_valid_favorites_count);
        else if(_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_ultra_light_min_valid_favorites_count);
        else if (_level == eONBOARDING_LEVEL.ONBOARDING_LEVEL_ULTRA_LIGHT)
            minValidFavorites = getApplicationContext().getResources().getInteger(R.integer.onboarding_none_min_valid_favorites_count);

        //count valid favorites
        if ( !mFavoriteListViewModel.hasAtleastNValidFavorites(
                getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS),
                minValidFavorites) ){

            if (_step == eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)
                setupShowcaseSearch();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_TRIP_TOTAL_SHOWCASE)
                setupShowcaseTripTotal();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT)
                setupHintMainChoice();
            else if (_step == eONBOARDING_STEP.ONBOARDING_STEP_TAP_FAV_NAME_HINT)
                setupHintTapFavName();
            else if(_step == eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_HINT)
                setupHintSearch();

            toReturn = true;
        }

        return toReturn;
    }

    @Override
    public void onBackPressed() {
        /*if (mFavoritesSheetFab.isSheetVisible()) {
            mFavoritesSheetFab.hideSheet();
            dismissOnboardingHint();
        }*/
        if (mNearbyActivityViewModel.isFavoriteSheetShown().getValue()) {
            mNearbyActivityViewModel.hideFavoriteSheet();
            dismissOnboardingHint();
        } else //noinspection StatementWithEmptyBody
            if(mOnboardingShowcaseView != null){
            //do nothing if onboarding is in progress
        } else if(isLookingForBike()){
            //A tab exploring

            //go back to B tab
                mStationTableViewPager.setCurrentItem(StationTablePagerAdapter.DOCK_STATIONS, true);

            //if no selection in B tab...
            if(mStationMapFragment.getMarkerBVisibleLatLng() == null)
            {
                //... check if full onboarding should happen
                if( !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) ) {

                    //... if it doesn't, display hint
                    setupHintMainChoice();
                }
            }
        }
        else if(!isLookingForBike() && mStationMapFragment.getMarkerBVisibleLatLng() != null){
            //B tab exploring and a station is selected

            clearBSelection();
            //in case of full onboarding, showcase search
            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE);

        } else if (!isLookingForBike() && mOnboardingSnackBar == null) {
            setupHintMainChoice();

        }else {
            //otherwise, pass it up ("exiting" app)
            super.onBackPressed();
        }
    }

    private void stopUIRefresh() {
        if (mUpdateRefreshHandler != null) {
            mUpdateRefreshHandler.removeCallbacks(mUpdateRefreshRunnableCode);
            mUpdateRefreshRunnableCode = null;
            mUpdateRefreshHandler = null;
        }
    }

    private void refreshMap(){

        /*Log.d("nearbyActivity", "refreshMap", new Exception());
        Log.d("nearbyActivity", "refreshMap outdateddata status :" + mDataOutdated );*/

        if (DBHelper.getInstance().isBikeNetworkIdAvailable(this)){

            if(mStationMapFragment.isMapReady()) {
                if (mRefreshMarkers && mRedrawMarkersTask == null) {

                    mRedrawMarkersTask = new RedrawMarkersTask();
                    mRedrawMarkersTask.execute(mDataOutdated, isLookingForBike());

                    mRefreshMarkers = false;
                }

                if (null != mSavedInstanceCameraPosition){
                    mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newCameraPosition(mSavedInstanceCameraPosition), false);
                    mSavedInstanceCameraPosition = null;
                }
            }
        }
    }

    private void setupTabPages() {

        //TAB A
        getTablePagerAdapter().setupUI(StationTablePagerAdapter.BIKE_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                true, null, R.drawable.ic_walking_24dp_white,
                "",
                mCurrentUserLatLng != null ? new StationTableRecyclerViewAdapter.DistanceComparator(mCurrentUserLatLng) : null);

        LatLng stationBLatLng = mStationMapFragment.getMarkerBVisibleLatLng();

        if (stationBLatLng == null) {
            //TAB B
            getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, new ArrayList<BikeStation>(),
                    false, null, null,
                    getString(R.string.b_tab_question), null);

            //TAB A
            getTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, false);
        }
        else {
            BikeStation highlighthedDockStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.DOCK_STATIONS);

            if (highlighthedDockStation != null) {
                setupBTabSelection(highlighthedDockStation.getLocationHash(), isLookingForBike());

                FavoriteEntityBase newFavForStation = new FavoriteEntityStation(highlighthedDockStation.getLocationHash(),
                        highlighthedDockStation.getName(),
                        mNearbyActivityViewModel.getCurrentBikeSytemId().getValue());

                boolean showFavoriteAddFab = false;

                if (!mStationMapFragment.isPickedFavoriteMarkerVisible()) {
                    if (mStationMapFragment.isPickedPlaceMarkerVisible())
                        showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                    else if (setupAddFavoriteFab(newFavForStation))
                        showFavoriteAddFab = true;
                }

                if (showFavoriteAddFab)
                    mAddFavoriteFAB.show();
                else
                    mAddFavoriteFAB.hide();
            }
            else    //B pin on map with no list selection can happen on rapid multiple screen orientation change
                clearBTab();
        }

        mRefreshTabs = false;
    }

    //TODO : This clearly turned into spaghetti. At least extract methods.
    private Runnable createUpdateRefreshRunnableCode(){
        return new Runnable() {

            private boolean mPagerReady = false;
            private NumberFormat mNumberFormat = NumberFormat.getInstance();

            /*private final long startTime = System.currentTimeMillis();
            private long lastRunTime;
            private long lastUpdateTime = System.currentTimeMillis();   //Update should be run automatically ?
            */
            @Override
            public void run() {

                long now = System.currentTimeMillis();

                if (mRedrawMarkersTask == null && getTablePagerAdapter().isViewPagerReady() &&
                        (!mPagerReady || mRefreshTabs ) ){
                    //When restoring, we don't need to setup everything from here
                    if (!mStationMapFragment.isRestoring()) { //TODO figure out how to properly determine restoration
                        setupTabPages();
                        if(isLookingForBike())  //onPageSelected is called by framework on B tab restoration
                            onPageSelected(StationTablePagerAdapter.BIKE_STATIONS);
                    }

                    mPagerReady = true;
                }

                //Update not already in progress
                if (mPagerReady && mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null) {

                    long runnableLastRefreshTimestamp = DBHelper.getInstance().getLastUpdateTimestamp(getApplicationContext());

                    long difference = now - runnableLastRefreshTimestamp;

                    StringBuilder pastStringBuilder = new StringBuilder();
                    StringBuilder futureStringBuilder = new StringBuilder();

                    if (DBHelper.getInstance().isBikeNetworkIdAvailable(getApplicationContext())) {
                        //First taking care of past time...
                        if (difference < DateUtils.MINUTE_IN_MILLIS)
                            pastStringBuilder.append(getString(R.string.moments));
                        else
                            pastStringBuilder.append(getString(R.string.il_y_a)).append(mNumberFormat.format(difference / DateUtils.MINUTE_IN_MILLIS)).append(" ").append(getString(R.string.min_abbreviated));
                    }
                    //mStatusTextView.setText(Long.toString(difference / DateUtils.MINUTE_IN_MILLIS) +" "+ getString(R.string.minsAgo) + " " + getString(R.string.fromCitibik_es) );

                    //long differenceInMinutes = difference / DateUtils.MINUTE_IN_MILLIS;

                    //from : http://stackoverflow.com/questions/25355611/how-to-get-time-difference-between-two-dates-in-android-app
                    //long differenceInSeconds = difference / DateUtils.SECOND_IN_MILLIS;
// formatted will be HH:MM:SS or MM:SS
                    //String formatted = DateUtils.formatElapsedTime(differenceInSeconds);

                    //... then about next update
                    if (Utils.Connectivity.isConnected(getApplicationContext())) {

                        getTablePagerAdapter().setRefreshEnableAll(true);
                        if (!mSearchFAB.isEnabled()) {
                            mSearchFAB.show();
                            mSearchFAB.setEnabled(true);

                            if (mOnboardingSnackBar != null && mOnboardingSnackBar.getView().getTag() != null)
                                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE))
                                    checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

                            mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
                        }

                        if (DBHelper.getInstance().isBikeNetworkIdAvailable(getApplicationContext())) {

                            if (!DBHelper.getInstance().getAutoUpdate(getApplicationContext())) {
                                futureStringBuilder.append(getString(R.string.pull_to_refresh));

                            } else {

                                //Should come from something keeping tabs on time, maybe this runnable itself
                                long wishedUpdateTime = runnableLastRefreshTimestamp + NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.update_auto_interval_minute) * 1000 * 60;  //comes from Prefs
                                //Debug
                                //long wishedUpdateTime = runnableLastRefreshTimestamp + 15 * 1000;  //comes from Prefs

                                if (now >= wishedUpdateTime) {

                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();

                                } else {

                                    futureStringBuilder.append(getString(R.string.nextUpdate)).append(" ");
                                    long differenceSecond = (wishedUpdateTime - now) / DateUtils.SECOND_IN_MILLIS;

                                    // formatted will be HH:MM:SS or MM:SS
                                    futureStringBuilder.append(DateUtils.formatElapsedTime(differenceSecond));
                                }
                            }

                            if (difference >= NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.outdated_data_time_minute) * 60 * 1000 &&
                                    !mDataOutdated) {

                                mDataOutdated = true;

                                if (mDownloadWebTask == null) {  //Auto update didn't kick in. If task cancels it will execute same code then

                                    mRefreshMarkers = true;
                                    refreshMap();
                                    mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                                    getTablePagerAdapter().setOutdatedDataAll(true);
                                }

                            }
                        }
                    } else {
                        futureStringBuilder.append(getString(R.string.no_connectivity));

                        getTablePagerAdapter().setRefreshEnableAll(false);
                        mSearchFAB.setEnabled(false);
                        mSearchFAB.hide();

                        if (getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS) != null &&
                                mOnboardingSnackBar != null && mOnboardingSnackBar.getView().getTag() != null && !((String)mOnboardingSnackBar.getView().getTag()).equalsIgnoreCase("NO_CONNECTIVITY"))
                            checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

                        mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this,R.color.theme_accent));
                    }

                    if (mDownloadWebTask == null)
                        mStatusTextView.setText(String.format(getString(R.string.status_string),
                                pastStringBuilder.toString(), futureStringBuilder.toString()));

                    //pulling the trigger on auto select
                    if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null &&
                            !mClosestBikeAutoSelected &&
                            getTablePagerAdapter().isRecyclerViewReadyForItemSelection(StationTablePagerAdapter.BIKE_STATIONS) &&
                            mStationMapFragment.isMapReady()){

                        //Requesting raw string with availability
                        String rawClosest = getTablePagerAdapter().retrieveClosestRawIdAndAvailability(true);
                        getTablePagerAdapter().highlightStationforId(true, Utils.extractNearestAvailableStationIdFromDataString(rawClosest));

                        getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.BIKE_STATIONS, isAppBarExpanded());
                        final BikeStation closestBikeStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
                        mStationMapFragment.setPinOnStation(true, closestBikeStation.getLocationHash());
                        getTablePagerAdapter().notifyStationAUpdate(closestBikeStation.getLocation(), mCurrentUserLatLng);
                        hideSetupShowTripDetailsWidget();
                        getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);

                        if (isLookingForBike()) {
                            if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                                mDirectionsLocToAFab.show();
                            }

                            mAutoSelectBikeFab.hide();
                            mStationMapFragment.setMapPaddingRight(0);

                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                                mStationTableViewPager.setCurrentItem(StationTablePagerAdapter.DOCK_STATIONS, true);


                                //mFavoritesSheetFab.showFab();
                                mNearbyActivityViewModel.showFavoriteFab();
                                //mFavoriteListViewModel.showFab();

                                //if onboarding not happening...
                                if (!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE) &&
                                        !checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT))
                                {
                                    //...open favorites sheet
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            if (!mFavoritePickerFAB.isShowRunning()){
                                                mNearbyActivityViewModel.showFavoriteSheet();
                                                //mFavoritesSheetFab.showSheet();
                                            }
                                            else
                                                handler.postDelayed(this, 10);
                                        }
                                    }, 50);

                                }
                            }
                            else {
                                animateCameraToShowUserAndStation(closestBikeStation);
                            }
                        }

                        //Bug on older API levels. Dismissing by hand fixes it.
                        // First biggest bug happened here. Putting defensive code
                        //TODO: investigate how state is maintained, Snackbar is destroyed by framework on screen orientation change
                        //TODO: Refactor this spgathetti is **TOP** priosity
                        //and probably long background state.
                        if (mFindBikesSnackbar != null){

                            mFindBikesSnackbar.dismiss();
                        }

                        Handler handler = new Handler();

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                if (mDataOutdated){

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_outdated,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));

                                }
                                else if(!closestBikeStation.isLocked() && closestBikeStation.getFreeBikes() > DBHelper.getInstance().getCriticalAvailabilityMax(NearbyActivity.this)) {

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_found,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.snackbar_green));
                                }
                                else{

                                    mFindBikesSnackbar = Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_bike_select_none,
                                            Snackbar.LENGTH_LONG, ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                                }

                                if (mOnboardingSnackBar == null)
                                    mFindBikesSnackbar.show();
                            }
                        }, 500);

                        mClosestBikeAutoSelected = true;
                        //launch twitter task if not already running, pass it the raw String
                        if ( Utils.Connectivity.isConnected(getApplicationContext()) && //data network available
                                mUpdateTwitterTask == null &&   //not already tweeting
                                rawClosest.length() > 32 + StationTableRecyclerViewAdapter.Companion.getAOK_AVAILABILITY_POSTFIX().length() && //validate format - 32 is station ID length
                                (rawClosest.contains(StationTableRecyclerViewAdapter.Companion.getAOK_AVAILABILITY_POSTFIX()) || rawClosest.contains(StationTableRecyclerViewAdapter.Companion.getBAD_AVAILABILITY_POSTFIX())) && //validate content
                                !mDataOutdated){

                            mUpdateTwitterTask = new UpdateTwitterStatusTask();
                            mUpdateTwitterTask.execute(rawClosest);

                        }
                    }

                    //Checking if station is closest bike
                    if (mDownloadWebTask == null && mRedrawMarkersTask == null && mFindNetworkTask == null && mStationMapFragment.isMapReady()){

                        if (!isStationAClosestBike()){
                            if (mStationMapFragment.getMarkerBVisibleLatLng() == null){
                                mClosestBikeAutoSelected = false;
                            }
                            else if (isLookingForBike() && !mClosestBikeAutoSelected) {
                                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                                mAutoSelectBikeFab.show();
                                animateCameraToShowUserAndStation(getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS));
                            }
                        }
                    }
                }

                //UI will be refreshed every second
                int nextTimeMillis = 1000;

                if (!mPagerReady) //Except on init
                    nextTimeMillis = 100;

                mUpdateRefreshHandler.postDelayed(mUpdateRefreshRunnableCode, nextTimeMillis);
            }
        };
    }

    private void setupShowcaseSearch() {

        if (Utils.Connectivity.isConnected(getApplicationContext())) {

            /*if(mFavoritesSheetFab.isSheetVisible())
                mFavoritesSheetFab.hideSheet();*/
            mNearbyActivityViewModel.hideFavoriteSheet();


            if (mOnboardingShowcaseView == null) {
                mOnboardingShowcaseView =
                        new ShowcaseView.Builder(NearbyActivity.this)
                                .setTarget(new ViewTarget(R.id.search_framelayout, NearbyActivity.this))
                                .setStyle(R.style.OnboardingShowcaseTheme)
                                .setContentTitle(R.string.onboarding_showcase_search_title)
                                .setContentText(R.string.onboarding_showcase_search_text)

                                .withMaterialShowcase()
                                .build();

                mOnboardingShowcaseView.hideButton();
            } else {
                mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_search_title));
                mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_search_text));
            }
        }
        else{
            setupHintMainChoice();
        }
    }

    private void setupShowcaseTripTotal() {
        if (mOnboardingShowcaseView != null)
            mOnboardingShowcaseView.hide();

        mOnboardingShowcaseView =
                new ShowcaseView.Builder(NearbyActivity.this)
                        .setTarget(new ViewTarget(R.id.trip_details_total, NearbyActivity.this))
                        .setStyle(R.style.OnboardingShowcaseTheme)
                        .setContentTitle(R.string.onboarding_showcase_total_time_title)
                        .withMaterialShowcase()
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!mStationMapFragment.isPickedFavoriteMarkerVisible())
                                    animateShowcaseToAddFavorite();
                                else
                                    animateShowcaseToItinerary();
                            }
                        })
                        .build();

        //TODO: position button depending on screen orientation
        //RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        /*lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.RIGHT_OF, R.id.trip_details_total);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            lps.addRule(RelativeLayout.END_OF, R.id.trip_details_total);
        }*/

        //lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        //lps.addRule(RelativeLayout.CENTER_IN_PARENT);
        //int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        //lps.setMargins(margin, margin, margin, margin);

        //mOnboardingShowcaseView.setButtonPosition(lps);

    }

    private void setupHintMainChoice(){

        int messageResourceId = R.string.onboarding_hint_main_choice;

        if (!Utils.Connectivity.isConnected(getApplicationContext()))
            messageResourceId = R.string.onboarding_hint_main_choice_no_connectivity;

        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, messageResourceId, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(this, R.color.theme_primary_dark))
                /*.setAction(R.string.gotit, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Snackbar dismisses itself on click
                    }
                })*/;
        if (!Utils.Connectivity.isConnected(getApplicationContext()))
            mOnboardingSnackBar.getView().setTag("NO_CONNECTIVITY");
        else
            mOnboardingSnackBar.getView().setTag("CONNECTIVITY");

        mOnboardingSnackBar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == DISMISS_EVENT_SWIPE)
                    mOnboardingSnackBar = null;
            }
        });


        mOnboardingSnackBar.show();
    }

    private void setupHintTapFavName(){
        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_tap_favorite_name, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
        mOnboardingSnackBar.getView().setTag(null);
        mOnboardingSnackBar.show();
    }

    private void setupHintSearch(){
        mOnboardingSnackBar =  Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_search, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));
        mOnboardingSnackBar.getView().setTag(null);
        mOnboardingSnackBar.show();
    }

    private void animateShowcaseToAddFavorite() {
        mOnboardingShowcaseView.hideButton();

        mOnboardingShowcaseView.setShowcase(new ViewTarget(mAddFavoriteFAB) , true);
        mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_add_favorite_title));
        mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_add_favorite_text));
    }

    private void animateShowcaseToItinerary(){
        mOnboardingShowcaseView.hideButton();

        mOnboardingShowcaseView.setShowcase(new ViewTarget(R.id.trip_details_directions_a_to_b, this), true);
        mOnboardingShowcaseView.setContentTitle(getString(R.string.onboarding_showcase_itinerary_title));
        mOnboardingShowcaseView.setContentText(getString(R.string.onboarding_showcase_itinerary_favorite_text));
    }

    private void setStatusBarClickListener() {
        //Because the citybik.es landing page is javascript heavy
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            mStatusBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.Connectivity.isConnected(getApplicationContext())) {
                        // use the android system webview
                        Intent intent = new Intent(NearbyActivity.this, WebViewActivity.class);
                        intent.putExtra(WebViewActivity.EXTRA_URL, "http://www.citybik.es");
                        intent.putExtra(WebViewActivity.EXTRA_ACTIONBAR_SUBTITLE, getString(R.string.hashtag_cities));
                        intent.putExtra(WebViewActivity.EXTRA_JAVASCRIPT_ENABLED, true);
                        startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public void onStationMapFragmentInteraction(final Uri uri) {
        //Will be warned of station details click, will make info fragment to replace list fragment

        //Map ready
        //TODO: delete, this is handled at fragment level now
        if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.Companion.getMAP_READY_PATH()))
        {
            long wishedUpdateTime = DBHelper.getInstance().getLastUpdateTimestamp(getApplicationContext()) + NearbyActivity.this.getApplicationContext().getResources().getInteger(R.integer.update_auto_interval_minute) * 1000 * 60;  //comes from Prefs

            if ( mDownloadWebTask == null && !( //if no download task been launched but conditions are met that one will be launched imminently, don't refresh map
                    DBHelper.getInstance().getAutoUpdate(this) && System.currentTimeMillis() >= wishedUpdateTime && Utils.Connectivity.isConnected(this)
                    ) )
                refreshMap();
        }
        //Marker click - ignored if onboarding is in progress
        else if (uri.getPath().equalsIgnoreCase("/" + StationMapFragment.Companion.getMARKER_CLICK_PATH()) && mOnboardingShowcaseView == null) {

            if(!isLookingForBike() || mStationMapFragment.getMarkerBVisibleLatLng() != null) {

                if (isLookingForBike()) {

                    if (getTablePagerAdapter().highlightStationForTable(uri.getQueryParameter(StationMapFragment.Companion.getMARKER_CLICK_TITLE_PARAM()),
                            StationTablePagerAdapter.BIKE_STATIONS)) {

                        getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.BIKE_STATIONS, isAppBarExpanded());

                        mStationMapFragment.setPinOnStation(true,
                                uri.getQueryParameter(StationMapFragment.Companion.getMARKER_CLICK_TITLE_PARAM()));
                        getTablePagerAdapter().setupBTabStationARecap(getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS), mDataOutdated);

                        if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                            LatLng newALatLng = mStationMapFragment.getMarkerALatLng();
                            getTablePagerAdapter().notifyStationAUpdate(newALatLng, mCurrentUserLatLng);
                            hideSetupShowTripDetailsWidget();

                            if ((getTablePagerAdapter().getClosestBikeLatLng().latitude != newALatLng.latitude) &&
                                    (getTablePagerAdapter().getClosestBikeLatLng().longitude != newALatLng.longitude)) {

                                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                                mAutoSelectBikeFab.show();
                                animateCameraToShowUserAndStation(getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS));

                            }
                        }
                    }
                } else {

                    if (mAppBarLayout != null)
                        mAppBarLayout.setExpanded(false, true);

                    //B Tab, looking for dock
                    final String clickedStationId = uri.getQueryParameter(StationMapFragment.Companion.getMARKER_CLICK_TITLE_PARAM());
                    setupBTabSelection(clickedStationId, false);

                    boolean showFavoriteAddFab = false;

                    if(!mStationMapFragment.isPickedFavoriteMarkerVisible()){
                        if (mStationMapFragment.isPickedPlaceMarkerVisible())
                            showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                        else if (setupAddFavoriteFab(new FavoriteEntityStation(clickedStationId,
                                getStation(clickedStationId).getName(),
                                mNearbyActivityViewModel.getCurrentBikeSytemId().getValue())))
                            showFavoriteAddFab = true;
                    }

                    if (showFavoriteAddFab)
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();
                }
            } else {

                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.onboarding_hint_main_choice, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                        .show();

                mStationTableViewPager.setCurrentItem(StationTablePagerAdapter.DOCK_STATIONS, true);
            }
        }
    }

    //TODO: explore refactoring with the following considerations
    //-stop relying on mapfragment markers visibility to branch code

    //Final destination is a place from the search widget
    //that means no markers are currently on map (due to app flow)
    private void setupBTabSelectionClosestDock(final Place _from){

        dismissOnboardingHint();

        //Remove any previous selection
        getTablePagerAdapter().removeStationHighlightForTable(StationTablePagerAdapter.DOCK_STATIONS);

        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }

        getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                true ,
                R.drawable.ic_destination_arrow_white_24dp,
                R.drawable.ic_pin_search_24dp_white,
                "",
                new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                        Utils.getAverageWalkingSpeedKmh(this),
                        Utils.getAverageBikingSpeedKmh(this),
                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), _from.getLatLng()));


        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
        mClearFAB.show();
        mNearbyActivityViewModel.hideFavoriteFab();
        mSearchFAB.hide();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (getTablePagerAdapter().isRecyclerViewReadyForItemSelection(StationTablePagerAdapter.DOCK_STATIONS) &&
                        mRedrawMarkersTask == null) {
                    //highlight B station in list

                    //the following is why the handler is required (to let time for things to settle after calling getTablePagerAdapter().setupUI)
                    String stationId = Utils.extractNearestAvailableStationIdFromDataString(getTablePagerAdapter().retrieveClosestRawIdAndAvailability(false));

                    getTablePagerAdapter().hideStationRecap();
                    mStationMapFragment.setPinOnStation(false, stationId);//set B pin on closest station with available dock
                    getTablePagerAdapter().highlightStationForTable(stationId, StationTablePagerAdapter.DOCK_STATIONS);
                    getTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, true);

                    mStationMapFragment.setPinForPickedPlace(_from.getName().toString(),
                            _from.getLatLng(),
                            _from.getAttributions());

                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_infowindow_padding));
                    animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                            _from.getLatLng(),
                            mStationMapFragment.getMarkerBVisibleLatLng(),
                            null);

                    getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.DOCK_STATIONS, isAppBarExpanded());
                } else {    //This is a repost if RecyclerView is not ready for selection

                    //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                    //so, retry setting up the UI before repost
                    //Replace recyclerview content
                    getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                            true ,
                            R.drawable.ic_destination_arrow_white_24dp,
                            R.drawable.ic_pin_search_24dp_white,
                            "",
                            new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                                    Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                    mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), _from.getLatLng()));
                    //end hackfix

                    handler.postDelayed(this, 10);
                }
            }
        }, 10);
    }

    //Final destination is a favorite
    //that means no markers are currently on map (due to app flow)
    private void setupBTabSelectionClosestDock(final String _favoriteId){

        dismissOnboardingHint();

        //Remove any previous selection
        getTablePagerAdapter().removeStationHighlightForTable(StationTablePagerAdapter.DOCK_STATIONS);

        //Silent parameter is ignored because widget needs refresh
        //TODO: find a more elegant solution than this damn _silent boolean, which is a hackfix - probably a refactor by splitting method in pieces
        //and call them independently as required from client
        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }

        final FavoriteEntityBase favorite = mFavoriteListViewModel.getFavoriteEntityForId(_favoriteId);

        getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                true,
                R.drawable.ic_destination_arrow_white_24dp,
                R.drawable.ic_pin_favorite_24dp_white,
                "",
                new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                        Utils.getAverageWalkingSpeedKmh(this),
                        Utils.getAverageBikingSpeedKmh(this),
                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId())));


        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
        mClearFAB.show();
        mNearbyActivityViewModel.hideFavoriteFab();
        mSearchFAB.hide();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (getTablePagerAdapter().isRecyclerViewReadyForItemSelection(StationTablePagerAdapter.DOCK_STATIONS)) {
                    //highlight B station in list

                    //the following is why the handler is required (to let time for things to settle after calling getTablePagerAdapter().setupUI)
                    String stationId = Utils.extractNearestAvailableStationIdFromDataString(getTablePagerAdapter().retrieveClosestRawIdAndAvailability(false));

                    getTablePagerAdapter().hideStationRecap();
                    mStationMapFragment.setPinOnStation(false, stationId);//set B pin on closest station with available dock
                    getTablePagerAdapter().highlightStationForTable(stationId, StationTablePagerAdapter.DOCK_STATIONS);
                    getTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, true);

                    if (!stationId.equalsIgnoreCase(favorite.getId())) {
                        //This is a three legged journey (either to a favorite station that has no dock or a place)

                        LatLng location = favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId());

                        mStationMapFragment.setPinForPickedFavorite(favorite.getDisplayName(),
                                location,
                                favorite.getAttributions());

                        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_infowindow_padding));
                        animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                                location,
                                mStationMapFragment.getMarkerBVisibleLatLng(),
                                null);
                    }
                    else    //trip to a favorite station that has docks
                    {
                        mStationMapFragment.setPinForPickedFavorite(favorite.getDisplayName(), favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId()), favorite.getAttributions());
                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                    }

                    getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.DOCK_STATIONS, isAppBarExpanded());

                } else {    //This is a repost if RecyclerView is not ready for selection

                    //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                    //so, retry stting up the UI before repost
                    //Replace recyclerview content
                    getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                            true,
                            R.drawable.ic_destination_arrow_white_24dp,
                            R.drawable.ic_pin_favorite_24dp_white,
                            "",
                            new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                                    Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                    Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                    mCurrentUserLatLng,
                                    mStationMapFragment.getMarkerALatLng(),
                                    favorite.getLocation() != null ? favorite.getLocation() : getLatLngForStation(favorite.getId())));
                    //end hackfix

                    handler.postDelayed(this, 10);
                }
            }
        }, 10);
    }

    private void setupBTabSelection(final String _selectedStationId, final boolean _silent){

        dismissOnboardingHint();

        //Remove any previous selection
        getTablePagerAdapter().removeStationHighlightForTable(StationTablePagerAdapter.DOCK_STATIONS);

        if (mTripDetailsWidget.getVisibility() == View.INVISIBLE){
            mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));
            setupTripDetailsWidget();
            showTripDetailsWidget();
        }
        else{
            hideSetupShowTripDetailsWidget();
        }

        final BikeStation selectedStation = getStation(_selectedStationId);

        getTablePagerAdapter().hideStationRecap();
        mStationMapFragment.setPinOnStation(false, _selectedStationId);
        getTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, true);

        if (!mFavoritePicked)
            mStationMapFragment.clearMarkerPickedFavorite();

        if (mStationMapFragment.isPickedPlaceMarkerVisible() || mStationMapFragment.isPickedFavoriteMarkerVisible())
        {
            LatLng locationToShow;

            if (mStationMapFragment.isPickedPlaceMarkerVisible())
                locationToShow = mStationMapFragment.getMarkerPickedPlaceVisibleLatLng();
            else
                locationToShow = mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng();

            if (!_silent) {
                if (locationToShow.latitude != selectedStation.getLocation().latitude ||
                        locationToShow.longitude != selectedStation.getLocation().longitude)
                {
                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_infowindow_padding));
                    animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                            selectedStation.getLocation(),//getLatLngForStation(_selectedStationId),
                            locationToShow,
                            null);
                }
                else{
                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedStation.getLocation(), 15));
                }
            }

            getTablePagerAdapter().highlightStationForTable(_selectedStationId, StationTablePagerAdapter.DOCK_STATIONS);
            getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.DOCK_STATIONS, isAppBarExpanded());
        }
        else{   //it's just an A-B trip
            getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                    false,
                    null,
                    null,
                    "",
                    new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                            Utils.getAverageWalkingSpeedKmh(this),
                            Utils.getAverageBikingSpeedKmh(this),
                            mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), selectedStation.getLocation()));

            if (!mFavoritePicked){
                FavoriteEntityBase fav = mFavoriteListViewModel.getFavoriteEntityForId(_selectedStationId);
                if (fav != null)
                    mStationMapFragment.setPinForPickedFavorite(fav.getDisplayName(), getLatLngForStation(_selectedStationId), null );
            }

            if (!_silent) {
                mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                mClearFAB.show();
                mNearbyActivityViewModel.hideFavoriteFab();
                mSearchFAB.hide();
            }

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (getTablePagerAdapter().isRecyclerViewReadyForItemSelection(StationTablePagerAdapter.DOCK_STATIONS)) {
                        //highlight B station in list

                        getTablePagerAdapter().highlightStationForTable(_selectedStationId, StationTablePagerAdapter.DOCK_STATIONS);
                        if (!_silent && mStationMapFragment.getMarkerBVisibleLatLng() != null)
                            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));

                        getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.DOCK_STATIONS, isAppBarExpanded());

                    } else {    //This is a repost if RecyclerView is not ready for selection

                        //hackfix. On some devices timing issues led to infinite loop with isRecyclerViewReadyForItemSelection always returning false
                        //so, retry stting up the UI before repost
                        //Replace recyclerview content
                        getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, RootApplication.Companion.getBikeNetworkStationList(),
                                false,
                                null,
                                null,
                                "",
                                new StationTableRecyclerViewAdapter.TotalTripTimeComparator(
                                        Utils.getAverageWalkingSpeedKmh(NearbyActivity.this),
                                        Utils.getAverageBikingSpeedKmh(NearbyActivity.this),
                                        mCurrentUserLatLng, mStationMapFragment.getMarkerALatLng(), selectedStation.getLocation()));
                        //end hackfix

                        handler.postDelayed(this, 10);
                    }
                }
            }, 10);
        }
    }

    private void dismissOnboardingHint()
    {
        if (mOnboardingSnackBar != null)
        {
            mOnboardingSnackBar.dismiss();
            mOnboardingSnackBar = null;
        }
    }

    //Assumption here is that there is an A and a B station selected (or soon will be)
    private void setupTripDetailsWidget() {

        final Handler handler = new Handler();    //Need to wait for list selection

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.DOCK_STATIONS) != null &&
                        getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS) != null) {

                    int locToAMinutes = 0;
                    int AToBMinutes = 0;
                    int BToSearchMinutes = 0;

                    BikeStation selectedStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
                    String formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                            mCurrentUserLatLng, true, null, NearbyActivity.this);
                    if (formattedProximityString.startsWith(">"))
                        locToAMinutes = 61;
                    else if (!formattedProximityString.startsWith("<"))
                        locToAMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityA.setText(formattedProximityString);


                    selectedStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.DOCK_STATIONS);
                    formattedProximityString = Utils.getBikingProximityString(selectedStation.getLocation(),
                            getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS).getLocation(),
                            true, null, NearbyActivity.this);
                    if (formattedProximityString.startsWith(">"))
                        AToBMinutes = 61;
                    else if (!formattedProximityString.startsWith("<"))
                        AToBMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                    mTripDetailsProximityB.setText(formattedProximityString);


                    //TODO: this string of if...elseif...elseif...else needs refactoring.
                    // Explore extract methods or create some kind of tripdetailswidget configurator
                    if (mStationMapFragment.getMarkerPickedPlaceVisibleLatLng() == null &&
                            mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng() == null) {
                        //no marker is showed

                        mTripDetailsBToDestinationRow.setVisibility(View.GONE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_a_to_b);
                    }
                    else if(mStationMapFragment.getMarkerPickedPlaceVisibleLatLng() != null){
                        //Place marker is showed

                        formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                                mStationMapFragment.getMarkerPickedPlaceVisibleLatLng(),
                                true, null, NearbyActivity.this);

                        if (formattedProximityString.startsWith(">"))
                            BToSearchMinutes = 61;
                        else if (!formattedProximityString.startsWith("<"))
                            BToSearchMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                        mTripDetailsProximitySearch.setText(formattedProximityString);

                        mTripDetailsPinSearch.setVisibility(View.VISIBLE);
                        mTripDetailsPinFavorite.setVisibility(View.INVISIBLE);
                        mTripDetailsBToDestinationRow.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_b_to_search);
                    }
                    else if (mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude != mStationMapFragment.getMarkerBVisibleLatLng().latitude ||
                                mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude != mStationMapFragment.getMarkerBVisibleLatLng().longitude) {
                        //Favorite marker is showed and not on B station

                        formattedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                                mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng(),
                                true, null, NearbyActivity.this);

                        if (formattedProximityString.startsWith(">"))
                            BToSearchMinutes = 61;
                        else if (!formattedProximityString.startsWith("<"))
                            BToSearchMinutes = Integer.valueOf(formattedProximityString.substring(1, 3));

                        mTripDetailsProximitySearch.setText(formattedProximityString);

                        mTripDetailsPinSearch.setVisibility(View.INVISIBLE);
                        mTripDetailsPinFavorite.setVisibility(View.VISIBLE);
                        mTripDetailsBToDestinationRow.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_b_to_search);
                    }
                    else{
                        //Favorite marker is showed and on B station

                        mTripDetailsBToDestinationRow.setVisibility(View.GONE);
                        ViewGroup.LayoutParams param = mTripDetailsSumSeparator.getLayoutParams();
                        ((RelativeLayout.LayoutParams) param).addRule(RelativeLayout.BELOW, R.id.trip_details_a_to_b);
                    }

                    int total = locToAMinutes + AToBMinutes + BToSearchMinutes;

                    mTripDetailsProximityTotal.setText(Utils.durationToProximityString(total, false, null, NearbyActivity.this));

                } else
                    handler.postDelayed(this, 10);
            }
        }, 10);
    }

    //For reusable Animators (which most Animators are, apart from the one-shot animator produced by createCircularReveal()
    private Animator buildTripDetailsWidgetAnimators(boolean _show, long _duration, float _minRadiusMultiplier){

        float minRadiusMultiplier = Math.min(1.f, _minRadiusMultiplier);

        Animator toReturn = null;

        // Use native circular reveal on Android 5.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Native circular reveal uses coordinates relative to the view
            int revealStartX = 0;
            int revealStartY = mTripDetailsWidget.getHeight();

            float radiusMax = (float)Math.hypot(mTripDetailsWidget.getHeight(), mTripDetailsWidget.getWidth());
            float radiusMin = radiusMax * minRadiusMultiplier;

            if (_show)
            {
                toReturn = ViewAnimationUtils.createCircularReveal(mTripDetailsWidget, revealStartX,
                                revealStartY, radiusMin, radiusMax);
            } else {
                toReturn = ViewAnimationUtils.createCircularReveal(mTripDetailsWidget, revealStartX,
                        revealStartY, radiusMax, radiusMin);
            }

            toReturn.setDuration(_duration);
            toReturn.setInterpolator(mCircularRevealInterpolator);
        }

        return toReturn;
    }

    private void showTripDetailsWidget(){

        mTripDetailsWidget.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {
                buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration), 0).start();
            }
            catch (IllegalStateException e){
                Log.i("NearbyActivity", "Trip widget show animation end encountered some trouble, skipping", e);
            }
        }
    }

    private void hideSetupShowTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f);

            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    try {
                        setupTripDetailsWidget();
                        buildTripDetailsWidgetAnimators(true, getResources().getInteger(R.integer.camera_animation_duration) / 2, .23f).start();
                    }
                    catch (IllegalStateException e){
                        Log.i("NearbyActivity", "Trip widget hide animation end encountered some trouble, skipping", e);
                    }
                }
            });

            hideAnimator.start();
        }
        else{
            setupTripDetailsWidget();
        }
    }

    private void hideTripDetailsWidget(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animator hideAnimator = buildTripDetailsWidgetAnimators(false, getResources().getInteger(R.integer.camera_animation_duration), 0);
            // make the view invisible when the animation is done
            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mTripDetailsWidget.setVisibility(View.INVISIBLE);
                }
            });
            hideAnimator.start();
        }
        else
            mTripDetailsWidget.setVisibility(View.INVISIBLE);
    }

    private void clearBTab(){
        getTablePagerAdapter().removeStationHighlightForTable(StationTablePagerAdapter.DOCK_STATIONS);

        getTablePagerAdapter().setupUI(StationTablePagerAdapter.DOCK_STATIONS, new ArrayList<BikeStation>(),
                false, null, null,
                getString(R.string.b_tab_question), null);

        mStationMapFragment.clearMarkerB();
        mStationMapFragment.clearMarkerPickedPlace();
        mStationMapFragment.clearMarkerPickedFavorite();

        //A TAB
        getTablePagerAdapter().setClickResponsivenessForTable(StationTablePagerAdapter.BIKE_STATIONS, false);

        if (!isLookingForBike()) {
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13));
            //mFavoritesSheetFab.showFab();
            mNearbyActivityViewModel.showFavoriteFab();
            //mFavoriteListViewModel.showFab();
            if (Utils.Connectivity.isConnected(NearbyActivity.this))
                mSearchFAB.show();
            mClearFAB.hide();
            mAddFavoriteFAB.hide();
        }
        else{
            BikeStation highlightedStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
            animateCameraToShowUserAndStation(highlightedStation);
        }
    }

    private boolean isLookingForBike() {
        return mStationTableViewPager.getCurrentItem() == StationTablePagerAdapter.BIKE_STATIONS;
    }

    private LatLng getLatLngForStation(String _stationId){
        LatLng toReturn = null;

        BikeStation station = getStation(_stationId);

        if (station != null)
            toReturn = station.getLocation();

        return toReturn;
    }

    private BikeStation getStation(String _stationId){
        BikeStation toReturn = null;

        List<BikeStation> networkStationList = RootApplication.Companion.getBikeNetworkStationList();
        for(BikeStation station : networkStationList){
            if (station.getLocationHash().equalsIgnoreCase(_stationId)){
                toReturn = station;
                break;
            }
        }

        return toReturn;

    }

    private void cancelDownloadWebTask() {
        if (mDownloadWebTask != null && !mDownloadWebTask.isCancelled())
        {
            mDownloadWebTask.cancel(false);
            mDownloadWebTask = null;
        }
    }

    @Override
    public void onStationListFragmentInteraction(final Uri uri) {

        if (uri.getPath().equalsIgnoreCase("/" + StationTableFragment.Companion.getSTATION_LIST_ITEM_CLICK_PATH()))
        {
            if(!isLookingForBike() || mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                //if null, means the station was clicked twice, hence unchecked
                final BikeStation clickedStation = getTablePagerAdapter().getHighlightedStationForTable(mTabLayout.getSelectedTabPosition());

                if (isLookingForBike()) {

                    if (mStationMapFragment.getMarkerBVisibleLatLng() != null) {

                        LatLng newALatLng = clickedStation.getLocation();
                        getTablePagerAdapter().notifyStationAUpdate(newALatLng, mCurrentUserLatLng);

                        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                        mAutoSelectBikeFab.show();
                        animateCameraToShowUserAndStation(getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS));

                        hideSetupShowTripDetailsWidget();
                    } else{

                        animateCameraToShowUserAndStation(clickedStation);
                    }

                    mStationMapFragment.setPinOnStation(true, clickedStation.getLocationHash());
                    getTablePagerAdapter().setupBTabStationARecap(clickedStation, mDataOutdated);
                } else {

                    if (mStationMapFragment.isPickedFavoriteMarkerVisible()) {

                        if(clickedStation.getLocation().latitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude ||
                                clickedStation.getLocation().longitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude)
                        {
                            mStationMapFragment.pickedFavoriteMarkerInfoWindowShow();
                        }
                        else {
                            mStationMapFragment.pickedFavoriteMarkerInfoWindowHide();
                        }
                    }

                    setupBTabSelection(clickedStation.getLocationHash(), false);

                    FavoriteEntityStation newFavForStation = new FavoriteEntityStation(clickedStation.getLocationHash(),
                            clickedStation.getName(),
                            mNearbyActivityViewModel.getCurrentBikeSytemId().getValue());

                    boolean showFavoriteAddFab = false;

                    if(!mStationMapFragment.isPickedFavoriteMarkerVisible()){
                        if (mStationMapFragment.isPickedPlaceMarkerVisible())
                            showFavoriteAddFab = true;  //Don't setup the fab as it's been done in OnActivityResult
                        else if (setupAddFavoriteFab(newFavForStation))
                            showFavoriteAddFab = true;
                    }

                    if (showFavoriteAddFab)
                        mAddFavoriteFAB.show();
                    else
                        mAddFavoriteFAB.hide();
                }
            }
        } else if (uri.getPath().equalsIgnoreCase("/" + StationTableFragment.Companion.getSTATION_LIST_INACTIVE_ITEM_CLICK_PATH())) {

            mStationTableViewPager.setCurrentItem(StationTablePagerAdapter.DOCK_STATIONS, true);
            setupHintMainChoice();
        } else if (uri.getPath().equalsIgnoreCase("/" + StationTableFragment.Companion.getSTATION_LIST_FAVORITE_FAB_CLICK_PATH())) {

            BikeStation clickedStation = getTablePagerAdapter().getHighlightedStationForTable(mTabLayout.getSelectedTabPosition());

            if (null != clickedStation) {

                boolean newState = !mFavoriteListViewModel.isFavorite(clickedStation.getLocationHash());

                if (newState) {

                    if(mOnboardingShowcaseView != null){
                        mOnboardingShowcaseView.hide();
                        mOnboardingShowcaseView = null;
                    }

                    if (mStationMapFragment.getMarkerPickedPlaceVisibleName().isEmpty()) {
                        addFavorite(new FavoriteEntityStation(clickedStation.getLocationHash(), clickedStation.getName(), mNearbyActivityViewModel.getCurrentBikeSytemId().getValue()), false);
                    }
                    else {   //there's a third destination
                        FavoriteEntityStation toAdd = new FavoriteEntityStation(clickedStation.getLocationHash(), mStationMapFragment.getMarkerPickedPlaceVisibleName(), mNearbyActivityViewModel.getCurrentBikeSytemId().getValue());
                        toAdd.setCustomName(mStationMapFragment.getMarkerPickedPlaceVisibleName());
                        addFavorite(toAdd, false);
                    }
                    //TODO: how should that happen ?
                    //mFavoritesSheetFab.scrollToTop();
                    //should be provided by favorite list fragment ?

                } else {
                    removeFavorite(clickedStation.getLocationHash(), false);
                }
            }
        } else if (uri.getPath().equalsIgnoreCase("/" + StationTableFragment.Companion.getSTATION_LIST_DIRECTIONS_FAB_CLICK_PATH())) {
            //http://stackoverflow.com/questions/6205827/how-to-open-standard-google-map-application-from-my-application

            final BikeStation curSelectedStation = getTablePagerAdapter().getHighlightedStationForTable(mTabLayout.getSelectedTabPosition());

            // Seen NullPointerException in crash report.
            if (null != curSelectedStation) {

                LatLng tripLegOrigin = isLookingForBike() ? mCurrentUserLatLng : mStationMapFragment.getMarkerALatLng();
                LatLng tripLegDestination = curSelectedStation.getLocation();
                boolean walkMode = isLookingForBike();

                launchGoogleMapsForDirections(tripLegOrigin, tripLegDestination, walkMode);
            }
        }
    }

    private void launchGoogleMapsForDirections(LatLng _origin, LatLng _destination, boolean _walking) {
        StringBuilder builder = new StringBuilder("http://maps.google.com/maps?&saddr=");

        builder.append(_origin.latitude).
                append(",").
                append(_origin.longitude);

        builder.append("&daddr=").
            append(_destination.latitude).
            append(",").
            append(_destination.longitude).
            //append("B"). Labeling doesn't work :'(
            append("&dirflg=");

        if (_walking)
            builder.append("w");
        else
            builder.append("b");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(builder.toString()));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            startActivity(intent); // launch the map activity
        } else {
            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.google_maps_not_installed,
                    Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();
        }
    }

    private boolean isAppBarExpanded(){
        return mAppBarLayout.getHeight() - mAppBarLayout.getBottom() == 0;
    }

    private void animateCameraToShowUserAndStation(BikeStation station) {

        if (mCurrentUserLatLng != null) {
            if (mTripDetailsWidget.getVisibility() != View.VISIBLE) //Directions to A fab is visible
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_fab_padding), station.getLocation(), mCurrentUserLatLng, null);
            else    //Map id padded on the left and interface is clear on the right
                animateCameraToShow((int)getResources().getDimension(R.dimen.camera_ab_pin_padding), station.getLocation(), mCurrentUserLatLng, null);

        }
        else{
            mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getLocation(), 15));
        }
    }

    //TODO: refactor this method such as
    //-passing only one valid LatLng leads to a regular animateCamera
    //-passing identical LatLng leads to a regular animateCamera, maybe with client code provided zoom level or a default one
    private void animateCameraToShow(int _cameraPaddingPx, LatLng _latLng0, LatLng _latLng1, LatLng _latLng2){
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        boundsBuilder.include(_latLng0).include(_latLng1);

        if (_latLng2 != null)
            boundsBuilder.include(_latLng2);

        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), _cameraPaddingPx)); //Pin icon is 36 dp
    }

    //Callback from pull-to-refresh
    @Override
    public void onRefresh() {

        if (mDownloadWebTask == null){
            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();
        }

    }

    private StationTablePagerAdapter getTablePagerAdapter() {
        return (StationTablePagerAdapter) mStationTableViewPager.getAdapter();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(final int position) {

        BikeStation stationA = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);

        if (stationA != null) {
            if (!getTablePagerAdapter().setupBTabStationARecap(stationA, mDataOutdated))
                mClosestBikeAutoSelected = false;   //to handle rapid multiple screen orientation change
        }

        //Happens on screen orientation change
        if (mStationMapFragment == null ||
                (mStationMapFragment.getMarkerBVisibleLatLng() != null && getTablePagerAdapter().getHighlightedStationForTable(position) == null) ||
                !mStationMapFragment.isMapReady()){
            Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onPageSelected(position);

                }
            }, 1000); //second long delay gives a nice UX with camera animation
        }
        else {

            BikeStation highlightedStation = getTablePagerAdapter().getHighlightedStationForTable(position);

            //A TAB
            if (position == StationTablePagerAdapter.BIKE_STATIONS) {

                dismissOnboardingHint();

                mStationMapFragment.setScrollGesturesEnabled(false);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {
                    mStationMapFragment.setMapPaddingLeft(0);
                    hideTripDetailsWidget();
                    mDirectionsLocToAFab.show();
                }

                mAppBarLayout.setExpanded(false, true);
                getTablePagerAdapter().smoothScrollHighlightedInViewForTable(position, true);

                mSearchFAB.hide();
                mAddFavoriteFAB.hide();
                mNearbyActivityViewModel.hideFavoriteFab();
                mClearFAB.hide();
                mStationMapFragment.setMapPaddingRight(0);

                if (!isStationAClosestBike()){
                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                    mAutoSelectBikeFab.show();
                }

                //just to be on the safe side
                if (highlightedStation != null ) {

                    mStationMapFragment.setPinOnStation(true, highlightedStation.getLocationHash());


                    animateCameraToShowUserAndStation(highlightedStation);

                    //if mDataOutdated is true, a Download task will be launched if auto update is also true and a connection is available
                    //That's because autoupdate max interval is SMALLER than outdating one
                    if (!(mDataOutdated && DBHelper.getInstance().getAutoUpdate(this) && Utils.Connectivity.isConnected(this)))
                        mStationMapFragment.lookingForBikes(mDataOutdated, true);
                }
            } else { //B TAB

                mAutoSelectBikeFab.hide();
                mStationMapFragment.setMapPaddingRight(0);

                //TODO: Should I lock that for regular users ?
                mStationMapFragment.setScrollGesturesEnabled(true);

                mAppBarLayout.setExpanded(true, true);

                if (mStationMapFragment.getMarkerBVisibleLatLng() == null) {

                    //check if showcasing should happen, if not check if hint should happen
                    if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE))
                        checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);

                    mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerALatLng(), 13.75f));

                    if (mPlaceAutocompleteLoadingProgressBar.getVisibility() != View.GONE){
                        mSearchFAB.show();
                        mSearchFAB.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_gray));
                    }
                    else {
                        mDirectionsLocToAFab.hide();
                        mNearbyActivityViewModel.showFavoriteFab();
                        //mFavoritesSheetFab.showFab();
                        if (Utils.Connectivity.isConnected(NearbyActivity.this))
                            mSearchFAB.show();
                    }

                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));

                } else {

                    getTablePagerAdapter().smoothScrollHighlightedInViewForTable(position, false);
                    mStationMapFragment.setMapPaddingLeft((int) getResources().getDimension(R.dimen.trip_details_widget_width));

                    if (mTripDetailsWidget.getVisibility() == View.INVISIBLE) {
                        showTripDetailsWidget();
                    }

                    mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_fab_padding));
                    mClearFAB.show();

                    LatLng locationToShow = null;

                    if (mStationMapFragment.isPickedPlaceMarkerVisible()) {
                        locationToShow = mStationMapFragment.getMarkerPickedPlaceVisibleLatLng();
                        mAddFavoriteFAB.show();
                    }
                    else if(mStationMapFragment.isPickedFavoriteMarkerVisible() &&
                            (mStationMapFragment.getMarkerBVisibleLatLng().latitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().latitude ||
                            mStationMapFragment.getMarkerBVisibleLatLng().longitude != mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng().longitude)
                            )
                        locationToShow = mStationMapFragment.getMarkerPickedFavoriteVisibleLatLng();
                    else if(!mStationMapFragment.isPickedFavoriteMarkerVisible() && !mFavoriteListViewModel.isFavorite(highlightedStation.getLocationHash()))
                        mAddFavoriteFAB.show();

                    if (locationToShow != null) {
                        mStationMapFragment.setMapPaddingRight((int) getResources().getDimension(R.dimen.map_infowindow_padding));
                        animateCameraToShow((int) getResources().getDimension(R.dimen.camera_search_infowindow_padding),
                                mStationMapFragment.getMarkerBVisibleLatLng(),
                                locationToShow,
                                null);
                    }
                    else
                        mStationMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(mStationMapFragment.getMarkerBVisibleLatLng(), 15));
                }

                //Log.d("NearbyActivity", "onPageSelected - about to update markers with mDataOutdated : " + mDataOutdated, new Exception());
                //if mDataOutdated is true, a Download task will be launched if auto update is also true and a connection is available
                //That's because autoupdate max interval is SMALLER than outdating one
                if (!(mDataOutdated && DBHelper.getInstance().getAutoUpdate(this) && Utils.Connectivity.isConnected(this)))
                    mStationMapFragment.lookingForBikes(mDataOutdated, false);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

        //pager will always transition from tab A to tab B on app launch
        if (state == ViewPager.SCROLL_STATE_IDLE && mSplashScreen.isShown()) {

            //If update mode is set to manual and user are out of the bound of the bike network
            //let's check if there's a better bike network avaialble.
            if (!DBHelper.getInstance().getAutoUpdate(this) && Utils.Connectivity.isConnected(NearbyActivity.this))
            {
                if (mCurrentUserLatLng != null && !DBHelper.getInstance().getBikeNetworkBounds(NearbyActivity.this, 5).contains(mCurrentUserLatLng)) {
                    //This task will possibly auto cancel if it can't find a better bike network
                    mFindNetworkTask = new FindNetworkTask(DBHelper.getInstance().getBikeNetworkName(NearbyActivity.this));
                    mFindNetworkTask.execute();
                }
            }

            if (mFindNetworkTask == null)
                mSplashScreen.setVisibility(View.GONE);
        }
    }

    //Google API client
    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();

    }

    //Google API client
    @Override
    public void onConnectionSuspended(int i) {

    }

    //Google API client
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if (mCurrentUserLatLng != null &&
                mCurrentUserLatLng.latitude == location.getLatitude() &&
                mCurrentUserLatLng.longitude == location.getLongitude())
            return;

        mCurrentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        boolean highlightedStationAVisibleInRecyclerViewBefore =
                getTablePagerAdapter().isHighlightedVisibleInRecyclerView();

        getTablePagerAdapter().setCurrentUserLatLng(mCurrentUserLatLng);

        if (mStationMapFragment != null){
            mStationMapFragment.onUserLocationChange(location);
            if (mStationMapFragment.getMarkerBVisibleLatLng() != null && mTripDetailsWidget.getVisibility() == View.VISIBLE)
                setupTripDetailsWidget();
        }

        if (highlightedStationAVisibleInRecyclerViewBefore && !getTablePagerAdapter().isHighlightedVisibleInRecyclerView())
            getTablePagerAdapter().smoothScrollHighlightedInViewForTable(StationTablePagerAdapter.BIKE_STATIONS, true);

    }

    private boolean isStationAClosestBike(){

        String stationAId = mStationMapFragment.getMarkerAStationId();
        String closestBikeId = Utils.extractNearestAvailableStationIdFromDataString(getTablePagerAdapter().retrieveClosestRawIdAndAvailability(true));

        return stationAId.equalsIgnoreCase(closestBikeId);
    }

    /*@Override
    public void onFavoriteListItemClick(String _favoriteID) {

        BikeStation stationA = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);

        if (stationA.getLocationHash().equalsIgnoreCase(_favoriteID)) {

            Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.such_short_trip, Snackbar.LENGTH_SHORT, ContextCompat.getColor(this, R.color.theme_primary_dark))
                    .show();

        } else {
            mFavoritePicked = true;
            setupBTabSelectionClosestDock(_favoriteID);
        }
    }*/

    /*@Override
    public void onFavoriteListItemNameEditBegin() {
        mFavoritesSheetFab.hideEditFab();
        mFavoriteItemEditInProgress = true;
    }*/

    /*@Override
    public void onFavoristeListItemNameEditAbort() {
        mFavoritesSheetFab.showEditFab();
        mFavoriteItemEditInProgress = false;
    }*/

    /*@Override
    public void onFavoriteListItemDelete(String _favoriteId) {
        removeFavorite(DBHelper.getFavoriteEntityForId(_favoriteId), true);
    }*/

    /*@Override
    public void onFavoriteListItemNameEditDone(String _favoriteId, String _newName) {

        if (!_favoriteId.startsWith(FavoriteEntityPlace.PLACE_ID_PREFIX)) {
            DBHelper.addOrUpdateFavorite(true, getStation(_favoriteId).getFavoriteEntityForDisplayName(_newName));
            BikeStation closestBikeStation = getTablePagerAdapter().getHighlightedStationForTable(StationTablePagerAdapter.BIKE_STATIONS);
            getTablePagerAdapter().setupBTabStationARecap(closestBikeStation, mDataOutdated);
            getTablePagerAdapter().notifyStationChangedAll(_favoriteId);
        }
        else{
            FavoriteEntityBase favEntity = DBHelper.getFavoriteEntityForId(_favoriteId);
            CharSequence attr = favEntity.getAttributions();
            String attrString = "";
            if (attr != null)
                attrString = attr.toString();

            DBHelper.addOrUpdateFavorite(true, new FavoriteEntityPlace(favEntity.getId(), _newName, favEntity.getLocation(), attrString));
        }

        mFavoritesSheetFab.showEditFab();
        mFavoriteRecyclerViewAdapter.setupFavoriteList(DBHelper.getFavoriteAll());
        mFavoriteItemEditInProgress = false;
    }*/

    private class RedrawMarkersTask extends AsyncTask<Boolean, Void, Void> {

        /*public RedrawMarkersTask(){
            Log.d("NearbyActivity", "redraw markers construction, mDataOutdated : " + mDataOutdated, new Exception());
        }*/

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.refreshing_map));
            mSplashScreenTextBottom.setText(getString(R.string.refreshing_map));
            mStationMapFragment.hideAllStations();
        }

        @Override
        protected Void doInBackground(Boolean... bools) {

            //This improves the UX by giving time to the listview to render
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Log.d("NearbyActivity", "redraw markers doInBackground, outdated : " + bools[0], new Exception());


            mStationMapFragment.clearMarkerGfxData();
            //SETUP MARKERS DATA
            List<BikeStation> networkStationList = RootApplication.Companion.getBikeNetworkStationList();
            for (BikeStation item : networkStationList){
                mStationMapFragment.addMarkerForBikeStation(bools[0], item, bools[1]);
            }

            return null;
        }

        @Override
        protected void onCancelled (Void aVoid) {
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."

            mRefreshMarkers = true;

            mRedrawMarkersTask = null;
            mStationMapFragment.showAllStations();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mStationMapFragment.redrawMarkers();

            if (getTablePagerAdapter().isViewPagerReady()) {
                BikeStation highlighted = getTablePagerAdapter().getHighlightedStationForTable(mTabLayout.getSelectedTabPosition());

                if (null != highlighted)
                    mStationMapFragment.setPinOnStation(isLookingForBike(), highlighted.getLocationHash());
            }

            mRedrawMarkersTask = null;
            mStationMapFragment.showAllStations();
        }
    }

    public class FindNetworkTask extends AsyncTask<Void, Void, Map<String,String>> {

        private static final String NEW_YORK_HUDSON_BIKESHARE_ID = "hudsonbikeshare-hoboken" ;
        String mOldBikeNetworkName = "";

        FindNetworkTask(String _currentNetworkName){ mOldBikeNetworkName = _currentNetworkName; }

        private static final String ERROR_KEY_NO_BETTER = "NO_BETTER_NETWORK" ;
        private static final String ERROR_KEY_IOEXCEPTION = "IOEXCEPTION" ;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mStatusTextView.setText(getString(R.string.searching_wait_location));
            mSplashScreenTextBottom.setText(getString(R.string.searching_wait_location));

            if (getTablePagerAdapter().isViewPagerReady())
                getTablePagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled(Map<String,String> _result) {
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."

            getTablePagerAdapter().setRefreshingAll(false);

            //This was initial setup
            if (!DBHelper.getInstance().isBikeNetworkIdAvailable(NearbyActivity.this)){
                mSplashScreenTextTop.setText(getString(R.string.sad_emoji));
                mSplashScreenTextBottom.setText("");
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                tryInitialSetup();
                            }
                        }).show();
            }
            else if (_result.containsKey(ERROR_KEY_IOEXCEPTION)){   //HTTP session ran into troubles
                Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_download_failed,
                        Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.resume, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DBHelper.getInstance().resumeAutoUpdate();
                            }
                        }).show();
            }
            else{//_result.containsValue(ERROR_VALUE_NO_BETTER)

                if (mSplashScreen.isShown()){
                    mSplashScreen.setVisibility(View.GONE);
                }

                mClosestBikeAutoSelected = false;

                mDataOutdated = false;
                mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));

                getTablePagerAdapter().setOutdatedDataAll(false);

                mRefreshMarkers = true;
                mRefreshTabs = true;

                refreshMap();

                if (mSaveNetworkToDatabaseTask == null) {
                    //new SaveNetworkToDatabaseTask().execute();
                    //Saving to database executes in parallel. Maybe a service should be used in place
                    mSaveNetworkToDatabaseTask = new SaveNetworkToDatabaseTask();
                    mSaveNetworkToDatabaseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            mFindNetworkTask = null;
        }

        @Override
        protected Map<String,String> doInBackground(Void... voids) {

            //noinspection StatementWithEmptyBody
            while (!getTablePagerAdapter().isViewPagerReady()) {
                //Waiting on viewpager init
            }

            publishProgress();

            //This mambo jambo because a NullPointerException on mCurrentUserLatLng been seen on Galaxy Nexus
            ///////////////////////
            LatLng userLoc;
            final LatLng finalUserLoc;

            while (true)
            {
                userLoc = mCurrentUserLatLng;
                //Waiting on location
                if (userLoc != null){
                    finalUserLoc = userLoc;
                    break;
                }
            }
            ////////////////////////

            publishProgress();

            Map<String,String> toReturn = new HashMap<>();

            Citybik_esAPI api = ((RootApplication) getApplication()).getCitybik_esApi();

            final Call<BikeNetworkListAnswerRoot> call = api.getBikeNetworkList();

            Response<BikeNetworkListAnswerRoot> listAnswer;

            try {
                listAnswer = call.execute();

                ArrayList<BikeNetworkDesc> answerList = listAnswer.body().networks;

                Collections.sort(answerList, new Comparator<BikeNetworkDesc>() {
                    @Override
                    public int compare(BikeNetworkDesc bikeNetworkDesc, BikeNetworkDesc t1) {

                        //NullPointerException on mCurrentUserLatLng been seen on Galaxy Nexus
                        return (int)(SphericalUtil.computeDistanceBetween(finalUserLoc, bikeNetworkDesc.location.getAsLatLng()) -
                                SphericalUtil.computeDistanceBetween(finalUserLoc, t1.location.getAsLatLng()));
                    }
                });

                BikeNetworkDesc closestNetwork = answerList.get(0);

                if (closestNetwork.id.equalsIgnoreCase(NEW_YORK_HUDSON_BIKESHARE_ID)){
                    closestNetwork = answerList.get(1);
                }

                //It seems we don't have a better candidate than the one we're presently using
                if (closestNetwork.id.equalsIgnoreCase(DBHelper.getInstance().getBikeNetworkId(NearbyActivity.this))){
                    toReturn.put(ERROR_KEY_NO_BETTER, "dummy");
                    cancel(false);
                }
                else{

                    if (DBHelper.getInstance().isBikeNetworkIdAvailable(NearbyActivity.this)){
                        toReturn.put("old_network_name", DBHelper.getInstance().getBikeNetworkName(NearbyActivity.this));
                    }

                    toReturn.put("new_network_city", closestNetwork.location.getCity());

                    BikeStationRepository.getInstance().setAll(null);
                    mNearbyActivityViewModel.postCurrentBikeSytemId(closestNetwork.id);
                    DBHelper.getInstance().saveBikeNetworkDesc(closestNetwork, NearbyActivity.this);
                }

            } catch (IOException e) {

                DBHelper.getInstance().pauseAutoUpdate();
                toReturn.put(ERROR_KEY_IOEXCEPTION, "dummy");

                cancel(false); //No need to try to interrupt the thread
            }

            return toReturn;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            if (getTablePagerAdapter().isViewPagerReady())
                getTablePagerAdapter().setRefreshingAll(true);

            if (mCurrentUserLatLng != null) {
                mStatusTextView.setText(getString(R.string.searching_bike_network));
                mSplashScreenTextBottom.setText(getString(R.string.searching_bike_network));
            }

        }

        @Override
        protected void onPostExecute(Map<String,String> backgroundResults) {
            super.onPostExecute(backgroundResults);

            //We get here only if the network was actually changed
            if(mStationMapFragment != null && mStationMapFragment.getMarkerBVisibleLatLng() != null) {
                clearBTab();
            }

            mClosestBikeAutoSelected = false;

            //noinspection ConstantConditions
            setupActionBarStrings();
            setupFavoriteSheet();

            if (mSplashScreen.isShown()){
                mSplashScreen.setVisibility(View.GONE);
            }

            AlertDialog alertDialog = new AlertDialog.Builder(NearbyActivity.this).create();
            //alertDialog.setTitle(getString(R.string.network_found_title));
            if (!backgroundResults.keySet().contains("old_network_name")) {
                alertDialog.setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.hello_city), "", backgroundResults.get("new_network_city") )));
                alertDialog.setMessage(Utils.fromHtml(String.format(getString(R.string.bike_network_found_message),
                        DBHelper.getInstance().getBikeNetworkName(NearbyActivity.this) )));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);
            }
            else{
                alertDialog.setTitle(Utils.fromHtml(String.format(getResources().getString(R.string.hello_city), getResources().getString(R.string.hello_travel), backgroundResults.get("new_network_city"))));
                alertDialog.setMessage(Utils.fromHtml(String.format(getString(R.string.bike_network_change_message),
                        DBHelper.getInstance().getBikeNetworkName(NearbyActivity.this), mOldBikeNetworkName)));
                Message toPass = null; //To resolve ambiguous call
                //noinspection ConstantConditions
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.ok), toPass);
                mStationMapFragment.doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(mCurrentUserLatLng, 15), true);
            }

            alertDialog.show();

            if(!checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_FULL, eONBOARDING_STEP.ONBOARDING_STEP_SEARCH_SHOWCASE)){
                checkOnboarding(eONBOARDING_LEVEL.ONBOARDING_LEVEL_LIGHT, eONBOARDING_STEP.ONBOARDING_STEP_MAIN_CHOICE_HINT);
            }

            mDownloadWebTask = new DownloadWebTask();
            mDownloadWebTask.execute();

            mFindNetworkTask = null;
        }
    }

    //TODO: NOT use an asynchtask for this long running database operation
    private class SaveNetworkToDatabaseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            DBHelper.getInstance().notifyBeginSavingStations(NearbyActivity.this);
        }


        @Override
        protected Void doInBackground(Void... params) {

            BikeStationRepository.getInstance().setAll(RootApplication.Companion.getBikeNetworkStationList());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            DBHelper.getInstance().notifyEndSavingStations(NearbyActivity.this);

            //must be done last
            mSaveNetworkToDatabaseTask = null;
        }
    }

    public class UpdateTwitterStatusTask extends AsyncTask<String, Void, Void>{

        private static final int REPLY_STATION_NAME_MAX_LENGTH = 54;
        private static final int STATION_ID_LENGTH = 32;

        @Override
        protected Void doInBackground(String... params) {

            List<BikeStation> networkStationList = RootApplication.Companion.getBikeNetworkStationList();

            Map<String, BikeStation> networkStationMap = new HashMap<>(networkStationList.size());

            for (BikeStation station : networkStationList){
                networkStationMap.put(station.getLocationHash(), station);
            }

            Twitter api = ((RootApplication) getApplication()).getTwitterApi();


            //Extract all stations from raw string
            //if only one station, call updateStatus with intended one and selected one + deduplication
            //if multiple stations, post selected one first and then all other in replies
            //////////////////////////////////////////////////////////////////////////////////////
            //FORMAT -- ROOT STATUS
            /*#findmybixibikes bike is not closest! Bikes:X BAD at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestation deduplicateZ
              #findmybixibikes bike is not closest! Bikes:XX AOK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID ~XXmin walk stationnamestationnamestatio deduplicateZ

              -- REPLIES
              #findmybixibikes discarded closer! Bikes:Y CRI at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam
              #findmybixibikes discarded closer! Bikes:Y LCK at IDIDIDIDIDIDIDIDIDIDIDIDIDIDIDID stationnamestationnamestationnamestationnamestationnam

              */
            String systemHashtag = getResources().getString(R.string.appbar_title_prefix) +
                    DBHelper.getInstance().getHashtaggableNetworkName(NearbyActivity.this) + //must be hastagable
                    getResources().getString(R.string.appbar_title_postfix);
            int selectedNbBikes = -1;
            String selectedBadorAok = "BAD";    //hashtagged
            String selectedStationId = "";
            String selectedProximityString = "XXmin";
            String selectedStationName = "Laurier / De Lanaudière";
            String deduplicate = "deduplicate";    //hashtagged

            //Pair of station id and availability code (always 'CRI' as of now)
            List<Pair<String,String>> discardedStations = new ArrayList<>();

            BikeStation selectedStation = null;
            List<String> extracted = Utils.extractOrderedStationIdsFromProcessedString(params[0]);
            //extracted will contain as firt element
            //   359f354466083c962d243bc238c95245_AVAILABILITY_AOK
            //OR 359f354466083c962d243bc238c95245_AVAILABILITY_BAD
            //followed by 1 or more string in the form of
            //   3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI

            boolean firstString = true;

            for (String e : extracted)
            {
                if (firstString){
                    //359f354466083c962d243bc238c95245_AVAILABILITY_BAD or
                    //359f354466083c962d243bc238c95245_AVAILABILITY_AOK

                    selectedStationId = e.substring(0,STATION_ID_LENGTH);
                    selectedBadorAok = e.substring(STATION_ID_LENGTH + StationTableRecyclerViewAdapter.Companion.getAVAILABILITY_POSTFIX_START_SEQUENCE().length(),
                            STATION_ID_LENGTH + StationTableRecyclerViewAdapter.Companion.getAVAILABILITY_POSTFIX_START_SEQUENCE().length() + 3); //'BAD' or 'AOK'

                    selectedStation = networkStationMap.get(selectedStationId);

                    selectedNbBikes = selectedStation.getFreeBikes();

                    selectedProximityString = Utils.getWalkingProximityString(selectedStation.getLocation(),
                            mCurrentUserLatLng, false, null, NearbyActivity.this);

                    //station name will be truncated to fit everything in a single tweet
                    //see R.string.twitter_not_closest_bike_data_format
                    int maxStationNameIdx = 138 - (deduplicate.length()+" ".length()
                            + " walk ".length()
                            + selectedProximityString.length()
                            + " ".length()
                            + STATION_ID_LENGTH
                            + " at ".length()
                            + selectedBadorAok.length()
                            + " #".length()
                            + Integer.toString(selectedNbBikes).length()
                            + " bike is not closest! Bikes:".length()
                            + systemHashtag.length());

                    selectedStationName = selectedStation.getName().substring(0, Math.min(selectedStation.getName().length(), maxStationNameIdx));

                    firstString = false;
                }
                else { //3c3bf5e74cb938e7d57641edaf909d24_AVAILABILITY_CRI

                    Pair<String, String> discarded = new Pair<>(e.substring(0,STATION_ID_LENGTH), e.substring(
                            STATION_ID_LENGTH + StationTableRecyclerViewAdapter.Companion.getAVAILABILITY_POSTFIX_START_SEQUENCE().length(),
                            STATION_ID_LENGTH + StationTableRecyclerViewAdapter.Companion.getAVAILABILITY_POSTFIX_START_SEQUENCE().length() + 3
                    ));

                    discardedStations.add(discarded);
                }

            }

            int deduplicateCounter = 0;

            deduplicate = deduplicate + deduplicateCounter;

            String newStatusString = String.format(getResources().getString(R.string.twitter_not_closest_bike_data_format),
                    systemHashtag, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate);

            StatusUpdate newStatus = new StatusUpdate(newStatusString);
            //noinspection ConstantConditions
            newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getLocation().latitude, selectedStation.getLocation().longitude));

            boolean deduplicationDone = false;


            while (!deduplicationDone){

                //post status before adding replies
                try {
                    //can be interrupted here (duplicate)
                    twitter4j.Status answerStatus = api.updateStatus(newStatus);

                    long replyToId = answerStatus.getId();

                    for (Pair<String, String> discarded : discardedStations ){
                        BikeStation discardedStationItem = networkStationMap.get(discarded.first);

                        String replyStatusString = String.format(getResources().getString(R.string.twitter_closer_discarded_reply_data_format),
                                systemHashtag, discardedStationItem.getFreeBikes(), discarded.second, discarded.first,
                                discardedStationItem.getName().substring(0, Math.min(discardedStationItem.getName().length(), REPLY_STATION_NAME_MAX_LENGTH)));

                        StatusUpdate replyStatus = new StatusUpdate(replyStatusString);

                        replyStatus.inReplyToStatusId(replyToId)
                                .displayCoordinates(true)
                                .location(new GeoLocation(discardedStationItem.getLocation().latitude, discardedStationItem.getLocation().longitude));

                        //that can also raise exception
                        api.updateStatus(replyStatus);

                    }

                    deduplicationDone = true;


                } catch (TwitterException e) {
                    String errorMessage = e.getErrorMessage();
                    if (errorMessage.contains("Status is a duplicate.")){
                        ++deduplicateCounter;

                        deduplicate = "deduplicate" + deduplicateCounter;

                        newStatusString = String.format(getResources().getString(R.string.twitter_not_closest_bike_data_format),
                                systemHashtag, selectedNbBikes, selectedBadorAok, selectedStationId, selectedProximityString, selectedStationName, deduplicate);

                        newStatus = new StatusUpdate(newStatusString);
                        //noinspection ConstantConditions
                        newStatus.displayCoordinates(true).location(new GeoLocation(selectedStation.getLocation().latitude, selectedStation.getLocation().longitude));

                        Log.d("TwitterUpdate", "TwitterUpdate duplication -- deduplicating now", e);

                    } else {
                        deduplicationDone = true;
                    }
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mUpdateTwitterTask = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            mUpdateTwitterTask = null;
        }
    }

    public class DownloadWebTask extends AsyncTask<Void, Void, Void> {

        private LatLngBounds mDownloadedBikeNetworkBounds;

        /*DownloadWebTask(){
            Log.d("nearbyActivity", "spawning new Download task", new Exception());
        }*/

        @Override
        protected Void doInBackground(Void... aVoid) {

            //noinspection StatementWithEmptyBody
            while (!getTablePagerAdapter().isViewPagerReady()) {
                //Waiting on viewpager init
            }

            publishProgress();

            Map<String, String> UrlParams = new HashMap<>();
            UrlParams.put("fields", "stations");

            Citybik_esAPI api = ((RootApplication) getApplication()).getCitybik_esApi();

            final Call<BikeSystemStatusAnswerRoot> call = api.getBikeNetworkStatus(DBHelper.getInstance().getBikeNetworkHRef(NearbyActivity.this), UrlParams);

            Response<BikeSystemStatusAnswerRoot> statusAnswer;

            try {
                statusAnswer = call.execute();

                List<BikeStation> newBikeNetworkStationList = RootApplication.Companion.addAllToBikeNetworkStationList(statusAnswer.body().network.getBikeStationList());

                //Calculate bounds
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                for (BikeStation station : newBikeNetworkStationList){
                    boundsBuilder.include(station.getLocation());
                }

                mDownloadedBikeNetworkBounds = boundsBuilder.build();

            } catch (IOException e) {

                cancel(false); //No need to try to interrupt the thread
            }

            //Log.d("NearbyActivity", "DownloadTaskBackground - start waiting");
            while (true){   //Must hang in the background if data saving is in progress already
                if (mSaveNetworkToDatabaseTask == null)
                    break;
                try {
                    //This has to happen otherwise release code seems to loop infinitely
                    Thread.sleep(250);  //Empirically determined

                } catch (InterruptedException e) {
                    Log.d("NearbyActivity", "Exception while trying to sleep", e);
                }
                //Log.d("NearbyActivity", "DownloadTaskBackground - waiting");
            }
            //Log.d("NearbyActivity", "DownloadTaskBackground - stopped waiting");

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            getTablePagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStatusTextView.setText(getString(R.string.downloading));
            mSplashScreenTextBottom.setText(getString(R.string.downloading));

            DBHelper.getInstance().resumeAutoUpdate();

            if (getTablePagerAdapter().isViewPagerReady())
                getTablePagerAdapter().setRefreshingAll(true);
        }

        @Override
        protected void onCancelled (Void aVoid){
            //super.onCancelled(aVoid);
            //https://developer.android.com/reference/android/os/AsyncTask.html#onCancelled(Result)
            //" If you write your own implementation, do not call super.onCancelled(result)."
            //Set interface back
            getTablePagerAdapter().setRefreshingAll(false);

            //wasn't initial download
            if(!RootApplication.Companion.getBikeNetworkStationList().isEmpty()) {

                if (mDataOutdated){
                    mRefreshMarkers = true;
                    refreshMap();
                    mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_accent));
                    getTablePagerAdapter().setOutdatedDataAll(true);
                }

                DBHelper.getInstance().pauseAutoUpdate(); //Must be done in all cases : getAutoUpdate factorizes in the suspend flag

                if (DBHelper.getInstance().getAutoUpdate(NearbyActivity.this)) {
                    Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.auto_download_failed,
                            Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.resume, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    DBHelper.getInstance().resumeAutoUpdate();
                                }
                            }).show();
                } else {
                    Utils.Snackbar.makeStyled(mCoordinatorLayout, R.string.manual_download_failed,
                            Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                            .setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mDownloadWebTask = new DownloadWebTask();
                                    mDownloadWebTask.execute();
                                }
                            }).show();
                }
            }
            else {

                mSplashScreenTextTop.setText(getString(R.string.sad_emoji));
                mSplashScreenTextBottom.setText("");
                Utils.Snackbar.makeStyled(mSplashScreen, R.string.connectivity_rationale, Snackbar.LENGTH_INDEFINITE, ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark))
                        .setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                tryInitialSetup();
                            }
                        }).show();
            }

            //must be done last
            mDownloadWebTask = null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //switch progressbar view visibility

            getTablePagerAdapter().setRefreshingAll(false);

            DBHelper.getInstance().saveLastUpdateTimestampAsNow(getApplicationContext());
            DBHelper.getInstance().saveBikeNetworkBounds(mDownloadedBikeNetworkBounds, NearbyActivity.this);

            Log.d("nearbyActivity", RootApplication.Companion.getBikeNetworkStationList().size() + " bikeStationList downloaded from citibik.es");

            //users are inside bounds
            if (mCurrentUserLatLng == null || DBHelper.getInstance().getBikeNetworkBounds(NearbyActivity.this, 5).contains(mCurrentUserLatLng) ){

                mClosestBikeAutoSelected = false;

                mDataOutdated = false;
                mStatusBar.setBackgroundColor(ContextCompat.getColor(NearbyActivity.this, R.color.theme_primary_dark));

                getTablePagerAdapter().setOutdatedDataAll(false);

                mRefreshMarkers = true;
                mRefreshTabs = true;

                refreshMap();

                //new SaveNetworkToDatabaseTask().execute();
                //Saving to database executes in parallel. Maybe a service should be used in place
                mSaveNetworkToDatabaseTask =  new SaveNetworkToDatabaseTask();
                mSaveNetworkToDatabaseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
            else if(mCurrentUserLatLng != null){    //users are outside bounds

                //This task will possibly auto cancel if it can't find a better bike network
                mFindNetworkTask = new FindNetworkTask(DBHelper.getInstance().getBikeNetworkName(NearbyActivity.this));
                mFindNetworkTask.execute();
            }

            //must be done last
            mDownloadWebTask = null;
        }
    }
}
