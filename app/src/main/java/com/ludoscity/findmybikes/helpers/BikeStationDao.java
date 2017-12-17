package com.ludoscity.findmybikes.helpers;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.ludoscity.findmybikes.citybik_es.model.BikeStation;

import java.util.List;

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Dao
public interface BikeStationDao {
    @Query("SELECT * FROM bikestation")
    List<BikeStation> getAll();
}
