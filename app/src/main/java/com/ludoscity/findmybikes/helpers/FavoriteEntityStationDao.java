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
    void insertOne(FavoriteEntityStation favoriteEntityStation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FavoriteEntityStation> favoriteEntityStationList);

    @Query("DELETE FROM FavoriteEntityStation WHERE id = :favoriteId")
    void deleteOne(String favoriteId);

    @Query("SELECT * FROM FavoriteEntityStation ORDER BY ui_index DESC")
    LiveData<List<FavoriteEntityStation>> getAll();

    //TODO: a more complex query that can be returned as a LiveData<FavoriteEntityBase> ?
    @Query("SELECT * FROM FavoriteEntityStation WHERE id = :favoriteId")
    LiveData<FavoriteEntityStation> getForId(String favoriteId);

    //TODO: this seems broken
    @Query("SELECT COUNT(*) FROM FavoriteEntityStation WHERE id <> :favoriteId")
    LiveData<Long> validFavoriteCount(String favoriteId);

    @Query("DELETE FROM favoriteentitystation")
    void deleteAll();

}
