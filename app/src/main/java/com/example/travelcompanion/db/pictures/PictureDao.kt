package com.example.travelcompanion.db.pictures

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert

@Dao
interface PictureDao {
    @Insert
    fun insertPicture(picture: Picture)

    @Delete
    fun deletePicture(picture: Picture)
}