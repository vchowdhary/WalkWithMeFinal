package com.example.girlswhocode.walkwithme;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by main on 8/1/17.
 */

public class RouteGetter {
    static ArrayList<String> uids = new ArrayList<String>();
    static ArrayList<EncodedPolyline> userPolylines = new ArrayList<EncodedPolyline>();
    static ArrayList<List<LatLng>> routePoints = new ArrayList<>();
    static ArrayList<List<LatLng>> wayPoints = new ArrayList<>();
    static GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
    static String[] routePath = {""};
    static ArrayList<double[]> optimalities = new ArrayList<>();


    public RouteGetter(ArrayList<String> userIDs) {
        System.out.println("Created new RouteGetter object.");
        uids = new ArrayList<>();
        for (String userID : userIDs) uids.add(userID);
        System.out.println("All user ids: " + uids);
        userPolylines = new ArrayList<>();
        routePoints = new ArrayList<>();
        wayPoints = new ArrayList<>();
        optimalities = new ArrayList<>();
        routePath = new String[1];
    }
}