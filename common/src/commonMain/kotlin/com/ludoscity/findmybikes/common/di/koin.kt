package com.ludoscity.findmybikes.common.di

import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.ludoscity.findmybikes.common.data.database.dao.BikeStationDao
import com.ludoscity.findmybikes.common.data.database.dao.BikeSystemDao
import com.ludoscity.findmybikes.common.data.repository.FindmybikesRepositorySqlDelight
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
    single { FindmybikesDatabase(get()) }

    //dao
    single { BikeSystemDao(get()) }
    single { BikeStationDao(get()) }
    // repo
    single { FindmybikesRepositorySqlDelight() }
}

internal inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}

expect val platformModule: Module
