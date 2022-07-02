package com.ludoscity.findmybikes.common.presentation.data.datasource.dao

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeSystemDao
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow


class BikeSystemDaoImpl(database: FindmybikesDatabase): BikeSystemDao {

    private val db = database.bikeSystemQueries

    override fun selectAll(): Flow<List<BikeSystem>> {
        return db.getAll().asFlow().mapToList()
    }

    override fun insert(item: BikeSystem) {
        db.insertOrReplace(item)
    }

    override fun getHashtaggableName(id: String): String {
        return db.getHashtaggableNameById(id).executeAsOne()
    }

    override fun getById(id: String): BikeSystem {
        return db.getById(id).executeAsOne()
    }

    override fun count(): Int {
        return db.idCount().executeAsOne().toInt()
    }

    override fun updateLastUpdateTimestamp(id:String, newUpdateTimestamp: Long) {
        db.updateLastUpdateTimestamp(newUpdateTimestamp, id)
    }

    override fun updateBoundingBox(id: String,
                                   bBoxNELat: Double,
                                   bBoxNELng: Double,
                                   bBoxSWLat: Double,
                                   bBoxSWLng: Double) {
        db.updateBounds(bBoxNELat, bBoxNELng, bBoxSWLat, bBoxSWLng, id)
    }
}
