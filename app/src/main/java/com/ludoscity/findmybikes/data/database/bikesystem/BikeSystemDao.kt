package com.ludoscity.findmybikes.data.database.bikesystem

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

/**
 * Created by F8Full on 2019-03-24.
 * Room retains the currently used bike system
 */
@Dao
interface BikeSystemDao {

    @get:Query("SELECT * FROM bikeSystem")
    val single: LiveData<BikeSystem>

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