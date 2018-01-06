package com.ludoscity.findmybikes.helpers;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.ludoscity.findmybikes.datamodel.FavoriteEntityPlace;

import java.util.List;

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 */
@Dao
public interface FavoriteEntityPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOne(FavoriteEntityPlace favoriteEntityPlace);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FavoriteEntityPlace> favoriteEntityPlaceList);

    @Query("DELETE FROM FavoriteEntityPlace WHERE id = :favoriteId")
    void deleteOne(String favoriteId);

    @Query("SELECT * FROM FavoriteEntityPlace ORDER BY ui_index DESC")
    LiveData<List<FavoriteEntityPlace>> getAll();

    //TODO: a more complex query that can be returned as a LiveData<FavoriteEntityBase> ?
    @Query("SELECT * FROM FavoriteEntityPlace WHERE id = :favoriteId")
    LiveData<FavoriteEntityPlace> getForId(String favoriteId);

    //TODO: this seems broken
    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id <> :favoriteId")
    LiveData<Long> validFavoriteCount(String favoriteId);
}
