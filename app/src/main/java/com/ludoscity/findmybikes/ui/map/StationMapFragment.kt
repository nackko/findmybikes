package com.ludoscity.findmybikes.ui.map

import android.Manifest
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
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
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

    private var mListener: OnStationMapFragmentInteractionListener? = null

    private var mMarkerStationA: Marker? = null
    private var mPinAIconBitmapDescriptor: BitmapDescriptor? = null
    private var mMarkerStationB: Marker? = null
    private var mPinBIconBitmapDescriptor: BitmapDescriptor? = null
    private var mMarkerPickedPlace: Marker? = null
    private var mPinSearchIconBitmapDescriptor: BitmapDescriptor? = null
    private var mMarkerPickedFavorite: Marker? = null
    private var mPinFavoriteIconBitmapDescriptor: BitmapDescriptor? = null
    private var mNoPinFavoriteIconBitmapDescriptor: BitmapDescriptor? = null

    private var mAttributionsText: TextView? = null

    private val MONTREAL_LATLNG = LatLng(45.5087, -73.554)

    //Pin markers can only be restored after mGoogleMap is ready
    private var mBufferedBundle: Bundle? = null

    val markerAStationId: String?
        get() {

            var toReturn: String? = ""

            if (mMarkerStationA != null)
                toReturn = mMarkerStationA!!.title
            else if (mBufferedBundle != null)
                toReturn = mBufferedBundle!!.getString("pin_a_station_id")

            return toReturn
        }

    val markerALatLng: LatLng?
        get() {
            var toReturn: LatLng? = null

            if (mMarkerStationA != null)
                toReturn = mMarkerStationA!!.position
            else if (mBufferedBundle != null)
                toReturn = mBufferedBundle!!.getParcelable("pin_A_latlng")

            return toReturn
        }

    val markerBVisibleLatLng: LatLng?
        get() {
            var toReturn: LatLng? = null
            if (mMarkerStationB != null) {
                if (mMarkerStationB!!.isVisible)
                    toReturn = mMarkerStationB!!.position
            } else if (mBufferedBundle != null && mBufferedBundle!!.getBoolean("pin_B_visibility"))
                toReturn = mBufferedBundle!!.getParcelable("pin_B_latlng")

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

        internal var mLookingForBikeWhenFinished: Boolean? = null
        internal var mOutdatedWhenFinished: Boolean? = null
        override fun onFinish() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mOutdatedWhenFinished!!, mLookingForBikeWhenFinished!!)

            showAllStations()

            mAnimCallback = null

        }

        override fun onCancel() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mOutdatedWhenFinished!!, mLookingForBikeWhenFinished!!)


            showAllStations()

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

        mPinAIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_a_36dp_black)
        mPinBIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_b_36dp_black)
        mPinSearchIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_search_24dp_black)
        mPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_favorite_24dp_black)
        mNoPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_nopin_favorite_24dp_white)

        mAttributionsText = inflatedView.findViewById<View>(R.id.attributions_text) as TextView

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
        outState.putBoolean("pin_A_visibility", mMarkerStationA != null && mMarkerStationA!!.isVisible)
        outState.putBoolean("pin_B_visibility", mMarkerStationB != null && mMarkerStationB!!.isVisible)
        outState.putParcelable("pin_A_latlng", if (mMarkerStationA != null) mMarkerStationA!!.position else MONTREAL_LATLNG)
        outState.putParcelable("pin_B_latlng", if (mMarkerStationB != null) mMarkerStationB!!.position else MONTREAL_LATLNG)
        outState.putString("pin_a_station_id", if (mMarkerStationA != null) mMarkerStationA!!.title else "")

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
        mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.5086699, -73.5539925), 13f))
        mGoogleMap!!.setOnMarkerClickListener(this)
        mGoogleMap!!.setOnInfoWindowClickListener(this)
        //mGoogleMap.setOnCameraChangeListener(this);
        //mGoogleMap.setOnMapClickListener(this);
        //héhéhé, feel the power of design !!
        //mGoogleMap!!.uiSettings.isZoomGesturesEnabled = false
        mGoogleMap!!.uiSettings.isRotateGesturesEnabled = false
        mGoogleMap!!.uiSettings.isIndoorLevelPickerEnabled = false
        mGoogleMap!!.uiSettings.isTiltGesturesEnabled = false


        val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application)

        mapFragmentModel = ViewModelProviders.of(this, modelFactory).get(MapFragmentViewModel::class.java)


        mapFragmentModel.mapGfxLiveData.observe(this, Observer { newGfxData ->

            redrawMarkers(newGfxData)

            Log.d(TAG, "Markers redrawned, size :" + newGfxData?.size)

            showAllStations(newGfxData)

        })
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        if (isPickedFavoriteMarkerVisible)
            mMarkerPickedFavorite!!.showInfoWindow()

        if (isPickedPlaceMarkerVisible)
            mMarkerPickedPlace!!.showInfoWindow()

        if (marker.title.equals(mMarkerPickedFavorite!!.title, ignoreCase = true) ||
                mMarkerStationA!!.isVisible &&
                mMarkerStationA!!.position.latitude == marker.position.latitude &&
                mMarkerStationA!!.position.longitude == marker.position.longitude ||
                mMarkerStationB!!.isVisible &&
                mMarkerStationB!!.position.latitude == marker.position.latitude &&
                mMarkerStationB!!.position.longitude == marker.position.longitude ||
                mMarkerPickedPlace!!.isVisible && //except if picked destination is favorite

                mMarkerPickedPlace!!.position.latitude == marker.position.latitude &&
                mMarkerPickedPlace!!.position.longitude == marker.position.longitude)
            return true

        if (mMarkerPickedFavorite!!.isVisible &&
                //TODO: the following seems a bit patterny, checking if B pin and Favorite one are not on the same station
                //check connection with NearbyActivity::setupBTabSelection refactor ideas
                mMarkerPickedFavorite!!.position.latitude == marker.position.latitude &&
                mMarkerPickedFavorite!!.position.longitude == marker.position.longitude) {
            mMarkerPickedFavorite!!.hideInfoWindow()
        }

        val builder = Uri.Builder()
        builder.appendPath(MARKER_CLICK_PATH)

        builder.appendQueryParameter(MARKER_CLICK_TITLE_PARAM, marker.title)

        if (mListener != null) {
            mListener!!.onStationMapFragmentInteraction(builder.build())
        }

        //So that info window will not be showed
        return true
    }

    fun onUserLocationChange(location: Location?) {
        if (location != null) {
            //Log.d("onMyLocationChange", "new location " + location.toString());
            if (!mInitialCameraSetupDone && mGoogleMap != null) {
                doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f), false)
            }
        }
    }

    fun enableMyLocationCheckingPermission() {
        if (ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap!!.isMyLocationEnabled = true
            mGoogleMap!!.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    fun doInitialCameraSetup(cameraUpdate: CameraUpdate, animate: Boolean) {
        if (animate)
            animateCamera(cameraUpdate)
        else
            mGoogleMap!!.moveCamera(cameraUpdate)

        mInitialCameraSetupDone = true
    }

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
        if (mMarkerStationB != null)
            mMarkerStationB!!.isVisible = false
    }

    fun addMarkerForBikeStation(_outdated: Boolean, item: BikeStation, lookingForBike: Boolean) {

        if (context == null)
            return

        mMapMarkersGfxData.add(StationMapGfx(_outdated, item, lookingForBike, context))
    }

    fun redrawMarkers(gfxData: List<StationMapGfx>?) {

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

            pinAVisible = mMarkerStationA != null && mMarkerStationA!!.isVisible
            pinBVisible = mMarkerStationB != null && mMarkerStationB!!.isVisible
            pinPickedPlaceVisible = mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible
            pinPickedFavoriteVisible = mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible

            pinALatLng = if (mMarkerStationA != null) mMarkerStationA!!.position else MONTREAL_LATLNG
            pinBLatLng = if (mMarkerStationB != null) mMarkerStationB!!.position else MONTREAL_LATLNG
            pinPickedPlaceLatLng = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG
            pinPickedFavoriteLatLng = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG

            pickedPlaceName = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else ""
            pickedFavoriteName = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else ""

            pinAStationId = if (mMarkerStationA != null) mMarkerStationA!!.title else ""
        }

        mGoogleMap!!.clear()

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
            it.addMarkerToMap(mGoogleMap)
        }
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

            pinAVisible = mMarkerStationA != null && mMarkerStationA!!.isVisible
            pinBVisible = mMarkerStationB != null && mMarkerStationB!!.isVisible
            pinPickedPlaceVisible = mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible
            pinPickedFavoriteVisible = mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible

            pinALatLng = if (mMarkerStationA != null) mMarkerStationA!!.position else MONTREAL_LATLNG
            pinBLatLng = if (mMarkerStationB != null) mMarkerStationB!!.position else MONTREAL_LATLNG
            pinPickedPlaceLatLng = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG
            pinPickedFavoriteLatLng = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG

            pickedPlaceName = if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else ""
            pickedFavoriteName = if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else ""

            pinAStationId = if (mMarkerStationA != null) mMarkerStationA!!.title else ""
        }

        mGoogleMap!!.clear()

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

        for (markerData in mMapMarkersGfxData) {
            markerData.addMarkerToMap(mGoogleMap)
        }
    }

    fun clearMarkerGfxData() {
        mMapMarkersGfxData.clear()
    }

    fun animateCamera(cameraUpdate: CameraUpdate) {

        if (context != null) {

            mAnimCallback = CustomCancellableCallback()

            hideAllStations()

            mGoogleMap!!.animateCamera(cameraUpdate, resources.getInteger(R.integer.camera_animation_duration), mAnimCallback)
        } else {
            mGoogleMap!!.animateCamera(cameraUpdate)
        }
    }

    fun hideAllStations() {

        try {
            for (markerData in mMapMarkersGfxData) {
                markerData.hide()
            }
        } catch (e: ConcurrentModificationException) {
            //Can happen on screen orientation change. Simply retry
            hideAllStations()
        }

    }

    fun showAllStations(gfxData: List<StationMapGfx>?) {

        try {

            gfxData?.forEach {
                it.show(mGoogleMap!!.cameraPosition.zoom)
            }


        } catch (e: ConcurrentModificationException) {
            //Can happen on screen orientation change. Simply retry
            showAllStations()
        }

    }

    fun showAllStations() {

        try {
            for (markerData in mMapMarkersGfxData) {
                markerData.show(mGoogleMap!!.cameraPosition.zoom)
            }
        } catch (e: ConcurrentModificationException) {
            //Can happen on screen orientation change. Simply retry
            showAllStations()
        }

    }

    fun lookingForBikes(_outdated: Boolean, _lookingForBike: Boolean) {

        if (mAnimCallback != null) {
            mAnimCallback!!.mLookingForBikeWhenFinished = _lookingForBike
            mAnimCallback!!.mOutdatedWhenFinished = _outdated
        } else
            updateMarkerAll(_outdated, _lookingForBike)
    }

    private fun updateMarkerAll(_outdated: Boolean, _lookingForBike: Boolean) {

        if (context == null)
            return

        //Log.d("StaitonMapFragment", "updateMarkerAll - about to update markers with _outdated : " + _outdated, new Exception());

        try {
            for (markerData in mMapMarkersGfxData) {
                markerData.updateMarker(_outdated, _lookingForBike, context)
            }
        } catch (e: ConcurrentModificationException) {
            updateMarkerAll(_outdated, _lookingForBike)
        }

    }

    //TODO: if clients have a stationname, maybe they have the station LatLng on hand
    fun setPinOnStation(_lookingForBike: Boolean, _stationId: String?) {

        try {
            for (markerData in mMapMarkersGfxData) {
                if (markerData.markerTitle.equals(_stationId!!, ignoreCase = true)) {
                    if (_lookingForBike) {
                        mMarkerStationA!!.position = markerData.markerLatLng
                        mMarkerStationA!!.isVisible = true
                        mMarkerStationA!!.title = markerData.markerTitle
                    } else {
                        mMarkerStationB!!.position = markerData.markerLatLng
                        mMarkerStationB!!.isVisible = true

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
