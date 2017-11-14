package com.bluetoothgroup.k1967.pictureframecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.DeviceListener {

    private ImageController mImageController;
    private BluetoothController mBluetoothController;

    private RecyclerView deviceRecycler;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private static final int IMAGECAPTURE_TAG = 664;
    private static final int ACTIVATE_BLUETOOTH_TAG = 738;
    private static final int BLUETOOTH_PERMISSION_TAG = 379;

    public static enum availableActions {
        Pair,
        unPair
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mImageController = new ImageController(this);

        boolean isBluetoothValid = true;

        try
        {
            mBluetoothController = new BluetoothController(this, mReceiver);
        }
        catch (NullPointerException error)
        {
            Log.e("Bluetooth_creation", "This devices cannot implement bluetooth connectinos");
            isBluetoothValid = false;
        }

        deviceRecycler = (RecyclerView)findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        deviceRecycler.setLayoutManager(layoutManager);

        if(isBluetoothValid)
        {
            //bluetooth-controller, where deviceselect interface is implemented, current activity
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode)
        {
            case IMAGECAPTURE_TAG:
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

    //---interface implementation---
    //Device from recycler view is selected
    @Override
    public void OnDeviceSelect(BluetoothDevice selectedDevice, availableActions action)
    {
        Log.i("DeviceOnSelect", "Device '" + selectedDevice.getAddress() + "' was selected");
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
