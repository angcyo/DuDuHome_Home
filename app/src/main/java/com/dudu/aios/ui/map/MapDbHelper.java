package com.dudu.aios.ui.map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dudu.aios.ui.map.observable.MapListItemObservable;
import com.dudu.android.launcher.LauncherApplication;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/2/14.
 */
public class MapDbHelper extends SQLiteOpenHelper {

    public final static String NAVIGATION_TABLE_NAME = "navi_history_tb";
    public final static String NAVIGATION_COLUMN_ID = "_id";
    public final static String NAVIGATION_COLUMN_ADDRESS = "navi_address";
    public final static String NAVIGATION_COLUMN_PLACENAME = "navi_place_name";
    public final static String NAVIGATION_COLUMN_DISTANCE = "distance";
    public final static String NAVIGATION_COLUMN_SEARCH_TIME = "search_time";
    public final static String NAVIGATION_COLUM_LAT = "navi_lat";
    public final static String NAVIGATION_COLUM_LON = "navi_lon";
    private final static String DATABASE_NAME = "navihistory.db";
    private final static int DB_VERSION = 1;
    private static final String CREATE_NAVIGATION_TABLE_SQL = "create table if not exists "
            + NAVIGATION_TABLE_NAME
            + " ("
            + NAVIGATION_COLUMN_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NAVIGATION_COLUMN_PLACENAME
            + " VARCHAR,"
            + NAVIGATION_COLUMN_ADDRESS
            + " VARCHAR,"
            + NAVIGATION_COLUM_LAT
            + " VARCHAR,"
            + NAVIGATION_COLUM_LON
            + " VARCHAR,"
            + NAVIGATION_COLUMN_DISTANCE
            + " VARCHAR)";
    private static MapDbHelper mDbHelper;
    private SQLiteDatabase db;

    private MapDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    public static MapDbHelper getDbHelper() {
        if (mDbHelper == null) {
            mDbHelper = new MapDbHelper(LauncherApplication.getContext());
        }
        return mDbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase sdb) {

        sdb.execSQL(CREATE_NAVIGATION_TABLE_SQL);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
    }

    public ArrayList<MapListItemObservable> getHistory() {
        db = getWritableDatabase();
        ArrayList<MapListItemObservable> historyList = new ArrayList<>();
        Cursor c = db.query(NAVIGATION_TABLE_NAME, null, null, null, null, null,
                NAVIGATION_COLUMN_ID + " desc");
        if (c != null) {
            while (c.moveToNext()) {
                String placeName = c.getString(c
                        .getColumnIndexOrThrow(NAVIGATION_COLUMN_PLACENAME));
                String address = c.getString(c
                        .getColumnIndexOrThrow(NAVIGATION_COLUMN_ADDRESS));
                String distance = c.getString(c
                        .getColumnIndexOrThrow(NAVIGATION_COLUMN_DISTANCE));
                double lat = Double.parseDouble(c.getString(c.getColumnIndexOrThrow(NAVIGATION_COLUM_LAT)));
                double lon = Double.parseDouble(c.getString(c.getColumnIndexOrThrow(NAVIGATION_COLUM_LON)));

                MapListItemObservable histiory = new MapListItemObservable(placeName, address, distance, null, lat, lon);
                historyList.add(histiory);
            }

            c.close();
        }

        return historyList;
    }


    public void saveHistory(MapListItemObservable mapListObservable) {
        db = getWritableDatabase();
        db.insertOrThrow(NAVIGATION_TABLE_NAME, null, getHistoryValues(mapListObservable));
    }

    private ContentValues getHistoryValues(MapListItemObservable naviHistory) {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
//                Locale.getDefault());
//        String dateString = format.format(new Date());
        
        ContentValues history = new ContentValues();
        history.put(NAVIGATION_COLUMN_PLACENAME, naviHistory.addressName.get());
        history.put(NAVIGATION_COLUMN_ADDRESS, naviHistory.address.get());
        history.put(NAVIGATION_COLUM_LAT, naviHistory.lat.get() + "");
        history.put(NAVIGATION_COLUM_LON, naviHistory.lon.get() + "");
        history.put(NAVIGATION_COLUMN_DISTANCE, naviHistory.distance.get());
        return history;
    }

    public void deleteHistory() {


    }

}
