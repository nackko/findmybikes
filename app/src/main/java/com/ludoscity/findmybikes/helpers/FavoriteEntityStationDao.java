package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

import java.util.List;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 */
@Dao
public interface FavoriteEntityStationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOne(FavoriteEntityStation favoriteEntityStation);

    @Query("DELETE FROM favoriteentitystation WHERE id = :favoriteId")
    void deleteOne(String favoriteId);

    @Query("SELECT * FROM favoriteentitystation")
    LiveData<List<FavoriteEntityStation>> getAll();

    @Query("SELECT * FROM favoriteentitystation WHERE id = :favoriteId")
    LiveData<FavoriteEntityStation> getForId(String favoriteId);

    @Query("DELETE FROM favoriteentitystation")
    void deleteAll();

}
