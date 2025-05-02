import android.icu.text.SimpleDateFormat
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.db.trip.Trip

class PlanViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    fun bind(trip: Trip){
        val dateTextView = view.findViewById<TextView>(R.id.tvDate)
        val typeTextView = view.findViewById<TextView>(R.id.tvType)
        val destinationTextView = view.findViewById<TextView>(R.id.tvDestination)

        // Format the date to DD/MM/YYYY format
        dateTextView.text = SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(trip.startTimestamp))
        typeTextView.text = trip.type.name
        destinationTextView.text = trip.destination

    }
}