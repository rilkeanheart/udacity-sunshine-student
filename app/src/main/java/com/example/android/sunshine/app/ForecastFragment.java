package com.example.android.sunshine.app;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.ActionBar;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.service.SunshineService;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private static final int FORECAST_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;



    private static final String LIST_ITEM_POSITION = "POSITION";

    private ShareActionProvider mShareActionProvider;
    private int mPosition = -1;
    private boolean mUseTodayLayout = false;

    public ForecastFragment() {
        // Required empty public constructor
    }

    private ForecastAdapter mForecastArrayAdapter;

    /*@Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }*/

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for the fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
/*        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        } else */
        if (id == R.id.action_view_location) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if(savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(LIST_ITEM_POSITION);
        } else {
            mPosition = -1;
        }

        /*String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order: Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting,
                System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null,
                null,
                null,
                sortOrder);

        /*String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/46",
                "Weds - Cloudy - 72/63",
                "Thurs - Rainy - 64/51",
                "Fri - Foggy - 70/46",
                "Sat - HELP TRAPPED IN WEATHERSTATION - 76/88",
                "Sun - Sunny - 80/68",
        };*/

        //List<String> weekForecast = new ArrayList<String>();
        mForecastArrayAdapter = new ForecastAdapter(getActivity(), null, 0);

        // Get a reference to the ListView, and attach the adaptor
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        View emptyView = rootView.findViewById(R.id.listview_forecast_empty);
        listView.setEmptyView(emptyView);
        listView.setAdapter(mForecastArrayAdapter);
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String forecastDetail = mForecastArrayAdapter.getItem(i);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, forecastDetail);
                startActivity(intent);
            }
        });*/

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                mPosition = position;
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
/*                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                    startActivity(intent);*/
                    Uri dateUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            locationSetting, cursor.getLong(COL_WEATHER_DATE));
                    ((Callback) getActivity()).onItemSelected(dateUri);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void updateWeather() {
        /*String location = Utility.getPreferredLocation(getActivity());
        Intent weatherFetchAlarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        weatherFetchAlarmIntent.putExtra(SunshineService.LOCATION_KEY, location);

        //Wrap in a pending intent which only fires once.
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(getActivity(), 0,  weatherFetchAlarmIntent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager =
                (AlarmManager) getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);

        //Set the AlarmManager to wake up the system.
        Log.d(LOG_TAG, "Alarm Send Signal");
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent);*/

        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    private void updateEmptyView() {
        if (mForecastArrayAdapter.getCount() == 0) {
            TextView tv = (TextView) getView().findViewById(R.id.listview_forecast_empty);
            if(tv != null) {
                // if cursor is empty, wh? do we have an invalid location
                int message = R.string.empty_forecast_list;
                @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
                switch(location) {
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.empty_forecast_list_server_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.empty_forecast_list_server_error;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                        message = R.string.empty_forecast_list_invalid_location;
                        break;
                    default:
                        if(!Utility.isConnectedToNetwork(getActivity())){
                            message = R.string.empty_forecast_list_no_internet;
                        }
                }

                tv.setText(message);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        // Get the location preference
        String strPreferredLocation =  Utility.getPreferredLocation(getActivity());

        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                strPreferredLocation,
                System.currentTimeMillis());

        Loader<Cursor> retValue = new CursorLoader(getActivity(),
                uri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);

        return retValue;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        mForecastArrayAdapter.swapCursor(cursor);
        if(cursor != null && cursor.getCount() > 0) {
            ListView listView =  (ListView) getActivity().findViewById(R.id.listview_forecast);
            if(mPosition != -1) {
                listView.setSelection(mPosition);
            }
        } else {
            updateEmptyView();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastArrayAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(LIST_ITEM_POSITION, mPosition);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastArrayAdapter != null) {
            mForecastArrayAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    private void openPreferredLocationInMap() {
        if (mForecastArrayAdapter != null) {
            Cursor cursor =mForecastArrayAdapter.getCursor();
            if(cursor != null) {
                cursor.moveToPosition(0);
                String posLat = cursor.getString(COL_COORD_LAT);
                String posLon = cursor.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLon);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
            }

        }
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }
}
