package com.ludoscity.findmybikes.data.database.station

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.google.maps.android.SphericalUtil

//TODO: Long term : separate mostly stable data (station location, name, etc...)
//from highly volatile one (nb of bikes/free slots). Could allow nice
//offline feature where stations location would be cached separately
//implements Parcelable {

//TODO: separate class used to retrieve data over network from the one used to save in Room
@Entity
data class BikeStation(

        @PrimaryKey
        var uid: String,
        @ColumnInfo(name = "empty_slots")
        @SerializedName("empty_slots")
        var emptySlots: Int,//TODO: introduce nullability
        @Embedded(prefix = "extra_")
        val extra: BikeStationExtra?,
        @ColumnInfo(name = "free_bikes")
        @SerializedName("free_bikes")
        val freeBikes: Int,//TODO: introduce nullability
        @ColumnInfo(name = "latitude")
        val latitude: Double,
        @ColumnInfo(name = "longitude")
        val longitude: Double,
        @ColumnInfo(name = "name")
        var name: String?,
        @ColumnInfo(name = "timestamp")
        val timestamp: String,
        @SerializedName("id")
        @ColumnInfo(name = "location_hash")
        val locationHash: String) {

    val location: LatLng
        get() = LatLng(latitude!!, longitude!!)

    val isLocked: Boolean
        get() {

            var toReturn = false

            if (extra != null) {

                if (extra!!.renting != null && extra!!.returning != null) {
                    toReturn = extra!!.renting == 0 || extra!!.returning == 0
                } else if (extra!!.locked != null) {
                    toReturn = extra!!.locked!!
                }
            }

            return toReturn
        }

    fun getMeterFromLatLng(userLocation: LatLng): Double {
        return SphericalUtil.computeDistanceBetween(userLocation, location)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //test data
    //Laurier / Brebeuf
    /*if (_bikeStation.id.equalsIgnoreCase("f132843c3c740cce6760167985bc4d17")){
            this.empty_slots = 35;
            this.free_bikes = 0;

            //Lanaudiere / Laurier
        }else if (_bikeStation.id.equalsIgnoreCase("92d97d6adec177649b366c36f3e8e2ff")){
            this.empty_slots = 17;
            this.free_bikes = 2;

        }else if (_bikeStation.id.equalsIgnoreCase("d20fea946f06e7e64e6da7d95b3c3a89")){
            this.empty_slots = 1;
            this.free_bikes = 19;
        }else if (_bikeStation.id.equalsIgnoreCase("3500704c9971a0c13924e696f5804bbd")){
            this.empty_slots = 0;
            this.free_bikes = 31;
        } else {*/
}
