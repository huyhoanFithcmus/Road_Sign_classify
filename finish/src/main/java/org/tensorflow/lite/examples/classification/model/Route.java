// Đường dẫn: app/src/main/java/org/tensorflow/lite/examples/classification/model/Route.java
package org.tensorflow.lite.examples.classification.model;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Route {
    private String id;
    private List<LatLng> routePoints;

    public Route() {
        // Default constructor required for Firestore
    }

    public Route(String id, List<LatLng> routePoints) {
        this.id = id;
        this.routePoints = routePoints;
    }

    public String getId() {
        return id;
    }

    public String getFormattedId() {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
            Date date = originalFormat.parse(id);
            SimpleDateFormat newFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            return newFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return id; // Trả về nguyên bản nếu không thể chuyển đổi
        }
    }

    public List<LatLng> getRoutePoints() {
        return routePoints;
    }
}
