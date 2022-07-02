package com.ludoscity.findmybikes.common.data.datasource

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeStationDao
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeSystemDao
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocalDataStore: KoinComponent {

    private val bikeSystemDao: BikeSystemDao by inject()
    private val bikeStationDao: BikeStationDao by inject()

    internal fun getBikeSystemAll(): Flow<List<BikeSystem>> = bikeSystemDao.selectAll()

    internal fun addBikeSystem(toAdd: BikeSystem) = bikeSystemDao.insert(toAdd)

    internal fun getBikeSystemHashtaggableNameById(id: String): String =
        bikeSystemDao.getHashtaggableName(id)

    internal fun getBikeSystemById(id: String): BikeSystem = bikeSystemDao.getById(id)

    internal fun countBikeSystem(): Int = bikeSystemDao.count()

    internal fun updateBikeSystemLastUpdateTimestamp(id: String, newUpdateTimestamp: Long) =
        bikeSystemDao.updateLastUpdateTimestamp(id, newUpdateTimestamp)

    internal fun updateBikeSystemBoundingBox(
        id: String,
        bBoxNELat: Double,
        bBoxNELng: Double,
        bBoxSWLat: Double,
        bBoxSWLng: Double
    ) = bikeSystemDao.updateBoundingBox(id, bBoxNELat, bBoxNELng, bBoxSWLat, bBoxSWLng)

    internal fun getBikeStationAll(): Flow<List<BikeStation>> = bikeStationDao.selectAll()

    internal fun addBikeStation(toAdd: BikeStation) = bikeStationDao.insert(toAdd)

    internal fun addBikeStationList(itemList: List<BikeStation>) =
        bikeStationDao.insertList(itemList)

    internal fun updateBikeStation(station: BikeStation) = bikeStationDao.update(station)

    internal fun updateBikeStationList(stationList: List<BikeStation>) =
        bikeStationDao.updateList(stationList)

    internal fun deleteBikeStationList(stationList: List<BikeStation>) =
        bikeStationDao.deleteList(stationList)

    internal fun deleteAllBikeStation() = bikeStationDao.deleteAll()

    internal fun getBikeStationById(uid: String): BikeStation = bikeStationDao.getById(uid)
}