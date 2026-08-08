package com.example.afetcomms.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS members (
                    userId TEXT NOT NULL PRIMARY KEY,
                    displayName TEXT NOT NULL,
                    familyId TEXT NOT NULL,
                    role TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_profile (
                    profileId INTEGER NOT NULL PRIMARY KEY,
                    accountRole TEXT NOT NULL,
                    firstName TEXT NOT NULL,
                    lastName TEXT NOT NULL,
                    userId TEXT NOT NULL,
                    familyId TEXT NOT NULL,
                    organizationName TEXT,
                    displayName TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS families (
                    familyCode TEXT NOT NULL PRIMARY KEY,
                    inviteToken TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    createdByUserId TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS members_new (
                    userId TEXT NOT NULL PRIMARY KEY,
                    userCode TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    familyId TEXT NOT NULL,
                    relationRole TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO members_new (userId, userCode, displayName, familyId, relationRole)
                SELECT
                    userId,
                    userId,
                    displayName,
                    familyId,
                    CASE
                        WHEN role IN ('Anne', 'ANNE') THEN 'ANNE'
                        WHEN role IN ('Baba', 'BABA') THEN 'BABA'
                        WHEN role IN ('Çocuk', 'Cocuk', 'COCUK') THEN 'COCUK'
                        WHEN role IN ('Kardeş', 'Kardes', 'KARDES') THEN 'KARDES'
                        ELSE 'DIGER'
                    END
                FROM members
                """.trimIndent()
            )
            db.execSQL("DROP TABLE members")
            db.execSQL("ALTER TABLE members_new RENAME TO members")
            db.execSQL(
                """
                ALTER TABLE user_profile ADD COLUMN userCode TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE user_profile ADD COLUMN memberRelation TEXT NOT NULL DEFAULT 'DIGER'
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE messages ADD COLUMN senderType TEXT NOT NULL DEFAULT 'CITIZEN'
                """.trimIndent()
            )
            db.execSQL(
                """
                ALTER TABLE messages ADD COLUMN senderDisplayName TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE messages ADD COLUMN rescuerId TEXT")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE members ADD COLUMN connectionStatus TEXT NOT NULL DEFAULT 'AWAY'
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE members ADD COLUMN lastLatitude REAL")
            db.execSQL("ALTER TABLE members ADD COLUMN lastLongitude REAL")
            db.execSQL("ALTER TABLE members ADD COLUMN lastLocationAtMillis INTEGER")
            db.execSQL(
                """
                ALTER TABLE members ADD COLUMN activeSos INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE members ADD COLUMN lastSeenAtMillis INTEGER")
        }
    }
}
