package com.ludoscity.findmybikes.common.data.datasource.dao

import com.ludoscity.findmybikes.common.data.BikeSystem
import kotlinx.coroutines.flow.Flow


abstract interface  BikeSystemDao {

    fun selectAll(): Flow<List<BikeSystem>>

    fun insert(item: BikeSystem)

    fun getHashtaggableName(id: String): String

    fun getById(id: String): BikeSystem

    fun count(): Int

    fun updateLastUpdateTimestamp(id:String, newUpdateTimestamp: Long)

    fun updateBoundingBox(
        id: String,
        bBoxNELat: Double,
        bBoxNELng: Double,
        bBoxSWLat: Double,
        bBoxSWLng: Double
    )
}
