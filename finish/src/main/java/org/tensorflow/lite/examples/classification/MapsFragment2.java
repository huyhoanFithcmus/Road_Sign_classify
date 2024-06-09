package org.tensorflow.lite.examples.classification;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsFragment2 extends Fragment {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private List<List<LatLng>> allRoutes;
    private List<LatLng> currentRoutePoints;
    private Polyline routePolyline;

    private Location currentLocation;
    private float currentSpeed;

    private Button startRouteButton;
    private Handler handler;
    private Runnable updateLocationRunnable;
    private static final long UPDATE_INTERVAL = 60000; // 1 phút

    private boolean isRouteDrawing = false;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

                fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult != null && locationResult.getLastLocation() != null && isRouteDrawing) {
                            currentLocation = locationResult.getLastLocation();
                            LatLng newPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            currentSpeed = currentLocation.getSpeed();

                            updateRoute(newPoint);
                            updateMapWithCurrentLocation();
                        }
                    }
                };

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0f));
                    }
                });

                handler = new Handler();
                updateLocationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isRouteDrawing) {
                            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                                if (location != null) {
                                    currentLocation = location;
                                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                                    currentSpeed = location.getSpeed();

                                    updateRoute(newPoint);
                                    updateMapWithCurrentLocation();
                                }
                            });

                            handler.postDelayed(this, UPDATE_INTERVAL);
                        }
                    }
                };

            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (isRouteDrawing) {
            handler.postDelayed(updateLocationRunnable, UPDATE_INTERVAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateLocationRunnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps2, container, false);
        startRouteButton = view.findViewById(R.id.startStopButton);

        Button openMapsButton = requireActivity().findViewById(R.id.openMapsButton);
        openMapsButton.setVisibility(View.INVISIBLE);

        Button backButton = view.findViewById(R.id.backToMainActivityButton);
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
            openMapsButton.setVisibility(View.VISIBLE);
        });

        Button viewRouteListButton = view.findViewById(R.id.viewRouteListButton);
        viewRouteListButton.setOnClickListener(v -> openRouteListFragment());

        startRouteButton.setOnClickListener(v -> {
            if (!isRouteDrawing) {
                startRoute();
            } else {
                stopRoute();
            }
        });

        // Initialize List for routes
        allRoutes = new ArrayList<>();

        return view;
    }

    private void openRouteListFragment() {
        // Replace current fragment with RouteListFragment
        RouteListFragment routeListFragment = new RouteListFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, routeListFragment)
                .addToBackStack(null)
                .commit();
    }

    private void startRoute() {
        isRouteDrawing = true;
        startRouteButton.setText("Stop Route");
        currentRoutePoints = new ArrayList<>();
        allRoutes.add(currentRoutePoints);
        handler.postDelayed(updateLocationRunnable, UPDATE_INTERVAL);
    }

    private void stopRoute() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (List<LatLng> route : allRoutes) {
            List<Map<String, Double>> routeCoordinates = new ArrayList<>();
            for (LatLng latLng : route) {
                Map<String, Double> coordinate = new HashMap<>();
                coordinate.put("latitude", latLng.latitude);
                coordinate.put("longitude", latLng.longitude);
                routeCoordinates.add(coordinate);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());

            Map<String, Object> routeData = new HashMap<>();
            routeData.put("routePoints", routeCoordinates);

            db.collection("routes")
                    .document(currentTime)
                    .set(routeData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
        }

        clearRouteData();
    }

    private void clearRouteData() {
        isRouteDrawing = false;
        startRouteButton.setText("Start Route");
        handler.removeCallbacks(updateLocationRunnable);
        currentRoutePoints.addAll(allRoutes.get(allRoutes.size() - 1));
        allRoutes.get(allRoutes.size() - 1).clear();

        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        mMap.clear();
    }

    private void updateRoute(LatLng newPoint) {
        currentRoutePoints.add(newPoint);

        if (routePolyline != null) {
            routePolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(currentRoutePoints)
                .color(R.color.black)
                .width(5);
        routePolyline = mMap.addPolyline(polylineOptions);
    }

    private void updateMapWithCurrentLocation() {
        if (currentLocation != null) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("My Location");
            mMap.addMarker(markerOptions);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
