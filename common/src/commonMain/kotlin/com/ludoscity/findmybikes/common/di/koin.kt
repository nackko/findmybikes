package com.ludoscity.findmybikes.common.di

import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.ludoscity.findmybikes.common.data.datasource.LocalDataStore
import com.ludoscity.findmybikes.common.data.datasource.RemoteDataApi
import com.ludoscity.findmybikes.common.data.datasource.RemoteDataSource
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeStationDao
import com.ludoscity.findmybikes.common.data.datasource.dao.BikeSystemDao
import com.ludoscity.findmybikes.common.domain.repository.FindmybikesRepositoryNew
import com.ludoscity.findmybikes.common.domain.usecase.GetBikeSystemList
import com.ludoscity.findmybikes.common.domain.usecase.SetRefreshDelay
import com.ludoscity.findmybikes.common.presentation.data.datasource.local.dao.BikeStationDaoImpl
import com.ludoscity.findmybikes.common.presentation.data.datasource.local.dao.BikeSystemDaoImpl
import com.ludoscity.findmybikes.common.presentation.data.datasource.remote.RemoteDataApiImpl
import com.ludoscity.findmybikes.common.presentation.data.datasource.remote.RemoteDataSourceImpl
import com.ludoscity.findmybikes.common.presentation.data.repository.FindmybikesRepositoryImpl
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import org.koin.dsl.module

fun initKoin(appModule: Module): KoinApplication {
    @Suppress("UnnecessaryVariable") val koinApp = startKoin {
        modules(
            appModule,
            platformModule,
            coreModule
        )
    }

    //TODO: additional koin app configuration can happen here
    //see: https://github.com/touchlab/KaMPKit/blob/0b1a956b3a0c1ee417916e3835062f55eaba78b3/shared/src/commonMain/kotlin/co/touchlab/kampkit/Koin.kt

    return koinApp
}

private val coreModule = module {

    // use cases
    single { GetBikeSystemList() }
    single { SetRefreshDelay() }


    // data
    single { FindmybikesDatabase(get()) }
    single { LocalDataStore() }
    //dao
    single<BikeSystemDao> { BikeSystemDaoImpl(get()) }
    single<BikeStationDao> { BikeStationDaoImpl(get()) }

    single<RemoteDataApi> { RemoteDataApiImpl() }
    single<RemoteDataSource> { RemoteDataSourceImpl() }
    // repo
    single<FindmybikesRepositoryNew> { FindmybikesRepositoryImpl() }


    //network
    single {
        HttpClient {
            install(ContentNegotiation) {

                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }
    }
}

internal inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}

expect val platformModule: Module
