import android.icu.text.SimpleDateFormat
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.Plan

class PlanViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    fun bind(plan: Plan){
        val dateTextView = view.findViewById<TextView>(R.id.tvDate)
        val typeTextView = view.findViewById<TextView>(R.id.tvType)
        val destinationTextView = view.findViewById<TextView>(R.id.tvDestination)

        // Format the date to DD/MM/YYYY format
        dateTextView.text = SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(plan.date))
        typeTextView.text = plan.type.name
        destinationTextView.text = plan.destination

    }
}