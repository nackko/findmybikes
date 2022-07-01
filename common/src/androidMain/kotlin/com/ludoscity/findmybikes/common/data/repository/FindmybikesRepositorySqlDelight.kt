package com.ludoscity.findmybikes.common.data.repository

import com.ludoscity.findmybikes.common.data.database.dao.BikeStationDao
import com.ludoscity.findmybikes.common.data.database.dao.BikeSystemDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// This should be split into multiple repos. BikeNetwork repo, BikeStation repo, Favorites
class FindmybikesRepositorySqlDelight() : KoinComponent {

    private val bikeSystemDao: BikeSystemDao by inject()
    private val bikeStationDao: BikeStationDao by inject()

    //suspend fun insertBikeStation
}
