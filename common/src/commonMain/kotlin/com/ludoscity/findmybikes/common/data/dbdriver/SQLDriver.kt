package com.ludoscity.findmybikes.common.data.dbdriver

import com.ludoscity.findmybikes.common.data.database.FindmybikesDatabase
import com.squareup.sqldelight.db.SqlDriver

expect class DbArgs

expect fun getSqlDriver(dbArgs: DbArgs): SqlDriver?

object DatabaseCreator {
    fun getDataBase(dbArgs: DbArgs): FindmybikesDatabase? {
        val sqlDriver = getSqlDriver(dbArgs)
        return if (sqlDriver != null) {
            FindmybikesDatabase(sqlDriver)
        } else {
            null
        }
    }
}