package com.ludoscity.findmybikes;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spanned;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each station to be shown in listViewAdapter
 */
public class StationItem implements Parcelable {
    private String id;
    private String name;
    private boolean locked;
    private int empty_slots;
    private int free_bikes;
    private double latitude;
    private double longitude;
   // private LatLng position;
    private String timestamp;

    public StationItem(String id, String name, LatLng position, int free_bikes, int empty_slots, String timestamp, boolean locked) {
        this.id = id;
        this.name = name;
        this.locked = locked;
        this.empty_slots = empty_slots;
        this.free_bikes = free_bikes;
        this.latitude = position.latitude;
        this.longitude = position.longitude;
        //this.position = position;
        this.timestamp = timestamp;
    }

    public StationItem(BikeStation _bikeStation) {

        this.id = _bikeStation.getLocationHash();

        if (null != _bikeStation.getExtra().getExtraName()) {
            try {
                this.name = new String(_bikeStation.getExtra().getExtraName().getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.d("StationItem constructor", "String trouble",e );
            }
        }
        else {
            try {
                this.name = new String(_bikeStation.getName().getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.d("StationItem constructor", "String trouble", e);
            }
        }

        if (null != _bikeStation.getExtra().getLocked())
            this.locked = _bikeStation.getExtra().getLocked();


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

        this.empty_slots = _bikeStation.getEmptySlots();
        this.free_bikes = _bikeStation.getFreeBikes();
        //}
        this.latitude = _bikeStation.getLatitude();
        this.longitude = _bikeStation.getLongitude();
        //this.position = new LatLng(_bikeStation.latitude, _bikeStation.longitude);
        this.timestamp = _bikeStation.getTimestamp();
    }

    private StationItem(Parcel in){
        id = in.readString();
        name = in.readString();
        locked = in.readByte() != 0;
        empty_slots = in.readInt();
        free_bikes = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        //position = in.readParcelable(LatLng.class.getClassLoader());
        timestamp = in.readString();
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getEmpty_slots() {
        return empty_slots;
    }

    public int getFree_bikes() { return free_bikes; }

    double getMeterFromLatLng(LatLng userLocation) {
        return SphericalUtil.computeDistanceBetween(userLocation, new LatLng(latitude,longitude));}

    //public double getBearingFromLatLng(LatLng userLocation){
    //    return SphericalUtil.computeHeading(userLocation, new LatLng(latitude,longitude) );}

    public LatLng getLocation() {return new LatLng(latitude,longitude);}

    public boolean isFavorite(Context _ctx) {
        return _ctx != null && DBHelper.isFavorite(id, _ctx);
    }

    public FavoriteItemStation getFavoriteItemForDisplayName(String _displayName){
        if (_displayName.equalsIgnoreCase(name))
            return new FavoriteItemStation(id, name, true);
        else
            return new FavoriteItemStation(id, _displayName, false);
    }

    public boolean isLocked() {
        return locked;
    }

    // you MUST call this on a favorite station. No validation to not got to SharedPref too much
    //Called multiple times (station binding happens on user location update)
    public Spanned getFavoriteName(Context _ctx, boolean _favoriteDisplayNameOnly){

        Spanned toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_only_italic),
                name));

        if (!DBHelper.getFavoriteItemForId(_ctx, id).isDisplayNameDefault()){
            if(_favoriteDisplayNameOnly){
                toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_only_bold),
                        DBHelper.getFavoriteItemForId(_ctx, id).getDisplayName()));
            } else {
                toReturn = Utils.fromHtml(String.format(_ctx.getString(R.string.favorite_display_name_complete),
                        DBHelper.getFavoriteItemForId(_ctx, id).getDisplayName(), name ));
            }
        }

        return toReturn;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeByte((byte) (locked ? 1 : 0));
        dest.writeInt(empty_slots);
        dest.writeInt(free_bikes);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        //dest.writeParcelable(position, flags);
        dest.writeString(timestamp);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public StationItem createFromParcel(Parcel source) {
            return new StationItem(source);
        }

        @Override
        public StationItem[] newArray(int size) {
            return new StationItem[size];
        }
    };


}
