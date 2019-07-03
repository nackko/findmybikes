package com.ludoscity.findmybikes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ludoscity.findmybikes.BuildConfig
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystem
import com.ludoscity.findmybikes.data.database.bikesystem.BikeSystemDao
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityPlace
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityPlaceDao
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityStation
import com.ludoscity.findmybikes.data.database.favorite.FavoriteEntityStationDao
import com.ludoscity.findmybikes.data.database.station.BikeStation
import com.ludoscity.findmybikes.data.database.station.BikeStationDao
import com.ludoscity.findmybikes.data.database.tracking.AnalTrackingDao
import com.ludoscity.findmybikes.data.database.tracking.AnalTrackingDatapoint
import com.ludoscity.findmybikes.data.database.tracking.GeoTrackingDao
import com.ludoscity.findmybikes.data.database.tracking.GeoTrackingDatapoint

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Database(entities = [BikeSystem::class, BikeStation::class,
    FavoriteEntityStation::class, FavoriteEntityPlace::class,
    GeoTrackingDatapoint::class, AnalTrackingDatapoint::class], version = 13)
@TypeConverters(LatLngTypeConverter::class)
abstract class FindMyBikesDatabase : RoomDatabase() {
    abstract fun bikeSystemDao(): BikeSystemDao
    abstract fun bikeStationDao(): BikeStationDao
    abstract fun favoriteEntityStationDao(): FavoriteEntityStationDao
    abstract fun favoriteEntityPlaceDao(): FavoriteEntityPlaceDao
    abstract fun geoTrackingDao(): GeoTrackingDao
    abstract fun analTrackingDao(): AnalTrackingDao

