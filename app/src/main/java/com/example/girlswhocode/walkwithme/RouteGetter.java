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


    public RouteGetter(ArrayList<String> userIDs)
    {
        for (String userID : userIDs) uids.add(userID);
    }

    public static ArrayList<List<LatLng>> findRoutes()
    {
        for (final String userID : uids)
        {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            final DatabaseReference user = db.getReference().child("users").child(userID);
            System.out.println("Got reference to the user when trying to make route request: " + user.toString());


            // set origin
            final DatabaseReference[] location = {user.child("location")};
            System.out.println("Got reference to the user's location when trying to make route request: " + location[0].toString());
            final String[] userLocation = new String[1];

            location[0].addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    System.out.println("Location accessed from the database--trying to make request: " + dataSnapshot.getValue().toString());
                    userLocation[0] = dataSnapshot.getValue().toString();
                    System.out.println("User location from array right after being updated: " + userLocation[0]);

                    DatabaseReference destination = user.child("destination");
                    final String[] userDestination = new String[1];
                    destination.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue().toString().equals("null")) routePath[0] = "Invalid: no destination";
                            userDestination[0] = dataSnapshot.getValue().toString();
                            System.out.println("User's destination after being retrieved from the database: " + userDestination[0]);

                            DirectionsApiRequest routeRequest = DirectionsApi.newRequest(context);
                            System.out.println("User location retrieved from the array: " + userLocation[0]);
                            String[] locationLatLng = userLocation[0].split(", ");
                            com.google.maps.model.LatLng userLocationCoord = new com.google.maps.model.LatLng(Double.parseDouble(locationLatLng[0]), Double.parseDouble(locationLatLng[1]));
                            System.out.println("Route request's origin: " + userLocationCoord);
                            routeRequest.origin(userLocationCoord);
                            routeRequest.destination(userDestination[0]);
                            System.out.println("Route request's destination: " + userDestination[0]);
                            routeRequest.mode(TravelMode.WALKING);
                            System.out.println("Route request after all qualities have been added: " + routeRequest.toString());

                            routeRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                                @Override
                                public void onResult(DirectionsResult result) {
                                    EncodedPolyline userRoutePolyline = result.routes[0].overviewPolyline;
                                    System.out.println("User's route polyline for uid " + userID + ": " + userRoutePolyline);
                                    userPolylines.add(userRoutePolyline);
                                    System.out.println("All polylines found: " + userPolylines);
                                    routePoints.add(userRoutePolyline.decodePath());
                                    System.out.println("Number of decoded polylines in routePoints: " + routePoints.size());
                                    System.out.println("All items in routePoints: " + routePoints);

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
        }
        return routePoints;
    }

    public static ArrayList<double[]> findOptimalities()
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

            DirectionsApiRequest wayPointsRequest = DirectionsApi.newRequest(context);
            wayPointsRequest.origin(userRoute.get(intersectionPts[0]));
            wayPointsRequest.destination(userRoute.get(intersectionPts[1]));
            wayPointsRequest.mode(TravelMode.WALKING);
            wayPointsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    List<com.google.maps.model.LatLng> points = result.routes[0].overviewPolyline.decodePath();
                    System.out.println("Waypoints found: " + points);
                    System.out.println("Number of waypoints found: " + points.size());
                    ArrayList<com.google.maps.model.LatLng> wayPtsToBeAdded = new ArrayList<>();
                    if (points.size() <= 23)
                    {
                        for (com.google.maps.model.LatLng point : points) wayPtsToBeAdded.add(point);
                        System.out.println("Size of wayPtsToBeAdded when points size <= 23: " + wayPtsToBeAdded.size());
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
                            wayPtsToBeAdded = new ArrayList<>();
                            for (int i = 0; i < points.size() - diff; i++)
                            {
                                wayPtsToBeAdded.add(points.get(i));
                            }
                        }
                        System.out.println("Size of wayPtsToBeAdded after additional check for right number of waypoints: " + wayPtsToBeAdded.size());
                    }

                    wayPoints.add(wayPtsToBeAdded);
                    System.out.println("Waypoints arraylist at this point: " + wayPoints);

                }

                @Override
                public void onFailure(Throwable e) {

                }
            });

        }
        return null;
    }

