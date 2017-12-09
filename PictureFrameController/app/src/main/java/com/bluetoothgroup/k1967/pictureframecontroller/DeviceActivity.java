package com.bluetoothgroup.k1967.pictureframecontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {

    private TextView deviceHeader;
    private TextView deviceAddress;
    private TextView devicePairing;

    private BluetoothController mmBluetoothController;
    private BluetoothDevice mmSelectedDevice;

    public static Handler handler = new Handler();
    private Thread currentlyRunningLoop;

    public enum MessageTypes {
        ResponseToMessage,
        TestingConnStatus,
        DataReceived,
        ImageReceived,
        ImageSent

    }

    ;

    //---Constructor---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceHeader = (TextView) findViewById(R.id.deviceHeader);
        deviceAddress = (TextView) findViewById(R.id.addressView);
        devicePairing = (TextView) findViewById(R.id.pairingStatus);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                MessageTypes messageType = MessageTypes.values()[msg.what];
                int response = msg.arg1;

                Log.v("Handler", "Got message! " + msg);
                Log.v("Handler_msg", "Got response of '" + messageType + "'");

                switch (messageType) {
                    case ResponseToMessage:
                        String responseStr = (String)msg.obj;
                        Log.i("Handler", "Data received from server: " + responseStr);
                        break;

                    //when results of connection testing are broadcasted
                    case TestingConnStatus:
                        Log.i("Handler", "Connection test completed. Status: " + msg.obj);
                        boolean conn_status = (boolean) msg.obj;

                        if (conn_status) {
                            setConnFieldColor(R.string.conn_available);
                        } else {
                            setConnFieldColor(R.string.conn_unavailable);
                        }
                        break;

                    case DataReceived:
                        byte[] buffer_2 = new byte[2048];
                        byte[] buffer = (byte[]) msg.obj;
                        String responseStr_2 = new String(buffer_2, 0, response);

                        Log.i("Handler", "Data received from server: " + responseStr_2);
                        break;

                    default:
                        Log.i("Handler", "Unkown messagetype");
                        break;
                }
            }
        };

        //fetching data from MainActivity
        Intent inputIntent = getIntent();
        if (inputIntent != null) {

            deviceHeader.setText(inputIntent.getStringExtra("deviceName"));
            String device_address = inputIntent.getStringExtra("deviceAddress");
            if (device_address != null) {
                deviceAddress.setText(device_address);
                mmBluetoothController = new BluetoothController(this, null);

                ArrayMap detectedDevices = mmBluetoothController.getDetectedDevices();

                if (detectedDevices.containsKey(device_address)) {
                    mmSelectedDevice = (BluetoothDevice) detectedDevices.get(device_address);

                    int bond_state;

                    switch (mmSelectedDevice.getBondState()) {
                        case BluetoothDevice.BOND_BONDED:
                            bond_state = R.string.Bonded;
                            break;

                        case BluetoothDevice.BOND_BONDING:
                            bond_state = R.string.Bonding;
                            break;

                        case BluetoothDevice.BOND_NONE:
                            bond_state = R.string.noBond;
                            break;

                        default:
                            bond_state = R.string.unkown_status;
                    }

                    devicePairing.setText(bond_state);
                } else {
                    Log.e("DeviceActivity", "Selected devices is not found");
                }
            } else {
                Log.e("DeviceActivity", "Device address not defined");
            }
        }

        //finally register broadcast receiver
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)); //connection created
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)); //connection lost
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)); //bluetooth on or off
    }


    //---button onClick---
    public void onReturnButtonClick(View view) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public void onConnectButtonClick(View view) {
        TextView connectView = (TextView) findViewById(R.id.connectionStatus);
        connectView.setText(R.string.conn_testing);
        mmBluetoothController.testConnection(mmSelectedDevice, handler);

    }

    //Send, view, receive pictures in picture_frame
    public void onPictureManagerClick(View view) {
        Log.i("DeviceActivity", "Changing Activity");
        Bundle bundle = new Bundle();
        bundle.putString("DeviceAddress", mmSelectedDevice.getAddress());

        Intent intent = new Intent(getApplicationContext(), PictureManagerActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    //---Label-element setup---

    /**
     * Setup connection status label and it's colors based on current status
     *
     * @param message_resource
     */
    private void setConnFieldColor(@NonNull int message_resource) {
        TextView view = (TextView) findViewById(R.id.connectionStatus);
        view.setText(message_resource);

        switch (message_resource) {
            case R.string.unkown_status:
                view.setTextColor(Color.GRAY);
                break;

            case R.string.conn_unavailable:
                view.setTextColor(Color.RED);
                break;

            case R.string.conn_available:
                view.setTextColor(Color.GREEN);
                break;

            case R.string.conn_testing:
                view.setTextColor(Color.BLACK);
                break;
        }


    }


    //---Broadcast receiver---
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.w("Receiver", "Created connection to device");
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.w("Receiver", "Connection to device lost");
                    break;

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int tmp = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    Log.i("ACTION_STATE", "Got state changed to '" + tmp);
                    if (tmp == BluetoothAdapter.STATE_TURNING_ON) {
                        Log.w("Bluetooth", "Bluetooth is turned on");
                    } else if (tmp == BluetoothAdapter.STATE_TURNING_OFF) {
                        Log.w("Bluetooth", "Bluetooth has been turned off");
                        getBluetoothOffAlert(getParent());
                    } else {
                        //we don't care about any other state changes, just on or off
                    }
                    break;
            }
        }
    };


    //---Alert Dialogs---
    public AlertDialog getBluetoothOffAlert(@NonNull Activity main) {
        AlertDialog.Builder bluetoothOffAlert = new AlertDialog.Builder(DeviceActivity.this)
                .setTitle("Bluetooth is turned off")
                .setMessage("Bluetooth should be turned on in order to use the application")
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("Bluetooth", "Turn Bluetooth off");
                        System.exit(0);
                    }
                })
                .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i("Bluetooth", "Turn Bluetooth on");
                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        adapter.enable();
                    }
                });

        AlertDialog alertDialog = bluetoothOffAlert.create();

        return alertDialog;
    }
}
