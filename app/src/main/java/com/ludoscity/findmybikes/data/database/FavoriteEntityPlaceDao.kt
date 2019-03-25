package com.ludoscity.findmybikes.data.database

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 */
@Dao
interface FavoriteEntityPlaceDao {

    @get:Query("SELECT * FROM FavoriteEntityPlace ORDER BY ui_index DESC")
    val all: LiveData<List<FavoriteEntityPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(favoriteEntityPlace: FavoriteEntityPlace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favoriteEntityPlaceList: List<FavoriteEntityPlace>)

    @Query("DELETE FROM FavoriteEntityPlace WHERE id = :favoriteId")
    fun deleteOne(favoriteId: String)

    //TODO: a more complex query that can be returned as a LiveData<FavoriteEntityBase> ?
    @Query("SELECT * FROM FavoriteEntityPlace WHERE id = :favoriteId")
    fun getForId(favoriteId: String): LiveData<FavoriteEntityPlace>

    //TODO: this seems broken
    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id <> :favoriteId")
    fun validFavoriteCount(favoriteId: String): LiveData<Long>
}
