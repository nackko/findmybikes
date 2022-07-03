package com.ludoscity.findmybikes.common.data.datasource

import com.ludoscity.findmybikes.common.data.BikeSystem
import kotlinx.coroutines.flow.Flow

interface RemoteDataSource {

    val bikeSystemListFlow: Flow<List<BikeSystem>>
    fun setRefreshDelay(delayMs: Long)
    fun fetchBikeSystemStatus(/*citybik_esAPI: Citybik_esAPI,*/bikeSystemHRef: String?)
}