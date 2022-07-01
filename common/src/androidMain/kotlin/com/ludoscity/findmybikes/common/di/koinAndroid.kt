package com.ludoscity.findmybikes.common.di

import com.ludoscity.findmybikes.FindmybikesBuildKonfig
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {

    single<SqlDriver> {
        AndroidSqliteDriver(
            FindmybikesDatabase.Schema,
            get(),
            FindmybikesBuildKonfig.DATABASE_NAME
        )
    }

    /*single { SecureDataStore(get()) }
    val baseKermit = Kermit(LogcatLogger()).withTag("KampKit")
    factory { (tag: String?) -> if (tag != null) baseKermit.withTag(tag) else baseKermit }*/
}
