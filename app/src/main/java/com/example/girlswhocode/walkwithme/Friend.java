package com.example.girlswhocode.walkwithme;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Girls Who Code on 7/19/2017.
 */

public class Friend {
    String uid;
    String username;
    String email;
    String password;
    String phoneNumber;
    Marker friendMarker;
    String location;
    MarkerOptions markerOptions = new MarkerOptions();
    boolean done = false;

    public Friend(final GoogleMap mGoogleMap, final Context context, final FirebaseUser currUser, final FirebaseDatabase db, final String s) {
        final DatabaseReference currUserNode = db.getReference("users").child(currUser.getUid());
        Toast.makeText(context, "got reference to current user", Toast.LENGTH_SHORT).show();

        final Query query = db.getReference("users").orderByChild("username").equalTo(s);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // dataSnapshot is the "issue" node with all children with id 0
                    for (DataSnapshot issue : dataSnapshot.getChildren()) {
                        // do something with the individual "issues"
                        db.getReference("users").child(currUser.getUid()).child("friends").child(issue.getKey()).setValue("");
                        Toast.makeText(context, "Added user " + s + "to your friends", Toast.LENGTH_SHORT).show();
                        //query.removeEventListener(this);
                        uid = issue.getKey();
                        System.out.println(uid);
                        if(uid!=null) done = true;
                        username = String.valueOf(issue.child("username").getValue());
                        email = String.valueOf(issue.child("email").getValue());
                        password = String.valueOf(issue.child("password").getValue());
                        phoneNumber = String.valueOf(issue.child("phonenumber").getValue());
                        location = String.valueOf(issue.child("location").getValue());

                        db.getReference("users").child(uid).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot ds) {
                                Toast.makeText(context, ds.child("username").getValue() + " was changed", Toast.LENGTH_SHORT).show();
                                if (friendMarker != null) friendMarker.remove();
                                String position = ds.child("location").getValue().toString();
                                Toast.makeText(context, position, Toast.LENGTH_SHORT).show();
                                    String[] parts = position.split(",");
                                    markerOptions.position(new LatLng(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                                    markerOptions.title(username);
                                    friendMarker = mGoogleMap.addMarker(markerOptions);
                                }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }
                } else
                    Toast.makeText(context, "user does not exist", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}