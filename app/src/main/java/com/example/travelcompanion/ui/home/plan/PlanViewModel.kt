package com.example.travelcompanion.ui.home.plan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.db.Plan
import com.example.travelcompanion.db.PlanDatabase
import com.example.travelcompanion.db.PlanType
import kotlinx.coroutines.launch
import java.util.Date

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val planDao = PlanDatabase.getDatabase(application).planDao()

    fun savePlan(date: Date, type: PlanType, destination: String) {
        Log.i("PlanViewModel", "Saving plan: $date, $type, $destination")
        val plan = Plan(0, date, type, destination)
        Log.i("PlanViewModel", "Plan instantiated: $plan")
        viewModelScope.launch {
            planDao.insertPlan(plan)
        }
    }

    fun updatePlan(plan: Plan) {
        viewModelScope.launch {
            planDao.updatePlan(plan)
        }
    }

    fun deletePlan(plan: Plan) {
        viewModelScope.launch {
            planDao.deletePlan(plan)
        }
    }

    fun getAllPlans() = planDao.getAllStudents()
}