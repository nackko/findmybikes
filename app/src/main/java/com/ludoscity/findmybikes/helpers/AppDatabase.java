package com.ludoscity.findmybikes.helpers;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Database(entities = {BikeStation.class, FavoriteEntityStation.class}, version = 1)
//@TypeConverters({LatLngTypeConverter.class})
abstract class AppDatabase extends RoomDatabase {
    public abstract BikeStationDao bikeStationDao();
    public abstract FavoriteEntityStationDao favoriteEntityStationDao();
    //public abstract FavoriteEntityPlaceDao favoriteEntityPlaceDao();
}
