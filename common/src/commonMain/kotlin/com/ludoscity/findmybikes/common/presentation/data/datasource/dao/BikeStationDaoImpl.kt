package com.ludoscity.findmybikes.common.presentation.data.datasource.dao

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeStationDao
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow


class BikeStationDaoImpl(database: FindmybikesDatabase): BikeStationDao {

    private val db = database.bikeStationQueries

    override fun selectAll(): Flow<List<BikeStation>> {
        return db.getAll().asFlow().mapToList()
    }

    override fun insert(item: BikeStation) = db.insertOrReplace(item)

    /*val players = listOf<Player>()
    database.playerQueries.transaction {
        players.forEach { player ->
            database.playerQueries.insert(
                player_number = player.number,
                full_name = player.fullName
            )
        }
    }*/
    override fun insertList(itemList: List<BikeStation>) {
        db.transaction {
            itemList.forEach { station ->
                db.insertOrReplace(station)
            }
        }
    }

    override fun update(station: BikeStation) {
        with(station) {
            db.updateById(
                empty_slots,
                free_bikes,
                latitude,
                longitude,
                name,
                timestamp_epoch,
                location_hash,
                extra_locked,
                extra_name,
                extra_uid,
                extra_renting,
                extra_returning,
                extra_status,
                extra_lastUpdated,
                uid
            )
        }
    }

    override fun updateList(stationList: List<BikeStation>) {

        stationList.forEach { station ->
            with(station) {
                db.updateById(
                    empty_slots,
                    free_bikes,
                    latitude,
                    longitude,
                    name,
                    timestamp_epoch,
                    location_hash,
                    extra_locked,
                    extra_name,
                    extra_uid,
                    extra_renting,
                    extra_returning,
                    extra_status,
                    extra_lastUpdated,
                    uid
                )
            }

        }
    }

    override fun deleteList(stationList: List<BikeStation>) {
        stationList.forEach { station ->
            db.deleteById(station.uid)
        }
    }

    override fun deleteAll() = db.deleteAll()

    override fun getById(uid: String) = db.getById(uid).executeAsOne()
}
