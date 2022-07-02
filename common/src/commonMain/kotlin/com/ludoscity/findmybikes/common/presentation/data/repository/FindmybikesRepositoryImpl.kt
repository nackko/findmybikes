package com.ludoscity.findmybikes.common.presentation.data.repository

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.datasource.LocalDataStore
import com.ludoscity.findmybikes.common.domain.repository.FindmybikesRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// This should be split into multiple repos. BikeNetwork repo, BikeStation repo, Favorites
class FindmybikesRepositoryImpl() : KoinComponent, FindmybikesRepository {

     private val localDataStore: LocalDataStore by inject()

    override fun getStationForId(targetId: String): BikeStation {
        TODO("Not yet implemented")
    }

    override fun hasAtLeastNValidStationFavorites(nearestStationId: String, n: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBikeSystemStationData(): Flow<List<BikeStation>> {
        TODO("Not yet implemented")
    }

    override fun getBikeSystemListData(): Flow<List<BikeSystem>> {
        TODO("Not yet implemented")
    }

    override fun getCurrentBikeSystem(): Flow<BikeSystem> {
        TODO("Not yet implemented")
    }

    override fun setCurrentBikeSystem(toSet: BikeSystem, alsoFetchStatus: Boolean) {
        TODO("Not yet implemented")
    }

    override fun invalidateCurrentBikeSystem() {
        TODO("Not yet implemented")
    }

    override fun invalidateBikeSystemStatus(systemHRef: String) {
        TODO("Not yet implemented")
    }

    override fun getHashtaggableCurBikeSystemName(): String {
        TODO("Not yet implemented")
    }

    //suspend fun insertBikeStation
}
