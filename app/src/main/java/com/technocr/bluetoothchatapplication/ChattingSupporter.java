
package com.technocr.bluetoothchatapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Rohit Sharma on 8/4/2017.
 * This class handles all the bluetooth connections as well as
 * Reading and writing of messages.
 */

public class ChattingSupporter implements Serializable {

    //Insecure UUID to get the incoming connection and to here for the Accept Thread
    private static final UUID INSECURE_UUID = UUID.fromString("069e4d24-749b-11e7-8cf7-a6006ad3dba0");

    private final BluetoothAdapter objBluetoothAdapter;
    private final Handler objHandler;
    private AcceptThread objAcceptThread;
    private ConnectionThread objConnectionThread;
    private ReadAndWriteThread objReadAndWriteThread;

    private int state;

    //Constant that are being passed to get the communication
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final String DEVICE_OBJECT = "device_name";
    private static final String TAG = ChattingSupporter.class.getSimpleName();

    //State of the bluetooth device
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public static final String APP_NAME = "BluetoothChatApplication";


    public ChattingSupporter(Context context, Handler objHandler) {
        objBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;

        this.objHandler = objHandler;
    }

    // Tells what is the current Status of the of the App
    public synchronized void setCurrentState(int currentState) {
        this.state = currentState;
        Log.d(TAG,"Current State is :"+currentState);
        objHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }


    //Starting the thread service
    public synchronized void start() {
        // If any connection or Thread is already running than it should
        //  get cancelled before running any more instances
        if (objConnectionThread != null) {
            objConnectionThread.cancel();
            objConnectionThread = null;
            Log.d(TAG, "Connection Thread is Cancelled");
        }

        if (objReadAndWriteThread != null) {
            objReadAndWriteThread.cancel();
            objReadAndWriteThread = null;
            Log.d(TAG, "Read and Write Thread is Cancelled");
        }

        setCurrentState(STATE_LISTEN);
        //The thread will start listening for incoming connections
        if (objAcceptThread == null) {
            objAcceptThread = new ChattingSupporter.AcceptThread();
            objAcceptThread.start();
            Log.d(TAG, "Accept Thread has been started");
        }
    }


    public synchronized void connectToSelectedDevice(BluetoothDevice device) {

        if (state == STATE_CONNECTING) {
            if (objConnectionThread != null) {
                objConnectionThread.cancel();
                objConnectionThread = null;
                Log.d(TAG, "Inside Connecting block, Connection thread is getting cancelled");
            }
        }

        if (objReadAndWriteThread != null) {
            objReadAndWriteThread.cancel();
            objReadAndWriteThread = null;
        }

        objConnectionThread = new ChattingSupporter.ConnectionThread(device);
        objConnectionThread.start();
        Log.d(TAG, "Connection has been started with :"+device.getName());
        setCurrentState(STATE_CONNECTING);
    }

    // Manges the incoming connections
    public synchronized void connectToIncomingConnection(BluetoothSocket socket, BluetoothDevice device) {

        if (objConnectionThread != null) {
            objConnectionThread.cancel();
            objConnectionThread = null;

        }


        if (objReadAndWriteThread != null) {
            objReadAndWriteThread.cancel();
            objReadAndWriteThread = null;
        }

        if (objAcceptThread != null) {
            objAcceptThread.cancel();
            objAcceptThread = null;
        }

        objReadAndWriteThread = new ChattingSupporter.ReadAndWriteThread(socket);
        objReadAndWriteThread.start();
        Log.d(TAG, "Read and Writh thread started and socket is implemented");

        // Send the name to get shown as a Toast Notification
        Message objMsg = objHandler.obtainMessage(MESSAGE_DEVICE_OBJECT);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DEVICE_OBJECT, device);
        objMsg.setData(bundle);
        objHandler.sendMessage(objMsg);

