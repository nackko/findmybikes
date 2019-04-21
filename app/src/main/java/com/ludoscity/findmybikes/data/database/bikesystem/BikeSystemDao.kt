package com.ludoscity.findmybikes.data.database.bikesystem

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Created by F8Full on 2019-03-24.
 * Room retains the currently used bike system
 */
@Dao
interface BikeSystemDao {

    @get:Query("SELECT * FROM bikeSystem")
    val single: LiveData<BikeSystem>

    @get:Query("SELECT * FROM bikeSystem")
    val singleSynchronous: BikeSystem

    @get:Query("SELECT id FROM BikeSystem")
    val singleIdSynchronous: String

    @get:Query("SELECT hashtaggableName FROM BikeSystem")
    val singleNameForHashtagUse: String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCurrentBikeSystem(bikeSystem: BikeSystem)

    @Query("DELETE FROM bikeSystem")
    fun deleteCurrentBikeSystem()

    @Query("SELECT COUNT(id) FROM bikeSystem")
    fun countCurrentBikeSystem(): Int

    @Query("UPDATE bikeSystem SET last_status_update = :newUpdateTimestamp WHERE id = :id")
    fun updateLastUpdateTimestamp(id: String, newUpdateTimestamp: Long)

    @Query("UPDATE bikeSystem SET bbox_north_east_lat =:bBoxNELat, bbox_north_east_lng = :bBoxNELng, bbox_south_west_lat =:bBoxSWLat, bbox_south_west_lng = :bBoxSWLng WHERE id = :id")
    fun updateBounds(id: String, bBoxNELat: Double, bBoxNELng: Double, bBoxSWLat: Double, bBoxSWLng: Double)
}