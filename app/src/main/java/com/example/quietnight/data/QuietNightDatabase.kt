package com.example.quietnight.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SleepSession::class, SnoreClip::class], version = 6)
abstract class QuietNightDatabase : RoomDatabase() {
    abstract fun snoreDao(): SnoreDao
}
