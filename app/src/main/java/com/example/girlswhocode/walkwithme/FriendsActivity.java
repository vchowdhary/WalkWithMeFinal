package com.example.girlswhocode.walkwithme;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    private EditText friendName;
    private Button addFriend;
    private ArrayList<String> names = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        System.out.println("Created recycler view");
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        System.out.println("Added linear layout manager to recycler view");
        friendName = (EditText) findViewById(R.id.nameToBeAdded);
        System.out.println("Accessed friendName editText");
        addFriend = (Button) findViewById(R.id.addFriendButton);
        System.out.println("Accessed addFriend button");
        friendName.setText(" ");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        System.out.println("Got the instance for FirebaseAuth");
        final FirebaseDatabase db = FirebaseDatabase.getInstance();
        System.out.println("Got the database");
        final DatabaseReference ref = db.getReference();
        System.out.println("Got the reference to the database");
        FirebaseUser currUser = mAuth.getCurrentUser();
        System.out.println("Got the current user");
        DatabaseReference currUserNode = db.getReference("users").child(currUser.getUid());
        System.out.println("Got the reference to the node for the current user: " + currUserNode.toString());
        DatabaseReference friendsNode = currUserNode.child("friends");
        System.out.println("Got a reference to the current user's friends: " + friendsNode.toString());

            System.out.println("Adding value event listener");
            friendsNode.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        System.out.println("Looking at snapshots");
                        String friendUserID = ds.getKey().toString();
                        System.out.println("Got the friend's user ID: " + friendUserID);

                        DatabaseReference users = ref.child("users");
                        System.out.println("Got the reference to the users node in the database");
                        Query usernameQuery = users.orderByKey().equalTo(friendUserID);
                        usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                System.out.println("Added listener for single value event");
                                for (DataSnapshot ds : dataSnapshot.getChildren())
                                {
                                    if (ds.exists()) // checks if there is a friend
                                    {
                                        System.out.println("Friend found " + ds);
                                        String friendUsername = ds.child("username").getValue().toString();
                                        System.out.println("Found friend's username: " + friendUsername);
                                        names.add(friendUsername);
                                        System.out.println(names.toString());

                                        // Potentially create a friend object, if you want to display information about friend
                                    }
                                }

                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(FriendsActivity.this, android.R.layout.simple_list_item_1, names);

                                mAdapter = new RecyclerAdapter(names);
                                mRecyclerView.setAdapter(mAdapter);
                                setRecylerViewItemTouchListener();

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                System.out.println("Something went wrong: " + databaseError);

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                names.add(friendName.getText().toString());
                int pos = names.indexOf(friendName.getText().toString());
                mRecyclerView.getAdapter().notifyItemInserted(pos);
                friendName.setText(" ");
            }
        });

    }

    private void setRecylerViewItemTouchListener() {

        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //  viewHolder.itemView.setBackgroundColor(Color.RED);
                int position = viewHolder.getAdapterPosition();
                mRecyclerView.getAdapter().notifyItemRemoved(position);
                names.remove(position);
            }

        };

        ItemTouchHelper helper = new ItemTouchHelper(itemTouchCallback);
        helper.attachToRecyclerView(mRecyclerView);
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
                System.out.println("Switching to the map screen.");
                Intent switchMap = new Intent(FriendsActivity.this, MapActivity.class);
                startActivity(switchMap);
                return true;

            case R.id.friends_action:
                System.out.println("Switching to the friends activity.");
                System.out.println("Already at the friends activity.");
                return true;

            case R.id.panic_action:
                System.out.println("Switching to the panic activity...");
                Intent switchPanic = new Intent(FriendsActivity.this, PanicActivity.class);
                startActivity(switchPanic);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
