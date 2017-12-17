package com.ludoscity.findmybikes.helpers;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ludoscity.findmybikes.citybik_es.model.BikeStation;

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Database(entities = {BikeStation.class}, version = 1)
abstract class AppDatabase extends RoomDatabase {
    public abstract BikeStationDao bikeStationDao();
}
