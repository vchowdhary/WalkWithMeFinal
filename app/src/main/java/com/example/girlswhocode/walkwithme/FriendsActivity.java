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
import android.widget.ImageButton;

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
    private ImageButton addFriend;
    private ArrayList<String> names = new ArrayList<String>();
    private ArrayList<String> uids = new ArrayList<String>();
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Intent i = getIntent();
        user = new User(FirebaseAuth.getInstance().getCurrentUser());
        user.setContext(FriendsActivity.this);
        user.setActivity(FriendsActivity.this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        System.out.println("Created recycler view:"+mRecyclerView.toString());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        System.out.println("Added linear layout manager to recycler view");
        friendName = (EditText) findViewById(R.id.nameToBeAdded);
        System.out.println("Accessed friendName editText");
        addFriend = (ImageButton) findViewById(R.id.addFriendButton);
        System.out.println("Accessed addFriend button");
        friendName.setText("");

        DatabaseReference currUserNode = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        System.out.println("Got the reference to the node for the current user: " + currUserNode.toString());
        DatabaseReference friendsNode = currUserNode.child("friends");
        System.out.println("Got a reference to the current user's friends: " + friendsNode.toString());

            System.out.println("Adding value event listener");
            friendsNode.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        System.out.println("Looking at snapshots");
                        final String friendUsername = ds.getValue().toString();
                        System.out.println("Got the friend " + friendUsername);

                       names.add(friendUsername);
                        uids.add(ds.getKey());

                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(FriendsActivity.this, android.R.layout.simple_list_item_1, names);

                    mAdapter = new RecyclerAdapter(names);
                    mRecyclerView.setAdapter(mAdapter);
                    setRecylerViewItemTouchListener();

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        addFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Friend newFriend = new Friend(FirebaseAuth.getInstance().getCurrentUser(), FirebaseDatabase.getInstance(), friendName.getText().toString());

                //Friend x = new Friend(friendName.getText().toString());
                //TODO: implement this:
                addFriendToDB(friendName.getText().toString());
                System.out.println("friendname:" + friendName.getText().toString());
                names.add(friendName.getText().toString());
                System.out.println(names);
                int pos = names.indexOf(friendName.getText().toString());
                System.out.println(pos);
                mRecyclerView.getAdapter().notifyItemInserted(pos);
                friendName.setText(" ");
            }
        });

    }

    private void addFriendToDB(final String s) {

        final FirebaseDatabase db = FirebaseDatabase.getInstance();

        System.out.println("Looking for: " + s);
        Query ref = db.getReference("users").orderByChild("username").equalTo(s);
        System.out.println("created query at: " + ref);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println("checking right now");
                if (dataSnapshot.exists()) {
                    for(DataSnapshot ds: dataSnapshot.getChildren())
                    {
                        System.out.println("data snapshot exists");
                        // dataSnapshot is the "issue" node with all children with id 0
                        System.out.println("issue: " + dataSnapshot);
                        db.getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("friends").child(ds.getKey()).setValue(s);
                        //query.removeEventListener(this);
                    }
                }
                else
                    System.out.println("does not exist");
                //Toast.makeText(context, "user does not exist", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

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
                FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("friends").child(uids.get(position)).removeValue();
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
                switchMap.putExtra("uids", uids);
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
            case R.id.settings_action:
                startActivity(new Intent(FriendsActivity.this, SettingsActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
