package com.ludoscity.findmybikes.data.database.tracking

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Created by F8Full on 2019-06-24. This file is part of #findmybikes
 * A data access object interface class for Room to manipulate GeoTrackingDatapoint table
 */
@Dao
interface GeoTrackingDao {
    @Insert
    fun insert(record: GeoTrackingDatapoint)

    @Update
    fun update(record: GeoTrackingDatapoint)

    @Query("DELETE FROM geotrackingdatapoint")
    fun deleteAll()

    @Query("SELECT * from geotrackingdatapoint ORDER BY id ASC")
    fun getAllList(): LiveData<List<GeoTrackingDatapoint>>

    @Query("SELECT * from geotrackingdatapoint WHERE upload_completed='0'")
    fun getNonUploadedList(): LiveData<List<GeoTrackingDatapoint>>
}