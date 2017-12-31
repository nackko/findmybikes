package com.ludoscity.findmybikes.helpers;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import com.ludoscity.findmybikes.citybik_es.model.BikeStation;
import com.ludoscity.findmybikes.datamodel.FavoriteEntityStation;

/**
 * Created by F8Full on 2017-12-17.
 * This file is part of #findmybikes
 */
@Database(entities = {BikeStation.class, FavoriteEntityStation.class}, version = 2)
//@TypeConverters({LatLngTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract BikeStationDao bikeStationDao();
    public abstract FavoriteEntityStationDao favoriteEntityStationDao();
    //public abstract FavoriteEntityPlaceDao favoriteEntityPlaceDao();

    static final Migration MIGRATION_1_2 = new Migration(1,2){

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //do the SQL
            //from
            //https://blog.xojo.com/2013/12/04/renaming-columns-in-sqlite-tables/
            database.execSQL("ALTER TABLE FavoriteEntityStation RENAME TO FavoriteEntityStation_orig");
            database.execSQL("CREATE TABLE IF NOT EXISTS FavoriteEntityStation (`id` TEXT NOT NULL, `custom_name` TEXT, `default_name` TEXT, PRIMARY KEY(`id`))");
            database.execSQL("INSERT INTO FavoriteEntityStation('id', 'default_name') SELECT id, display_name FROM FavoriteEntityStation_orig");
            database.execSQL("DROP TABLE FavoriteEntityStation_orig");

            //After migration all custom_name column values are NULL. A more refined migration would have read the boolean values in the v1 table and used it to generate variable SQL queries for insert in v2 table
            //TODO: you still have to check the data migration (using SQL)
        }
    };
}
