package com.example.travelcompanion.db.pictures

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "picture_table")
data class Picture(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "picture_id")
    var id: Int,

    @ColumnInfo(name = "picture_date")
    var date: Long,

    @ColumnInfo(name = "picture_title")
    var title: String,

    @ColumnInfo(name = "picture_uri")
    var uri: String,
)