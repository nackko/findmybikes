package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.ludoscity.findmybikes.citybik_es.model.BikeStation;

import java.util.List;

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Dao
public interface BikeStationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBikeStationList(List<BikeStation> bikeStationList);

    /*@Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertBikeStation(BikeStation toInsert);*/

    @Update
    void updateBikeStation(BikeStation toUpdate);

    @Update
    void updateBikeStationList(BikeStation... bikeStationList);

    @Delete
    void deleteBikeStationList(BikeStation... bikeStationList);

    @Query("DELETE FROM bikestation")
    void deleteAllBikeStation();

    @Query("SELECT * FROM bikestation")
    LiveData<List<BikeStation>> getAll();

    @Query("SELECT * FROM bikestation WHERE location_hash = :stationId")
    LiveData<BikeStation> getStation(String stationId);

    //TODO: Add queries for inserting or removing only one BikeStation ? Or use the list one with a list of size 1 ?
    //@Insert(onConflict = REPLACE)
    //void save(BikeStation bikestation);
    //@Query("SELECT * FROM user WHERE location_hash = :locationHash")
    //LiveData<BikeStation> load(String locationHash);

}
