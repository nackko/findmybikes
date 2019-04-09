package com.ludoscity.findmybikes.data.database.favorite

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

    @get:Query("SELECT * FROM FavoriteEntityPlace ORDER BY ui_index ASC")
    val all: LiveData<List<FavoriteEntityPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(favoriteEntityPlace: FavoriteEntityPlace)

    @Query("UPDATE FavoriteEntityPlace SET custom_name = :newCustomName WHERE id = :favoriteIdToUpdate")
    fun updateCustomNameByFavoriteId(favoriteIdToUpdate: String, newCustomName: String)

    @Query("UPDATE FavoriteEntityPlace SET ui_index = :newUiIndex WHERE id = :favoriteIdToUpdate")
    fun updateUiIndexByFavoriteId(favoriteIdToUpdate: String, newUiIndex: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favoriteEntityPlaceList: List<FavoriteEntityPlace>)

    @Query("DELETE FROM FavoriteEntityPlace WHERE id = :favoriteId")
    fun deleteOne(favoriteId: String)

    //TODO: a more complex query that can be returned as a LiveData<FavoriteEntityBase> ?
    @Query("SELECT * FROM FavoriteEntityPlace WHERE id = :favoriteId")
    fun getForId(favoriteId: String): FavoriteEntityPlace?

    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id <> :favoriteIdToExclude")
    fun validFavoriteCount(favoriteIdToExclude: String): Int

    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id = :favoriteIdToCheck")
    fun isFavoriteId(favoriteIdToCheck: String): Int
}
