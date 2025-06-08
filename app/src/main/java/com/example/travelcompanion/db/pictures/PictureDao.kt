package com.example.travelcompanion.db.pictures

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PictureDao {
    @Insert
    fun insertPicture(picture: Picture)

    @Delete
    fun deletePicture(picture: Picture)

    @Query("SELECT * FROM picture_table WHERE trip_id = :tripId")
    fun getPicturesByTripId(tripId: Long): List<Picture>

    @Query("SELECT * FROM picture_table")
    fun getAllPictures(): List<Picture>
}