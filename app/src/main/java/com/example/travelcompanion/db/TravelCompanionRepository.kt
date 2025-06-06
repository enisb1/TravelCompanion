package com.example.travelcompanion.db

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.travelcompanion.db.locations.TripLocation
import com.example.travelcompanion.db.locations.TripLocationDao
import com.example.travelcompanion.db.notes.Note
import com.example.travelcompanion.db.notes.NoteDao
import com.example.travelcompanion.db.pictures.Picture
import com.example.travelcompanion.db.pictures.PictureDao
import com.example.travelcompanion.db.trip.Trip
import com.example.travelcompanion.db.trip.TripDao
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date

class TravelCompanionRepository(app: Application) {

    private var tripDao : TripDao
    private var noteDao: NoteDao
    private var pictureDao: PictureDao
    private var tripLocationDao: TripLocationDao

    init {
        val db = TravelCompanionDatabase.getInstance(app)
        tripDao = db.tripDao()
        noteDao = db.noteDao()
        pictureDao = db.pictureDao()
        tripLocationDao = db.locationDao()
    }

    // -------------------- TRIPS --------------------
    fun insertTrip(startDate: Date, type: TripType, destination: String, state: TripState,
                   duration: Long = 0, distance: Double = 0.0): Long {
        // Convert Date to milliseconds since epoch
        val startInMillis = startDate.time
        val trip = Trip(
            id = 0,
            startTimestamp = startInMillis,
            endTimestamp = startInMillis + duration * 1000,
            type = type,
            destination = destination,
            state = state,
            duration = duration,
            distance = distance
        )

        return tripDao.insertTrip(trip)
    }

    fun getLocations(): List<TripLocation> {
        return tripLocationDao.getLocations()
    }

    fun saveLocations(tripLocations: List<TripLocation>) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            for (location in tripLocations) {
                tripLocationDao.insertLocation(location)
            }
        }
    }

    fun updateTrip(trip: Trip) {
        GlobalScope.launch {
            tripDao.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        GlobalScope.launch {
            tripDao.deleteTrip(trip)
        }
    }

    fun getAllTrips(): LiveData<List<Trip>> {
        return tripDao.getAllTrips()
    }

    fun getTripsByState(state: TripState): LiveData<List<Trip>> {
        return tripDao.getTripsByState(state)
    }

    // Not sure this will work
    fun getTripById(id: Long): Trip? {
        return tripDao.getTripById(id)
    }

    // Not sure this will work
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip> {
        return tripDao.getTripsByDateRange(startDate, endDate)
    }

    // -------------------- NOTES --------------------
    fun saveNote(note: Note) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            noteDao.insertNote(note)
        }
    }

    // -------------------- PICTURES --------------------
    fun savePicture(picture: Picture) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            pictureDao.insertPicture(picture)
        }
    }
}