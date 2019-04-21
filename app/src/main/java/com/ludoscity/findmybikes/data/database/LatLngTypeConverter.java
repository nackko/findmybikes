package com.ludoscity.findmybikes.data.database;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 */

public class LatLngTypeConverter{

    @TypeConverter
    public  static String toString(LatLng value)
    {
        return value == null ? null : value.latitude + "|" + value.longitude;
    }

    @TypeConverter
    public static LatLng toLatLng(String value){
        int separatorIdx = value.indexOf("|");

        return new LatLng(Double.valueOf(value.substring(0,separatorIdx-1)), Double.valueOf(value.substring(separatorIdx+1)));
    }

}
