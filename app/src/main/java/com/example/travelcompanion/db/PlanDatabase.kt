package com.example.travelcompanion.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.util.Log

@Database(
    entities = [Plan::class],
    version = 1,
    exportSchema = false
)
abstract class PlanDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao

    companion object{
        @Volatile
        private var INSTANCE : PlanDatabase? = null
        fun getInstance(context: Context): PlanDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    Log.d("PlanDatabase", "Creating new instance of PlanDatabase")
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PlanDatabase::class.java,
                        "plan_database"
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}