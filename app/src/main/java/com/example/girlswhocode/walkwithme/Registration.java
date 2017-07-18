package com.example.girlswhocode.walkwithme;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Registration extends AppCompatActivity {

    EditText email;
    EditText password;
    EditText username;
    EditText phoneNumber;
    Button regButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference users = db.getReference().child("users");

        // Get UI elements
        Button cancelReg = (Button) findViewById(R.id.cancelRegistrationButton);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        username = (EditText) findViewById(R.id.emailAddress);
        phoneNumber = (EditText) findViewById(R.id.phoneNumber);
        regButton = (Button) findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                  System.out.println("onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    System.out.println( "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mAuth.createUserWithEmailAndPassword(String.valueOf(email.getText()), String.valueOf(password.getText()))
                        .addOnCompleteListener(Registration.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                System.out.println("createUserWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(Registration.this, R.string.auth_failed,
                                            Toast.LENGTH_SHORT).show();
                                }

                                // ...
                            }
                        });

                mAuth.signInWithEmailAndPassword(String.valueOf(email.getText()), String.valueOf(password.getText()))
                        .addOnCompleteListener(Registration.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                System.out.println("signInWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Log.wtf("signInWithEmail:failed", task.getException());
                                    Toast.makeText(Registration.this, R.string.auth_failed,
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    System.out.println("Email Address: " + email.getText());
                                    System.out.println("Password: " + password.getText());
                                    System.out.println("Username: " + username.getText());
                                    System.out.println("Phone Number: " + phoneNumber.getText());

                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String[] keys = {"username", "email", "password", "phonenumber"};
                                    String[] values = {username.getText().toString(), email.getText().toString(), password.getText().toString(), phoneNumber.getText().toString()};
                                    for (int i = 0; i < keys.length; i++) {
                                        users.child(user.getUid()).child(keys[i]).setValue(values[i]);
                                    }
                                    launchLogin();
                                }

                                // ...
                            }
                        });
            }
    });

        cancelReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent returnHome = new Intent(Registration.this, Login.class);
                startActivity(returnHome);
            }
            //launchPanicActivity();
        });
    }


    private void launchLogin() {
        Intent switchLogin = new Intent(Registration.this, Login.class);
        startActivity(switchLogin);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


}
