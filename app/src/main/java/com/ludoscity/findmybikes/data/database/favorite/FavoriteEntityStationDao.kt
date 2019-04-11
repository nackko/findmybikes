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
interface FavoriteEntityStationDao {

    @Query("SELECT * FROM FavoriteEntityStation WHERE bike_system_id = :bikeSystemId ORDER BY ui_index ASC")
    fun getAllForBikeSystemId(bikeSystemId: String): LiveData<List<FavoriteEntityStation>>

    @get:Query("SELECT id FROM FavoriteEntityStation")
    val allId: List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOne(favoriteEntityStation: FavoriteEntityStation)

    @Query("UPDATE FavoriteEntityStation SET custom_name = :newCustomName WHERE id = :favoriteIdToUpdate")
    fun updateCustomNameByFavoriteId(favoriteIdToUpdate: String, newCustomName: String)

    @Query("UPDATE FavoriteEntityStation SET ui_index = :newUiIndex WHERE id = :favoriteIdToUpdate")
    fun updateUiIndexByFavoriteId(favoriteIdToUpdate: String, newUiIndex: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(favoriteEntityStationList: List<FavoriteEntityStation>)

    @Query("DELETE FROM FavoriteEntityStation WHERE id = :favoriteId")
    fun deleteOne(favoriteId: String)

    //TODO: a more complex query that can be returned as a LiveData<FavoriteEntityBase> ?
    @Query("SELECT * FROM FavoriteEntityStation WHERE id = :favoriteId")
    fun getForId(favoriteId: String): FavoriteEntityStation?

    @get:Query("SELECT COUNT(*) FROM FavoriteEntityStation")
    val count: Int

    @Query("SELECT COUNT(*) FROM FavoriteEntityStation WHERE id <> :favoriteIdToExclude")
    fun validFavoriteCount(favoriteIdToExclude: String): Int

    @Query("SELECT COUNT(*) FROM FavoriteEntityStation WHERE id = :favoriteIdToCheck")
    fun isFavoriteId(favoriteIdToCheck: String): Int

    /*@Query("DELETE FROM favoriteentitystation")
    void deleteAll();*/

}
