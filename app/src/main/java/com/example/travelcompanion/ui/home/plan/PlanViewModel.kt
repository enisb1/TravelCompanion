package com.example.travelcompanion.ui.home.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcompanion.db.Plan
import com.example.travelcompanion.db.PlanDao
import com.example.travelcompanion.db.PlanType
import kotlinx.coroutines.launch
import java.util.Date

class PlanViewModel(private val dao: PlanDao) : ViewModel() {
    val plans = dao.getAllPlans()

    fun savePlan(date: Date, type: PlanType, destination: String) {
        val plan = Plan(0, date, type, destination)
        viewModelScope.launch {
            dao.insertPlan(plan)
        }
    }

    fun updatePlan(plan: Plan) {
        viewModelScope.launch {
            dao.updatePlan(plan)
        }
    }

    fun deletePlan(plan: Plan) {
        viewModelScope.launch {
            dao.deletePlan(plan)
        }
    }

}