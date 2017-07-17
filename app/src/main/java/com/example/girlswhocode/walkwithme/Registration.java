package com.example.girlswhocode.walkwithme;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Registration extends AppCompatActivity {

    EditText email;
    EditText password;
    EditText username;
    EditText phoneNumber;
    Button regButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Get UI elements
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        username = (EditText) findViewById(R.id.emailAddress);
        phoneNumber = (EditText) findViewById(R.id.phoneNumber);
        regButton = (Button) findViewById(R.id.registerButton);

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Email Address: " + email.getText());
                System.out.println("Password: " + password.getText());
                System.out.println("Username: " + username.getText());
                System.out.println("Phone Number: " + phoneNumber.getText());
                launchPanicActivity();
            }
        });
    }

    private void launchPanicActivity() {
        Intent switchPanic = new Intent(Registration.this, PanicActivity.class);
        startActivity(switchPanic);
    }
}
