package com.ludoscity.findmybikes.helpers;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.ludoscity.findmybikes.data.database.FindMyBikesDatabase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;


/**
 * Created by F8Full on 2017-12-30. This file is part of #findmybikes
 * Instrumented test that checks if database format after migration checkout with the corresponding schema file
 * It inserts some data in previous format and then use a magig helper provided by Google to validate the migration
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                FindMyBikesDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate1To2() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        db.execSQL("INSERT into favoriteentitystation(id, displayNameIsDefault, display_name)"
                +"VALUES('TEST_ID_0', 'true', 'display_name_default_true')"
                );

        db.execSQL("INSERT into favoriteentitystation(id, displayNameIsDefault, display_name)"
                +"VALUES('TEST_ID_1', 'false', 'display_name_default_false')"
        );

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, FindMyBikesDatabase.Companion.getMIGRATION_1_2());

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    public void migrate2To3() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // db has schema version 2. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name)"
                +"VALUES('TEST_ID_0', 'Custom', 'Default')"
        );

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name)"
                +"VALUES('TEST_ID_1', NULL , 'Default')"
        );

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, FindMyBikesDatabase.Companion.getMIGRATION_2_3());

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    public void migrate3To4() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);

        // db has schema version 3. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name, ui_index)"
                +"VALUES('TEST_ID_0', 'Custom', 'Default', '0')"
        );

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name, ui_index)"
                +"VALUES('TEST_ID_1', NULL , 'Default', '1')"
        );

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, FindMyBikesDatabase.Companion.getMIGRATION_3_4());
        // MigrationTestHelper automatically verifies the schema changes

        db.execSQL("INSERT into favoriteentityplace(id, custom_name, default_name, ui_index, location, attributions)"
                +"VALUES('TEST_ID_1', NULL , 'Default', '2', '45.508897|-73.554279', 'Attributions string')"
        );

        db.close();

    }

    @Test
    public void migrate4To5() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 4);

        // db has schema version 4. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.

        db.execSQL("INSERT into favoriteentityplace(id, custom_name, default_name, ui_index, location, attributions)"
                +"VALUES('TEST_ID_0', NULL , 'Default', '0', '45.508897|-73.554279', 'Attributions string')"
        );

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name, ui_index)"
                +"VALUES('TEST_ID_1', 'Custom', 'Default', '1')"
        );

        db.execSQL("INSERT into favoriteentitystation(id, custom_name, default_name, ui_index)"
                +"VALUES('TEST_ID_2', NULL , 'Default', '2')"
        );

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 5 and provide
        // MIGRATION_4_5 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, FindMyBikesDatabase.Companion.getMIGRATION_4_5());
        // MigrationTestHelper automatically verifies the schema changes

        db.close();

    }
}