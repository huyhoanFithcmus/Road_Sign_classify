package org.tensorflow.lite.examples.classification.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Tên cơ sở dữ liệu
    private static final String DATABASE_NAME = "routes_database";
    // Phiên bản cơ sở dữ liệu
    private static final int DATABASE_VERSION = 1;

    // Tên bảng và các cột
    private static final String TABLE_ROUTES = "routes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng routes
        String CREATE_ROUTES_TABLE = "CREATE TABLE " + TABLE_ROUTES +
                "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_LATITUDE + " REAL," +
                COLUMN_LONGITUDE + " REAL" +
                ")";
        db.execSQL(CREATE_ROUTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Xóa bảng cũ nếu tồn tại
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTES);
        // Tạo lại bảng
        onCreate(db);
    }

    // Trong DatabaseHelper
// Bổ sung phương thức để thêm và lấy tuyến từ cơ sở dữ liệu.

// ...

    public void addRoute(List<LatLng> route) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        for (LatLng point : route) {
            values.put(COLUMN_LATITUDE, point.latitude);
            values.put(COLUMN_LONGITUDE, point.longitude);
            db.insert(TABLE_ROUTES, null, values);
        }

        db.close();
    }

    public List<List<LatLng>> getAllRoutes() {
        List<List<LatLng>> allRoutes = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ROUTES;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                List<LatLng> route = new ArrayList<>();
                do {
                    double latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE));
                    route.add(new LatLng(latitude, longitude));
                } while (cursor.moveToNext());
                allRoutes.add(route);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return allRoutes;
    }

// ...

}
