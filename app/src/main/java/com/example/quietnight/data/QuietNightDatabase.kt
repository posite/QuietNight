package com.example.quietnight.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SleepSession::class], version = 5)
abstract class QuietNightDatabase : RoomDatabase() {
    abstract fun snoreDao(): SnoreDao
}