//    public static ArrayList<double[]> sortOptimalities() {
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
//        System.out.println("All user ids to find polylines for: " + uids);
//        System.out.println("All polylines found: " + userPolylines);
//        System.out.println("All routes found: " + routePoints);
//        System.out.println("All waypoint lists found: " + wayPoints);
//        System.out.println("Chosen user id: " + uids.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen polyline: " + userPolylines.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen route: " + routePoints.get((int) optimalities.get(0)[1]));
//        System.out.println("Chosen waypoint list: " + routePoints.get((int) optimalities.get(0)[1]-1));
//
//        return optimalities;
//    }

    private static int[] findIntersection(List<com.google.maps.model.LatLng> routePoints, List<com.google.maps.model.LatLng> usrRtPts) {
        System.out.println("Finding intersection points");
        int i = 0;
        int j = 0;
        int startIntersectionIndex = -1;
        System.out.println("Finding start intersection");
        while (i < routePoints.size() || j < usrRtPts.size())
        {
            com.google.android.gms.maps.model.LatLng routePointLatLng = new com.google.android.gms.maps.model.LatLng(routePoints.get(i).lat, routePoints.get(i).lng);
            com.google.android.gms.maps.model.LatLng usrRtPtLatLng = new com.google.android.gms.maps.model.LatLng(usrRtPts.get(j).lat, usrRtPts.get(j).lng);
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
            com.google.android.gms.maps.model.LatLng routePointLatLng = new com.google.android.gms.maps.model.LatLng(routePoints.get(l).lat, routePoints.get(l).lng);
            com.google.android.gms.maps.model.LatLng usrRtPtLatLng = new com.google.android.gms.maps.model.LatLng(usrRtPts.get(m).lat, usrRtPts.get(m).lng);
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

    private static double distance(double lat1, double lat2, double lon1,
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

    private static double findBearing(double lat1, double lat2, double lon1,
                                      double lon2)
    {
        double y = Math.sin(lon2-lon1)*Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1);
        return Math.atan2(y, x)*180/Math.PI;
    }

    public static ArrayList<double[]> sortOptimalities(ArrayList<double[]> doubles) {
        optimalities = doubles;
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

        System.out.println("Sorted optimalities: " + optimalities);
        System.out.println("Index of the most optimal friend: " + (int) optimalities.get(0)[1]);
        System.out.println("All user ids to find polylines for: " + uids);
        System.out.println("All polylines found: " + userPolylines);
        System.out.println("All routes found: " + routePoints);
        System.out.println("All waypoint lists found: " + wayPoints);
        System.out.println("Chosen user id: " + uids.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen polyline: " + userPolylines.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen route: " + routePoints.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen waypoint list: " + routePoints.get((int) optimalities.get(0)[1]-1));

        return optimalities;
    }

    public static Object findOptimalities(Object o) {
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
            System.out.println("Optimalities Array at this point: "+optimalities);
            DirectionsApiRequest wayPointsRequest = DirectionsApi.newRequest(context);
            wayPointsRequest.origin(userRoute.get(intersectionPts[0]));
            wayPointsRequest.destination(userRoute.get(intersectionPts[1]));
            wayPointsRequest.mode(TravelMode.WALKING);
            wayPointsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    List<com.google.maps.model.LatLng> points = result.routes[0].overviewPolyline.decodePath();
                    System.out.println("Waypoints found: " + points);
                    System.out.println("Number of waypoints found: " + points.size());
                    ArrayList<com.google.maps.model.LatLng> wayPtsToBeAdded = new ArrayList<>();
                    if (points.size() <= 23)
                    {
                        for (com.google.maps.model.LatLng point : points) wayPtsToBeAdded.add(point);
                        System.out.println("Size of wayPtsToBeAdded when points size <= 23: " + wayPtsToBeAdded.size());
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
                            wayPtsToBeAdded = new ArrayList<>();
                            for (int i = 0; i < points.size() - diff; i++)
                            {
                                wayPtsToBeAdded.add(points.get(i));
                            }
                        }
                        System.out.println("Size of wayPtsToBeAdded after additional check for right number of waypoints: " + wayPtsToBeAdded.size());
                    }

                    wayPoints.add(wayPtsToBeAdded);
                    System.out.println("Waypoints arraylist at this point: " + wayPoints);
                }

                @Override
                public void onFailure(Throwable e) {

                }
            });

        }
        return optimalities;
    }

    public static void sortOptimalities(Object o) {
        optimalities = (ArrayList<double[]>) o;
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

        System.out.println("Sorted optimalities: " + optimalities);
        System.out.println("Index of the most optimal friend: " + (int) optimalities.get(0)[1]);
        System.out.println("All user ids to find polylines for: " + uids);
        System.out.println("All polylines found: " + userPolylines);
        System.out.println("All routes found: " + routePoints);
        System.out.println("All waypoint lists found: " + wayPoints);
        System.out.println("Chosen user id: " + uids.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen polyline: " + userPolylines.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen route: " + routePoints.get((int) optimalities.get(0)[1]));
        System.out.println("Chosen waypoint list: " + routePoints.get((int) optimalities.get(0)[1]-1));
    }
}
