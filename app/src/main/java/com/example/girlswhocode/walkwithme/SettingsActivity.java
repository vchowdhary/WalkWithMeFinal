package com.example.girlswhocode.walkwithme;

import android.content.Intent;
import android.location.SettingInjectorService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");

        DatabaseReference userNode = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userNode.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                EditText username = (EditText) findViewById(R.id.usernameEdit);
                username.setText(dataSnapshot.child("username").getValue().toString());

                EditText email = (EditText) findViewById(R.id.emailEdit);
                email.setText(dataSnapshot.child("email").getValue().toString());

                EditText phone = (EditText) findViewById(R.id.phoneEdit);
                phone.setText(dataSnapshot.child("phonenumber").getValue().toString());

                EditText smsMessage = (EditText) findViewById(R.id.smsEdit);
                smsMessage.setText(dataSnapshot.child("smsMessage").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        Button saveButton = (Button) findViewById(R.id.saveSettings);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference userNode = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                EditText sms = (EditText) findViewById(R.id.smsEdit);
                userNode.child("smsMessage").setValue(sms.getText().toString());

                EditText email = (EditText) findViewById(R.id.emailEdit);

                userNode.child("email").setValue(email.getText().toString());
                FirebaseAuth.getInstance().getCurrentUser().updateEmail(email.getText().toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_action:
                System.out.println("Switching to the map screen.");
                Intent switchMap = new Intent(SettingsActivity.this, MapActivity.class);
                startActivity(switchMap);
                return true;

            case R.id.friends_action:
                System.out.println("Switching to the friends activity.");
                startActivity(new Intent(SettingsActivity.this, FriendsActivity.class));
                return true;

            case R.id.panic_action:
                System.out.println("Switching to the panic activity...");
                Intent switchPanic = new Intent(SettingsActivity.this, PanicActivity.class);
                startActivity(switchPanic);
                return true;
            case R.id.settings_action:
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
