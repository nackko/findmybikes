package com.ludoscity.findmybikes.common.data.datasource

interface RemoteDataSource {
    fun fetchBikeSystemList(/*citybik_esAPI: Citybik_esAPI*/)
    fun fetchBikeSystemStatus(/*citybik_esAPI: Citybik_esAPI,*/bikeSystemHRef: String?)
}