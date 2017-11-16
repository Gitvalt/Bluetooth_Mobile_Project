package com.bluetoothgroup.k1967.pictureframecontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.DeviceListener {

    public BluetoothController mBluetoothController;

    private Handler handler;

    private RecyclerView deviceRecycler;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    private int time_discoverable_seconds = 20;  //in seconds

    private static final int IMAGE_CAPTURE_TAG = 664;
    private static final int ACTIVATE_BLUETOOTH_TAG = 738;
    private static final int BLUETOOTH_PERMISSION_TAG = 379;

    private Thread buttonDiscoverableThread = new Thread(){
        @Override
        public void run() {
            Log.i("Discoverable", "Discovery ends");
            FloatingActionButton visibilityButton = (FloatingActionButton)findViewById(R.id.buttonMakeDiscoverable);
            TextView label = (TextView)findViewById(R.id.Discover_Label);

            label.setText(R.string.discoverableLabel);
            visibilityButton.setImageResource(R.drawable.device_invisible);
            visibilityButton.setEnabled(true);

        }
    };


    //---Constructor---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handler = new Handler();
        boolean isBluetoothValid = true;

        //start listening if bluetooth is turned on or off
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        try
        {
            mBluetoothController = new BluetoothController(this, mReceiver);
        }
        catch (NullPointerException error)
        {
            Log.e("Bluetooth_creation", "This devices cannot implement bluetooth connections");
            isBluetoothValid = false;
        }

        deviceRecycler = (RecyclerView)findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        deviceRecycler.setLayoutManager(layoutManager);

        if(isBluetoothValid)
        {
            //bluetooth-controller, where device-select interface is implemented, current activity
            DeviceAdapter deviceAdapter = new DeviceAdapter(mBluetoothController, this, this);
            deviceRecycler.setAdapter(deviceAdapter);

            runDeviceScan();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Bluetooth is not implemented", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mBluetoothController != null)
        {
            BluetoothAdapter mAdapter = mBluetoothController.getBluetoothAdapter();
            if(mAdapter.isDiscovering())
            {
                mAdapter.cancelDiscovery();
            }
        }
    }


    //---Handling Broadcast and Activity responses---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode)
        {
            case IMAGE_CAPTURE_TAG:
                Log.i("IMAGE_ACTIVITY_RESULT", "Got response of take image activity");
                if(resultCode == RESULT_OK)
                {
                    //Image has been taken successfully
                    Log.e("IMAGE_ACTIVITY_RESULT", "Taking image has succeeded");

                    //get image from intent
                    Bundle bundle = data.getExtras();
                    Bitmap image = (Bitmap)bundle.get("data");
                }
                else
                {
                    //Image has failed
                    Log.e("IMAGE_ACTIVITY_RESULT", "Taking image has failed");
                }
                break;


            case ACTIVATE_BLUETOOTH_TAG:

                if(resultCode == RESULT_OK)
                {
                    Log.i("Activate_Bluetooth", "Activating Bluetooth has succeeded");
                    mBluetoothController.findDevices();
                }
                else
                {
                    Log.e("Activate_Bluetooth", "Activating Bluetooth has failed");
                    mBluetoothController.activateBluetooth(true);
                }
                break;

            case BluetoothController.MAKE_DEVICE_DISCOVERABLE:

                if(requestCode == RESULT_CANCELED)
                {
                    Log.e("Discoverable", "Discovery has been declined. Making floatbutton available");
                    handler.post(buttonDiscoverableThread);
                }
                else
                {
                    Log.i("Discoverable", "Phone has been made discoverable for certain duration: " + resultCode);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case BLUETOOTH_PERMISSION_TAG:
                mBluetoothController.findDevices();
                break;
        }
    }


    //---Alert Dialogs---
    public AlertDialog getBluetoothOffAlert(Activity main)
    {
        AlertDialog.Builder bluetoothOffAlert = new AlertDialog.Builder( MainActivity.this)
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
                        mBluetoothController.activateBluetooth(true);
                    }
                });

        AlertDialog alertDialog = bluetoothOffAlert.create();

        return alertDialog;
    }


    //---Updating recycler view---
    //scan for new devices
    private void runDeviceScan(){
        mBluetoothController.findDevices();
    }

    //update bluetooth-controller array to recycler-view
    private void updateRecyclerList()
    {
        deviceRecycler.setLayoutManager(layoutManager);
        deviceRecycler.getAdapter().notifyDataSetChanged();
    }


    //---onClick---
    public void onPhotoActivity(View view)
    {
        Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
        startActivity(intent);
    }

    public void onListRefresh(View view)
    {
        mBluetoothController.clearDeviceList();
        deviceRecycler.getAdapter().notifyDataSetChanged();
        Log.i("Device_Scan", "Refresh list click received");
        runDeviceScan();
    }

    public void onDiscoverableButton(View view)
    {
        if(mBluetoothController != null) {

            int time_discoverable_milliseconds = time_discoverable_seconds * 1000;

            //set "Active"-mode
            FloatingActionButton visibilityButton = (FloatingActionButton)findViewById(R.id.buttonMakeDiscoverable);
            TextView label = (TextView)findViewById(R.id.Discover_Label);

            label.setText(R.string.discoverableLabel);
            visibilityButton.setImageResource(R.drawable.device_visible);
            visibilityButton.setEnabled(false);

            Log.i("Discoverable", "Making this device discoverable for 20 second");
            mBluetoothController.makeDiscoverable(20);

            //after time has passed make button available again
            handler.postDelayed(buttonDiscoverableThread, time_discoverable_milliseconds);
        }
        else {
            Log.e("Discoverable", "Cannot make devices discoverable, This devices does not implement bluetooth!");
        }
    }

    //---interface implementation---
    //Device from recycler view is selected
    @Override
    public void OnDeviceSelect(@NonNull BluetoothDevice selectedDevice) {

        if(selectedDevice == null)
        {
            Log.e("DeviceSelect", "Selected device is empty!");
            return;
        }

        Log.i("DeviceOnSelect", "Device '" + selectedDevice.getAddress() + "' was selected");

        switch (selectedDevice.getBondState()){
            case BluetoothDevice.BOND_BONDING:
                Log.e("DeviceSelect", "Got a click, even though that should have not been possible");
                //if device is still bonding, then getting a click should have not been possible
                break;

            case BluetoothDevice.BOND_NONE:
                selectedDevice.createBond();
                updateRecyclerList();
                break;

            case BluetoothDevice.BOND_BONDED:
                Intent deviceActivity = new Intent(getApplicationContext(), DeviceActivity.class);
                Bundle savedData = new Bundle();

                savedData.putString("deviceName", selectedDevice.getName());
                savedData.putString("deviceAddress", selectedDevice.getAddress());

                deviceActivity.putExtras(savedData);

                startActivity(deviceActivity);
                break;

            default:
                Log.e("DeviceSelect", "Undefined bond state");
                break;
        }
    }


    //---Broadcast receiver---
    /**
     * @callback mReceiver
     * When new bluetooth device is detected, while we are scanning for new devices
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action)
            {

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.i("Bluetooth_broadcast", "staring discovery...");
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.i("Bluetooth_broadcast", "discovery finished");
                    updateRecyclerList();
                    break;

                case BluetoothDevice.ACTION_FOUND:
                    //bluetooth device is found.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothClass deviceClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                    mBluetoothController.addDeviceToList(device);
                    Log.i("Bluetooth_broadcast", "Bluetooth device detected!");
                    updateRecyclerList();
                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    Log.i("Bluetooth_state_changed", "state has changed!");
                    BluetoothDevice deviceInput = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (deviceInput.getBondState()) {
                        case BluetoothDevice.BOND_BONDED:
                            Log.i("state_changed", "Bonded!");
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            Log.i("state_changed", "Bonding!");
                            break;
                        case BluetoothDevice.BOND_NONE:
                            Log.i("state_changed", "No Bond!");
                            break;
                    }
                    updateRecyclerList();
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
                        getBluetoothOffAlert(getParent()).show();
                    }
                    else {

                    }
                    break;
            }
        }
    };


    //---pre-created methods---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
