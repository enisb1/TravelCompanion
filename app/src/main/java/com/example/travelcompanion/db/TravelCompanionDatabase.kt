package com.example.travelcompanion.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.db.locations.TripLocationDao
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.notes.NoteDao
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.pictures.PictureDao
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripDao
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(
    entities = [Trip::class, Note::class, Picture::class, TripLocation::class],
    version = 1,
    exportSchema = false
)
abstract class TravelCompanionDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun noteDao(): NoteDao
    abstract fun pictureDao(): PictureDao
    abstract fun locationDao(): TripLocationDao

    companion object{
        @Volatile
        private var INSTANCE : TravelCompanionDatabase? = null

        private const val THREADS_NUMBER: Int = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(THREADS_NUMBER)

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