        setCurrentState(STATE_CONNECTED);
    }

    // Stop all the running Threads
    public synchronized void stop() {
        if (objConnectionThread != null) {
            objConnectionThread.cancel();
            objConnectionThread = null;
        }

        if (objReadAndWriteThread != null) {
            objReadAndWriteThread.cancel();
            objReadAndWriteThread = null;
        }

        if (objAcceptThread != null) {
            objAcceptThread.cancel();
            objAcceptThread = null;
        }
        Log.d(TAG, "All the threads are stopped");
        setCurrentState(STATE_NONE);
    }

    public void write(byte[] out) {
        ChattingSupporter.ReadAndWriteThread objReadAndWriteThread;
        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            objReadAndWriteThread = this.objReadAndWriteThread;
        }
        objReadAndWriteThread.write(out);
    }

    // If the connection was unsuccessful
    private void connectionFailed() {
        Message msg = objHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Unable to connectToSelectedDevice device");
        msg.setData(bundle);
        objHandler.sendMessage(msg);
        Log.d(TAG, "The connection was unsuccessful");

        // Restart the Service
        ChattingSupporter.this.start();
    }

    // Runs when the connection lost
    private void connectionLost() {
        Message msg = objHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Device connection was lost");
        msg.setData(bundle);
        objHandler.sendMessage(msg);
        Log.d(TAG, "The connection to the host is lost");

        // Restart the Service
        ChattingSupporter.this.start();
    }

    // This class is responsible for accepting the incoming connections
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            Log.d(TAG, "Inside the AcceptThread");
            BluetoothServerSocket tmp = null;
            try {
                tmp = objBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, INSECURE_UUID);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            serverSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket;
            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // Connection Accepted
                if (socket != null) {
                    synchronized (ChattingSupporter.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //Start the connection to connect
                                connectToIncomingConnection(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Connection is unsuccessful
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG,"Error in Connecting : "+e.getMessage());
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    // runs while attempting to make an outgoing connection
    private class ConnectionThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectionThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(INSECURE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            objBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChattingSupporter.this) {
                objConnectionThread = null;
            }

            // Start the connectToIncomingConnection thread
            connectToIncomingConnection(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ReadAndWriteThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ReadAndWriteThread(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            InputStream tempInputStream = null;
            OutputStream tempOutputStream = null;

            try {
                tempInputStream = socket.getInputStream();
                tempOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG,"Error in InputOutputStream : "+e.getMessage());
            }

            inputStream = tempInputStream;
            outputStream = tempOutputStream;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);

                    objHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    Log.e(TAG,e.getMessage());
                    ChattingSupporter.this.start();
                    break;
                }
            }
        }


        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                objHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG,"Exception in writing method : "+e.getMessage());
            }
        }

        //TO STRING METHOD
        @Override
        public String toString() {
            return "ReadWriteThread{" +
                    "bluetoothSocket=" + bluetoothSocket +
                    ", inputStream=" + inputStream +
                    ", outputStream=" + outputStream +
                    '}';
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Getters and Setters
    public BluetoothAdapter getObjBluetoothAdapter() {
        return objBluetoothAdapter;
    }

    public Handler getObjHandler() {
        return objHandler;
    }

    public AcceptThread getObjAcceptThread() {
        return objAcceptThread;
    }

    public void setObjAcceptThread(AcceptThread objAcceptThread) {
        this.objAcceptThread = objAcceptThread;
    }

    public ConnectionThread getObjConnectionThread() {
        return objConnectionThread;
    }

    public void setObjConnectionThread(ConnectionThread objConnectionThread) {
        this.objConnectionThread = objConnectionThread;
    }

    public ReadAndWriteThread getObjReadAndWriteThread() {
        return objReadAndWriteThread;
    }

    public void setObjReadAndWriteThread(ReadAndWriteThread objReadAndWriteThread) {
        this.objReadAndWriteThread = objReadAndWriteThread;
    }

    public synchronized int getState()
    {
        return state;
    }
}
