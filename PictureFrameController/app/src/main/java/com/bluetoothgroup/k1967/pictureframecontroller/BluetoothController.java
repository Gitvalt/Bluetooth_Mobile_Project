package com.bluetoothgroup.k1967.pictureframecontroller;;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.util.ArrayMap;
import android.util.Log;

import com.bluetoothgroup.k1967.pictureframecontroller.MainActivity;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by Valtteri on 14.11.2017.
 */

public class BluetoothController {

    private ArrayMap<String, BluetoothDevice> DetectedDevices;
    private BluetoothAdapter mmBluetoothAdapter;
    private Activity mmParent;
    private BroadcastReceiver mmBroadcastReceiver;

    private static final int ACTIVATE_BLUETOOTH_TAG = 738;
    private static final int BLUETOOTH_PERMISSION_TAG = 379;

    private static final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

        }
    };

    private static final int a_ScanningTimer_s = 20; //20 seconds
    public static final int a_ScanningTimer = (1000 * a_ScanningTimer_s);

    /**
     *
     * 1. Enable Bluetooth
     * 2. Ask Permissions
     * 3. Register Service
     * 4. Find devices
     * 5. Pair with devices
     *
     */

    //---Constructor---
    public BluetoothController(Activity activity, BroadcastReceiver broadC)
    {
        DetectedDevices = new ArrayMap<>();
        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mmParent = activity;
        mmBroadcastReceiver = broadC;
    }


    //---Getters----
    public BluetoothAdapter getBluetoothAdapter() {
        return mmBluetoothAdapter;
    }

    public ArrayMap<String, BluetoothDevice> getDetectedDevices() {
        return DetectedDevices;
    }


    //---Enable Bluetooth---
    public void activateBluetooth(boolean setState){

        //turn on or off?
        if(setState)
        {
            //if bluetooth is wanted to be activated
            if (mmBluetoothAdapter.isEnabled())
            {
                Log.i("ENABLE_BLUETOOTH", "Bluetooth is already on!");
            }
            else
            {
                //Not on --> ask to be turned on --> get result in onActivityResult
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mmParent.startActivityForResult(enableBluetooth, ACTIVATE_BLUETOOTH_TAG);
                return;
            }
        }

        //off
        else
        {
            if (mmBluetoothAdapter.isEnabled())
            {
                //Bluetooth is on --> shutdown
                mmBluetoothAdapter.disable();
                mmParent.unregisterReceiver(mmBroadcastReceiver);
            }
            else
            {
                //if bluetooth is already turned off
                Log.i("ENABLE_BLUETOOTH", "Bluetooth is already off!");
            }
        }
    }


    //---Bluetooth Permissions---
    public boolean bluetoothPermissions()
    {

        Context mContext = mmParent.getApplicationContext();

        //check if app has permission to use Bluetooth (X >= Android 6.0)
        int selfPermission = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.BLUETOOTH);
        int adminPermission = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN);

        int fineLocation = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocation = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        int granted = PermissionChecker.PERMISSION_GRANTED;

        /**
         * If some of the necessary permissions are not granted, App will ask the user for them...
         */
        if(selfPermission != granted || adminPermission != granted || fineLocation != granted || coarseLocation != granted)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void askPermissions()
    {
        /**
         * Show alert dialog informing what permissions are required for this app to work
         */
        AlertDialog.Builder alert = new AlertDialog.Builder(mmParent.getWindow().getContext());
        alert.setTitle("Permissions have not been granted");
        alert.setMessage("Permissions for coarse/fine location and Bluetooth are required");
        alert.setCancelable(false);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                //Send the required permissions permissionRequest
                ActivityCompat.requestPermissions(mmParent, new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, BLUETOOTH_PERMISSION_TAG);
            }
        });

        alert.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close application
                System.exit(0);
            }
        });

        alert.show();
        //App continues in BroadcastReceiver
    }


    //---Register broadcasts---
    public void registerBluetoothService(){
        BroadcastReceiver receiver = mmBroadcastReceiver;
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
    }


    //---Finding devices---
    public void findDevices()
    {
        try {
            mmBluetoothAdapter.cancelDiscovery();

            if (!mmBluetoothAdapter.isEnabled())
            {
                Log.e("FINDING_DEVICES", "Cant find devices. Bluetooth is Disabled!");
                activateBluetooth(true);
                return;
            }

            DetectedDevices.clear();
            addPairedDevicesToList();

            if (!bluetoothPermissions())
            {
                askPermissions();
            }
            else
            {

                registerBluetoothService();

                //makes this device discoverable for 20 seconds
                makeDiscoverable(20);

                mmBluetoothAdapter.startDiscovery();
                Thread stopScanning = new Thread() {
                    @Override
                    public void run() {
                        if (mmBluetoothAdapter.isDiscovering()) {
                            mmBluetoothAdapter.cancelDiscovery();
                            Log.i("FINDING_DEVICES", "Cancelling device Discovery");
                        }
                    }
                };

                //activate the cancelling thread
                mHandler.postDelayed(stopScanning, a_ScanningTimer);
                Log.i("FINDING_DEVICES", "Started to scan surroundings for new bluetooth devices");
            }
        }
        catch (Exception error)
        {
            Log.e("FINDING_DEVICES", "Finding new devices has failed!", error);
        }
    }

    public void makeDiscoverable(int seconds)
    {
        //Discover this devices
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        mmParent.startActivity(discoverableIntent);
    }

    public void addDeviceToList(@NonNull BluetoothDevice mDevice)
    {
        try
        {
            DetectedDevices.put(mDevice.getAddress(), mDevice);
        }
        catch (Exception error)
        {
            Log.e("Adding Device", "Adding device to list has failed", error);
        }
    }

    private void addPairedDevicesToList()
    {
        ArrayMap<String, BluetoothDevice> pairedDevices = getPairedDevices();

        if(pairedDevices == null){
            Log.w("Paired_devices", "No paired devices to add");
        }
        else
        {
            //put all found devices to main arraymap
            DetectedDevices.putAll(pairedDevices);
            Log.i("Paired_devices", "Devices added to main array");
        }
    }

    private ArrayMap getPairedDevices()
    {
        Set<BluetoothDevice> devices = mmBluetoothAdapter.getBondedDevices();
        ArrayMap<String, BluetoothDevice> tmp = new ArrayMap<>();

        if(devices.size() > 0)
        {

            for (BluetoothDevice device : devices) {
                tmp.put(device.getAddress(), device);
            }

            Log.i("Paired_Devices", "Found '" + tmp.size() + "' paired devices");
            return tmp;
        }
        else
        {
            Log.w("Paired_Devices", "No paired devices available");
            return null;
        }
    }


    //---Pairing with device---
    @NonNull
    private void PairDevices(BluetoothDevice mDevice)
    {
        try
        {
            if (mmBluetoothAdapter.getBondedDevices().contains(mDevice)) {
                Log.e("Bluetooth_Pairing", "Device has already been paired with");
            } else {
                mDevice.createBond();
                Log.i("Bluetooth_Pairing", "Pairing devices completed!");
            }
        }
        catch (Exception error)
        {
            Log.e("Bluetooth_Pairing", "Unkown error with pairing", error);
        }
    }

    @NonNull
    private void UnPairDevices(BluetoothDevice mDevice)
    {
        try {

            if(!mmBluetoothAdapter.getBondedDevices().contains(mDevice))
            {
                Log.e("Bluetooth_Pairing", "Device has no bond with this device");
            }

            Method m = mDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(mDevice, (Object[]) null);
            Log.i("Bluetooth_Pairing", "Unpairing devices completed!");
        }
        catch (Exception e)
        {
            Log.e("Bluetooth_Pairing", "Error in unpairing devices", e);
        }
    }
}
