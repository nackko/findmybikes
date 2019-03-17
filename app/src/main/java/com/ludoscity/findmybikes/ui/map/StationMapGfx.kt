package com.ludoscity.findmybikes.ui.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.citybik_es.model.BikeStation
import com.ludoscity.findmybikes.helpers.DBHelper
import com.ludoscity.findmybikes.utils.Utils

/**
 * Created by F8Full on 2015-07-12.
 * This class is intended to retain the nescessary components to create and display a marker on a Google map
 * It's been created when StationItem was rendered jsonable
 */
class StationMapGfx(_outdated: Boolean, private val mItem: BikeStation //corresponding data
                    , _lookingForBike: Boolean, _ctx: Context) {

    private val markerOptions: MarkerOptions
    private val groundOverlayOptions: GroundOverlayOptions
    private var marker: Marker? = null
    private var groundOverlay: GroundOverlay? = null

    val markerTitle: String
        get() = marker!!.title

    val markerLatLng: LatLng
        get() = marker!!.position

    init {

        //Marker setup
        markerOptions = MarkerOptions()
                .position(mItem.location)
                .title(mItem.locationHash)
                .alpha(0f)
                .zIndex(1f)//so that invisible clicking marker is in front of Favorite pin
                .anchor(0.5f, 0.5f)
                .infoWindowAnchor(0.5f, 0.5f)

        // Since googleMap doesn't allow marker resizing we have to use ground overlay to not clog the map when we zoom out...
        groundOverlayOptions = GroundOverlayOptions()
                .position(mItem.location, maxOverlaySize)
                .transparency(0.1f)
                .visible(false)

        if (!_outdated) {
            if (mItem.isLocked)
                groundOverlayOptions.image(greyIcon)
            else {
                if (_lookingForBike) {
                    if (mItem.freeBikes <= DBHelper.getInstance().getCriticalAvailabilityMax(_ctx))
                        groundOverlayOptions.image(redIcon)
                    else if (mItem.freeBikes <= DBHelper.getInstance().getBadAvailabilityMax(_ctx))
                        groundOverlayOptions.image(yellowIcon)
                    else
                        groundOverlayOptions.image(greenIcon)
                } else {
                    if (mItem.emptySlots != -1 && mItem.emptySlots <= DBHelper.getInstance().getCriticalAvailabilityMax(_ctx))
                        groundOverlayOptions.image(redIcon)
                    else if (mItem.emptySlots != -1 && mItem.emptySlots <= DBHelper.getInstance().getBadAvailabilityMax(_ctx))
                        groundOverlayOptions.image(yellowIcon)
                    else
                        groundOverlayOptions.image(greenIcon)
                }
            }
        } else {
            groundOverlayOptions.image(pinkIcon)
        }
    }

    fun addToMap(map: GoogleMap) {
        marker = map.addMarker(markerOptions)
        groundOverlay = map.addGroundOverlay(groundOverlayOptions)
    }

    fun updateMarker(_outdated: Boolean, _isLookingForBikes: Boolean, _ctx: Context) {

        //happens on screen orientation change with slow devices
        if (groundOverlay == null)
            return

        if (!_outdated) {
            if (_isLookingForBikes) {
                if (!mItem.isLocked) {
                    if (mItem.freeBikes <= DBHelper.getInstance().getCriticalAvailabilityMax(_ctx))
                        groundOverlay!!.setImage(redIcon)
                    else if (mItem.freeBikes <= DBHelper.getInstance().getBadAvailabilityMax(_ctx))
                        groundOverlay!!.setImage(yellowIcon)
                    else
                        groundOverlay!!.setImage(greenIcon)
                } else
                    groundOverlay!!.setImage(greyIcon)
            } else {
                if (!mItem.isLocked) {
                    if (mItem.emptySlots != -1 && mItem.emptySlots <= DBHelper.getInstance().getCriticalAvailabilityMax(_ctx))
                        groundOverlay!!.setImage(redIcon)
                    else if (mItem.emptySlots != -1 && mItem.emptySlots <= DBHelper.getInstance().getBadAvailabilityMax(_ctx))
                        groundOverlay!!.setImage(yellowIcon)
                    else
                        groundOverlay!!.setImage(greenIcon)
                } else
                    groundOverlay!!.setImage(greyIcon)
            }
        } else {
            groundOverlay!!.setImage(pinkIcon)
        }
    }

    fun hide() {
        if (groundOverlay != null)
            groundOverlay!!.isVisible = false
    }

    fun show(_zoom: Float) {

        if (groundOverlay != null) {
            groundOverlay!!.setDimensions(Utils.map(_zoom, maxZoomOut, maxZoomIn, maxOverlaySize, minOverlaySize))
            groundOverlay!!.isVisible = true
        }
    }

    companion object {

        private val redIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_red)
        private val greyIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_grey)
        private val greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_green)
        private val yellowIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_yellow)
        private val pinkIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_outdated)

        //For linear mappig of zoom level to oberlay size. Empirically determined.
        private val maxZoomOut = 13.75f
        private val maxZoomIn = 21f
        private val minOverlaySize = 1f
        private val maxOverlaySize = 50f
    }
}
