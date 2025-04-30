package com.example.travelcompanion.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripDao

@Database(
    entities = [Trip::class],
    version = 1,
    exportSchema = false
)
abstract class TravelCompanionDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object{
        @Volatile
        private var INSTANCE : TravelCompanionDatabase? = null
        fun getInstance(context: Context): TravelCompanionDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        TravelCompanionDatabase::class.java,
                        "travel_companion_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}