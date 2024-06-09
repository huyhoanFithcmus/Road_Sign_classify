package org.tensorflow.lite.examples.classification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.tensorflow.lite.examples.classification.R;

import java.util.ArrayList;
import java.util.List;

public class MapsFragment3 extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<LatLng> routePoints;

    public static MapsFragment3 newInstance(List<LatLng> routePoints) {
        MapsFragment3 fragment = new MapsFragment3();
        Bundle args = new Bundle();
        args.putParcelableArrayList("routePoints", new ArrayList<>(routePoints));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            routePoints = getArguments().getParcelableArrayList("routePoints");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.mapContainer, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (routePoints != null && routePoints.size() > 0) {
            for (LatLng point : routePoints) {
                mMap.addMarker(new MarkerOptions().position(point));
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.get(0), 15f));
        }
    }
}
