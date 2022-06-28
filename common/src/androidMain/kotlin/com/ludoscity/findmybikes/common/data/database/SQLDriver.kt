package com.ludoscity.findmybikes.common.data.database

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase


actual class DbArgs(
    var context: Context
)

actual fun getSqlDriver(dbArgs: DbArgs): SqlDriver? {
    val driver: SqlDriver = AndroidSqliteDriver(FindmybikesDatabase.Schema, dbArgs.context, "findmybikes.db")
    //return driver
    return null
}