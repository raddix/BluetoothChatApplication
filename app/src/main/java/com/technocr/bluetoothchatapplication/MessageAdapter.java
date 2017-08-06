package com.technocr.bluetoothchatapplication;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Rohit Sharma on 8/5/2017.
 * This class acts as an Adapter for Messages to show a
 * Bubbly Chat boxes with messages in it.
 * The Text boxes are using a 9-Patch image as a background in here.
 */

public class MessageAdapter extends ArrayAdapter<ChatMessages>{

    public MessageAdapter(Activity context, List<ChatMessages> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listView = convertView;
        ChatMessages chatMessage = getItem(position);
        TextView messageTextView;
        if(listView==null)
        {
            if(chatMessage.isFromMe())
            {
                listView = LayoutInflater.from(getContext())
                        .inflate(R.layout.chat_for_me,parent,false);
                messageTextView = (TextView) listView.findViewById(R.id.msg_for_me);
                messageTextView.setText(chatMessage.getMessage());
            }
            else
            {
                listView = LayoutInflater.from(getContext())
                        .inflate(R.layout.chat_for_other,parent,false);
                messageTextView = (TextView) listView.findViewById(R.id.msg_for_other);
                messageTextView.setText(chatMessage.getMessage());
            }
        }


        return listView;
    }
}
