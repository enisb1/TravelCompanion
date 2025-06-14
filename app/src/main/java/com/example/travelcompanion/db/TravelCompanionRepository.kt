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
    fun insertTrip(title: String, start: Long, type: TripType, destination: String, state: TripState,
                   duration: Long = 0, distance: Double = 0.0): Long {
        val trip = Trip(
            id = 0,
            title = title,
            startTimestamp = start,
            endTimestamp = start + duration * 1000,
            type = type,
            destination = destination,
            state = state,
            duration = duration,
            distance = Math.round(distance * 10) / 10.0
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
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            tripDao.updateTrip(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {

            val notes = noteDao.getNotesByTripId(trip.id)
            for (note in notes) {
                noteDao.deleteNote(note)
            }

            val pictures = pictureDao.getPicturesByTripId(trip.id)
            for (picture in pictures) {
                pictureDao.deletePicture(picture)
            }

            val locations = tripLocationDao.getLocationsByTripId(trip.id)
            for (location in locations) {
                tripLocationDao.deleteLocation(location)
            }

            tripDao.deleteTrip(trip)

            // TODO: update the shown notes and pictures in the Archive fragment
        }
    }

    fun getAllTrips(): LiveData<List<Trip>> {
        return tripDao.getAllTrips()
    }

    fun getTripsByState(state: TripState): LiveData<List<Trip>> {
        return tripDao.getTripsByState(state)
    }

    fun getTripsListByState(state: TripState): List<Trip> {
        return tripDao.getTripsListByState(state)
    }

    // Not sure this will work
    fun getTripById(id: Long): Trip? {
        return tripDao.getTripById(id)
    }

    // Not sure this will work
    suspend fun getTripsByDateRange(startDate: Long, endDate: Long): List<Trip> {
        return tripDao.getTripsByDateRange(startDate, endDate)
    }

    suspend fun getDistinctDestinations(): List<String> {
        return tripDao.getDistinctDestinations()
    }

    // -------------------- NOTES --------------------
    fun saveNote(note: Note) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            noteDao.insertNote(note)
        }
    }

    fun getAllNotes(): List<Note> {
        return noteDao.getAllNotes()
    }

    fun getNotesByTripId(tripId: Long): List<Note> {
        return noteDao.getNotesByTripId(tripId)
    }

    // -------------------- PICTURES --------------------
    fun savePicture(picture: Picture) {
        TravelCompanionDatabase.databaseWriteExecutor.execute {
            pictureDao.insertPicture(picture)
        }
    }

    fun getAllPictures(): List<Picture> {
        return pictureDao.getAllPictures()
    }

    fun getPicturesByTripId(tripId: Long): List<Picture> {
        return pictureDao.getPicturesByTripId(tripId)
    }
}