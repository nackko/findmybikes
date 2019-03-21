package com.ludoscity.findmybikes.ui.map

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.ui.main.NearbyActivityViewModel
import com.ludoscity.findmybikes.utils.InjectorUtils
import com.ludoscity.findmybikes.utils.Utils

/**
 * Created by F8Full on 2019-03-09. This file is part of #findmybikes
 * Fragment class to display a map with colored overlays scaling with zoom level.
 * Also carries invisible map marker that can be tapped.
 */
class StationMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        /*GoogleMap.OnCameraChangeListener,*/
        GoogleMap.OnInfoWindowClickListener {

    //models. TODO:Retrieve at construction time ?
    //TODO: answer to question : yes, like table fragment gets availability data source through findmybikes repo
    //private val favoriteListViewModel: FavoriteListViewModel

    private var mInitialCameraSetupDone: Boolean = false

    private var mGoogleMap: GoogleMap? = null
    private var mAnimCallback: CustomCancellableCallback? = null

    private var gfxData: List<StationMapGfx> = emptyList()

    private var mMarkerPickedFavorite: Marker? = null

    private var mAttributionsText: TextView? = null

    //private val MONTREAL_LATLNG = LatLng(45.5087, -73.554)

    private lateinit var pinAIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinBIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinSearchIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinFavoriteIconBitmapDescriptor: BitmapDescriptor
    private lateinit var noPinFavoriteIconBitmapDescriptor: BitmapDescriptor
    private lateinit var pinAMarker: Marker
    private var pinBMarker: Marker? = null
    private var finalDestinationMarker: Marker? = null

    val markerALatLng: LatLng?
        get() {
            return null
        }

    val markerBVisibleLatLng: LatLng?
        get() {
            return null
        }

    private val activityModel: NearbyActivityViewModel
        get() {
            val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

            return ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)
        }

    //TODO: this seems related to onSaveInstanceState, see if relevant
    /*val cameraPosition: CameraPosition?
        get() {
            var toReturn: CameraPosition? = null
            if (isMapReady)
                toReturn = mGoogleMap!!.cameraPosition

            return toReturn
        }*/

    private inner class CustomCancellableCallback : GoogleMap.CancelableCallback {
        private val fragmentModel: MapFragmentViewModel

        init {
            val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

            val findMyBikesActivityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)

            //TODO: pass prebuilt factories to fragment (like table fragment ?)
            val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application,
                    findMyBikesActivityModel.isLookingForBike,
                    findMyBikesActivityModel.isDataOutOfDate,
                    findMyBikesActivityModel.userLocation,
                    findMyBikesActivityModel.getStationA(),
                    findMyBikesActivityModel.getStationB(),
                    findMyBikesActivityModel.finalDestinationLatLng,
                    findMyBikesActivityModel.isFinalDestinationFavorite)

            fragmentModel = ViewModelProviders.of(this@StationMapFragment, modelFactory).get(MapFragmentViewModel::class.java)
        }

        override fun onFinish() {

            fragmentModel.showMapItems()

            mAnimCallback = null

        }

        override fun onCancel() {

            fragmentModel.showMapItems()

            mAnimCallback = null

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflatedView = inflater.inflate(R.layout.fragment_station_map, container, false)

        if (mGoogleMap == null)
            (activity!!.fragmentManager.findFragmentById(R.id.mapNearby) as MapFragment).getMapAsync(this)

        mAttributionsText = inflatedView.findViewById<View>(R.id.attributions_text) as TextView

        pinAIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_a_36dp_black)
        pinBIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_b_36dp_black)
        pinSearchIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_search_24dp_black)
        pinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_pin_favorite_24dp_black)
        noPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(context!!, R.drawable.ic_nopin_favorite_24dp_white)

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

        val findMyBikesActivityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)

        //TODO: pass prebuilt factories to fragment (like table fragment ?)
        val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application,
                findMyBikesActivityModel.isLookingForBike,
                findMyBikesActivityModel.isDataOutOfDate,
                findMyBikesActivityModel.userLocation,
                findMyBikesActivityModel.getStationA(),
                findMyBikesActivityModel.getStationB(),
                findMyBikesActivityModel.finalDestinationLatLng,
                findMyBikesActivityModel.isFinalDestinationFavorite)

        val fragmentModel = ViewModelProviders.of(this, modelFactory).get(MapFragmentViewModel::class.java)


        fragmentModel.lastClickedWhileLookingForBike.observe(this, Observer {
            activityModel.setStationA(it)
        })
        fragmentModel.lastClickedWhileLookingForDock.observe(this, Observer {
            activityModel.setStationB(it)
        })

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

        //outState.putBoolean("pin_picked_place_visibility", mMarkerPickedPlace != null && mMarkerPickedPlace!!.isVisible)
        //outState.putParcelable("pin_picked_place_latlng", if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.position else MONTREAL_LATLNG)
        //outState.putString("picked_place_name", if (mMarkerPickedPlace != null) mMarkerPickedPlace!!.title else "")

        //outState.putBoolean("pin_picked_favorite_visibility", mMarkerPickedFavorite != null && mMarkerPickedFavorite!!.isVisible)
        //outState.putParcelable("pin_picked_favorite_latlng", if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.position else MONTREAL_LATLNG)
        //outState.putString("picked_favorite_name", if (mMarkerPickedFavorite != null) mMarkerPickedFavorite!!.title else "")
        super.onSaveInstanceState(outState)
    }

    //This is for debug
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
        //TODO: rethink gestue. Also gesture availability observed from fragment or activity model
        //mGoogleMap!!.uiSettings.isZoomGesturesEnabled = false
        mGoogleMap!!.uiSettings.isRotateGesturesEnabled = false
        mGoogleMap!!.uiSettings.isIndoorLevelPickerEnabled = false
        mGoogleMap!!.uiSettings.isTiltGesturesEnabled = false

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

        val findMyBikesActivityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)

        //TODO: pass prebuilt factories to fragment (like table fragment ?)
        val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application,
                findMyBikesActivityModel.isLookingForBike,
                findMyBikesActivityModel.isDataOutOfDate,
                findMyBikesActivityModel.userLocation,
                findMyBikesActivityModel.getStationA(),
                findMyBikesActivityModel.getStationB(),
                findMyBikesActivityModel.finalDestinationLatLng,
                findMyBikesActivityModel.isFinalDestinationFavorite)

        val fragmentModel = ViewModelProviders.of(this, modelFactory).get(MapFragmentViewModel::class.java)

        fragmentModel.mapPaddingLeftPx.observe(this, Observer {
            mGoogleMap!!.setPadding(it ?: 0, 0, fragmentModel.mapPaddingRightPx.value ?: 0, 0)
        })

        fragmentModel.mapPaddingRightPx.observe(this, Observer {
            mGoogleMap!!.setPadding(fragmentModel.mapPaddingLeftPx.value ?: 0, 0, it ?: 0, 0)
        })

        fragmentModel.showMapItems.observe(this, Observer {
            Log.d(TAG, "map item new visibility : $it")
            if (it == true) {
                gfxData.forEach { mapGfx ->
                    mapGfx.show(mGoogleMap!!.cameraPosition.zoom)
                }
            } else {
                gfxData.forEach { mapGfx ->
                    mapGfx.hide()
                }
            }
        })

        fragmentModel.isDataOutOfDate.observe(this, Observer { dataIsOutOfDate ->
            gfxData.forEach {
                it.updateMarker(dataIsOutOfDate == true, fragmentModel.isLookingForBike.value == true,
                        context!!)
            }
        })

        fragmentModel.isLookingForBike.observe(this, Observer { isLookingForBike ->
            gfxData.forEach {
                it.updateMarker(fragmentModel.isDataOutOfDate.value == true,
                        isLookingForBike == true, context!!)
            }
        })

        fragmentModel.mapGfxLiveData.observe(this, Observer { newGfxData ->


            //TODO: should local gfxData list be cleared ?
            gfxData = newGfxData ?: emptyList()

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


            finalDestinationMarker = mGoogleMap!!.addMarker(
                    MarkerOptions().position(
                            fragmentModel.finalDestinationLatLng.value ?: LatLng(0.0, 0.0))
                            .icon(pinFavoriteIconBitmapDescriptor)
                            .visible(fragmentModel.finalDestinationLatLng.value != null)
                            .title("")
            )
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
                it.addToMap(mGoogleMap!!)
            }

            Log.d(TAG, "Markers redrawned, size :" + gfxData.size)

            if (mAnimCallback == null) {
                fragmentModel.showMapItems()
            }
        })

        fragmentModel.cameraAnimationTarget.observe(this, Observer {

            Log.d("truc", "MappPaddingLeft: ${fragmentModel.mapPaddingLeftPx.value}, MapPaddingRight: ${fragmentModel.mapPaddingRightPx.value}")

            if (it != null) {
                Log.d(TAG, "observed new camera target")
                mAnimCallback = CustomCancellableCallback()
                mGoogleMap!!.animateCamera(it, resources.getInteger(R.integer.camera_animation_duration), mAnimCallback)
            }
        })

        fragmentModel.finalDestinationLatLng.observe(this, Observer {
            if (it != null) {
                finalDestinationMarker?.position = it
                finalDestinationMarker?.isVisible = true
            } else {
                finalDestinationMarker?.isVisible = false
            }
        })

        fragmentModel.isFinalDestinationFavorite.observe(this, Observer {
            if (it == true) {
                finalDestinationMarker?.setIcon(pinFavoriteIconBitmapDescriptor)
            } else {
                finalDestinationMarker?.setIcon(pinSearchIconBitmapDescriptor)
            }
        })

        fragmentModel.isScrollGesturesEnabled.observe(this, Observer {
            mGoogleMap!!.uiSettings.isScrollGesturesEnabled = it == true
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
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        val activityModelFactory = InjectorUtils.provideMainActivityViewModelFactory(activity!!.application)

        val findMyBikesActivityModel = ViewModelProviders.of(activity!!, activityModelFactory).get(NearbyActivityViewModel::class.java)

        //TODO: pass prebuilt factories to fragment (like table fragment ?)
        val modelFactory = InjectorUtils.provideMapFragmentViewModelFactory(activity!!.application,
                findMyBikesActivityModel.isLookingForBike,
                findMyBikesActivityModel.isDataOutOfDate,
                findMyBikesActivityModel.userLocation,
                findMyBikesActivityModel.getStationA(),
                findMyBikesActivityModel.getStationB(),
                findMyBikesActivityModel.finalDestinationLatLng,
                findMyBikesActivityModel.isFinalDestinationFavorite)

        val fragmentModel = ViewModelProviders.of(this, modelFactory).get(MapFragmentViewModel::class.java)

        //TODO: this is the beginning of going full model
        fragmentModel.setLastClickedStationById(marker.title)

        //following code is "legacy"
        //TODO: in model
        if (fragmentModel.finalDestinationLatLng.value != null)
            finalDestinationMarker?.showInfoWindow()


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

        //So that info window will not be showed
        return true
    }

    //TODO: replug location permission request (splash screen ?)
    fun enableMyLocationCheckingPermission() {
        if (ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this.context!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap!!.isMyLocationEnabled = true
            mGoogleMap!!.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    //TODO: in model
    fun pickedFavoriteMarkerInfoWindowShow() {
        mMarkerPickedFavorite!!.showInfoWindow()
    }

    //TODO: in model
    fun pickedFavoriteMarkerInfoWindowHide() {
        mMarkerPickedFavorite!!.hideInfoWindow()
    }

    companion object {

        private val TAG = StationMapFragment::class.java.simpleName
        val INFOWINDOW_CLICK_PATH = "infowindow_click"

        val INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM = "infowindow_click_marker_lat"
        val INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM = "infowindow_click_marker_lng"
    }

}
