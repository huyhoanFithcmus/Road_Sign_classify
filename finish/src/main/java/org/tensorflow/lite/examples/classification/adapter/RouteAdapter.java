package org.tensorflow.lite.examples.classification.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.tensorflow.lite.examples.classification.R;
import org.tensorflow.lite.examples.classification.model.Route;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<Route> routeList;
    private OnRouteClickListener onRouteClickListener;

    public RouteAdapter(List<Route> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.onRouteClickListener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);
        holder.bind(route);
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    public class RouteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Button showMapButton;
        TextView routeIdTextView;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            routeIdTextView = itemView.findViewById(R.id.routeIdTextView);
            showMapButton = itemView.findViewById(R.id.showMapButton);
            showMapButton.setOnClickListener(this);
        }

        public void bind(Route route) {
            // Bind route data to views
            routeIdTextView.setText("Route: " + route.getFormattedId());
        }


        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && onRouteClickListener != null) {
                Route route = routeList.get(position);
                onRouteClickListener.onRouteClick(route);
            }
        }
    }

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }
}
