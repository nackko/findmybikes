package com.ludoscity.findmybikes.ui.map

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.ui.main.NearbyActivityViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils
import java.util.*

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * Fragment class to display a map with colored overlays scaling with zoom level.
 * Also carries invisible map marker that can be tapped.
 */
class StationMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        /*GoogleMap.OnCameraChangeListener,*/
        GoogleMap.OnInfoWindowClickListener/*,
        GoogleMap.OnMapClickListener*/ {

    //models. TODO:Retrieve at construction time ?
    //private val favoriteListViewModel: FavoriteListViewModel
    private lateinit var mapFragmentModel: MapFragmentViewModel

    private var mInitialCameraSetupDone: Boolean = false

    private var mGoogleMap: GoogleMap? = null
    private var mAnimCallback: CustomCancellableCallback? = null

    //TODO: this comes from model and is observed
    private val mMapMarkersGfxData = ArrayList<StationMapGfx>()

    private var gfxData: List<StationMapGfx> = emptyList()

    private var mListener: OnStationMapFragmentInteractionListener? = null

    private var mMarkerPickedPlace: Marker? = null
    private var mPinSearchIconBitmapDescriptor: BitmapDescriptor? = null
    private var mMarkerPickedFavorite: Marker? = null
    private var mPinFavoriteIconBitmapDescriptor: BitmapDescriptor? = null
    private var mNoPinFavoriteIconBitmapDescriptor: BitmapDescriptor? = null

    private var mAttributionsText: TextView? = null

    private val MONTREAL_LATLNG = LatLng(45.5087, -73.554)

    private lateinit var pinAIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinBIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinAMarker: Marker
    private var pinBMarker: Marker? = null

    //Pin markers can only be restored after mGoogleMap is ready
    private var mBufferedBundle: Bundle? = null

    val markerALatLng: LatLng?
        get() {
            var toReturn: LatLng? = null

            /*if (mMarkerStationA != null)
                toReturn = mMarkerStationA!!.position
            else if (mBufferedBundle != null)
                toReturn = mBufferedBundle!!.getParcelable("pin_A_latlng")*/

            return toReturn
        }

    val markerBVisibleLatLng: LatLng?
        get() {
            var toReturn: LatLng? = null
            /*if (mMarkerStationB != null) {
                if (mMarkerStationB!!.isVisible)
                    toReturn = mMarkerStationB!!.position
            } else if (mBufferedBundle != null && mBufferedBundle!!.getBoolean("pin_B_visibility"))
                toReturn = mBufferedBundle!!.getParcelable("pin_B_latlng")*/

            return toReturn
        }

    //TODO: refactor so that the map fragment is a simple dumb view
    //Model and controller could be NearbyActivity
    //in any case, try to remove checks for visibility as a basis fro branching in NearbyActivity
    val isPickedPlaceMarkerVisible: Boolean
        get() = mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible

    //TODO: refactor so that the map fragment is a simple dumb view
    //Model and controller could be NearbyActivity
    //in any case, try to remove checks for visibility as a basis fro branching in NearbyActivity
    //investigate if that should extend to grabbing LatLng and Name
    val isPickedFavoriteMarkerVisible: Boolean
        get() = mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible

    //LatLng
    val markerPickedPlaceVisibleLatLng: LatLng?
        get() {
            var toReturn: LatLng? = null
            if (mMarkerPickedPlace != null) {
                if (mMarkerPickedPlace!!.isVisible)
                    toReturn = mMarkerPickedPlace!!.position
            } else if (mBufferedBundle != null && mBufferedBundle!!.getBoolean("pin_picked_place_visibility"))
                toReturn = mBufferedBundle!!.getParcelable("pin_picked_place_latlng")

            return toReturn
        }
    //Name
    val markerPickedPlaceVisibleName: String?
        get() {
            var toReturn: String? = ""
            if (mMarkerPickedPlace != null) {
                if (mMarkerPickedPlace!!.isVisible)
                    toReturn = mMarkerPickedPlace!!.title
            } else if (mBufferedBundle != null && mBufferedBundle!!.getBoolean("pin_picked_place_visibility"))
                toReturn = mBufferedBundle!!.getString("pin_picked_place_name")

            return toReturn
        }

    //LatLng
    val markerPickedFavoriteVisibleLatLng: LatLng?
        get() {
            var toReturn: LatLng? = null
            if (mMarkerPickedFavorite != null) {
                if (mMarkerPickedFavorite!!.isVisible)
                    toReturn = mMarkerPickedFavorite!!.position
            } else if (mBufferedBundle != null && mBufferedBundle!!.getBoolean("pin_picked_favorite_visibility"))
                toReturn = mBufferedBundle!!.getParcelable("pin_picked_favorite_latlng")

            return toReturn
        }

    val cameraLatLngBounds: LatLngBounds
        get() = mGoogleMap!!.projection.visibleRegion.latLngBounds

    val isRestoring: Boolean
        get() = mBufferedBundle != null

    val isMapReady: Boolean
        get() = mGoogleMap != null

    val cameraPosition: CameraPosition?
        get() {
            var toReturn: CameraPosition? = null
            if (isMapReady)
                toReturn = mGoogleMap!!.cameraPosition

            return toReturn
        }

    //Used to buffer markers update requests (avoids glitchy anim)
    private inner class CustomCancellableCallback : GoogleMap.CancelableCallback {

        override fun onFinish() {

            gfxData.forEach {
                it.updateMarker(mapFragmentModel.isDataOutOfDate.value == true,
                        mapFragmentModel.isLookingForBike.value == true, context!!)
            }

            mapFragmentModel.showMapItems()

            mAnimCallback = null

        }

        override fun onCancel() {

            gfxData.forEach {
                it.updateMarker(mapFragmentModel.isDataOutOfDate.value == true,
                        mapFragmentModel.isLookingForBike.value == true, context!!)
            }

            mapFragmentModel.showMapItems()

            mAnimCallback = null

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_station_map, container, false)

        if (mGoogleMap == null)
            (activity!!.fragmentManager.findFragmentById(R.id.mapNearby) as MapFragment).getMapAsync(this)

        mBufferedBundle = savedInstanceState

        mPinSearchIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_search_24dp_black)
        mPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_favorite_24dp_black)
        mNoPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_nopin_favorite_24dp_white)

        mAttributionsText = inflatedView.findViewById<View>(R.id.attributions_text) as TextView

        pinAIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_a_36dp_black)
        pinBIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_b_36dp_black)

        return inflatedView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val f = activity!!.fragmentManager
                .findFragmentById(R.id.mapNearby)
        if (f != null) {
            activity!!.fragmentManager.beginTransaction().remove(f).commit()
            mGoogleMap = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putBoolean("pin_picked_place_visibility", mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible)
        outState.putParcelable("pin_picked_place_latlng", if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG)
        outState.putString("picked_place_name", if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else "")

        outState.putBoolean("pin_picked_favorite_visibility", mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible)
        outState.putParcelable("pin_picked_favorite_latlng", if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG)
        outState.putString("picked_favorite_name", if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else "")
        super.onSaveInstanceState(outState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        try {
            mListener = activity as OnStationMapFragmentInteractionListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /*@Override
    public void onCameraChange(CameraPosition cameraPosition) {
        //Log.d("CameraZoomLevel", Float.toString(cameraPosition.zoom));

    }*/

    override fun onInfoWindowClick(marker: Marker) {

        val builder = Uri.Builder()
        builder.appendPath(INFOWINDOW_CLICK_PATH)

        marker.hideInfoWindow()

        builder.appendQueryParameter(INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM, marker.position.latitude.toString())
        builder.appendQueryParameter(INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM, marker.position.longitude.toString())

        if (mListener != null) {
            mListener!!.onStationMapFragmentInteraction(builder.build())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mInitialCameraSetupDone = false
        mGoogleMap = googleMap
        enableMyLocationCheckingPermission()
        mGoogleMap!!.setOnMarkerClickListener(this)
        mGoogleMap!!.setOnInfoWindowClickListener(this)
        //mGoogleMap.setOnCameraChangeListener(this);
        //mGoogleMap.setOnMapClickListener(this);
        //héhéhé, feel the power of design !!
        //mGoogleMap!!.uiSettings.isZoomGesturesEnabled = false
        mGoogleMap!!.uiSettings.isRotateGesturesEnabled = false
        mGoogleMap!!.uiSettings.isIndoorLevelPickerEnabled = false
        mGoogleMap!!.uiSettings.isTiltGesturesEnabled = false

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

        val activityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)


        val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application,
                activityModel.isLookingForBike,
                activityModel.isDataOutOfDate,
                activityModel.userLocation,
                activityModel.getStationA(),
                activityModel.getStationB(),
                MutableLiveData<LatLng>())

        mapFragmentModel = ViewModelProviders.of(this, modelFactory).get(MapFragmentViewModel::class.java)

        mapFragmentModel.mapPaddingLeftPx.observe(this, Observer {
            mGoogleMap!!.setPadding(it ?: 0, 0, mapFragmentModel.mapPaddingRightPx.value ?: 0, 0)
        })

        mapFragmentModel.mapPaddingRightPx.observe(this, Observer {
            mGoogleMap!!.setPadding(mapFragmentModel.mapPaddingLeftPx.value ?: 0, 0, it ?: 0, 0)
        })

        mapFragmentModel.showMapItems.observe(this, Observer {
            Log.d(TAG, "map item new visibility : $it")
            if (it == true) {
                gfxData.forEach { mapGfx ->
                    mapGfx.show(mGoogleMap!!.cameraPosition.zoom)
                }
            } else {
                //TODO: tab switching performance
                //Commenting this, map marker overlyas still disappear, because the
                //mGoogleMapp!!.clear() call and then re-adding all markers happens during the
                //camera animation, leading to jerky behavior.
                //See how to make these events happen in the right timing
                //-map clear (can it happen in the background ?)
                //-overlays visibility switch (very fast, that is what this hide call does)
                //-map markers and their overlays re-adding (addMarker calss)
                gfxData.forEach { mapGfx ->
                    mapGfx.hide()
                }
            }
        })
        mapFragmentModel.mapGfxLiveData.observe(this, Observer { newGfxData ->


            //TODO: should local gfxData list be cleared ?
            gfxData = newGfxData ?: emptyList()


            //redrawMarkers(gfxData)

            mGoogleMap!!.clear()

            pinAMarker = mGoogleMap!!.addMarker(
                    MarkerOptions().position(
                            LatLng(activityModel.getStationA().value?.latitude ?: 0.0,
                                    activityModel.getStationA().value?.longitude ?: 0.0))
                            .icon(pinAIconBitmapDescriptor)
                            .visible(activityModel.getStationA().value != null)
                            .title(activityModel.getStationA().value?.locationHash))

            pinBMarker = mGoogleMap!!.addMarker(
                    MarkerOptions().position(
                            LatLng(activityModel.getStationB().value?.latitude ?: 0.0,
                                    activityModel.getStationB().value?.longitude ?: 0.0))
                            .icon(pinBIconBitmapDescriptor)
                            .visible(activityModel.getStationB().value != null)
                            .title(activityModel.getStationB().value?.locationHash))
            /*mMarkerPickedPlace = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedPlaceLatLng!!)
                    .icon(mPinSearchIconBitmapDescriptor)
                    .visible(pinPickedPlaceVisible)
                    .title(pickedPlaceName))
            mMarkerPickedFavorite = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedFavoriteLatLng!!)
                    .icon(mPinFavoriteIconBitmapDescriptor)
                    .visible(pinPickedFavoriteVisible)
                    .zIndex(.5f)//so that it's on top of B pin (default Z is 0)
                    .title(pickedFavoriteName))

            if (pinPickedPlaceVisible)
                mMarkerPickedPlace!!.showInfoWindow()

            if (pinPickedFavoriteVisible && (mMarkerPickedFavorite!!.position.latitude != pinBLatLng.latitude || mMarkerPickedFavorite!!.position.longitude != pinBLatLng.longitude))
                mMarkerPickedFavorite!!.showInfoWindow()*/

            gfxData.forEach {
                it.addMarkerToMap(mGoogleMap!!)
            }

            Log.d(TAG, "Markers redrawned, size :" + gfxData.size)

            if (mAnimCallback == null) {
                mapFragmentModel.showMapItems()
            }
        })

        mapFragmentModel.cameraAnimationTarget.observe(this, Observer {
            if (it != null) {
                mAnimCallback = CustomCancellableCallback()
                mGoogleMap!!.animateCamera(it, resources.getInteger(R.integer.camera_animation_duration), mAnimCallback)
            }
        })

        activityModel.getStationA().observe(this, Observer {
            if (it != null) {
                //TODO: find a way to animate that
                pinAMarker.position = LatLng(it.latitude, it.longitude)
                pinAMarker.isVisible = true
            } else
                pinAMarker.isVisible = false
        })

        activityModel.getStationB().observe(this, Observer {
            if (it != null) {
                pinBMarker?.position = LatLng(it.latitude, it.longitude)
                pinBMarker?.isVisible = true
            } else {
                pinBMarker?.isVisible = false
            }
        })


        //That is not map related and may be done earlier
        mapFragmentModel.lastClickedWhileLookingForBike.observe(this, Observer {
            activityModel.setStationA(it)
        })
        mapFragmentModel.lastClickedWhileLookingForDock.observe(this, Observer {
            activityModel.setStationB(it)
        })
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        //TODO: in model
        if (isPickedFavoriteMarkerVisible)
            mMarkerPickedFavorite!!.showInfoWindow()

        //TODO: in model
        if (isPickedPlaceMarkerVisible)
            mMarkerPickedPlace!!.showInfoWindow()

        if (marker.title.equals(pinAMarker.title, ignoreCase = true) ||
                pinAMarker.isVisible &&
                pinAMarker.position.latitude == marker.position.latitude &&
                pinAMarker.position.longitude == marker.position.longitude ||
                pinBMarker!!.position.latitude == marker.position.latitude &&
                pinBMarker!!.position.longitude == marker.position.longitude || false
        /*mMarkerPickedPlace!!.isVisible && //except if picked destination is favorite

        mMarkerPickedPlace!!.position.latitude == marker.position.latitude &&
        mMarkerPickedPlace!!.position.longitude == marker.position.longitude*/)
            return true

        /*if (mMarkerPickedFavorite!!.isVisible &&
                //TODO: the following seems a bit patterny, checking if B pin and Favorite one are not on the same station
                //check connection with NearbyActivity::setupBTabSelection refactor ideas
                mMarkerPickedFavorite!!.position.latitude == marker.position.latitude &&
                mMarkerPickedFavorite!!.position.longitude == marker.position.longitude) {
            mMarkerPickedFavorite!!.hideInfoWindow()
        }*/

        val builder = Uri.Builder()
        builder.appendPath(MARKER_CLICK_PATH)

        builder.appendQueryParameter(MARKER_CLICK_TITLE_PARAM, marker.title)

        //TODO: this is the beginning of going full model
        mapFragmentModel.setLastClickedStationById(marker.title)
        //if (mListener != null) {
        //    mListener!!.onStationMapFragmentInteraction(builder.build())
        //}

        //So that info window will not be showed
        return true
    }

    fun onUserLocationChange(location: Location?) {
        /*if (location != null) {
            //Log.d("onMyLocationChange", "new location " + location.toString());
            if (!mInitialCameraSetupDone && mGoogleMap != null) {
                doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f), false)
            }
        }*/
    }

    fun enableMyLocationCheckingPermission() {
        if (ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap!!.isMyLocationEnabled = true
            mGoogleMap!!.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    /*fun doInitialCameraSetup(cameraUpdate: CameraUpdate, animate: Boolean) {
        if (animate)
            //animateCamera(cameraUpdate)
        else
            mGoogleMap!!.moveCamera(cameraUpdate)

        mInitialCameraSetupDone = true
    }*/

    fun setMapPaddingLeft(_paddingPx: Int) {
        CURRENT_MAP_PADDING_LEFT = _paddingPx
        mGoogleMap!!.setPadding(CURRENT_MAP_PADDING_LEFT, 0, CURRENT_MAP_PADDING_RIGHT, 0)
    }

    fun setMapPaddingRight(_paddingPx: Int) {
        CURRENT_MAP_PADDING_RIGHT = _paddingPx
        mGoogleMap!!.setPadding(CURRENT_MAP_PADDING_LEFT, 0, CURRENT_MAP_PADDING_RIGHT, 0)   //seen java.lang.NullPointerException on Galaxy Nexus
        //on rapid multiple screen orientation change
        //timing issue with Handler runnable (NearbyActivity.java:2653)
    }

    fun setScrollGesturesEnabled(_toSet: Boolean) {
        mGoogleMap!!.uiSettings.isScrollGesturesEnabled = _toSet
    }

    fun pickedFavoriteMarkerInfoWindowShow() {
        mMarkerPickedFavorite!!.showInfoWindow()
    }

    fun pickedFavoriteMarkerInfoWindowHide() {
        mMarkerPickedFavorite!!.hideInfoWindow()
    }

    fun clearMarkerB() {
        /*if (mMarkerStationB != null)
            mMarkerStationB!!.isVisible = false*/
    }

    fun addMarkerForBikeStation(_outdated: Boolean, item: BikeStation, lookingForBike: Boolean) {

        if (context == null)
            return

        mMapMarkersGfxData.add(StationMapGfx(_outdated, item, lookingForBike, context!!))
    }

    private fun redrawMarkers(gfxData: List<StationMapGfx>?) {

        val pinAVisible: Boolean
        val pinAStationId: String?
        val pinBVisible: Boolean
        val pinPickedPlaceVisible: Boolean
        val pinPickedFavoriteVisible: Boolean
        val pinALatLng: LatLng?
        val pinBLatLng: LatLng?
        val pinPickedPlaceLatLng: LatLng?
        val pinPickedFavoriteLatLng: LatLng?

        val pickedPlaceName: String?
        val pickedFavoriteName: String?

        if (mBufferedBundle != null) {

            pinAVisible = mBufferedBundle!!.getBoolean("pin_A_visibility")
            pinBVisible = mBufferedBundle!!.getBoolean("pin_B_visibility")
            pinPickedPlaceVisible = mBufferedBundle!!.getBoolean("pin_picked_place_visibility")
            pinPickedFavoriteVisible = mBufferedBundle!!.getBoolean("pin_picked_favorite_visibility")

            pinALatLng = mBufferedBundle!!.getParcelable("pin_A_latlng")
            pinBLatLng = mBufferedBundle!!.getParcelable("pin_B_latlng")
            pinPickedPlaceLatLng = mBufferedBundle!!.getParcelable("pin_picked_place_latlng")
            pinPickedFavoriteLatLng = mBufferedBundle!!.getParcelable("pin_picked_favorite_latlng")

            pickedPlaceName = mBufferedBundle!!.getString("picked_place_name")
            pickedFavoriteName = mBufferedBundle!!.getString("picked_favorite_name")

            pinAStationId = mBufferedBundle!!.getString("pin_a_station_id")

            mBufferedBundle = null
        } else {

            pinPickedPlaceVisible = mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible
            pinPickedFavoriteVisible = mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible

            pinPickedPlaceLatLng = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG
            pinPickedFavoriteLatLng = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG

            pickedPlaceName = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else ""
            pickedFavoriteName = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else ""

        }

        /*mGoogleMap!!.clear()

        mMarkerStationA = mGoogleMap!!.addMarker(MarkerOptions().position(pinALatLng!!)
                .icon(mPinAIconBitmapDescriptor)
                .visible(pinAVisible)
                .title(pinAStationId))
        mMarkerStationB = mGoogleMap!!.addMarker(MarkerOptions().position(pinBLatLng!!)
                .icon(mPinBIconBitmapDescriptor)
                .visible(pinBVisible))
        mMarkerPickedPlace = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedPlaceLatLng!!)
                .icon(mPinSearchIconBitmapDescriptor)
                .visible(pinPickedPlaceVisible)
                .title(pickedPlaceName))
        mMarkerPickedFavorite = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedFavoriteLatLng!!)
                .icon(mPinFavoriteIconBitmapDescriptor)
                .visible(pinPickedFavoriteVisible)
                .zIndex(.5f)//so that it's on top of B pin (default Z is 0)
                .title(pickedFavoriteName))

        if (pinPickedPlaceVisible)
            mMarkerPickedPlace!!.showInfoWindow()

        if (pinPickedFavoriteVisible && (mMarkerPickedFavorite!!.position.latitude != pinBLatLng.latitude || mMarkerPickedFavorite!!.position.longitude != pinBLatLng.longitude))
            mMarkerPickedFavorite!!.showInfoWindow()

        gfxData?.forEach {
            it.addMarkerToMap(mGoogleMap!!)
        }*/
    }

    fun redrawMarkers() {

        val pinAVisible: Boolean
        val pinAStationId: String?
        val pinBVisible: Boolean
        val pinPickedPlaceVisible: Boolean
        val pinPickedFavoriteVisible: Boolean
        val pinALatLng: LatLng?
        val pinBLatLng: LatLng?
        val pinPickedPlaceLatLng: LatLng?
        val pinPickedFavoriteLatLng: LatLng?

        val pickedPlaceName: String?
        val pickedFavoriteName: String?

        if (mBufferedBundle != null) {

            pinPickedPlaceVisible = mBufferedBundle!!.getBoolean("pin_picked_place_visibility")
            pinPickedFavoriteVisible = mBufferedBundle!!.getBoolean("pin_picked_favorite_visibility")

            pinPickedPlaceLatLng = mBufferedBundle!!.getParcelable("pin_picked_place_latlng")
            pinPickedFavoriteLatLng = mBufferedBundle!!.getParcelable("pin_picked_favorite_latlng")

            pickedPlaceName = mBufferedBundle!!.getString("picked_place_name")
            pickedFavoriteName = mBufferedBundle!!.getString("picked_favorite_name")

            mBufferedBundle = null
        } else {

            pinPickedPlaceVisible = mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible
            pinPickedFavoriteVisible = mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible

            pinPickedPlaceLatLng = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG
            pinPickedFavoriteLatLng = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG

            pickedPlaceName = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else ""
            pickedFavoriteName = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else ""

            //pinAStationId = if (mMarkerStationA != null) mMarkerStationA!!.title else ""
        }

        mGoogleMap!!.clear()

        mMarkerPickedPlace = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedPlaceLatLng!!)
                .icon(mPinSearchIconBitmapDescriptor)
                .visible(pinPickedPlaceVisible)
                .title(pickedPlaceName))
        mMarkerPickedFavorite = mGoogleMap!!.addMarker(MarkerOptions().position(pinPickedFavoriteLatLng!!)
                .icon(mPinFavoriteIconBitmapDescriptor)
                .visible(pinPickedFavoriteVisible)
                .zIndex(.5f)//so that it's on top of B pin (default Z is 0)
                .title(pickedFavoriteName))

        if (pinPickedPlaceVisible)
            mMarkerPickedPlace!!.showInfoWindow()

        //TODO: that seem kinda relevant for a use case (display info window)
        /*if (pinPickedFavoriteVisible && (mMarkerPickedFavorite!!.position.latitude != pinBLatLng.latitude || mMarkerPickedFavorite!!.position.longitude != pinBLatLng.longitude))
            mMarkerPickedFavorite!!.showInfoWindow()*/

        for (markerData in mMapMarkersGfxData) {
            markerData.addMarkerToMap(mGoogleMap!!)
        }
    }

    //TODO: if clients have a stationname, maybe they have the station LatLng on hand
    fun setPinOnStation(_lookingForBike: Boolean, _stationId: String?) {

        try {
            for (markerData in mMapMarkersGfxData) {
                if (markerData.markerTitle.equals(_stationId!!, ignoreCase = true)) {
                    if (_lookingForBike) {
                        pinAMarker.position = markerData.markerLatLng
                        pinAMarker.isVisible = true
                        pinAMarker.title = markerData.markerTitle
                    } else {
                        pinBMarker!!.position = markerData.markerLatLng
                        pinBMarker!!.isVisible = true

                        if (isPickedFavoriteMarkerVisible) {
                            if (markerPickedFavoriteVisibleLatLng!!.latitude == markerBVisibleLatLng!!.latitude && markerPickedFavoriteVisibleLatLng!!.longitude == markerBVisibleLatLng!!.longitude) {
                                mMarkerPickedFavorite!!.setIcon(mNoPinFavoriteIconBitmapDescriptor)
                                mMarkerPickedFavorite!!.hideInfoWindow()
                            } else {
                                mMarkerPickedFavorite!!.setIcon(mPinFavoriteIconBitmapDescriptor)
                                mMarkerPickedFavorite!!.showInfoWindow()
                            }
                        }
                    }

                    break
                }
            }
        } catch (e: ConcurrentModificationException) {
            //Can happen on screen orientation change. Simply retry
            setPinOnStation(_lookingForBike, _stationId)
        }

    }

    fun setPinForPickedPlace(_placeName: String, _placePosition: LatLng, _attributions: CharSequence?) {

        mMarkerPickedPlace!!.title = _placeName
        mMarkerPickedPlace!!.position = _placePosition
        mMarkerPickedPlace!!.isVisible = true
        mMarkerPickedPlace!!.showInfoWindow()

        if (_attributions != null) {
            mAttributionsText!!.text = Utils.fromHtml(_attributions.toString())
            mAttributionsText!!.visibility = View.VISIBLE
        }
    }

    fun setPinForPickedFavorite(_favoriteName: String, _favoritePosition: LatLng, _attributions: CharSequence?) {

        mMarkerPickedFavorite!!.title = _favoriteName
        mMarkerPickedFavorite!!.position = _favoritePosition
        mMarkerPickedFavorite!!.isVisible = true

        if (_favoritePosition.latitude == markerBVisibleLatLng!!.latitude && _favoritePosition.longitude == markerBVisibleLatLng!!.longitude) {
            mMarkerPickedFavorite!!.setIcon(mNoPinFavoriteIconBitmapDescriptor)
            mMarkerPickedFavorite!!.hideInfoWindow()
        } else {
            mMarkerPickedFavorite!!.setIcon(mPinFavoriteIconBitmapDescriptor)
            mMarkerPickedFavorite!!.showInfoWindow()
        }

        if (_attributions != null) {
            mAttributionsText!!.text = Utils.fromHtml(_attributions.toString())
            mAttributionsText!!.visibility = View.VISIBLE
        }
    }

    fun clearMarkerPickedPlace() {
        if (mMarkerPickedPlace != null)
            mMarkerPickedPlace!!.isVisible = false

        mAttributionsText!!.visibility = View.GONE
        mAttributionsText!!.text = ""
    }

    fun clearMarkerPickedFavorite() {
        if (mMarkerPickedFavorite != null)
            mMarkerPickedFavorite!!.isVisible = false
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnStationMapFragmentInteractionListener {
        fun onStationMapFragmentInteraction(uri: Uri)
    }

    companion object {

        private val TAG = StationMapFragment::class.java.simpleName
        val INFOWINDOW_CLICK_PATH = "infowindow_click"
        val MARKER_CLICK_PATH = "marker_click"
        val MAP_READY_PATH = "map_ready"
        val MAP_CLICK_PATH = "map_click"

        val INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM = "infowindow_click_marker_lat"
        val INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM = "infowindow_click_marker_lng"
        val MARKER_CLICK_TITLE_PARAM = "marker_click_title"

        private var CURRENT_MAP_PADDING_LEFT = 0
        private var CURRENT_MAP_PADDING_RIGHT = 0
    }

}
