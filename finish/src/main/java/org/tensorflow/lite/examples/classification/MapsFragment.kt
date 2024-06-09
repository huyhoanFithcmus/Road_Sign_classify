package org.tensorflow.lite.examples.classification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private val REQUEST_CODE = 100 // Replace with a unique integer

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        getCurrentLocation()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)

        val openMapsButton = requireActivity().findViewById<Button>(R.id.openMapsButton)
        openMapsButton.visibility = View.INVISIBLE

        val backButton = view.findViewById<Button>(R.id.backToMainActivityButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
            openMapsButton.visibility = View.VISIBLE
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return view
    }

    private fun getCurrentLocation() {
        if (isGooglePlayServicesAvailable()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request necessary permissions
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    REQUEST_CODE
                )
            } else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            map.addMarker(MarkerOptions().position(latLng).title("Your Location"))
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng,
                                    15f
                                )
                            ) // Adjust zoom level as needed
                        }
                    }
            }
        } else {
            Toast.makeText(requireContext(), "Google Play Services are not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        return status == ConnectionResult.SUCCESS
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, get the location
                getCurrentLocation()
            } else {
                // Permissions denied, handle the case
                Toast.makeText(requireContext(), "Location permissions are required", Toast.LENGTH_SHORT).show()

                // Optionally, prompt the user to open settings and enable permissions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }
}
