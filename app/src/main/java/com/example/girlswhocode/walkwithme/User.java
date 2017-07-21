package com.example.girlswhocode.walkwithme;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;

import static android.support.v4.app.ActivityCompat.requestPermissions;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;

/**
 * Created by Girls Who Code on 7/21/2017.
 */

public class User implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    String username;
    String email;
    String password;
    String phonenumber;
    String location;
    ArrayList<Friend> friends;
    String uid;
    GoogleApiClient googleApiClient;
    LatLng mLatLng;
    Location mLocation;
    LocationRequest mLocationRequest;
    Context context;
    Activity activity;

    //login
    public User(final FirebaseUser currUser) {
        email = currUser.getEmail();
        username = currUser.getDisplayName();
        uid = currUser.getUid();

        FirebaseDatabase.getInstance().getReference("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                password = dataSnapshot.child("password").getValue().toString();
                phonenumber = dataSnapshot.child("phonenumber").getValue().toString();
                location = dataSnapshot.child("location").getValue().toString();
                FirebaseDatabase.getInstance().getReference("users").child(uid).child("friends").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds: dataSnapshot.getChildren())
                        {
                            friends.add(new Friend(currUser, FirebaseDatabase.getInstance(), ds.getValue().toString()));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        friends = new ArrayList<Friend>();
    }

    public User(Parcel in){
        String[] data= new String[13];

        in.readStringArray(data);
        //this.username= data[0];
        //this.password= data[1];
        //this.email = data[2];
        //this.Action= Integer.parseInt(data[2]);
    }

    public String getUid()
    {
        return uid;
    }

    public void startLocation()
    {
        googleApiClient = new GoogleApiClient.Builder(context, this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("connected");
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermission
            requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location loc = FusedLocationApi.getLastLocation(googleApiClient);
        mLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        if(mLatLng == null) mLatLng = new LatLng(-71, 42.3);
        System.out.println(mLatLng.toString());

        mLocation = FusedLocationApi.getLastLocation(googleApiClient);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(50); //5 seconds; function takes millisecond parameter
        mLocationRequest.setFastestInterval(30); //3 seconds; function takes millisecond parameter
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

        Toast.makeText(context, "checking permission in on connected", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now

        } else {
            // permission has been granted, continue as usual
            Toast.makeText(context, "permission granted in on connected", Toast.LENGTH_SHORT).show();
            FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(context, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(context, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("Location changed!");
        Toast.makeText(context,"Location Changed",Toast.LENGTH_SHORT).show();
        mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    public void startUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                } else if ((ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                }
            }
        }
    }
    public void stopUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                }
            }
        }
    }

    public LatLng getLatLng()
    {
        return mLatLng;
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //Toast.makeText(this, "onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Toast.makeText(context, "requesting location updates from within onReqPermResults", Toast.LENGTH_SHORT).show();
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                } else {
                    Toast.makeText(context, "Need your location!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void setContext(Context newcontext)
    {
        context = newcontext;
    }

    public void setActivity(Activity newActivity)
    {
        activity = newActivity;
    }

    public ArrayList<String> getFriendNames()
    {
        final ArrayList<String> names = new ArrayList<String>();
        DatabaseReference currUserNode = FirebaseDatabase.getInstance().getReference("users").child(uid);
        System.out.println("Got the reference to the node for the current user: " + currUserNode.toString());
        DatabaseReference friendsNode = currUserNode.child("friends");
        System.out.println("Got a reference to the current user's friends: " + friendsNode.toString());

        System.out.println("Adding value event listener");
        friendsNode.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    System.out.println("Looking at snapshots");
                    String friendUserID = ds.getKey().toString();
                    System.out.println("Got the friend's user ID: " + friendUserID);

                    DatabaseReference users = FirebaseDatabase.getInstance().getReference().child("users");
                    System.out.println("Got the reference to the users node in the database");
                    Query usernameQuery = users.orderByKey().equalTo(friendUserID);
                    usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            System.out.println("Added listener for single value event");
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                if (ds.exists()) // checks if there is a friend
                                {
                                    System.out.println("Friend found " + ds);
                                    String friendUsername = ds.child("username").getValue().toString();
                                    System.out.println("Found friend's username: " + friendUsername);
                                    friends.add(new Friend(FirebaseAuth.getInstance().getCurrentUser(), FirebaseDatabase.getInstance(), friendUsername));
                                    //System.out.println(names.toString());
                                    names.add(friendUsername);
                                    // Potentially create a friend object, if you want to display information about friend
                                }
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("Something went wrong: " + databaseError);

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return names;
    }

}
