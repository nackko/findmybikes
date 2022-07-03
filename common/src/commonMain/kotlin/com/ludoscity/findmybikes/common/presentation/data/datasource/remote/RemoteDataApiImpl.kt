package com.ludoscity.findmybikes.common.presentation.data.datasource.remote

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.data.datasource.RemoteDataApi
import com.ludoscity.findmybikes.common.presentation.data.datasource.remote.citybik_es.BikeSystemListAnswerRoot
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.util.date.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteDataApiImpl : RemoteDataApi, KoinComponent {

    private val httpClient: HttpClient by inject()

    override suspend fun retrieveBikeSystemList(): Result<List<BikeSystem>> {

        val toReturn: MutableList<BikeSystem> = ArrayList()

        return try {

            val reply: BikeSystemListAnswerRoot =
                httpClient.get("https://api.citybik.es/v2/networks") {
                    //parameter("Path", fullPathWithSlashes)
                }.body()

            reply.networks?.forEach {
                toReturn.add(
                    BikeSystem(
                        it.id,
                        getTimeMillis(),
                        it.href,
                        it.name,
                        "superHashtag",
                        it.location.latitude,
                        it.location.longitude,
                        it.location.city,
                        "country",
                        null,
                        null,
                        null,
                        null
                    )
                )
            }
            Result.success(toReturn)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
