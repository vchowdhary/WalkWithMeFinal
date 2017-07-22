package com.example.girlswhocode.walkwithme;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Map;

import static android.R.attr.key;
import static android.R.attr.type;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    static MapActivity INSTANCE;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    ArrayList<String> uids = new ArrayList<String>();
    int counter;
    LatLng latLng;
    GoogleMap mGoogleMap;
    SupportMapFragment mFragment;
    Marker currLocationMarker;
    Marker newMarker;
    Friend janeDoe;
    Friend bobJones;
    Friend[] friends;
    User user;
    MyFirebaseMessagingService service = new MyFirebaseMessagingService();
    private BroadcastReceiver mMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

         mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract data included in the Intent
                String from = intent.getStringExtra("from");
                String body = intent.getStringExtra("body");
                String title = intent.getStringExtra("title");
                alert(from, body, title);
                //alert data here
            }
        };

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = new User(mAuth.getCurrentUser());
        user.setContext(MapActivity.this);
        user.setActivity(MapActivity.this);

        System.out.println("Created user.");

        mFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //gets fragment of map
        mFragment.getMapAsync(this);

        String username = "Vanshika Chowdhary";

        Log.wtf("MADE IT", "Created messaging service");
        Toast.makeText(MapActivity.this, "Created messaging service", Toast.LENGTH_SHORT).show();
        try {
            service.sendNotificationToUser("Friend Request", "shreyofsunshine", "Hi there");
            Log.wtf("WTF I ACTUALLY DID IT", "Notification sent to the damn user fool");
        }
        catch(Exception e)
        {
            Log.wtf("HELP", e.getMessage());
        }

        MyFirebaseInstanceIDService secondservice = new MyFirebaseInstanceIDService();
        secondservice.onRefreshToken();

        Button go = (Button) findViewById(R.id.doneButton);
        go.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
               //set up navigation here too
                //switch to panic activity

                TextView endField = (TextView) findViewById(R.id.endField);
                String destination = endField.getText().toString();
                String[] parts1 = destination.split(",");
                String input = "";
                for(int i = 0; i < parts1.length; i++)
                {
                    String[] parts = parts1[i].split(" ");
                    for(int j = 0; j < parts.length; j++)
                    {
                        input+=parts[j]+"+";
                    }
                    input+="%2C";
                }

                final String[] waypoints = {"42.2839,-71.654", "42.285659, -71.653883"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setTitle("Make your selection");
                final String finalInput = input;
                builder.setItems(waypoints, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.DONUT)
                    public void onClick(DialogInterface dialog, int item) {
                        // Do something with the selection
                        Toast.makeText(MapActivity.this, finalInput, Toast.LENGTH_SHORT).show();
                        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin="+ latLng.latitude +","+ latLng.longitude +"&destination="+ finalInput +"&travelmode=walking&waypoints=" + waypoints[item]);

                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void alert(String from, String body, final String title) {
        Toast.makeText(MapActivity.this, title, Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(title)
                .setMessage(body)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        if(title.contains("Friend Request"))
                        {
                            System.out.println("Accepted friend request");
                            service.sendNotificationToUser("Request accepted", "Vanshika Chowdhary", "The user clicked yes");
                        }
                        else if(title.contains("Request")) dialog.dismiss();
                            //TODO: implement this and move all this to the friendsctivity, requires two emulators working
                        // addFriend();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        if(title.contains("Friend Request"))
                            service.sendNotificationToUser("Request denied", "Vanshika Chowdhary", "The user clicked no");
                        else if(title.contains("Request")) dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_action:
                System.out.println("Switched to the map screen.");
                System.out.println("Already at the map screen.");
                return true;

            case R.id.friends_action:
                System.out.println("Switching to the friends activity.");
                Intent switchFriends = new Intent(MapActivity.this, FriendsActivity.class);
                startActivity(switchFriends);
                return true;

            case R.id.panic_action:
                System.out.println("Switching to the panic activity...");
                Intent switchPanic = new Intent(MapActivity.this, PanicActivity.class);
                startActivity(switchPanic);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    /**
     * This is activated by the OnMapReadyCallback interface when the map loads
     */
    public void onMapReady(GoogleMap gMap) {
        mGoogleMap = gMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mGoogleMap.setMyLocationEnabled(true);//sets blue point on map
        //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        buildGoogleApiClient();

        mGoogleApiClient.connect();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currUser = mAuth.getCurrentUser();

        FirebaseDatabase db = FirebaseDatabase.getInstance();

        /*janeDoe = new Friend(currUser, db, "Jane Doe");
        bobJones = new Friend(currUser, db, "Bob Jones");
        user.friends.add(janeDoe);
        user.friends.add(bobJones);*/

    }

    /**
     * Sets up the Google API Client
     */
    protected synchronized void buildGoogleApiClient() {
        Toast.makeText(this, "buildGoogleApiClient", Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)//handles connection information
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)//can add more apis here
                .build();
    }

    /**
     * Once Google API is built and connected, this starts the location requests
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        //Intent intent = this.getIntent();
        Toast.makeText(this, "Getting friends", Toast.LENGTH_SHORT).show();

        //Friend b = new Friend(FirebaseAuth.getInstance().getCurrentUser(), FirebaseDatabase.getInstance(), "Jane Doe");
        //b.addToMap(mGoogleMap, MapActivity.this);

           getFriends();
           System.out.println("Friends: " + uids);

           for(String uid: uids)
           {
               System.out.println("current friend: " + uid);
               Friend x = new Friend(uid);
               System.out.println(x.toString());
               x.addToMap(mGoogleMap, MapActivity.this);
           }

        Location mLastLocation = FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //place marker at current position
            //mGoogleMap.clear();
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));//,akes the marker pink
            currLocationMarker = mGoogleMap.addMarker(markerOptions);
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); //5 seconds; function takes millisecond parameter
        mLocationRequest.setFastestInterval(3000); //3 seconds; function takes millisecond parameter
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    10);
        } else {
            // permission has been granted, continue as usual
            FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void getFriends() {

        DatabaseReference currUserFriends = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("friends");
        currUserFriends.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren())
                {
                    //Toast.makeText(MapActivity.this, ds.getValue().toString(), Toast.LENGTH_SHORT).show();
                    //Toast.makeText(MapActivity.this, ds.getKey().toString(), Toast.LENGTH_SHORT).show();
                    uids.add(ds.getKey());
                }
                System.out.println("Getting uids of friends: " + uids);
                for(String uid: uids)
                {
                    System.out.println("current friend: " + uid);
                    Friend x = new Friend(uid);
                    System.out.println(x.toString());
                    x.addToMap(mGoogleMap, MapActivity.this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Once the permission request is granted
     * @param requestCode the code hardcoded to represent the location request
     * @param permissions what you want
     * @param grantResults what the user enteredt
     */
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 10) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                   //this should be true, so nothing happens here
                    return;
                }
                FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    public void startUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                } else if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }
            }
        }
    }
    public void stopUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed",Toast.LENGTH_SHORT).show();
    }

    /**
     * This is called by LocationListener once the requests start (FusedLocationAPI)
     * @param location - LatLng
     */
    @Override
    public void onLocationChanged(final Location location) {

        //place marker at current position
        //mGoogleMap.clear();
        final FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference users = db.getReference("users");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser currUser = mAuth.getCurrentUser();


        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        users.child(currUser.getUid()).child("location").setValue(location.getLatitude() + ", " + location.getLongitude());

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        currLocationMarker = mGoogleMap.addMarker(markerOptions);

        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();

        //zoom to current position:
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));

    }
        public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public static MapActivity getActivityInstance()
    {
        return INSTANCE;
    }

    public LatLng getData()
    {
        return latLng;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(MapActivity.this).registerReceiver(mMessageReceiver,
                new IntentFilter("myFunction"));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(MapActivity.this).unregisterReceiver(mMessageReceiver);
    }
}



