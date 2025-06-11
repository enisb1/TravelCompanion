import android.os.Build
import androidx.annotation.RequiresApi
import com.example.travelcompanion.db.TravelCompanionRepository
import com.example.travelcompanion.db.trip.TripState
import com.example.travelcompanion.db.trip.TripType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
fun seedTrips(repository: TravelCompanionRepository) {
    GlobalScope.launch {
        val now = LocalDate.now()
        for (i in 0 until 12) {
            val tripDate = now.minusMonths(i.toLong()).withDayOfMonth(10)
            val startMillis = tripDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val tripsThisMonth = kotlin.random.Random.nextInt(1, 6) // 1-5 trips per month
            for (j in 0 until tripsThisMonth) {
                val durationDays = 3L
                val durationSeconds = durationDays * 24 * 60 * 60
                repository.insertTrip(
                    title = "Trip $i-$j",
                    start = startMillis + j * 3600 * 1000,
                    type = TripType.MULTIDAY,
                    destination = "Destination $i-$j",
                    state = TripState.COMPLETED,
                    duration = durationSeconds,
                    distance = kotlin.random.Random.nextDouble(1000.0, 15000.0)
                )
            }
        }
    }
}

class DatabaseSeeder {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun seed(repository: TravelCompanionRepository) {
            seedTrips(repository)
        }
    }
}
