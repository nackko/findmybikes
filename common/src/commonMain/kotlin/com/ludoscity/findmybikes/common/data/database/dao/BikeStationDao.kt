package com.ludoscity.findmybikes.common.data.database.dao

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow


class BikeStationDao(database: FindmybikesDatabase) {

    private val db = database.bikeStationQueries

    internal fun selectAll(): Flow<List<BikeStation>> {
        return db.getAll().asFlow().mapToList()
    }

    internal fun insert(item: BikeStation) = db.insertOrReplace(item)

    /*val players = listOf<Player>()
    database.playerQueries.transaction {
        players.forEach { player ->
            database.playerQueries.insert(
                player_number = player.number,
                full_name = player.fullName
            )
        }
    }*/
    internal fun insertList(itemList: List<BikeStation>) {
        db.transaction {
            itemList.forEach { station ->
                db.insertOrReplace(station)
            }
        }
    }

    internal fun updateBikeStation(station: BikeStation) {
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

    internal fun updateBikeStationList(stationList: List<BikeStation>) {

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

    internal fun deleteBikeStationList(stationList: List<BikeStation>) {
        stationList.forEach { station ->
            db.deleteById(station.uid)
        }
    }

    internal fun deleteAllBikeStation() = db.deleteAll()

    internal fun getById(uid: String) = db.getById(uid)
}
