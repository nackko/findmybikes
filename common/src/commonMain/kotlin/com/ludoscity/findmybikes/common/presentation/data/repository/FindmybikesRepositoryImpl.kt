package com.ludoscity.findmybikes.common.presentation.data.repository

import com.ludoscity.findmybikes.common.data.BikeStation
import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.datasource.LocalDataStore
import com.ludoscity.findmybikes.common.data.datasource.RemoteDataSource
import com.ludoscity.findmybikes.common.domain.repository.FindmybikesRepositoryNew
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// This should be split into multiple repos. BikeNetwork repo, BikeStation repo, Favorites
class FindmybikesRepositoryImpl : FindmybikesRepositoryNew, KoinComponent  {

    private val localDataStore: LocalDataStore by inject()
    private val remoteDataSource: RemoteDataSource by inject()

    override val bikeSystemList: Flow<List<BikeSystem>> = remoteDataSource.bikeSystemListFlow
    // Intermediate operation to filter the list of favorite topics
    //.map { news -> news.filter { userData.isFavoriteTopic(it) } }
    // Intermediate operation to save the latest news in the cache
    //.onEach { news -> saveInCache(news) }

    override fun setRefreshDelay(newDelayMs: Long) {
        remoteDataSource.setRefreshDelay(newDelayMs)
    }

    override fun getStationForId(targetId: String): BikeStation {
        TODO("Not yet implemented")
    }

    override fun hasAtLeastNValidStationFavorites(nearestStationId: String, n: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBikeSystemStationData(): Flow<List<BikeStation>> {
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
