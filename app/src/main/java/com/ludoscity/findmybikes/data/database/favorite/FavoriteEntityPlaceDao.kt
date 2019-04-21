package com.ludoscity.findmybikes.data.database.favorite

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Created by F8Full on 2017-12-23. This file is part of #findmybikes
 */
@Dao
interface FavoriteEntityPlaceDao {

    @Query("SELECT * FROM FavoriteEntityPlace WHERE bike_system_id = :bikeSystemId ORDER BY ui_index ASC")
    fun getAllForBikeSystemId(bikeSystemId: String): LiveData<List<FavoriteEntityPlace>>

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

    @get:Query("SELECT COUNT(*) FROM FavoriteEntityPlace")
    val count: Int

    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id <> :favoriteIdToExclude")
    fun validFavoriteCount(favoriteIdToExclude: String): Int

    @Query("SELECT COUNT(*) FROM FavoriteEntityPlace WHERE id = :favoriteIdToCheck")
    fun isFavoriteId(favoriteIdToCheck: String): Int
}
