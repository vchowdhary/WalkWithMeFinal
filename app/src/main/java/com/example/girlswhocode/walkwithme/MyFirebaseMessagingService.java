package com.example.girlswhocode.walkwithme;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by main on 7/17/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService
{
    private static final String TAG = "FCM Service";

    public MyFirebaseMessagingService()
    {
        super();
        //Looper.prepare();
    }

    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
       super.onMessageReceived(remoteMessage);
        Log.wtf(TAG, "This is your message: " + remoteMessage.getData().get("body").toString());
        createAlert(remoteMessage);

        //Toast.makeText(this, "Received message", Toast.LENGTH_SHORT).show();
    }

    private void createAlert(RemoteMessage remoteMessage) {
        Intent intent = new Intent("myFunction");
        // add data
        intent.putExtra("from", remoteMessage.getFrom() );
        intent.putExtra("body", remoteMessage.getData().get("body").toString());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static void sendNotificationToUser(final String titleText, String user, final String message) {
        final FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference();
        final DatabaseReference notifications = ref.child("notificationRequests");

        final DatabaseReference users = ref.child("users");
        Query usernameQuery = users.orderByChild("username").equalTo(user);
        usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren())
                {
                    if(ds.exists())
                    {
                        System.out.println(ds.getKey());
                        DatabaseReference ref = db.getReference().child("registration tokens").child(ds.getKey());
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String uid = dataSnapshot.getValue().toString();
                                Map notification = new HashMap<>();
                                notification.put("username", uid);
                                notification.put("message", message);
                                notification.put("titleText", titleText);

                                notifications.push().setValue(notification);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }
}

