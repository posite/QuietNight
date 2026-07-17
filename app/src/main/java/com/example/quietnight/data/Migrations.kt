package com.example.quietnight.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5: SleepSession 에서 positionStatsJson(가짜 자세 통계) 제거, timelineCsv(밤새 dB 추이) 추가.
 * SQLite 의 컬럼 삭제 호환성을 위해 새 스키마로 테이블을 재생성하고 기존 데이터를 복사한다.
 * 새 테이블 정의는 Room 이 기대하는 5.json 스키마와 정확히 일치해야 한다.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_sessions_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`score` INTEGER NOT NULL, " +
                "`snoreTime` INTEGER NOT NULL, " +
                "`sleepTime` INTEGER NOT NULL, " +
                "`timelineCsv` TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO `sleep_sessions_new` " +
                "(`id`, `date`, `score`, `snoreTime`, `sleepTime`, `timelineCsv`) " +
                "SELECT `id`, `date`, `score`, `snoreTime`, `sleepTime`, '' FROM `sleep_sessions`"
        )
        db.execSQL("DROP TABLE `sleep_sessions`")
        db.execSQL("ALTER TABLE `sleep_sessions_new` RENAME TO `sleep_sessions`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_sessions_date` " +
                "ON `sleep_sessions` (`date`)"
        )
    }
}
