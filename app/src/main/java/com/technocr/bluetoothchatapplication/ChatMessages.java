package com.technocr.bluetoothchatapplication;

/**
 * Created by Rohit Sharma on 8/5/2017.
 * This is a POJO class for handling the messages
 */

public class ChatMessages {

    private String message;
    private boolean fromMe;

    public ChatMessages(String content, boolean fromMe) {
        this.message = content;
        this.fromMe = fromMe;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFromMe() {
        return fromMe;
    }

    public void setFromMe(boolean fromMe) {
        this.fromMe = fromMe;
    }
}
