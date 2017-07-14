package com.example.girlswhocode.walkwithme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.w3c.dom.Text;

public class PanicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic);

        TextView call911 = (TextView) findViewById(R.id.call911Button);
        call911.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                System.out.println("Call 911 button click!");
                Intent call911Intent = new Intent(Intent.ACTION_DIAL);
                call911Intent.setData(Uri.parse("tel:911"));
                if (ActivityCompat.checkSelfPermission(PanicActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
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
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                String friendNumber = "16099062677";

                callIntent.setData(Uri.parse("tel:" + friendNumber));
                /*if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }*/
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
                System.out.println("Attempting to send a message");
                String phoneNumber = "6099062677";
                String smsBody = "This is an SMS!";

                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:6099062676;" + phoneNumber));
                // Invokes only SMS/MMS clients
                //smsIntent.setType("vnd.android-dir/mms-sms");
                // Specify the Phone Number
                //smsIntent.putExtra("address", phoneNumber, "1234567890");
                // Specify the Message
                smsIntent.putExtra("sms_body", smsBody);

                // Shoot!
                startActivity(smsIntent);
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
}
