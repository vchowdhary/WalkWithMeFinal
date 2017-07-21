package com.example.girlswhocode.walkwithme;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by main on 7/20/17.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.FriendHolder>
{
    private ArrayList<String> mFriends;

    public RecyclerAdapter(ArrayList<String> friends)
    {
        System.out.println("Creating a recycler adapter");
        mFriends = friends;
    }

    @Override
    public RecyclerAdapter.FriendHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row, parent, false);
        return new FriendHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.FriendHolder holder, int position)
    {
        String friend = mFriends.get(position);
        holder.bindFriend(friend);

    }

    @Override
    public int getItemCount()
    {
        return mFriends.size();
    }

    public class FriendHolder extends RecyclerView.ViewHolder
    {
        private TextView friendName;

        public FriendHolder(View itemView)
        {
            super(itemView);
            friendName = (TextView) itemView.findViewById(R.id.friend_name);
            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Context context = view.getContext();
                    Intent showFriendNameIntent = new Intent(context, FriendsActivity.class);
                    showFriendNameIntent.putExtra("FRIEND_NAME", friendName.getText());
                    context.startActivity(showFriendNameIntent);
                }
            });
        }

        public void bindFriend(String name)
        {
            friendName.setText(name);
        }
    }
}
