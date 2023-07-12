/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.autofill.store

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.duckduckgo.autofill.store.sync.LoginCredentialsSync
import com.duckduckgo.autofill.store.sync.LoginCredentialsSyncDao

@Database(
    exportSchema = true,
    version = 2,
    entities = [
        AutofillExceptionEntity::class,
        LoginCredentialsSync::class,
    ],
)
abstract class AutofillDatabase : RoomDatabase() {
    abstract fun autofillDao(): AutofillDao
    abstract fun syncLoginCredentialsDao(): LoginCredentialsSyncDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `website_login_credentials_sync_meta` (`syncId` TEXT NOT NULL, `id` INTEGER NOT NULL, `deleted_at` TEXT, PRIMARY KEY(`syncId`))",
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
