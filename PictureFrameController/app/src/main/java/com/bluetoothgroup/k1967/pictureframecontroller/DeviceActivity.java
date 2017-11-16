package com.bluetoothgroup.k1967.pictureframecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {

    private TextView deviceHeader;
    private TextView deviceAddress;
    private TextView devicePairing;

    private BluetoothController mmBluetoothController;
    private BluetoothDevice mmSelectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceHeader = (TextView)findViewById(R.id.deviceHeader);
        deviceAddress = (TextView)findViewById(R.id.addressView);
        devicePairing = (TextView)findViewById(R.id.pairingStatus);


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
    }

    //---button onClick---
    public void onReturnButtonClick(View view){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}
