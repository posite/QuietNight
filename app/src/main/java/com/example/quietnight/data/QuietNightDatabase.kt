package com.example.quietnight.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SleepSession::class], version = 4)
abstract class QuietNightDatabase : RoomDatabase() {
    abstract fun snoreDao(): SnoreDao
}