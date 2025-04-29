package com.example.travelcompanion.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Trip::class],
    version = 1,
    exportSchema = false
)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object{
        @Volatile
        private var INSTANCE : TripDatabase? = null
        fun getInstance(context: Context): TripDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TripDatabase::class.java,
                        "trip_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}