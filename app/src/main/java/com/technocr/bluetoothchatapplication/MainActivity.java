package com.technocr.bluetoothchatapplication;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Rohit Sharma on 8/02/2017.
 * The MainActivity that is being used when the application starts
 */

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView currentStatus;
    private Button connectButton;
    private ListView listView;
    private Dialog dialog;
    private EditText messageEditBox;
    private BluetoothAdapter objBluetoothAdapter;
    private Button mSendButton;
    private List<ChatMessages> chatMessagesList;
    private MessageAdapter messageAdapter;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private ChattingSupporter objChattingSupporter;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    private static final int STATE_OF_CONNECTED=3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //currentStatus = (TextView) findViewById(R.id.status);
        connectButton = (Button) findViewById(R.id.btn_connect);
        listView = (ListView) findViewById(R.id.list);

        listView = (ListView) findViewById(R.id.list);
        messageEditBox = (EditText) findViewById(R.id.input_layout1);
        mSendButton = (Button) findViewById(R.id.btn_send);

        listView.setDivider(null);
        listView.setDividerHeight(0);

        //check device support bluetooth or not
        objBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (objBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available_error, Toast.LENGTH_SHORT).show();
            Log.d(TAG,"Bluetooth is not available, EXITING");
            finish();
        }

        //Chat features are hidden before a connection is successful
        messageEditBox.setVisibility(View.GONE);
        mSendButton.setVisibility(View.GONE);

        //Show a dialog box which shows the list of devices
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
                deviceDialogueBox();
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(messageEditBox.getText().toString())) {
                    Toast.makeText(MainActivity.this, R.string.empty_send_error, Toast.LENGTH_SHORT).show();
                } else {
                    try
                    {
                        sendMessage(messageEditBox.getText().toString());
                        messageEditBox.setText(R.string.empty_string);
                    }catch (NullPointerException e)
                    {
                        Log.e(TAG,e.getMessage());
                    }

                }
            }
        });

        chatMessagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this,chatMessagesList);
        listView.setAdapter(messageAdapter);
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChattingSupporter.STATE_CONNECTED:
                            //setCurrentStatus("Connected to: " + connectingDevice.getName());
                            connectButton.setEnabled(false);
                            messageEditBox.setVisibility(View.VISIBLE);
                            mSendButton.setVisibility(View.VISIBLE);
                            //connectButton.setVisibility(View.INVISIBLE);
                            objChattingSupporter.setCurrentState(ChattingSupporter.STATE_CONNECTED);
                            break;
                        case ChattingSupporter.STATE_CONNECTING:
                            //setCurrentStatus("Connecting...");
                            connectButton.setEnabled(false);
                            break;
                        case ChattingSupporter.STATE_LISTEN:
                        case ChattingSupporter.STATE_NONE:
                            //setCurrentStatus("Not connectToIncomingConnection");
                            //connectButton.setVisibility(View.VISIBLE);
                            connectButton.setEnabled(true);
                            messageEditBox.setVisibility(View.GONE);
                            mSendButton.setVisibility(View.GONE);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    chatMessagesList.add(new ChatMessages(writeMessage,true));
                    messageAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatMessagesList.add(new ChatMessages(readMessage,false));
                    messageAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), getString(R.string.connected_to) + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    updateTheUi();
                    break;
            }
            return false;
        }
    });

    private void deviceDialogueBox() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_bluetooth);
        dialog.setTitle(R.string.title_bluetooth_devices);

        // If currently a discovering is going on than cancel it
        if (objBluetoothAdapter.isDiscovering()) {
            objBluetoothAdapter.cancelDiscovery();
        }
        objBluetoothAdapter.startDiscovery();

        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        ListView pairedDeviceList = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView unPairedDeviceList = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        pairedDeviceList.setAdapter(pairedDevicesAdapter);
        unPairedDeviceList.setAdapter(discoveredDevicesAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        objBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = objBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(getString(R.string.none_paired));
        }

        pairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                objBluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                Log.d(TAG, getString(R.string.device_address)+address);
                dialog.dismiss();
            }

        });

        unPairedDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                objBluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                Log.d(TAG, getString(R.string.device_address)+address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }


    private void connectToDevice(String deviceAddress) {
        objBluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = objBluetoothAdapter.getRemoteDevice(deviceAddress);
        objChattingSupporter.connectToSelectedDevice(device);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    objChattingSupporter = new ChattingSupporter(this, handler);
                } else {
                    Toast.makeText(this, R.string.bluetooth_disabled_error, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (objChattingSupporter.getState() != STATE_OF_CONNECTED) {
            Toast.makeText(this, R.string.connection_lost_error, Toast.LENGTH_SHORT).show();
            Log.d(TAG,"Current State : "+objChattingSupporter.getState());
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            objChattingSupporter.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!objBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            objChattingSupporter = new ChattingSupporter(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (objChattingSupporter != null) {
            if (objChattingSupporter.getState() == ChattingSupporter.STATE_NONE) {
                objChattingSupporter.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (objChattingSupporter != null)
            objChattingSupporter.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(getString(R.string.none_found));
                }
            }
        }
    };

    private void setCurrentStatus(String s)
    {
        currentStatus.setText(s);
    }

    public void updateTheUi() {

        new Thread() {
            public void run() {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                connectButton.setEnabled(true);
                                messageEditBox.setEnabled(false);
                                mSendButton.setEnabled(false);
                                //connectButton.setVisibility(View.VISIBLE);
                                //("Not Connected");
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }.start();

    }
}
