package org.tensorflow.lite.examples.classification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import org.tensorflow.lite.examples.classification.R
import org.tensorflow.lite.examples.classification.model.SpeedData
import java.util.*

class MapsFragment4 : Fragment(), OnMapReadyCallback {

    private lateinit var speedData: SpeedData

    companion object {
        private const val ARG_SPEED_DATA = "speed_data"

        @JvmStatic
        fun newInstance(speedData: SpeedData) =
            MapsFragment4().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SPEED_DATA, speedData)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps4, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        arguments?.let { args ->
            speedData = args.getParcelable(ARG_SPEED_DATA) ?: SpeedData(0.0, Timestamp.now(), null)
            val location = speedData.routePoint
            location?.let {
                googleMap.addMarker(MarkerOptions().position(it).title("Speed Data Location"))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            }
        }
    }

}
