package com.example.aleksei.stormy;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Geocoder;

import com.example.aleksei.stormy.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather currentWeather;

    private ImageView iconImage;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private ProgressDialog progressDialog;

    private double latitude;
    private double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location)
            {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s)
            {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 10);
                return;
            }

        }

        else
        {
            locationManager.requestLocationUpdates("gps", 0000, 0, locationListener);

        }



        String currentCity = currentLocation(latitude, longitude);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Obtaining your geo location via gps");






        getForecast(latitude, longitude, currentCity);



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    locationManager.requestLocationUpdates("gps", 0000, 0, locationListener);
                }
        }
    }



    //Get the nearest city name
    private String currentLocation(double latitude, double longitude)
    {
        String currentCity = "";

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        List<Address> addressList;

        try
        {
            addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList.size() > 0)
            {
                currentCity = addressList.get(0).getLocality();
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        return currentCity;

    }

    private void getForecast(double latitude, double longitude, final String currentCity) {
        final ActivityMainBinding binding = DataBindingUtil.setContentView(MainActivity.this, R.layout.activity_main);

        TextView darkSky = findViewById(R.id.darkSkyAttribution);

        //Used to check for clicking links
        darkSky.setMovementMethod(LinkMovementMethod.getInstance());

        String apiKey = "e3386b28e1ae72def9071a009d5fa1e2";

        iconImage = findViewById(R.id.iconImageView);



        if (isNetworkAvailable())
        {

            String forecastUrl = "https://api.darksky.net/forecast/" + apiKey + "/" + latitude + "," + longitude;

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder().url(forecastUrl).build();

            Call call = client.newCall(request);
            call.enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    try {

                        String jsonData = response.body().string();

                        Log.v(TAG, jsonData);

                        if (response.isSuccessful())
                        {
                            currentWeather = getCurrentDetails(jsonData, currentCity);


                            final CurrentWeather displayWeather = new CurrentWeather(
                                    currentWeather.getLocationLabel(),
                                    currentWeather.getIcon(),
                                    currentWeather.getTime(),
                                    currentWeather.getTemperature(),
                                    currentWeather.getHumidity(),
                                    currentWeather.getPrecipChance(),
                                    currentWeather.getSummary(),
                                    currentWeather.getTimeZone()
                            );

                            binding.setWeather(displayWeather);

                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Drawable drawable = getResources().getDrawable(displayWeather.getIconId());
                                    iconImage.setImageDrawable(drawable);
                                }
                            });


                        }

                        else
                        {
                            alertUserAboutError();
                        }
                    }

                    catch (IOException e)
                    {
                        Log.e(TAG, "IO Exception caught: ", e);
                    }

                    catch (JSONException e)
                    {
                        Log.e(TAG, "JSON Exception Caught: ", e);
                    }
                }
            });
        }

        else
        {
            alertUserAboutNetworkError();
        }
    }

    private CurrentWeather getCurrentDetails(String jsonData, String currentCity) throws JSONException
    {
        JSONObject forecast = new JSONObject(jsonData);

        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setLocationLabel(currentCity);

        Log.i(TAG, "currentCity Value: " + currentCity);

        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);

        return currentWeather;
    }

    private void alertUserAboutNetworkError()
    {
        NetworkDialogError dialog = new NetworkDialogError();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    private boolean isNetworkAvailable()
    {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
        {
            return true;
        }
        //Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
        return false;
    }

    private void alertUserAboutError()
    {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    public void refreshOnClick(View view)
    {
        Toast.makeText(this, "Refreshing Data", Toast.LENGTH_LONG).show();

        String currentCity = currentLocation(latitude, longitude);
        getForecast(latitude, longitude, currentCity);
    }
}
