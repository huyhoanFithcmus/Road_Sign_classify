import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import org.tensorflow.lite.examples.classification.R
import org.tensorflow.lite.examples.classification.model.SpeedData

class SpeedDataAdapter(
    private val speedDataList: List<SpeedData>,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<SpeedDataAdapter.SpeedDataViewHolder>() {

    interface OnItemClickListener {
        fun onViewLocationClick(speedData: SpeedData)
    }

    class SpeedDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val speedTextView: TextView = itemView.findViewById(R.id.speedTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        val viewLocationButton: Button = itemView.findViewById(R.id.viewLocationButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeedDataViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_data, parent, false)
        return SpeedDataViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SpeedDataViewHolder, position: Int) {
        val currentItem = speedDataList[position]

        holder.speedTextView.text = "Speed: ${currentItem.speed} km/h"
        val timestamp = currentItem.timestamp?.toDate().toString()
        holder.timestampTextView.text = "Timestamp: $timestamp"
        holder.locationTextView.text =
            "Location: ${currentItem.routePoint?.latitude}, ${currentItem.routePoint?.longitude}"

        holder.viewLocationButton.setOnClickListener {
            onItemClickListener.onViewLocationClick(currentItem)
        }
    }

    override fun getItemCount() = speedDataList.size
}
