package com.example.girlswhocode.walkwithme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class PanicActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    LatLng mLatLng;
    Location mLocation;
    private GoogleApiClient googleApiClient;
    LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic);
        googleApiClient = new GoogleApiClient.Builder(this, this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        TextView call911 = (TextView) findViewById(R.id.call911Button);
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

        ImageButton callAFriend = (ImageButton) findViewById(R.id.phoneCallButton);
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

        ImageButton sendAMessage = (ImageButton) findViewById(R.id.sendAMessage);
        sendAMessage.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                sendMessage();
            }
        });

        Button cancel = (Button) findViewById(R.id.cancelButton);
        cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                System.out.println("Switching to map!");
                Intent call911Intent = new Intent(PanicActivity.this, MapActivity.class);
                startActivity(call911Intent);
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
        Location loc = FusedLocationApi.getLastLocation(googleApiClient);
        mLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        if(mLatLng == null) mLatLng = new LatLng(-71, 42.3);
        Toast.makeText(PanicActivity.this, mLatLng.toString(), Toast.LENGTH_SHORT).show();
        System.out.println(mLatLng.toString());
        String address = getMyLocation(mLatLng);
        System.out.println(address);

        String smsBody = "I'm in trouble. If I don't respond in 15 minutes, call the police. My current GPS coordinates are " +
                mLatLng.latitude + ", " + mLatLng.longitude + ". My current location is " + address;
        //smsBody += address;

        smsIntent.putExtra("sms_body", smsBody);

        // Shoot!
        if(!address.equals(""))
        {
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
            List<Address> addresses = reversegeocode.getFromLocation(latLng.latitude, latLng.longitude, 1);

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
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Toast.makeText(this, "requesting location updates from within onReqPermResults", Toast.LENGTH_SHORT).show();
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                } else {
                    Toast.makeText(this, "Need your location!", Toast.LENGTH_SHORT).show();
                }
                break;
            case 20:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Intent call911Intent = new Intent(Intent.ACTION_CALL);
                    call911Intent.setData(Uri.parse("tel:911"));
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
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();

        mLocation = FusedLocationApi.getLastLocation(googleApiClient);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(50); //5 seconds; function takes millisecond parameter
        mLocationRequest.setFastestInterval(30); //3 seconds; function takes millisecond parameter
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

        Toast.makeText(this, "checking permission in on connected", Toast.LENGTH_SHORT).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    10);

        } else {
            // permission has been granted, continue as usual
            Toast.makeText(this, "permission granted in on connected", Toast.LENGTH_SHORT).show();
            FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("Location changed!");
        Toast.makeText(this,"Location Changed",Toast.LENGTH_SHORT).show();
        mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    public void startUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                } else if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
                }
            }
        }
    }
    public void stopUpdates() {
        if(FusedLocationApi != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                }
            }
        }
    }
}

