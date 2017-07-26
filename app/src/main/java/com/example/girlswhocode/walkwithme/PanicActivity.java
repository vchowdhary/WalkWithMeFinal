package com.example.girlswhocode.walkwithme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class PanicActivity extends AppCompatActivity {
    LatLng mLatLng;
    Location mLocation;
    private GoogleApiClient googleApiClient;
    LocationRequest mLocationRequest;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic);
        setTitle("Panic");

        ImageView lineColorCode = (ImageView) findViewById(R.id.police);
        int color = Color.parseColor("#FFFFFF"); //The color u want
        lineColorCode.setColorFilter(color);

        ImageView lineColorCode2 = (ImageView) findViewById(R.id.phoneImage);
        int color2 = Color.parseColor("#FFFFFF"); //The color u want
        lineColorCode2.setColorFilter(color2);

        Intent i = getIntent();
        user = new User(FirebaseAuth.getInstance().getCurrentUser());
        user.setContext(PanicActivity.this);
        user.setActivity(PanicActivity.this);
        user.startLocation();

        LinearLayout call911 = (LinearLayout) findViewById(R.id.call911Button);
        call911.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                System.out.println("Call 911 button click!");
                Intent call911Intent = new Intent(Intent.ACTION_CALL);
                call911Intent.setData(Uri.parse("tel:911"));
                if (ContextCompat.checkSelfPermission(PanicActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 20);
                    }
                    return;
                } else {
                    startActivity(call911Intent);
                }

            }
        });

        LinearLayout callAFriend = (LinearLayout) findViewById(R.id.phoneCallButton);
        callAFriend.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                System.out.println("Attempting to call a friend!");
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                String friendNumber = "16099062677";

                callIntent.setData(Uri.parse("tel:" + friendNumber));
                if (ActivityCompat.checkSelfPermission(PanicActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 30);
                    }
                    return;
                }
                //else
                //{
                System.out.println("Calling a friend: " + friendNumber);
                try {
                    startActivity(callIntent);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                //}

            }
        });

        LinearLayout sendAMessage = (LinearLayout) findViewById(R.id.sendAMessage);
        sendAMessage.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        System.out.println("Attempting to send a message");
        String phoneNumber = "6099062677";

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:6099062676;" + phoneNumber));
        // Invokes only SMS/MMS clients
        //smsIntent.setType("vnd.android-dir/mms-sms");
        // Specify the Phone Number
        //smsIntent.putExtra("address", phoneNumber, "1234567890");
        // Specify the Message
        if (ActivityCompat.checkSelfPermission(PanicActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        String address = getMyLocation(user.getLatLng());
        System.out.println(address);
        System.out.println(address);

        String smsBody = "I'm in trouble. If I don't respond in 15 minutes, call the police. My current GPS coordinates are " +
                user.getLatLng().latitude + ", " + user.getLatLng().longitude + ". My current location is " + address;
        //smsBody += address;

        smsIntent.putExtra("sms_body", smsBody);

        // Shoot!
        if (!address.equals("")) {
            startActivity(smsIntent);
            address = "";
        }

    }

    private String getMyLocation(LatLng latLng) {
        Toast.makeText(this, "Getting your location", Toast.LENGTH_SHORT).show();
        Geocoder reversegeocode = new Geocoder(this, Locale.getDefault());
        String address = "";
        try {

            //Place your latitude and longitude
            List<Address> addresses = reversegeocode.getFromLocation(user.getLatLng().latitude, user.getLatLng().longitude, 1);

            if (addresses != null) {

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append(", ");
                }
                address = strAddress.toString();
                Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Could not get address..!", Toast.LENGTH_LONG).show();
        }
        return address;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Toast.makeText(this, "onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
        switch (requestCode) {
            case 20:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent call911Intent = new Intent(Intent.ACTION_CALL);
                    call911Intent.setData(Uri.parse("tel:911"));
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivity(call911Intent);
                }
                break;
            case 30:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    String friendNumber = "16099062677";

                    callIntent.setData(Uri.parse("tel:" + friendNumber));
                    startActivity(callIntent);
                }
                break;

        }
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
                Intent switchMap = new Intent(PanicActivity.this, MapActivity.class);
                startActivity(switchMap);
                return true;

            case R.id.friends_action:
                System.out.println("Switching to the friends activity.");
                Intent switchFriends = new Intent(PanicActivity.this, FriendsActivity.class);
                startActivity(switchFriends);
                return true;

            case R.id.panic_action:
                System.out.println("Switching to the panic activity...");
                System.out.println("Already at the panic activity.");
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}

