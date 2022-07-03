package com.ludoscity.findmybikes.common.data.datasource

import com.ludoscity.findmybikes.common.data.BikeSystem

interface RemoteDataApi {

    suspend fun retrieveBikeSystemList(): Result<List<BikeSystem>>
}
