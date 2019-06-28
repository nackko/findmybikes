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
interface AnalTrackingDao {
    @Insert
    fun insert(record: AnalTrackingDatapoint)

    @Update
    fun update(record: AnalTrackingDatapoint)

    @Query("DELETE FROM analtrackingdatapoint")
    fun deleteAll()

    @Query("SELECT * from analtrackingdatapoint ORDER BY id ASC")
    fun getAllList(): LiveData<List<AnalTrackingDatapoint>>

    @Query("SELECT * from analtrackingdatapoint WHERE upload_completed='0'")
    fun getNonUploadedList(): List<AnalTrackingDatapoint>
}