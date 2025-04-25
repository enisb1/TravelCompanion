package com.example.travelcompanion.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "plan_table")
data class Plan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plan_id")
    var id: Int,
    @ColumnInfo(name = "plan_date")
    var date: Date,
    @ColumnInfo(name = "plan_type")
    var type: PlanType,
    @ColumnInfo(name = "plan_destination")
    var destination: String
)
