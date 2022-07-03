package com.ludoscity.findmybikes.common.domain.repository

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.BikeSystem
import kotlinx.coroutines.flow.Flow

abstract interface FindmybikesRepositoryNew {

    val bikeSystemList: Flow<List<BikeSystem>>

    fun setRefreshDelay(newDelayMs: Long)

    fun getStationForId(targetId: String): BikeStation

    fun hasAtLeastNValidStationFavorites(nearestStationId: String, n: Int): Boolean

    fun getBikeSystemStationData(): Flow<List<BikeStation>>

    fun getCurrentBikeSystem(): Flow<BikeSystem>

    fun setCurrentBikeSystem(toSet: BikeSystem, alsoFetchStatus: Boolean = false)

    fun invalidateCurrentBikeSystem(/*ctx: Context*/)

    fun invalidateBikeSystemStatus(/*ctx: Context,*/systemHRef: String)

    fun getHashtaggableCurBikeSystemName(): String
}