    companion object {

        private val TAG = FindMyBikesDatabase::class.java.simpleName

        // For Singleton instantiation
        @Volatile
        private var INSTANCE: FindMyBikesDatabase? = null

        fun getDatabase(ctx: Context): FindMyBikesDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val builder = Room.databaseBuilder(ctx.applicationContext,
                        FindMyBikesDatabase::class.java, BuildConfig.DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .addMigrations(MIGRATION_7_8)
                        .addMigrations(MIGRATION_8_9)
                        .addMigrations(MIGRATION_9_10)
                        .addMigrations(MIGRATION_10_11)
                        .addMigrations(MIGRATION_11_12)
                        .addMigrations(MIGRATION_12_13)

                if (BuildConfig.DEBUG)
                    builder.setJournalMode(JournalMode.TRUNCATE)

                val instance = builder.build()

                INSTANCE = instance
                return instance
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {
                //do the SQL
                //from
                //https://blog.xojo.com/2013/12/04/renaming-columns-in-sqlite-tables/
                database.execSQL("ALTER TABLE FavoriteEntityStation RENAME TO FavoriteEntityStation_orig")
                database.execSQL("CREATE TABLE IF NOT EXISTS FavoriteEntityStation (`id` TEXT NOT NULL, `custom_name` TEXT, `default_name` TEXT, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO FavoriteEntityStation('id', 'default_name') SELECT id, display_name FROM FavoriteEntityStation_orig")
                database.execSQL("DROP TABLE FavoriteEntityStation_orig")

                //After migration all custom_name column values are NULL. A more refined migration would have read the boolean values in the v1 table and used it to generate variable SQL queries for insert in v2 table
                //TODO: you still have to check the data migration (using SQL)
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("ALTER TABLE FavoriteEntityStation ADD COLUMN ui_index INTEGER DEFAULT '-1'")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteEntityPlace` (`location` TEXT, `attributions` TEXT, `id` TEXT NOT NULL, `custom_name` TEXT, `default_name` TEXT, `ui_index` INTEGER DEFAULT '-1', PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("ALTER TABLE FavoriteEntityStation ADD COLUMN bike_system_id TEXT DEFAULT 'bixi-montreal'")
                database.execSQL("ALTER TABLE FavoriteEntityPlace ADD COLUMN bike_system_id TEXT DEFAULT 'bixi-montreal'")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE BikeStation")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeStation` (`uid` TEXT NOT NULL, `empty_slots` INTEGER, `free_bikes` INTEGER, `latitude` REAL, `longitude` REAL, `name` TEXT, `timestamp` TEXT, `locationHash` TEXT, `extra_locked` INTEGER, `extra_name` TEXT, `extra_uid` INTEGER, `extra_renting` INTEGER, `extra_returning` INTEGER, `extra_status` TEXT, PRIMARY KEY(`uid`))")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE BikeStation")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeStation` (`uid` TEXT NOT NULL, `empty_slots` INTEGER NOT NULL, `free_bikes` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `name` TEXT, `timestamp` TEXT NOT NULL, `location_hash` TEXT NOT NULL, `extra_locked` INTEGER, `extra_name` TEXT, `extra_uid` INTEGER, `extra_renting` INTEGER, `extra_returning` INTEGER, `extra_status` TEXT, PRIMARY KEY(`uid`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeSystem` (`id` TEXT NOT NULL, `citybik_dot_es_url` TEXT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `city` TEXT NOT NULL, `country` TEXT NOT NULL, `bbox_north_east_lat` REAL, `bbox_north_east_lng` REAL, `bbox_south_west_lat` REAL, `bbox_south_west_lng` REAL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE FavoriteEntityStation")
                database.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteEntityStation` (`id` TEXT NOT NULL, `default_name` TEXT NOT NULL, `custom_name` TEXT NOT NULL, `ui_index` INTEGER NOT NULL, `bike_system_id` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE BikeStation")
                database.execSQL("DROP TABLE BikeSystem")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeStation` (`uid` TEXT NOT NULL, `empty_slots` INTEGER NOT NULL, `free_bikes` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `name` TEXT, `timestamp` TEXT NOT NULL, `location_hash` TEXT NOT NULL, `extra_locked` INTEGER, `extra_name` TEXT, `extra_uid` TEXT, `extra_renting` INTEGER, `extra_returning` INTEGER, `extra_status` TEXT, PRIMARY KEY(`uid`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeSystem` (`id` TEXT NOT NULL, `citybik_dot_es_url` TEXT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `city` TEXT NOT NULL, `country` TEXT NOT NULL, `bbox_north_east_lat` REAL, `bbox_north_east_lng` REAL, `bbox_south_west_lat` REAL, `bbox_south_west_lng` REAL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE BikeStation")
                database.execSQL("DROP TABLE BikeSystem")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeStation` (`uid` TEXT NOT NULL, `empty_slots` INTEGER NOT NULL, `free_bikes` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `name` TEXT, `timestamp` TEXT NOT NULL, `location_hash` TEXT NOT NULL, `extra_locked` INTEGER, `extra_name` TEXT, `extra_uid` TEXT, `extra_renting` INTEGER, `extra_returning` INTEGER, `extra_status` TEXT, `extra_lastUpdated` INTEGER, PRIMARY KEY(`uid`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeSystem` (`id` TEXT NOT NULL, `last_status_update` INTEGER NOT NULL, `citybik_dot_es_url` TEXT NOT NULL, `name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `city` TEXT NOT NULL, `country` TEXT NOT NULL, `bbox_north_east_lat` REAL, `bbox_north_east_lng` REAL, `bbox_south_west_lat` REAL, `bbox_south_west_lng` REAL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("DROP TABLE BikeSystem")
                database.execSQL("CREATE TABLE IF NOT EXISTS `BikeSystem` (`id` TEXT NOT NULL, `last_status_update` INTEGER NOT NULL, `citybik_dot_es_url` TEXT NOT NULL, `name` TEXT NOT NULL, `hashtaggableName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `city` TEXT NOT NULL, `country` TEXT NOT NULL, `bbox_north_east_lat` REAL, `bbox_north_east_lng` REAL, `bbox_south_west_lat` REAL, `bbox_south_west_lng` REAL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `GeoTrackingDatapoint` (`timestamp_epoch` INTEGER NOT NULL, `altitude` REAL, `accuracy_horizontal_meters` REAL NOT NULL, `accuracy_vertical_meters` REAL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `upload_completed` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `AnalTrackingDatapoint` (`app_version` TEXT NOT NULL, `os_version` TEXT NOT NULL, `device_model` TEXT NOT NULL, `language` TEXT NOT NULL, `country` TEXT NOT NULL, `battery_level` INTEGER, `timestamp_epoch` INTEGER NOT NULL, `description` TEXT NOT NULL, `upload_completed` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL)")
            }
        }

        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE AnalTrackingDatapoint")
                database.execSQL("DROP TABLE GeoTrackingDatapoint")
                database.execSQL("CREATE TABLE IF NOT EXISTS `AnalTrackingDatapoint` (`app_version` TEXT NOT NULL, `api_level` INTEGER NOT NULL, `device_model` TEXT NOT NULL, `language` TEXT NOT NULL, `country` TEXT NOT NULL, `battery_level` INTEGER, `timestamp_epoch` INTEGER NOT NULL, `description` TEXT NOT NULL, `upload_completed` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `GeoTrackingDatapoint` (`timestamp_epoch` INTEGER NOT NULL, `altitude` REAL, `accuracy_horizontal_meters` REAL NOT NULL, `accuracy_vertical_meters` REAL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `upload_completed` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL)")
            }
        }
    }
}
