package com.example.user.aplikasipencarikuliner;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback {


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

    }


    private static final String TAG = "MainActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int ERROR_DIALOG_REQUEST = 9001;

    //vars
    DataHelper dbHelper;
    int PROXIMITY_RADIUS = 5000;
    double latitude,longitude;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    double end_latitude, end_longitude;
    String nama,alamat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServicesOK()){
            final View BottomSheet = findViewById(R.id.bottomsheet);
            //Menempatkan LinearLayout kedalam BottomSheetBehavior
            final BottomSheetBehavior SheetBehavior = BottomSheetBehavior.from(BottomSheet);
            //Set BottomSheet view agar dapat disembunyikan semuanya
            SheetBehavior.setHideable(true);
            SheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            getLocationPermission();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Place Services is working");
            return true;
        }else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;

    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            latitude = currentLocation.getLatitude();
                            longitude = currentLocation.getLongitude();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM);
                            getLocationRestaurant(latitude,longitude);

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MainActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }
    
    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MainActivity.this);
    }


    private void getLocationRestaurant(double latitude , double longitude){
        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        mMap.clear();
        String resturant = "restaurant";
        String url = getUrl(latitude, longitude, resturant);
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;

        getNearbyPlacesData.execute(dataTransfer);


        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Log.d(TAG, "getInfoContents: hihi "+marker.getTitle()+marker.getSnippet()+" "+marker.getPosition());


                //Inisialisasi LinearLayout sebagai bottom sheet view
                final View BottomSheet = findViewById(R.id.bottomsheet);
                //Menempatkan LinearLayout kedalam BottomSheetBehavior
                final BottomSheetBehavior SheetBehavior = BottomSheetBehavior.from(BottomSheet);
                //Set BottomSheet view agar dapat disembunyikan semuanya
                SheetBehavior.setHideable(false);
                SheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                TextView title = ((TextView) BottomSheet.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) BottomSheet.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                nama = marker.getTitle();
                alamat = marker.getSnippet();
                end_latitude = marker.getPosition().latitude;
                end_longitude =  marker.getPosition().longitude;

                return null;
            }
        });
        
        Toast.makeText(MainActivity.this, "Tampil Kuliner Terdekat", Toast.LENGTH_SHORT).show();
    }


    public void onClick(View v)
    {
        Object dataTransfer[] = new Object[2];

        //PolylineOptions options = new PolylineOptions();
        //polylineFinal = mMap.addPolyline(options);

        switch(v.getId()) {

            case R.id.direction:
                final View BottomSheet = findViewById(R.id.bottomsheet);
                final BottomSheetBehavior SheetBehavior = BottomSheetBehavior.from(BottomSheet);
                SheetBehavior.setHideable(false);

                dataTransfer = new Object[3];
                String url = getDirectionsUrl();
                GetDirectionsData getDirectionsData = new GetDirectionsData();
                dataTransfer[0] = mMap;
                dataTransfer[1] = url;
                dataTransfer[2] = new LatLng(end_latitude, end_longitude);
                getDirectionsData.execute(dataTransfer);
                SheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                Toast.makeText(this, "Tampil Direction", Toast.LENGTH_SHORT).show();
                break;

            case R.id.bookmarksimpan:


                dbHelper = new DataHelper(this);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                Log.d(TAG, "onClick: "+nama+alamat+end_latitude+end_longitude);
                        db.execSQL("insert into kuliner(no, nama, alamat, latitude, longitude) values('" +
                                5 + "','" +
                                nama + "','" +
                                alamat + "','" +
                                end_latitude + "','" +
                                end_longitude + "')");
                Toast.makeText(this, "berhasil bookmark", Toast.LENGTH_SHORT).show();
                Intent intentku =  new Intent(MainActivity.this, BookmarkActivity.class);
                startActivity(intentku);
                finish();
            break;

            case R.id.share:

                Intent i=new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "My Status");
                i.putExtra(Intent.EXTRA_TEXT, nama+","+alamat);
                startActivity(Intent.createChooser(i, "Share"));
                Toast.makeText(this, "Share Berhasil", Toast.LENGTH_SHORT).show();
                break;


        }
    }




    private String getDirectionsUrl()
    {
        StringBuilder googleDirectionsUrl = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        googleDirectionsUrl.append("origin="+latitude+","+longitude);
        googleDirectionsUrl.append("&destination="+end_latitude+","+end_longitude);
        googleDirectionsUrl.append("&key="+"AIzaSyC0sgWhxVbwyy4nIeSSUpC9xG_k6uXDlN0");

        return googleDirectionsUrl.toString();
    }



    private String getUrl(double latitude , double longitude , String nearbyPlace)
    {
        Log.d(TAG, "getUrl: ambil url");

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type="+nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key="+"AIzaSyAoJmSpD92tjLt9W87V3CKLfMK57ObENs4");

        Log.d(TAG, "getUrl: url = "+googlePlaceUrl.toString());

        return googlePlaceUrl.toString();
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "getLocationPermission: h1");
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "getLocationPermission: h2");
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                Log.d(TAG, "getLocationPermission: h3");
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            Log.d(TAG, "getLocationPermission: h4");
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home Dipilih", Toast.LENGTH_SHORT).show();
            Intent intentku =  new Intent(MainActivity.this, MainActivity.class);
            startActivity(intentku);
        } else if (id == R.id.nav_bookmark) {
            Toast.makeText(this, "Bookmark dipilih", Toast.LENGTH_SHORT).show();
            Intent intentku =  new Intent(MainActivity.this, BookmarkActivity.class);
            startActivity(intentku);
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "About Dipilih", Toast.LENGTH_SHORT).show();
            Intent intentku =  new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intentku);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}
