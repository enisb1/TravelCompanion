package com.example.travelcompanion.ui.planned

import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.Plan
import com.example.travelcompanion.ui.home.plan.PlanViewModel
import com.example.travelcompanion.ui.home.plan.PlanViewModelFactory
import com.example.travelcompanion.db.PlanDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlannedTripsFragment : Fragment() {

    private lateinit var planViewModel: PlanViewModel
    private lateinit var planRecyclerView : RecyclerView
    private lateinit var planAdapter : PlanRecyclerViewAdapter
    private lateinit var plusButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_planned_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plannedTripsVM : PlannedTripsViewModel = ViewModelProvider(this)
            .get(PlannedTripsViewModel::class.java)

        planRecyclerView = view.findViewById(R.id.rvPlanned)
        plusButton = view.findViewById(R.id.fabAdd)

        val dao = PlanDatabase.getInstance(requireContext()).planDao()
        val factory = PlanViewModelFactory(dao)
        planViewModel = ViewModelProvider(this, factory)[PlanViewModel::class.java]

        initRecyclerView()

        plusButton.setOnClickListener {
            // Navigate using action action_nav_planned_to_plan implicitly passing 1 for parameter "tab"
            val action = PlannedTripsFragmentDirections.actionNavPlannedToPlan()
            findNavController().navigate(action)
        }

    }

    private fun initRecyclerView() {
        planRecyclerView.layoutManager = LinearLayoutManager(context)
        planAdapter = PlanRecyclerViewAdapter{
            plan -> showPlanDialog(plan)
        }
        planRecyclerView.adapter = planAdapter

        displayPlanList()
    }

    private fun showPlanDialog(plan: Plan) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_plan_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvPlanDetails).text = "Destination: ${plan.destination}\nDate: ${SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(plan.date))}\nType: ${plan.type}"
        dialogView.findViewById<Button>(R.id.btnStartTrip).setOnClickListener {
            // TODO
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnEditTrip).setOnClickListener {
            // TODO
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnDeleteTrip).setOnClickListener{
            planViewModel.deletePlan(plan)
            dialog.dismiss()
            displayPlanList()
        }

        dialog.show()
    }

    private fun displayPlanList(){
        planViewModel.plans.observe(viewLifecycleOwner, {
            planAdapter.setList(it)
            planAdapter.notifyDataSetChanged()
        })
    }
}