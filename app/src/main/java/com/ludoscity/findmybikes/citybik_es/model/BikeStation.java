package com.ludoscity.findmybikes.citybik_es.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spanned;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.SphericalUtil;
import com.ludoscity.findmybikes.FavoriteItemStation;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

@SuppressWarnings("unused")
@Entity
public class BikeStation {//implements Parcelable {

    public BikeStation() {}

    @SuppressWarnings("NullableProblems")
    @SerializedName("id")
    @ColumnInfo(name = "location_hash")
    @PrimaryKey
    @NonNull
    private String locationHash; //used as a uid

    @ColumnInfo(name = "empty_slots")
    @SerializedName("empty_slots")
    private Integer emptySlots;
    @Embedded(prefix = "extra_")
    private BikeStationExtra extra;
    @ColumnInfo(name = "free_bikes")
    @SerializedName("free_bikes")
    private Integer freeBikes;
    @ColumnInfo(name = "latitude")
    private Double latitude;
    @ColumnInfo(name = "longitude")
    private Double longitude;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "timestamp")
    private String timestamp;

    @NonNull
    public String getLocationHash() {
        return locationHash;
    }

    public void setLocationHash(@NonNull String locationHash) {
        this.locationHash = locationHash;
    }

    public Integer getEmptySlots() {
        return emptySlots;
    }

    public void setEmptySlots(Integer empty_slots) {
        this.emptySlots = empty_slots;
    }

    public BikeStationExtra getExtra() {
        return extra;
    }

    public void setExtra(BikeStationExtra bikeStationExtra) {
        this.extra = bikeStationExtra;
    }

    public Integer getFreeBikes() {
        return freeBikes;
    }

    public void setFreeBikes(Integer freeBikes) {
        this.freeBikes = freeBikes;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return extra.getName();
    }

    public void setName(String name) {
        extra.setName(name);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getMeterFromLatLng(LatLng userLocation) {
        return SphericalUtil.computeDistanceBetween(userLocation, getLocation());}

    public LatLng getLocation() {return new LatLng(latitude,longitude);}

    //TODO: move this closer to the user, or with its own Room storage mechanism
    //TODO: reference BikeStation entities by locationHash (ie : id)
    public boolean isFavorite(Context _ctx) {
        return false;
    }

    //TODO: that's why isFavorite always returns false for now
    // you MUST call this on a favorite station. No validation to not got to SharedPref too much
    //Called multiple times (station binding happens on user location update)
    public Spanned getFavoriteName(Context _ctx, boolean _favoriteDisplayNameOnly){

        Spanned toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_only_italic),
                name));

        if (!DBHelper.getFavoriteItemForId(_ctx, locationHash).isDisplayNameDefault()){
            if(_favoriteDisplayNameOnly){
                toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_only_bold),
                        DBHelper.getFavoriteItemForId(_ctx, locationHash).getDisplayName()));
            } else {
                toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_complete),
                        DBHelper.getFavoriteItemForId(_ctx, locationHash).getDisplayName(), name ));
            }
        }

        return toReturn;
    }

    public FavoriteItemStation getFavoriteItemForDisplayName(String _displayName){
        if (_displayName.equalsIgnoreCase(name))
            return new FavoriteItemStation(locationHash, name, true);
        else
            return new FavoriteItemStation(locationHash, _displayName, false);
    }

    public boolean isLocked() {
        return extra.getLocked();
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //Following code to make it Parcelable
    /*@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(locationHash);
        dest.writeString(name);
        dest.writeByte((byte) (extra.getLocked() ? 1 : 0));
        dest.writeInt(emptySlots);
        dest.writeInt(freeBikes);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        //dest.writeParcelable(position, flags);
        dest.writeString(timestamp);

    }

    private BikeStation(Parcel in){
        locationHash = in.readString();
        name = in.readString();
        extra.setLocked(in.readByte() != 0);
        emptySlots = in.readInt();
        freeBikes = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        //position = in.readParcelable(LatLng.class.getClassLoader());
        timestamp = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public BikeStation createFromParcel(Parcel source) {
            return new BikeStation(source);
        }

        @Override
        public BikeStation[] newArray(int size) {
            return new BikeStation[size];
        }
    };*/
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
