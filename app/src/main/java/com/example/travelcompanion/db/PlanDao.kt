package com.example.travelcompanion.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlanDao {
    @Insert
    suspend fun insertPlan(plan: Plan)

    @Update
    suspend fun updatePlan(plan: Plan)

    @Delete
    suspend fun deletePlan(plan: Plan)

    @Query("SELECT * FROM plan_table")
    fun getAllStudents(): LiveData<List<Plan>>
}