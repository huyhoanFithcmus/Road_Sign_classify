package org.tensorflow.lite.examples.classification;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.tensorflow.lite.examples.classification.adapter.RouteAdapter;
import org.tensorflow.lite.examples.classification.model.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteListFragment extends Fragment implements RouteAdapter.OnRouteClickListener {

    private RecyclerView recyclerView;
    private RouteAdapter adapter;
    private List<Route> routeList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_list, container, false);

        recyclerView = view.findViewById(R.id.routeRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        routeList = new ArrayList<>();
        adapter = new RouteAdapter(routeList, this); // Pass this as listener
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadRoutesFromFirebase();

        return view;
    }

    private void loadRoutesFromFirebase() {
        db.collection("routes").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String routeId = document.getId();
                        List<Map<String, Double>> routeCoordinates = (List<Map<String, Double>>) document.get("routePoints");
                        List<LatLng> routePoints = new ArrayList<>();
                        for (Map<String, Double> coordinate : routeCoordinates) {
                            double latitude = coordinate.get("latitude");
                            double longitude = coordinate.get("longitude");
                            routePoints.add(new LatLng(latitude, longitude));
                        }
                        routeList.add(new Route(routeId, routePoints));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }

    @Override
    public void onRouteClick(Route route) {
        MapsFragment3 mapFragment = MapsFragment3.newInstance(route.getRoutePoints());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, mapFragment, "MapFragment3")
                .addToBackStack(null)
                .commit();
    }
}
