package com.example.girlswhocode.walkwithme;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Runnable {
    static MapActivity INSTANCE;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    static GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
    static String[] routePath = {""};
    static ArrayList<String> userIdsToFindPolylines = new ArrayList<>();
    static ArrayList<EncodedPolyline> userPolylines = new ArrayList<>();
    static ArrayList<List<com.google.maps.model.LatLng>> routePoints = new ArrayList<>();
    ArrayList<List<com.google.maps.model.LatLng>> wayPoints = new ArrayList<>();
    static ArrayList<double[]> optimalities = new ArrayList<>();
    static RouteGetter getRoute;

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

                getRoute = new RouteGetter(userIdsToFindPolylines);


                CompletableFuture routes = CompletableFuture.supplyAsync(() -> RouteGetter.findRoutes());
                /*routes.thenAccept(new Consumer() {
                    @Override
                    public void accept(Object o) {
                        System.out.println("CompleteableFuture routes is done. Printing optimalities");
                        setOptimalities();
                    }
                });*/

              //  CompletableFuture getRoutes = CompletableFuture.supplyAsync(() -> RouteGetter.findRoutes()).thenRun(() -> System.out.println(getRoutes.isDone() + ": " + RouteGetter.routePoints));
               // thenRun(() -> System.out.println("Route Points from GetRoute: " + RouteGetter.routePoints));
//                CompletableFuture optimalities = getRoutes.thenComposeAsync(RouteGetter::findOptimalities).thenRunAsync(RouteGetter::sortOptimalities).thenRun(MapActivity::setOptimalities);
//                optimalities.complete(null);
//                System.out.println(optimalities);
              //  CompletableFuture getOptimalities = getRoutes.thenApplyAsync(RouteGetter::findOptimalities).thenAcceptAsync(RouteGetter::sortOptimalities).thenAccept(MapActivity::setOptimalities);
            }
        });
    }

    public static void setOptimalities() {
        optimalities = getRoute.optimalities;
        System.out.println("Updated optimalities after callbacks & completable futures: " + optimalities);
    }

    private int[] findIntersection(List<com.google.maps.model.LatLng> routePoints, List<com.google.maps.model.LatLng> usrRtPts) {
        System.out.println("Finding intersection points");
        int i = 0;
        int j = 0;
        int startIntersectionIndex = -1;
        System.out.println("Finding start intersection");
        while (i < routePoints.size() || j < usrRtPts.size())
        {
            LatLng routePointLatLng = new LatLng(routePoints.get(i).lat, routePoints.get(i).lng);
            LatLng usrRtPtLatLng = new LatLng(usrRtPts.get(j).lat, usrRtPts.get(j).lng);
            double distance = distance(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude, 0, 0);
            //System.out.println("distance between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + distance);
            double bearing = findBearing(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude);
            //System.out.println("bearing between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + bearing);
            if (distance > 10 )
            {
                if (bearing < 0)
                {
                    //System.out.println("farther behind; need to move up");
                    j++;
                }
                else
                {
                    //System.out.println("further ahead, need the other route to move up");
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
        while (l > 0 || m > 0)
        {
            LatLng routePointLatLng = new LatLng(routePoints.get(l).lat, routePoints.get(l).lng);
            LatLng usrRtPtLatLng = new LatLng(usrRtPts.get(m).lat, usrRtPts.get(m).lng);
            double distance = distance(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude, 0, 0);
            //System.out.println("distance between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + distance);
            double bearing = findBearing(routePointLatLng.latitude, usrRtPtLatLng.latitude, routePointLatLng.longitude, usrRtPtLatLng.longitude);
            //System.out.println("bearing between " + routePointLatLng + " and " + usrRtPtLatLng + ": " + bearing);
            if (distance > 10)
            {
                if(bearing < 0)
                {
                    //System.out.println("further behind; need to move up");
                    m--;
                }
                else
                {
                    //System.out.println("further ahead, need the other route to move up");
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
        System.out.println("intersection pts: " + intersectionPts);
        return intersectionPts;
    }


    // TODO Auto-generated method stub
            /*
                apiRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                    // Handle result
                    @Override
                    public void onResult(DirectionsResult result) {
                        // Get routes
                        DirectionsRoute[] routes = result.routes;
                        System.out.println("ROUTES FOUND: " + routes);
                        // For each route
                        for (DirectionsRoute route : routes)
                        {
                            // Get all points in the polyline
                            List<com.google.maps.model.LatLng> routePoints = route.overviewPolyline.decodePath();
                            System.out.println("POINTS IN ROUTE: " + routePoints);
                          //  System.out.println(userPoints);
                            userRoutes.add(routePoints);
                            System.out.println("userRoutes after added: " + userRoutes);

                        }

                        //System.out.println(userRoutes);

                        ArrayList<String[]> optimalFriends = new ArrayList<String[]>();
                        List<LatLng> friendPoints;
                        for (String uid: uids)
                        {
                            GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
                            DirectionsApiRequest friendRouteRequest = DirectionsApi.newRequest(context);
                            FirebaseDatabase datab = FirebaseDatabase.getInstance();
                            DatabaseReference location = datab.getReference("users").child(uid).child("location");
                            location.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    System.out.println(dataSnapshot.toString());
                                    friendLoc = dataSnapshot.getValue().toString();
                                    System.out.println("Friend's location retrieved from db: " + friendLoc);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            DatabaseReference dest = datab.getReference("users").child(uid).child("destination");

                            dest.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    friendDest = dataSnapshot.getValue().toString();
                                    System.out.println("Destination retrieved from database: " + friendDest);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            //System.out.println(friendLoc);
                            String[] fLoc = friendLoc.split(",");
                            //System.out.println(friendDest);
                            friendRouteRequest.origin(new com.google.maps.model.LatLng(Double.parseDouble(fLoc[0]), Double.parseDouble(fLoc[1])));
                            friendRouteRequest.destination(friendDest);
                            friendRouteRequest.mode(TravelMode.WALKING);
                            friendRouteRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                                @Override
                                public void onResult(DirectionsResult result) {
                                    System.out.println("Friend route finding request successful");
                                    DirectionsRoute[] routes = result.routes;
                                    for (DirectionsRoute route : routes)
                                    {
                                        List<com.google.maps.model.LatLng> routePts = route.overviewPolyline.decodePath();
                                        friendRoutes.add(routePts);
                                    }
                                    System.out.println("Possible friend routes: " + friendRoutes);
                                    System.out.println("friendRoutes size: " + friendRoutes.size());
                                    System.out.println("userRoutes size: " + userRoutes.size());

                                    if (friendRoutes.size() < userRoutes.size())
                                    {
                                        System.out.println("FriendRoutes < userRoutes");
                                        for (final List<com.google.maps.model.LatLng> routePoints : friendRoutes)
                                        {
                                            for (List<com.google.maps.model.LatLng> usrRtPts : userRoutes)
                                            {
                                                //System.out.println("Friend route: " + routePoints);
                                                //System.out.println("User route: " + usrRtPts);
                                                int[] intersectionPts = findIntersection(routePoints, usrRtPts);
                                                System.out.println("Intersection Points: " + intersectionPts);

                                                System.out.println("Finding waypoints");
                                                com.google.maps.model.LatLng startIntersection = routePoints.get(intersectionPts[0]);
                                                com.google.maps.model.LatLng endIntersection = routePoints.get(intersectionPts[1]);
                                                GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
                                                DirectionsApiRequest wayPointsRequest = DirectionsApi.newRequest(context);
                                                wayPointsRequest.origin(startIntersection);
                                                wayPointsRequest.destination(endIntersection);
                                                wayPointsRequest.mode(TravelMode.WALKING);
                                                final com.google.maps.model.LatLng firstRoutePoint = routePoints.get(0);
                                                final com.google.maps.model.LatLng lastRoutePoint = routePoints.get(routePoints.size() - 1);
                                                wayPointsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                                                    @Override
                                                    public void onResult(DirectionsResult result) {
                                                        wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                        System.out.println("Found waypoints successfully");
                                                        DirectionsRoute[] routesFound = result.routes;
                                                        ArrayList<com.google.maps.model.LatLng> routeWayPoints = (ArrayList<com.google.maps.model.LatLng>) routesFound[0].overviewPolyline.decodePath();
                                                        System.out.println("Number of way points found: " + routeWayPoints.size());
                                                        if (routeWayPoints.size() <= 23)
                                                        {
                                                            wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                            for (com.google.maps.model.LatLng routeWayPoint : routeWayPoints)
                                                            {
                                                                wayPoints.add(routeWayPoint);
                                                            }
                                                            System.out.println("Waypoints: " + wayPoints);
                                                            double intersectionDist = distance(wayPoints.get(0).lat, wayPoints.get(wayPoints.size()-1).lat,
                                                                    wayPoints.get(0).lng, wayPoints.get(wayPoints.size()-1).lng,
                                                                    0, 0);
                                                            System.out.println("Intersection dist: " + intersectionDist);
                                                            double routeLength = distance(firstRoutePoint.lat, lastRoutePoint.lat,
                                                                    firstRoutePoint.lng, lastRoutePoint.lng,
                                                                    0, 0);
                                                            System.out.println("Route length: " + routeLength);
                                                            double percentage = intersectionDist/routeLength;
                                                            System.out.println("Percentage: " + percentage);
                                                            percentageOverlapsForFriend.add(percentage);
                                                        }
                                                        else
                                                        {
                                                            System.out.println(routeWayPoints.size()/23.0);
                                                            double factor = (double) (Math.round((double) routeWayPoints.size()/23.0 * Math.pow(10, 1)))/Math.pow(10, 1);
                                                            factor = (int) (Math.round(factor));
                                                            System.out.println("Factor: " + factor);
                                                            int index = 0;
                                                            wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                            while (index < routeWayPoints.size())
                                                            {
                                                                wayPoints.add(routeWayPoints.get(index));
                                                                index += factor;

                                                            }
                                                            System.out.println("Number of waypoints, final: " + wayPoints.size());
                                                            System.out.println("Waypoints: " + wayPoints);

                                                            double intersectionDist = distance(wayPoints.get(0).lat, wayPoints.get(wayPoints.size()-1).lat,
                                                                    wayPoints.get(0).lng, wayPoints.get(wayPoints.size()-1).lng,
                                                                    0, 0);
                                                            System.out.println("Intersection dist: " + intersectionDist);
                                                            double routeLength = distance(firstRoutePoint.lat, lastRoutePoint.lat,
                                                                    firstRoutePoint.lng, lastRoutePoint.lng,
                                                                    0, 0);
                                                            System.out.println("Route length: " + routeLength);
                                                            double percentage = intersectionDist/routeLength;
                                                            System.out.println("Percentage: " + percentage);
                                                            percentageOverlapsForFriend.add(percentage);
                                                        }

                                                    }

                                                    @Override
                                                    public void onFailure(Throwable e) {

                                                    }
                                                });

                                            }
                                        }
                                    }
                                    else
                                    {
                                        System.out.println("UserRoutes > FriendRoutes");
                                        for (final List<com.google.maps.model.LatLng> routePoints: userRoutes)
                                        {
                                            for (List<com.google.maps.model.LatLng> usrRtPts : friendRoutes)
                                            {
                                                int[] intersectionPts = findIntersection(routePoints, usrRtPts);
                                                //System.out.println(intersectionPts);
                                                System.out.println("Finding waypoints");
                                                com.google.maps.model.LatLng startIntersection = routePoints.get(intersectionPts[0]);
                                                com.google.maps.model.LatLng endIntersection = routePoints.get(intersectionPts[1]);
                                                GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBORcg3FJS35RW4G8bCddA-jcGyQc7M6Vk");
                                                DirectionsApiRequest wayPointsRequest = DirectionsApi.newRequest(context);
                                                wayPointsRequest.origin(startIntersection);
                                                wayPointsRequest.destination(endIntersection);
                                                wayPointsRequest.mode(TravelMode.WALKING);
                                                final com.google.maps.model.LatLng firstRoutePoint = routePoints.get(0);
                                                final com.google.maps.model.LatLng lastRoutePoint = routePoints.get(routePoints.size() - 1);
                                                wayPointsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                                                    @Override
                                                    public void onResult(DirectionsResult result) {
                                                        wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                        System.out.println("Found waypoints successfully");
                                                        DirectionsRoute[] routesFound = result.routes;
                                                        ArrayList<com.google.maps.model.LatLng> routeWayPoints = (ArrayList<com.google.maps.model.LatLng>) routesFound[0].overviewPolyline.decodePath();
                                                        System.out.println("Number of way points found: " + routeWayPoints.size());
                                                        if (routeWayPoints.size() <= 23)
                                                        {
                                                            wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                            for (com.google.maps.model.LatLng routeWayPoint : routeWayPoints)
                                                            {
                                                                wayPoints.add(routeWayPoint);
                                                            }
                                                            System.out.println("Waypoints: " + wayPoints);
                                                            double intersectionDist = distance(wayPoints.get(0).lat, wayPoints.get(wayPoints.size()-1).lat,
                                                                    wayPoints.get(0).lng, wayPoints.get(wayPoints.size()-1).lng,
                                                                    0, 0);
                                                            System.out.println("Intersection dist: " + intersectionDist);
                                                            double routeLength = distance(firstRoutePoint.lat, lastRoutePoint.lat,
                                                                    firstRoutePoint.lng, lastRoutePoint.lng,
                                                                    0, 0);
                                                            System.out.println("Route length: " + routeLength);
                                                            double percentage = intersectionDist/routeLength;
                                                            System.out.println("Percentage: " + percentage);
                                                            percentageOverlapsForFriend.add(percentage);
                                                        }
                                                        else
                                                        {
                                                            System.out.println(routeWayPoints.size()/23.0);
                                                            double factor = (double) (Math.round((double) routeWayPoints.size()/23.0 * Math.pow(10, 1)))/Math.pow(10, 1);
                                                            factor = (int) (Math.round(factor));
                                                            System.out.println("Factor: " + factor);
                                                            int index = 0;
                                                            wayPoints = new ArrayList<com.google.maps.model.LatLng>();
                                                            while (index < routeWayPoints.size())
                                                            {
                                                                wayPoints.add(routeWayPoints.get(index));
                                                                index += factor;

                                                            }
                                                            System.out.println("Number of waypoints, final: " + wayPoints.size());
                                                            System.out.println("Waypoints: " + wayPoints);

                                                            double intersectionDist = distance(wayPoints.get(0).lat, wayPoints.get(wayPoints.size()-1).lat,
                                                                    wayPoints.get(0).lng, wayPoints.get(wayPoints.size()-1).lng,
                                                                    0, 0);
                                                            System.out.println("Intersection dist: " + intersectionDist);
                                                            double routeLength = distance(firstRoutePoint.lat, lastRoutePoint.lat,
                                                                    firstRoutePoint.lng, lastRoutePoint.lng,
                                                                    0, 0);
                                                            System.out.println("Route length: " + routeLength);
                                                            double percentage = intersectionDist/routeLength;
                                                            System.out.println("Percentage: " + percentage);
                                                            percentageOverlapsForFriend.add(new Object[] (percentage, );
                                                        }

                                                    }

                                                    @Override
                                                    public void onFailure(Throwable e) {

                                                    }
                                                });
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Throwable e) {
                                    System.out.println("Error with friend request: " + e.getMessage());
                                }
                            });

                            percentageOverlapsForFriend = sort(percentageOverlapsForFriend, friendRo);
                            optimalFriends.add(new Object[]{uid, friendRo})
                        }
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        System.out.println(e.getMessage());
                    }
                });

              //  final String[] waypoints = {"42.2839,-71.654", "42.285659, -71.653883"};

//                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
//                builder.setTitle("Make your selection");
//                final String finalInput = input;
//                builder.setItems(waypoints, new DialogInterface.OnClickListener() {
//                    @RequiresApi(api = Build.VERSION_CODES.DONUT)
//                    public void onClick(DialogInterface dialog, int item) {
//                        // Do something with the selection
//                        Toast.makeText(MapActivity.this, finalInput, Toast.LENGTH_SHORT).show();
//                        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin="+ latLng.latitude +","+ latLng.longitude +"&destination="+ finalInput +"&travelmode=walking&waypoints=" + waypoints[item]);
//
//                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                        mapIntent.setPackage("com.google.android.apps.maps");
//                        startActivity(mapIntent);
//                    }
//                });
//                AlertDialog alert = builder.create();
//                alert.show();
            }
        });*/

//    private static void getRoutes()
//    {
//        for (final String userID : userIdsToFindPolylines)
//        {
//            // Get database reference
//            FirebaseDatabase db = FirebaseDatabase.getInstance();
//            final DatabaseReference user = db.getReference().child("users").child(userID);
//            System.out.println("Got reference to the user when trying to make route request: " + user.toString());
//
//
//            // set origin
//            final DatabaseReference[] location = {user.child("location")};
//            System.out.println("Got reference to the user's location when trying to make route request: " + location[0].toString());
//            final String[] userLocation = new String[1];
//
//            location[0].addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    System.out.println("Location accessed from the database--trying to make request: " + dataSnapshot.getValue().toString());
//                    userLocation[0] = dataSnapshot.getValue().toString();
//                    System.out.println("User location from array right after being updated: " + userLocation[0]);
//
//                    DatabaseReference destination = user.child("destination");
//                    final String[] userDestination = new String[1];
//                    destination.addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            if (dataSnapshot.getValue().toString().equals("null")) routePath[0] = "Invalid: no destination";
//                            userDestination[0] = dataSnapshot.getValue().toString();
//                            System.out.println("User's destination after being retrieved from the database: " + userDestination[0]);
//
//                            DirectionsApiRequest routeRequest = DirectionsApi.newRequest(context);
//                            System.out.println("User location retrieved from the array: " + userLocation[0]);
//                            String[] locationLatLng = userLocation[0].split(", ");
//                            com.google.maps.model.LatLng userLocationCoord = new com.google.maps.model.LatLng(Double.parseDouble(locationLatLng[0]), Double.parseDouble(locationLatLng[1]));
//                            System.out.println("Route request's origin: " + userLocationCoord);
//                            routeRequest.origin(userLocationCoord);
//                            routeRequest.destination(userDestination[0]);
//                            System.out.println("Route request's destination: " + userDestination[0]);
//                            routeRequest.mode(TravelMode.WALKING);
//                            System.out.println("Route request after all qualities have been added: " + routeRequest.toString());
//
//                            routeRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
//                                @Override
//                                public void onResult(DirectionsResult result) {
//                                    EncodedPolyline userRoutePolyline = result.routes[0].overviewPolyline;
//                                    System.out.println("User's route polyline for uid " + userID + ": " + userRoutePolyline);
//                                    userPolylines.add(userRoutePolyline);
//                                    System.out.println("All user ids for which polylines have been found: " + userIdsToFindPolylines);
//                                    System.out.println("All polylines found: " + userPolylines);
//                                    routePoints.add(userRoutePolyline.decodePath());
//                                    System.out.println("Number of decoded polylines in routePoints: " + routePoints.size());
//                                    System.out.println("All items in routePoints: " + routePoints);
//
//                                }
//                                @Override
//                                public void onFailure(Throwable e) {
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError databaseError) {
//
//                        }
//                    });
//
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                }
//            });
//        }
//    }
//
//    private void findOptimalities() {
//        System.out.println("Finding optimalities");
//        System.out.println("RoutePoints size: " + routePoints.size());
//        for (int i = 1; i < routePoints.size(); i++)
//        {
//            List<com.google.maps.model.LatLng> userRoute = routePoints.get(0);
//            List<com.google.maps.model.LatLng> friendRoute = routePoints.get(i);
//            double userRouteDist = distance(userRoute.get(0).lat, userRoute.get(userRoute.size()-1).lat, userRoute.get(0).lng, userRoute.get(userRoute.size() -1).lng, 0, 0);
//            double friendRouteDist = distance(friendRoute.get(0).lat, friendRoute.get(friendRoute.size()-1).lat, friendRoute.get(0).lng, friendRoute.get(friendRoute.size()-1).lng, 0, 0);
//            int[] intersectionPts = findIntersection(userRoute, friendRoute);
//            System.out.println("User Route Distance: " + userRouteDist);
//            System.out.println("Friend Route Distance: " + friendRouteDist);
//            System.out.println("Intersection Points: " + intersectionPts);
//            double intersectionDist = distance(userRoute.get(intersectionPts[0]).lat, userRoute.get(intersectionPts[1]).lat,
//                                                userRoute.get(intersectionPts[0]).lng, userRoute.get(intersectionPts[1]).lng,
//                                                0, 0);
//            double optimality = intersectionDist/userRouteDist;
//            System.out.println("Optimality found: " + optimality);
//            double[] optimalityArr = {optimality, (double) i};
//            optimalities.add(optimalityArr);
//
//            DirectionsApiRequest wayPointsRequest = DirectionsApi.newRequest(context);
//            wayPointsRequest.origin(userRoute.get(intersectionPts[0]));
//            wayPointsRequest.destination(userRoute.get(intersectionPts[1]));
//            wayPointsRequest.mode(TravelMode.WALKING);
//            wayPointsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
//                @Override
//                public void onResult(DirectionsResult result) {
//                    List<com.google.maps.model.LatLng> points = result.routes[0].overviewPolyline.decodePath();
//                    System.out.println("Waypoints found: " + points);
//                    System.out.println("Number of waypoints found: " + points.size());
//                    ArrayList<com.google.maps.model.LatLng> wayPtsToBeAdded = new ArrayList<>();
//                    if (points.size() <= 23)
//                    {
//                        for (com.google.maps.model.LatLng point : points) wayPtsToBeAdded.add(point);
//                        System.out.println("Size of wayPtsToBeAdded when points size <= 23: " + wayPtsToBeAdded.size());
//                    }
//                    else
//                    {
//                        System.out.println(points.size()/23.0);
//                        double factor = (double) (Math.round((double) points.size()/23.0 * Math.pow(10, 1)))/Math.pow(10, 1);
//                        factor = (int) (Math.round(factor));
//                        System.out.println("Factor: " + factor);
//                        for (int i = 0; i < points.size(); i+=factor)
//                        {
//                            wayPtsToBeAdded.add(points.get(i));
//                        }
//                        System.out.println("Size of wayPtsToBeAdded when points size > 23: " + wayPtsToBeAdded.size());
//                        if (wayPtsToBeAdded.size() > 23)
//                        {
//                            int diff = wayPtsToBeAdded.size() - 23;
//                            wayPtsToBeAdded = new ArrayList<>();
//                            for (int i = 0; i < points.size() - diff; i++)
//                            {
//                                wayPtsToBeAdded.add(points.get(i));
//                            }
//                        }
//                        System.out.println("Size of wayPtsToBeAdded after additional check for right number of waypoints: " + wayPtsToBeAdded.size());
//                    }
//
//                    wayPoints.add(wayPtsToBeAdded);
//                    System.out.println("Waypoints arraylist at this point: " + wayPoints);
//                    sortOptimalities();
//
//                }
//
//                @Override
//                public void onFailure(Throwable e) {
//
//                }
//            });
//
//        }
//    }
//
//    private void sortOptimalities() {
//        System.out.println("Sorting optimalities");
//        double maxOptimality = 0;
//        int index = 0;
//        while (index < optimalities.size())
//        {
//            System.out.println("Max optimality at this point: " + maxOptimality);
//            System.out.println("Optimality of current element: " + optimalities.get(index)[0]);
//            if (optimalities.get(index)[0] >= maxOptimality)
//            {
//                maxOptimality = optimalities.get(index)[0];
//                optimalities.add(0, optimalities.remove(index));
//            }
//            index++;
//        }
//
//        System.out.println("Sorted optimalities: " + optimalities);
//        System.out.println("Index of the most optimal friend: " + (int) optimalities.get(0)[1]);
//        System.out.println("All user ids to find polylines for: " + userIdsToFindPolylines);
//        System.out.println("All polylines found: " + userPolylines);
//        System.out.println("All routes found: " + routePoints);
//        System.out.println("All waypoint lists found: " + wayPoints);
//        System.out.println("Chosen user id: " + userIdsToFindPolylines.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen polyline: " + userPolylines.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen route: " + routePoints.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen waypoint list: " + routePoints.get((int) optimalities.get(0)[1]-1));
//    }


    public static double distance(double lat1, double lat2, double lon1,
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



