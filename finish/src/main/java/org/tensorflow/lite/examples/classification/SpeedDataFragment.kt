package org.tensorflow.lite.examples.classification

import SpeedDataAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.examples.classification.R
import org.tensorflow.lite.examples.classification.model.SpeedData
import java.util.*

class SpeedDataFragment : Fragment(), SpeedDataAdapter.OnItemClickListener {

    private lateinit var speedDataRecyclerView: RecyclerView
    private lateinit var speedDataAdapter: SpeedDataAdapter
    private var speedDataList: MutableList<SpeedData> = mutableListOf()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speed_data, container, false)

        speedDataRecyclerView = view.findViewById(R.id.speedDataRecyclerView)
        speedDataAdapter = SpeedDataAdapter(speedDataList, this)

        speedDataRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = speedDataAdapter
        }

        loadSpeedDataFromFirestore()

        return view
    }

    private fun loadSpeedDataFromFirestore() {
        db.collection("speedData").get()
            .addOnSuccessListener { result ->
                speedDataList.clear()
                for (document in result) {
                    val speed = document.getDouble("speed") ?: 0.0
                    val timestamp = document.getTimestamp("timestamp") ?: Timestamp(Date())
                    val routePoint = document["routePoint"] as? Map<*, *>
                    val latitude = routePoint?.get("latitude") as? Double ?: 0.0
                    val longitude = routePoint?.get("longitude") as? Double ?: 0.0
                    val routePointLatLng = LatLng(latitude, longitude)
                    speedDataList.add(SpeedData(speed, timestamp, routePointLatLng))
                }
                speedDataAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    override fun onViewLocationClick(speedData: SpeedData) {
        speedData.routePoint?.let { routePoint ->
            val latitude = routePoint.latitude
            val longitude = routePoint.longitude
            val newSpeedData = SpeedData(speedData.speed, speedData.timestamp, LatLng(latitude, longitude))
            val mapsFragment = MapsFragment4.newInstance(newSpeedData)
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, mapsFragment, "MapFragment4")
                .addToBackStack(null)
                .commit()
        }
    }

}
