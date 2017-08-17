package com.example.girlswhocode.walkwithme;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import com.google.maps.model.TravelMode;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Runnable {
    static MapActivity INSTANCE;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

     GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
     String[] routePath = {""};
     ArrayList<String> userIdsToFindPolylines = new ArrayList<>();
     ArrayList<EncodedPolyline> userPolylines = new ArrayList<>();
     ArrayList<List<com.google.maps.model.LatLng>> routePoints = new ArrayList<>();
     ArrayList<ArrayList<com.google.maps.model.LatLng>> wayPoints = new ArrayList<>();
     ArrayList<double[]> optimalities = new ArrayList<>();
     RouteGetter getRoute;

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
    BlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<>();
    Executor executor = new ThreadPoolExecutor(100, 500, (long) 10000, TimeUnit.MILLISECONDS, workQueue);

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
        Toast.makeText(MapActivity.this, mAuth.getCurrentUser().getUid().toString(), Toast.LENGTH_SHORT).show();
        user = new User(mAuth.getCurrentUser());
        user.setContext(MapActivity.this);
        user.setActivity(MapActivity.this);

        System.out.println("Created user.");

        mFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //gets fragment of map
        mFragment.getMapAsync(this);

        final String[] username = {"Vanshika Chowdhary"};

        Log.wtf("MADE IT", "Created messaging service");
        Toast.makeText(MapActivity.this, "Created messaging service", Toast.LENGTH_SHORT).show();
        DatabaseReference user = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getCurrentUser().getUid().toString()).child("username");
        user.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                username[0] = dataSnapshot.getValue().toString();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // service.sendNotificationToUser("Friend Request", "shreyofsunshine", "Hi there", username[0]);
        Log.wtf("WTF I ACTUALLY DID IT", "Notification sent to the damn user fool");

        userIdsToFindPolylines.add(FirebaseAuth.getInstance().getCurrentUser().getUid());

        MyFirebaseInstanceIDService secondservice = new MyFirebaseInstanceIDService();
        secondservice.onRefreshToken();

        Button go = (Button) findViewById(R.id.doneButton);
        go.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                //Getting the user's destination from the textfield
                final TextView endField = (TextView) findViewById(R.id.endField);
                String destination = endField.getText().toString();
                String[] parts1 = destination.split(",");
                String input = "";
                for (int i = 0; i < parts1.length; i++) {
                    String[] parts = parts1[i].split(" ");
                    for (int j = 0; j < parts.length; j++) {
                        input += parts[j] + "+";
                    }
                    input += "%2C";
                }

                DatabaseReference db = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("destination");
                db.setValue(destination);
                System.out.println("Destination updated");

                System.out.println("Getting the user's route polyline");
                System.out.println(userIdsToFindPolylines);
                for (String userID : userIdsToFindPolylines)
                {
                    findRoutes(userID);
                }
            }
        });
    }

    public ArrayList<List<com.google.maps.model.LatLng>> findRoutes(String userID)
    {
        System.out.println("USER ID: " + userID);
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference user = db.getReference().child("users").child(userID);
        System.out.println("Got reference to the user when trying to make route request: " + user.toString());


        // set origin
        final DatabaseReference[] location = {user.child("location")};
        System.out.println("Got reference to the user's location when trying to make route request: " + "User id: " + userID + "/" + location[0].toString());
        final String[] userLocation = new String[1];

        location[0].addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println("Location accessed from the database--trying to make request: " + "User id: " + userID + "/" + dataSnapshot.getValue().toString());
                userLocation[0] = dataSnapshot.getValue().toString();
                System.out.println("User location from array right after being updated: " + "User id: " + userID + "/" + userLocation[0]);

                DatabaseReference destination = user.child("destination");
                final String[] userDestination = new String[1];
                destination.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue().toString().equals("null")) routePath[0] = "Invalid: no destination";
                        userDestination[0] = dataSnapshot.getValue().toString();
                        System.out.println("User's destination after being retrieved from the database: " + "User id: " + userID + "/" + userDestination[0]);

                        DirectionsApiRequest routeRequest = DirectionsApi.newRequest(context);
                        System.out.println("User location retrieved from the array: " + "User id: " + userID + "/" + userLocation[0]);
                        String[] locationLatLng = userLocation[0].split(", ");
                        com.google.maps.model.LatLng userLocationCoord = new com.google.maps.model.LatLng(Double.parseDouble(locationLatLng[0]), Double.parseDouble(locationLatLng[1]));
                        System.out.println("Route request's origin: " + "User id: " + userID + "/" + userLocationCoord);
                        routeRequest.origin(userLocationCoord);
                        routeRequest.destination(userDestination[0]);
                        System.out.println("Route request's destination: " + userDestination[0]);
                        routeRequest.mode(TravelMode.WALKING);
                        System.out.println("Route request after all qualities have been added: " + "User id: " + userID + "/" + routeRequest.toString());

                        routeRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                            @Override
                            public void onResult(DirectionsResult result) {
                                EncodedPolyline userRoutePolyline = result.routes[0].overviewPolyline;
                                System.out.println("User's route polyline for uid " + userID + ": " + userRoutePolyline);
                                userPolylines.add(userRoutePolyline);
                                System.out.println("All polylines found: " + userPolylines);
                                if (!routePoints.contains(userRoutePolyline.decodePath())) routePoints.add(userRoutePolyline.decodePath());
                                System.out.println("Number of decoded polylines in routePoints: " + routePoints.size());
                                System.out.println("All items in routePoints: " + routePoints);
                                optimalities = findOptimalities();


                            }
                            @Override
                            public void onFailure(Throwable e) {
                            }
                        });
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
        return routePoints;
    }

    public ArrayList<double[]> findOptimalities()
    {
        System.out.println("Finding optimalities");
        System.out.println("RoutePoints size: " + routePoints.size());
        for (int i = 1; i < routePoints.size(); i++)
        {
            List<com.google.maps.model.LatLng> userRoute = routePoints.get(0);
            List<com.google.maps.model.LatLng> friendRoute = routePoints.get(i);
            double userRouteDist = distance(userRoute.get(0).lat, userRoute.get(userRoute.size()-1).lat, userRoute.get(0).lng, userRoute.get(userRoute.size() -1).lng, 0, 0);
            double friendRouteDist = distance(friendRoute.get(0).lat, friendRoute.get(friendRoute.size()-1).lat, friendRoute.get(0).lng, friendRoute.get(friendRoute.size()-1).lng, 0, 0);
            int[] intersectionPts = findIntersection(userRoute, friendRoute);
            System.out.println("User Route Distance: " + userRouteDist);
            System.out.println("Friend Route Distance: " + friendRouteDist);
            System.out.println("Intersection Points: " + intersectionPts);
            double intersectionDist = distance(userRoute.get(intersectionPts[0]).lat, userRoute.get(intersectionPts[1]).lat,
                    userRoute.get(intersectionPts[0]).lng, userRoute.get(intersectionPts[1]).lng,
                    0, 0);
            double optimality = intersectionDist/userRouteDist;
            System.out.println("Optimality found: " + optimality);
            double[] optimalityArr = {optimality, (double) i};
            optimalities.add(optimalityArr);
            System.out.println("Opimalities ArrayList at this point: " + optimalities);
        }
        sortOptimalities();
        return optimalities;
    }

    public int[] findIntersection(List<com.google.maps.model.LatLng> routePoints, List<com.google.maps.model.LatLng> usrRtPts) {
        System.out.println("Finding intersection points");
        int i = 0;
        int j = 0;
        int startIntersectionIndex = -1;
        System.out.println("Finding start intersection");
        while (i < routePoints.size() && j < usrRtPts.size())
        {
            com.google.android.gms.maps.model.LatLng routePointLatLng = new com.google.android.gms.maps.model.LatLng(routePoints.get(i).lat, routePoints.get(i).lng);
            com.google.android.gms.maps.model.LatLng usrRtPtLatLng = new com.google.android.gms.maps.model.LatLng(usrRtPts.get(j).lat, usrRtPts.get(j).lng);
            double distance = distance(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude, 0, 0);
            //  System.out.println("distance between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + distance);
            double bearing = findBearing(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude);
            //  System.out.println("bearing between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + bearing);
            if (distance > 10 )
            {
                if (bearing < 0)
                {
                    // System.out.println("farther behind; need to move up");
                    j++;
                }
                else
                {
                    // System.out.println("further ahead, need the other route to move up");
                    i++;
                }
            }
            else
            {
                startIntersectionIndex = i;
                break;
            }
        }
        System.out.println("Start Intersection Index: " + startIntersectionIndex);

        System.out.println("route points size: " + routePoints.size());
        System.out.println("usr rt pts size: " + usrRtPts.size());
        int l = routePoints.size() - 1;
        int m = usrRtPts.size() - 1;
        int endIntersectionIndex = -1;
        System.out.println("Finding end intersection");
        while (l > 0 && m > 0)
        {
            com.google.android.gms.maps.model.LatLng routePointLatLng = new com.google.android.gms.maps.model.LatLng(routePoints.get(l).lat, routePoints.get(l).lng);
            com.google.android.gms.maps.model.LatLng usrRtPtLatLng = new com.google.android.gms.maps.model.LatLng(usrRtPts.get(m).lat, usrRtPts.get(m).lng);
            double distance = distance(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude, 0, 0);
//            System.out.println("distance between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + distance);
            double bearing = findBearing(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude);
//            System.out.println("bearing between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + bearing);
            if (distance > 10)
            {
                if(bearing < 0)
                {
//                    System.out.println("further behind; need to move up");
                    m--;
                }
                else
                {
//                    System.out.println("further ahead, need the other route to move up");
                    l--;
                }
            }
            else
            {
                endIntersectionIndex = l;
                break;
            }
        }
        System.out.println("End Intersection Index: " + endIntersectionIndex);

        int[] intersectionPts = {startIntersectionIndex, endIntersectionIndex};
//        System.out.println("intersection pts: " + intersectionPts);
        return intersectionPts;
    }

    private double distance(double lat1, double lat2, double lon1,
                                   double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    private double findBearing(double lat1, double lat2, double lon1,
                                      double lon2)
    {
        double y = Math.sin(lon2-lon1)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1);
        return Math.atan2(y, x)*180/Math.PI;
    }

    public ArrayList<double[]> sortOptimalities() {
        System.out.println("Sorting optimalities");
        double maxOptimality = 0;
        int index = 0;
        while (index < optimalities.size())
        {
            System.out.println("Max optimality at this point: " + maxOptimality);
            System.out.println("Optimality of current element: " + optimalities.get(index)[0]);
            if (optimalities.get(index)[0] >= maxOptimality)
            {
                maxOptimality = optimalities.get(index)[0];
                optimalities.add(0, optimalities.remove(index));
            }
            index++;
        }

        for (double[] optim : optimalities)
        {
            System.out.println("Optimality: " + optim[0] + "/ index in routePoints: " + optim[1]);
        }
        System.out.println("Index of the most optimal friend: " + (int) optimalities.get(0)[1]);
        System.out.println("All user ids to find polylines for: " + uids);
        System.out.println("All polylines found: " + userPolylines);
        System.out.println("All routes found: " + routePoints);
        int optimIndex = (int) optimalities.get(0)[1];
        System.out.println("Chosen user id: " + uids.get(optimIndex-1));
        System.out.println("Chosen polyline: " + userPolylines.get(optimIndex));
        System.out.println("Chosen route: " + routePoints.get(optimIndex));
        setOptimalities();

        return optimalities;
    }

    public void setOptimalities() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(MapActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(MapActivity.this);
        }

        ArrayList<String> top3friends = new ArrayList<String>();
        DatabaseReference usernames = FirebaseDatabase.getInstance().getReference("users");
        usernames.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(int i = 0; i < optimalities.size(); i++)
                {
                    String newUsername = dataSnapshot.child(uids.get(((int) (optimalities.get(i)[1] - 1)))).child("username").getValue().toString();
                    if(!top3friends.contains(newUsername))
                        top3friends.add(newUsername);
                }
                System.out.println("added all friends to list based on optimalities");

                final CharSequence[] top3friendsList;
                if(top3friends.size() >= 3)
                    top3friendsList = new CharSequence[]{top3friends.get(0), top3friends.get(1), top3friends.get(2)};
                else if(top3friends.size() == 2)
                    top3friendsList = new CharSequence[]{top3friends.get(0), top3friends.get(1)};
                else if(top3friends.size() == 1)
                    top3friendsList = new CharSequence[]{top3friends.get(0)};
                else
                    top3friendsList = new CharSequence[1];

                System.out.println("creating builder");
                AlertDialog.Builder newbuilder = new AlertDialog.Builder(MapActivity.this);
                newbuilder.setTitle("Choose a friend to walk with");
                newbuilder.setItems(top3friendsList, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.out.println(top3friendsList[i]);
                        int[] intersections = findIntersection(routePoints.get(0), routePoints.get((int) optimalities.get(i)[1]));
                        int startInt = intersections[0];
                        int endInt = intersections[1];
                        getWayPoints(routePoints.get(0), startInt, endInt);
                    }
                });

                AlertDialog finalAlert = newbuilder.create();
                finalAlert.show();
                //alert = newbuilder.show();
                System.out.println("showing builder");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getWayPoints(List<com.google.maps.model.LatLng> latLngs, int startInt, int endInt) {
        System.out.println("Getting wayPoints");
        DirectionsApiRequest wayPointsReq = DirectionsApi.newRequest(context);
        wayPointsReq.origin(routePoints.get(0).get(startInt));
        wayPointsReq.destination(routePoints.get(0).get(endInt));
        wayPointsReq.mode(TravelMode.WALKING);
        System.out.println("WayPoints request created: " + wayPointsReq);
        wayPointsReq.setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                System.out.println("Got result: " + result);
                List<com.google.maps.model.LatLng> points = result.routes[0].overviewPolyline.decodePath();
                System.out.println("Waypoints found: " + points);
                System.out.println("Number of waypoints found: " + points.size());
                ArrayList<com.google.maps.model.LatLng> wayPtsToBeAdded = new ArrayList<>();
                if (points.size() <= 23)
                {
                    for (com.google.maps.model.LatLng point : points) wayPtsToBeAdded.add(point);
                    System.out.println("Size of wayPtsToBeAdded when points size <= 23: " + wayPtsToBeAdded.size());
                    com.google.maps.model.LatLng startLatLng = routePoints.get(0).get(0);
                    System.out.println("Received startLatLng: " + startLatLng);
                    com.google.maps.model.LatLng endLatLng = routePoints.get(0).get(routePoints.get(0).size()-1);
                    System.out.println("Received endLatLng: " + endLatLng);
                    System.out.println("Creating url...");
                    createUrl(startLatLng, endLatLng, wayPtsToBeAdded);
                }
                else
                {
                    System.out.println(points.size()/23.0);
                    double factor = (double) (Math.round((double) points.size()/23.0 * Math.pow(10, 1)))/Math.pow(10, 1);
                    factor = (int) (Math.round(factor));
                    System.out.println("Factor: " + factor);
                    for (int i = 0; i < points.size(); i+=factor)
                    {
                        wayPtsToBeAdded.add(points.get(i));
                    }
                    System.out.println("Size of wayPtsToBeAdded when points size > 23: " + wayPtsToBeAdded.size());
                    if (wayPtsToBeAdded.size() > 23)
                    {
                        int diff = wayPtsToBeAdded.size() - 23;
                        System.out.println("DIFF: " + diff);
                        for (int i = 22; i < wayPtsToBeAdded.size()-1+diff; i++)
                        {
                            System.out.println(wayPtsToBeAdded.get(i));
                            wayPtsToBeAdded.remove(i);
                        }
                    }
                    com.google.maps.model.LatLng startLatLng = routePoints.get(0).get(0);
                    System.out.println("Received startLatLng: " + startLatLng);
                    com.google.maps.model.LatLng endLatLng = routePoints.get(0).get(routePoints.get(0).size()-1);
                    System.out.println("Received endLatLng: " + endLatLng);
                    System.out.println("Creating url...");
                    createUrl(startLatLng, endLatLng, wayPtsToBeAdded);
                    //System.out.println("Size of wayPtsToBeAdded after additional check for right number of waypoints: " + wayPtsToBeAdded.size());
                }

            }

            @Override
            public void onFailure(Throwable e) {

            }
        });
    }

    private void createUrl(com.google.maps.model.LatLng latLng, com.google.maps.model.LatLng latLng1, ArrayList<com.google.maps.model.LatLng> wayPtsToBeAdded)
    {
        System.out.println("Starting LatLng: " + latLng);
        System.out.println("Ending latLng: " + latLng1);
        System.out.println("Points in between: " + wayPtsToBeAdded);

        String start = "https://www.google.com/maps/dir/?api=1&";
        start+="origin="+latLng.toUrlValue();
        start+="&destination="+latLng1.toUrlValue();
        start+="&travelmode=walking&waypoints=";
        for (int i = 0; i < wayPtsToBeAdded.size()-1;i++)
        {
            start+=wayPtsToBeAdded.get(i).toUrlValue()+"%7C";
        }
        start+=wayPtsToBeAdded.get(wayPtsToBeAdded.size()-1).toUrlValue();
        System.out.println(start);
        startIntent(start);
    }

    private void startIntent(String start) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(start));
        startActivity(intent);
    }

    private void alert(final String from, String body, final String title) {
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
                    public void onClick(final DialogInterface dialog, int which) {
                        // continue with delete
                        DatabaseReference user = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("username");
                        user.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(title.equals("Walk With Me: Friend Request"))
                                {

                                    System.out.println("Accepted friend request");
                                    service.sendNotificationToUser("Request accepted", from, dataSnapshot.getValue().toString() +" clicked yes", dataSnapshot.getValue().toString());
                                }
                                else if(title.contains("Request")) dialog.dismiss();
                                //TODO: implement this and move all this to the friendsctivity, requires two emulators working
                                // addFriend();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {
                        // do nothing
                        DatabaseReference user = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("username");
                        user.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(title.equals("Walk With Me: Friend Request"))
                                    service.sendNotificationToUser("Request denied", from, "The user clicked no", dataSnapshot.getValue().toString());
                                else if(title.contains("Request")) dialog.dismiss();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

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

            case R.id.settings_action:
                startActivity(new Intent(MapActivity.this, SettingsActivity.class));
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
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
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

                for (String uid : uids)
                {
                    userIdsToFindPolylines.add(uid);
                }
                System.out.println("All items in userIdsToFindPolylines: " + userIdsToFindPolylines);
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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        return Actions.newView("Map", "http://[ENTER-YOUR-URL-HERE]");
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().start(getIndexApiAction());
    }

    @Override
    public void onStop() {

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        FirebaseUserActions.getInstance().end(getIndexApiAction());
        super.onStop();
    }

    @Override
    public void run() {

    }
}



