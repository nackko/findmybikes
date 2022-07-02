package com.ludoscity.findmybikes.common.data.dbdriver

import android.content.Context
import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver


actual class DbArgs(
    var context: Context
)

actual fun getSqlDriver(dbArgs: DbArgs): SqlDriver? {
    val driver: SqlDriver = AndroidSqliteDriver(FindmybikesDatabase.Schema, dbArgs.context, "findmybikes.db")
    //return driver
    return null
}