package com.bignerdranch.android.adexercise;

/**
 * Created by Christian on 3/3/2018.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class ActivityFindCourse extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    GoogleMap mGoogleMap;
    GoogleApiClient mGoogleApiClient;

    private EditText mSearchText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (googleServicesAvailable()) {
            Toast.makeText(this, "Perfect", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_find_course);
            // mSearchText = (EditText) findViewById(R.id.input_search);
            initMap();
        }
    }



    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) return true;
        else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Cant connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        // goToLocation();

        // mGoogleMap.setMyLocationEnabled(true);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker arg0) {
                if(arg0 != null && arg0.getTitle().equals("Mission Hills Golf Course")){
                    Intent intent1 = new Intent(ActivityFindCourse.this, MissionHills.class);
                    startActivity(intent1);}

                /*if(arg0 != null && arg0.getTitle().equals("German")){
                    Intent intent2 = new Intent(ActivityFindCourse.this, Skywest.class);
                    startActivity(intent2);}

                if(arg0 != null && arg0.getTitle().equals("Italian")){
                    Intent intent3 = new Intent(ActivityFindCourse.this, Fremont.class);
                    startActivity(intent3);}

                if(arg0 != null && arg0.getTitle().equals("Spanish")){
                    Intent intent4 = new Intent(ActivityFindCourse.this, Summitpointe.class);
                    startActivity(intent4);}
                    */
            }
                                                });
        LatLng missionHills = new LatLng(37.626471,-122.050156);
        LatLng skyWest = new LatLng(37.662246, -122.133347);
        LatLng fremont = new LatLng(37.557329, -121.957250);
        LatLng summitpoint = new LatLng(37.455172, -121.881510);
        LatLng monarchBay = new LatLng(37.694850, -122.184810);
        LatLng marinersPoint = new LatLng(37.572195, -122.283205);
        LatLng redwooodCanyon = new LatLng(37.726070, -122.081934);
        LatLng lakeChabot = new LatLng(37.741883, -122.120531);

        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.626471,-122.050156)).title("Mission Hills Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.662246, -122.133347)).title("Skywest Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.557329, -121.957250)).title("Fremont Park Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.455172, -121.881510)).title("Summitpointe Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.694850, -122.184810)).title("Monarch Bay Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.572195, -122.283205)).title("Mariners Point Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.726070, -122.081934)).title("Redwood Canyon Golf Course").snippet("(Click here to select!)"));
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(37.741883, -122.120531)).title("Lake Chabot Golf Course").snippet("(Click here to select!)"));

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(missionHills));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(skyWest));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(fremont));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(summitpoint));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(monarchBay));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(marinersPoint));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(redwooodCanyon));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lakeChabot));
    }

    private void goToLocation(double lat, double lng) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLng(ll);
        mGoogleMap.moveCamera(update);
    }

    private void goToLocationZoom(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mGoogleMap.moveCamera(update);
    }

    LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location == null) {
            Toast.makeText(this, "Can't get current location", Toast.LENGTH_LONG).show();
        } else {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            goToLocationZoom(location.getLatitude(), location.getLongitude(), 10);
        }
    }
}
