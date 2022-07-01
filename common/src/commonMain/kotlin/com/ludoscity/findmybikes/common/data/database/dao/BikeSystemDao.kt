package com.ludoscity.findmybikes.common.data.database.dao

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow


class BikeSystemDao(database: FindmybikesDatabase) {

    private val db = database.bikeSystemQueries

    internal fun selectAll(): Flow<List<BikeSystem>> {
        return db.getAll().asFlow().mapToList()
    }

    internal fun insert(item: BikeSystem) {
        db.insertOrReplace(item)
    }

    internal fun gethashtaggableName(): String {
        return db.getHashtaggableName().executeAsOne()
    }

    internal fun getById(id: String): BikeSystem {
        return db.getById(id).executeAsOne()
    }

    internal fun count(id: String): Int {
        return db.idCount().executeAsOne().toInt()
    }

    internal fun updateLastUpdateTimestamp(id:String, newUpdateTimestamp: Long) {
        db.updateLastUpdateTimestamp(newUpdateTimestamp, id)
    }

    internal fun updateBoundingBox(id: String,
                                   bBoxNELat: Double,
                                   bBoxNELng: Double,
                                   bBoxSWLat: Double,
                                   bBoxSWLng: Double) {
        db.updateBounds(bBoxNELat, bBoxNELng, bBoxSWLat, bBoxSWLng, id)
    }
}
