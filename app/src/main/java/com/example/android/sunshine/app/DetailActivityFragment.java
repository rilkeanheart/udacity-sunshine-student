package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.support.v7.widget.ShareActionProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;

import org.w3c.dom.Text;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DETAIL_LOADER = 10;
    public static final String DETAIL_URI = "URI";

    private final String LOG_TAG = DetailActivity.class.getSimpleName();
    private final String FORECAST_SHARE_HASH_TAG = " #SunshineApp";

    private ShareActionProvider mShareActionProvider;
    private String mForecastStr;
    private Uri mUri;

    private static final String[] DETAIL_COLUMNS = {
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
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_WIND_SPEED = 6;
    static final int COL_WEATHER_DEGREES = 7;
    static final int COL_WEATHER_PRESSURE = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    private TextView mDayTextView;
    private TextView mdateView;
    private TextView mLowTempView;
    private TextView mHighTempView;
    private ImageView mIconView;
    private TextView mDescriptionView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private CompassView mCompass;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mDayTextView = (TextView) rootView.findViewById(R.id.text_day_view);
        mdateView = (TextView) rootView.findViewById(R.id.text_date_view);
        mHighTempView = (TextView) rootView.findViewById(R.id.text_high_temp_view);
        mLowTempView = (TextView) rootView.findViewById(R.id.text_low_temp_view);
        mIconView = (ImageView) rootView.findViewById(R.id.icon);
        mHumidityView = (TextView) rootView.findViewById(R.id.text_humidity_view);
        mWindView = (TextView) rootView.findViewById(R.id.text_wind_view);
        mPressureView = (TextView) rootView.findViewById(R.id.text_pressure_view);
        mDescriptionView = (TextView) rootView.findViewById(R.id.text_forecast_view);
        mCompass = (CompassView)rootView.findViewById(R.id.wind_compass);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate menu resource file.
        inflater.inflate(R.menu.detailfragment, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mForecastStr != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else {
            Log.d(LOG_TAG, "Share Action Provider is NULL");
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASH_TAG);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        Loader<Cursor> retValue = null;

        if (mUri != null) {
             retValue = new CursorLoader(getActivity(),
                    mUri,
                     DETAIL_COLUMNS,
                    null,
                    null,
                    null);
        }

        return retValue;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (cursor.moveToFirst()) {
            boolean isMetric = Utility.isMetric(getActivity());

            Long weatherDate = cursor.getLong(DetailActivityFragment.COL_WEATHER_DATE);
            String friendlyDay = Utility.getDayName(getActivity(), weatherDate);
            mDayTextView.setText(friendlyDay);

            String friendlyDate = Utility.getFormattedMonthDay(getActivity(), weatherDate);
            mdateView.setText(friendlyDate);

            // Read high temperature from cursor
            double high = cursor.getDouble(DetailActivityFragment.COL_WEATHER_MAX_TEMP);
            mHighTempView.setText(Utility.formatTemperature(getActivity(), high));
            mHighTempView.setContentDescription(getString(R.string.a11y_high_temp, high));

            // Read low temperature from cursor
            double low = cursor.getDouble(DetailActivityFragment.COL_WEATHER_MIN_TEMP);
            mLowTempView.setText(Utility.formatTemperature(getActivity(), low));
            mLowTempView.setContentDescription(getString(R.string.a11y_low_temp, low));

            // Set Image
            int weatherId = cursor.getInt(DetailActivityFragment.COL_WEATHER_CONDITION_ID);
            int weatherArtResource = Utility.getArtResourceForWeatherCondition(weatherId);
            Glide.with(this)
                    .load(Utility.getArtUrlForWeatherCondition(getActivity(),weatherArtResource))
                    .error(weatherArtResource)
                    .into(mIconView);
            //mIconView.setImageResource(weatherArtResource);

            // Read forecast
            String weatherForecast = cursor.getString(DetailActivityFragment.COL_WEATHER_DESC);
            mDescriptionView.setText(weatherForecast);
            mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, weatherForecast));
            mIconView.setContentDescription(weatherForecast);
            mIconView.setContentDescription(getString(R.string.a11y_forecast_icon, weatherForecast));

            // Read Humidity
            double humidity = cursor.getDouble(DetailActivityFragment.COL_WEATHER_HUMIDITY);
            mHumidityView.setText(Utility.formatHumidity(getActivity(), humidity));

            // Read wind
            float windSpeed = cursor.getFloat(DetailActivityFragment.COL_WEATHER_WIND_SPEED);
            float degrees = cursor.getFloat(DetailActivityFragment.COL_WEATHER_DEGREES);
            mWindView.setText(Utility.formatWindSpeed(getActivity(), windSpeed, degrees));
            mCompass.update(degrees);

            // Read pressure
            double pressure = cursor.getDouble(DetailActivityFragment.COL_WEATHER_PRESSURE);
            mPressureView.setText(Utility.formatPressure(getActivity(), pressure));

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    void onLocationChanged( String newLocation ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }
}
