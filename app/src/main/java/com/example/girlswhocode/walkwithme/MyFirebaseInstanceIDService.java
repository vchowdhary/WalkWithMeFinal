package com.example.girlswhocode.walkwithme;

import android.app.Service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by main on 7/17/17.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    public void onRefreshToken()
    {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        System.out.println("Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken) {

    }


}
