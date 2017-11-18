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

    private static final String CREATING_B_SOCKET = "Creating Socket";
    public static final UUID myUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");

    public enum MessageTypes {
        ResponseToMessage,
        TestingConnStatus,
        DataReceived

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceHeader = (TextView)findViewById(R.id.deviceHeader);
        deviceAddress = (TextView)findViewById(R.id.addressView);
        devicePairing = (TextView)findViewById(R.id.pairingStatus);

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg)
            {

                MessageTypes messageType = MessageTypes.values()[msg.what];
                int response = msg.arg1;

                Log.i("Handler", "Got message! " + msg);
                Log.i("Handler_msg", "Got response of '" + messageType + "'");

                switch (messageType)
                {
                    case ResponseToMessage:
                        byte[] buffer = new byte[2048];
                        buffer = (byte[])msg.obj;
                        String responseStr = new String(buffer, 0, response);
                        Log.i("Handler", "Data received from server: " + responseStr);
                        break;

                    case TestingConnStatus:
                        Log.i("Handler", "Connection test completed. Status: " + msg.obj);
                        boolean conn_status = (boolean)msg.obj;

                        if(conn_status)
                        {
                            setConnFieldColor(R.string.conn_available);
                        }
                        else
                        {
                            setConnFieldColor(R.string.conn_unavailable);
                        }
                        break;

                    case DataReceived:
                        byte[] buffer_2 = new byte[2048];
                        buffer = (byte[])msg.obj;
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
        if(inputIntent != null)
        {

            deviceHeader.setText(inputIntent.getStringExtra("deviceName"));
            String device_address = inputIntent.getStringExtra("deviceAddress");
            if(device_address != null)
            {
                deviceAddress.setText(device_address);
                mmBluetoothController = new BluetoothController(this, null);

                ArrayMap detectedDevices = mmBluetoothController.getDetectedDevices();

                if(detectedDevices.containsKey(device_address))
                {
                    mmSelectedDevice = (BluetoothDevice)detectedDevices.get(device_address);

                    int bond_state;

                    switch (mmSelectedDevice.getBondState())
                    {
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
                }
                else
                {
                    Log.e("DeviceActivity", "Selected devices is not found");
                }
            }
            else
            {
                Log.e("DeviceActivity", "Device address not defined");
            }
        }

        //finally register broadcast receiver
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)); //connection created
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)); //connection lost
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)); //bluetooth on or off
    }

    //---button onClick---
    public void onReturnButtonClick(View view){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public void onConnectButtonClick(View view)
    {
        TextView connectView = (TextView)findViewById(R.id.connectionStatus);
        connectView.setText(R.string.conn_testing);

        final creatingConnectionThread thread = new creatingConnectionThread(mmSelectedDevice);
        thread.run();

        /**
         * Handle messages:
         */

        if(thread.isConnected)
        {
                final handleSocket handleSocketThread = new handleSocket(thread.mmSocket);
                handleSocketThread.getInputstream(MessageTypes.DataReceived);

                //send results to handler
                Message readMsg = handler.obtainMessage(MessageTypes.TestingConnStatus.ordinal(), true);
                readMsg.sendToTarget();
        }
        else
        {
            Log.e("Bluetooth_conn", "Connection not created, cannot continue");

            //send results to handler
            Message readMsg = handler.obtainMessage(MessageTypes.TestingConnStatus.ordinal(), false);
            readMsg.sendToTarget();
        }
    }

    //Send, view, receive pictures in picture_frame
    public void onPictureManagerClick(View view)
    {
        Log.i("DeviceActivity", "Changing Activity");
        Intent intent = new Intent(getApplicationContext(), PictureManagerActivity.class);
        startActivity(intent);
    }

    //---Label-element setup---
    /**
     * Setup connection status label and it's colors based on current status
     * @param message_resource
     */
    private void setConnFieldColor(@NonNull int message_resource)
    {
        TextView view = (TextView)findViewById(R.id.connectionStatus);
        view.setText(message_resource);

        switch (message_resource)
        {
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

            switch (action)
            {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.w("Receiver", "Created connection to device");
                    setConnFieldColor(R.string.conn_available);
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.w("Receiver", "Connection to device lost");
                    setConnFieldColor(R.string.conn_unavailable);
                    break;

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int tmp = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    Log.i("ACTION_STATE", "Got state changed to '" + tmp);
                    if(tmp == BluetoothAdapter.STATE_TURNING_ON)
                    {
                        Log.w("Bluetooth", "Bluetooth is turned on");
                    }
                    else if(tmp == BluetoothAdapter.STATE_TURNING_OFF)
                    {
                        Log.w("Bluetooth","Bluetooth has been turned off");
                        getBluetoothOffAlert(getParent());
                    }
                    else
                    {
                        //we don't care about any other state changes, just on or off
                    }
                    break;
            }
        }
    };

    //---Alert Dialogs---
    public AlertDialog getBluetoothOffAlert(@NonNull Activity main)
    {
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

    //---Internal Classes---
    public class creatingConnectionThread extends Thread
    {
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;
        public boolean isConnected;
        private static final int maxSocketCycle = 5;
        private static final int maxConnCycle = 5;
        private static final int maxTimeMillis = (5)*(1000);

        public creatingConnectionThread(@NonNull BluetoothDevice bluetoothDevice)
        {
            mmDevice = bluetoothDevice;

            //creating bluetooth socket
            if(createSocket())
            {
                Log.i("SOCKET_CREATION", "Creating Socket Succeeded");
            }
            else
            {
                Log.e("SOCKET_CREATION", "Creating Socket failed");
            }

        }

        private boolean createSocket()
        {
            BluetoothSocket tmp;
            for (int i = 0; i < maxSocketCycle; i++)
            {
                try
                {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(myUUID);
                    mmSocket = tmp;
                    Log.i("CREATING_B_SOCKET", "Successfully created a bluetooth rfcomm socket");
                    return true;
                }
                catch (IOException error)
                {
                    Log.e("CREATING_B_SOCKET", "Cannot create bluetooth rfcomm socket. Sleep and try again.", error);
                    try
                    {
                        Thread.sleep(maxTimeMillis);
                        //for loop starts a new
                    }
                    catch (InterruptedException err)
                    {
                        Log.e("CREATING_B_SOCKET", "Cannot make thread to sleep", err);
                    }
                }
            }

            //if socket could not be created durning for loop, then function has failed
            return false;
        }

        public BluetoothSocket getSocket()
        {
            return mmSocket;
        }

        private void connectToDevice(@NonNull BluetoothSocket BlueSocket)
        {
            for(int i = 0; i < maxConnCycle; i++)
            {
                try
                {
                    BlueSocket.connect();
                    isConnected = true;
                    Log.i("Socket_Connection", "Connection has been created");
                    return;
                }
                catch (Exception error)
                {
                    Log.e("Socket_Connection", "Error in connecting to server", error);
                    try
                    {
                        Thread.sleep(maxTimeMillis);
                        //for loop starts a new
                    }
                    catch (InterruptedException err)
                    {
                        Log.e("Socket_Connection", "Cannot make thread to sleep", err);
                    }
                }
            }

            isConnected = false;
        }

        @Override
        public void run()
        {

            //just in case, stop discovery.
            mmBluetoothController.getBluetoothAdapter().cancelDiscovery();


            if(mmSocket == null)
            {
                Log.e(CREATING_B_SOCKET, "Cannot create connection. Socket is null", new NullPointerException("Socket is empty!"));
                isConnected = false;
                return;
            }
            else
            {
                connectToDevice(mmSocket);

                if(!isConnected)
                {
                    Log.e("Socket_Connection", "Still no connection", new Exception("No Connection"));
                }
            }
        }

        public void cancel()
        {
            if(mmSocket.isConnected())
            {
                try {
                    mmSocket.close();
                }
                catch (IOException error)
                {
                    Log.e("Socket_Connection", "Socket could not be closed", error);
                }
            }
        }
    }

    public class handleSocket extends Thread
    {

        private BluetoothSocket mmSocket;
        private InputStream mmIputStream;
        private OutputStream mmOutputStream;
        private byte[] buffer = new byte[2048];
        private static final String Bluetooth_handler = "Bluetooth_handler";

        public handleSocket(@NonNull BluetoothSocket socket)
        {

            mmSocket = socket;
            try {

                //if not connected
                if(!socket.isConnected())
                {
                    Log.w(Bluetooth_handler, "There is no connection to socket. Trying to create connection");
                    if(!connect())
                    {
                        Log.e(Bluetooth_handler, "Connecting to device failed");
                        return;
                    }
                }

                    mmIputStream = socket.getInputStream();
                    mmOutputStream = socket.getOutputStream();

            }
            catch (IOException ioError)
            {
                Log.e(Bluetooth_handler, "Fetching streams from socket has failed", ioError);
            }
        }

        /**
         * listen for communication from the device
         */
        public void getInputstream(MessageTypes inqueryType) {
            try {

                if(mmSocket == null || mmIputStream == null || mmOutputStream == null)
                {
                    Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                    return;
                }

                buffer = new byte[2048];

                int response = mmIputStream.read(buffer);

                String responseStr = new String(buffer, 0, response);

                Log.i(Bluetooth_handler, "Received message: " + responseStr);
                Message readMsg = handler.obtainMessage(MessageTypes.valueOf(inqueryType.name()).ordinal(), response, -1, buffer);
                readMsg.sendToTarget();

            }
            catch (IOException e)
            {
                Log.e(Bluetooth_handler, "Couldn't receive msg", e);
            }
        }


        public void sendMessage(String message)
        {
            if(mmSocket == null || mmIputStream == null || mmOutputStream == null)
            {
                Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                return;
            }

            try {
                byte[] send = message.getBytes();

                //send the information
                mmOutputStream.write(send);

                //start listening for response
                getInputstream(MessageTypes.ResponseToMessage);

            } catch (IOException e) {
                Log.e(Bluetooth_handler, "Couldn't send msg", e);
            }
        }

        public void sendImage(Bitmap image)
        {
            if(mmSocket == null || mmIputStream == null || mmOutputStream == null)
            {
                Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                return;
            }

            try {

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] send = stream.toByteArray();

                //send the information
                mmOutputStream.write(send);

                //start listening for response
                getInputstream(MessageTypes.ResponseToMessage);

            } catch (IOException e) {
                Log.e(Bluetooth_handler, "Couldn't send msg", e);
            }
        }


        @Override
        public void run()
        {
            mmBluetoothController.getBluetoothAdapter().cancelDiscovery();
        }

        public boolean connect(){
            try
            {
                mmSocket.connect();

                if(mmSocket.isConnected())
                {
                    Log.i("Socket_Connection", "Bluetooth socket has established connection with server");
                    return true;
                }
                else
                {
                    Log.e("Socket_Connection", "Bluetooth socket could not connect to server", new Exception("Connection not established"));
                    return false;
                }

            }
            catch (Exception error)
            {
                Log.e("Socket_Connection", "Error in connecting to server", error);
                return false;
            }
        }

        public void closeConnection()
        {
            try
            {
                if(mmSocket.isConnected())
                {
                    mmSocket.close();
                    Log.i(Bluetooth_handler, "Connection to device closed");
                }
            }
            catch (IOException ioerror)
            {
                Log.e(Bluetooth_handler, "Connection could not be closed", ioerror);
            }
        }

        public void cancel()
        {
            mmOutputStream = null;
            mmIputStream = null;


            if(mmSocket.isConnected()){
                closeConnection();
            }

        }
    }
}
