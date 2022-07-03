package com.ludoscity.findmybikes.common.presentation.data.datasource.remote

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.datasource.RemoteDataSource

import com.ludoscity.findmybikes.common.data.datasource.RemoteDataApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteDataSourceImpl: RemoteDataSource, KoinComponent {

    private val dataAPi: RemoteDataApi by inject()

    private var refreshIntervalMs: Long = 1000

    override val bikeSystemListFlow: Flow<List<BikeSystem>> = flow {
        while(false) {
            dataAPi.retrieveBikeSystemList()
                .onSuccess {
                    emit(it)
                }
                .onFailure {
                    //throw it
                }

            delay(refreshIntervalMs) // Suspends the coroutine for some time
        }
    }

    override fun setRefreshDelay(delayMs: Long) {
        refreshIntervalMs = delayMs
    }

    override fun fetchBikeSystemStatus(bikeSystemHRef: String?) {
        TODO("Not yet implemented")
    }
}