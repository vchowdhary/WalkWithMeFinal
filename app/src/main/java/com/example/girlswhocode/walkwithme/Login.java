package com.example.girlswhocode.walkwithme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Login extends AppCompatActivity {

    Button loginButton;
    TextView registerButton;
    EditText emailAddress;
    EditText password;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Login");

        // Obtain UI elementschowdhar
        loginButton = (Button) findViewById(R.id.loginButton);
        emailAddress = (EditText) findViewById(R.id.name);
        password = (EditText) findViewById(R.id.password);
        registerButton = (TextView) findViewById(R.id.regButton);
 
        // Initialize firebse
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    System.out.println("onAuthStateChanged:signed_in: "+user.getUid());
                }
                else
                {
                    System.out.println("onAuthStateChanged:signed_out");
                }
            }
        };

        // Create method to handle login button clicked
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Email Address: " + emailAddress.getText());
                System.out.println("Password: " + password.getText());
                signIn();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Switching to the registration screen...");
                launchRegistrationActivity();
            }
        });

    }

    private void signIn() {
        System.out.println("Attempting sign in...");
        final String email = emailAddress.getText().toString();
        final String pass = password.getText().toString();
        final ProgressDialog progressDialog = new ProgressDialog(Login.this,
                R.style.AppTheme);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, pass) .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                System.out.println("signInWithEmail:onComplete:" + task.isSuccessful());

                // If sign in fails, display a message to the user. If sign in succeeds
                // the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.
                if (!task.isSuccessful()) {
                    System.out.println( "signInWithEmail:failed" + task.getException());
                    launchRegistrationActivity();
                }
                else
                {
                    switchToMap();

                }

                // ...
            }
        });
    }

    private void launchRegistrationActivity() {
        Intent switchRegister = new Intent(Login.this, Registration.class);
        startActivity(switchRegister);
    }

    private void switchToMap()
    {
        Intent switchMap = new Intent(Login.this, MapActivity.class);
        startActivity(switchMap);
    }

    // Attach listener to FirebaseAuth instance
    @Override
    public void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    // Remove listener to FirebaseAuth instance
    @Override
    public void onStop()
    {
        super.onStop();
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
