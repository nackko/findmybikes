package com.ludoscity.findmybikes.common.data.datasource.dao

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow


abstract interface BikeStationDao {

    fun selectAll(): Flow<List<BikeStation>>

    fun insert(item: BikeStation)

    fun insertList(itemList: List<BikeStation>)

    fun update(station: BikeStation)

    fun updateList(stationList: List<BikeStation>)

    fun deleteList(stationList: List<BikeStation>)

    fun deleteAll()

    fun getById(uid: String): BikeStation
}
