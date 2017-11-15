package com.bluetoothgroup.k1967.pictureframecontroller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {

    private TextView deviceHeader;
    private TextView deviceAddress;
    private TextView devicePairing;

    private BluetoothController mmBluetoothController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        deviceHeader = (TextView)findViewById(R.id.deviceHeader);
        deviceAddress = (TextView)findViewById(R.id.addressView);
        devicePairing = (TextView)findViewById(R.id.pairingStatus);

        deviceHeader.setText(savedInstanceState.getString("deviceName"));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
        if(requestCode == IMAGECAPTURE_TAG)
        {
            Log.i("IMAGE_ACTIVITY_RESULT", "Got response of take image activity");
            if(resultCode == RESULT_OK)
            {
                //Image has been taken successfully
                Log.i("IMAGE_ACTIVITY_RESULT", "Taking image has succeeded");

                //get image from intent
                Bundle bundle = data.getExtras();
                Bitmap image = (Bitmap)bundle.get("data");

                //show image
                previewImage.setImageBitmap(image);
            }
            else
            {
                //Image has failed
                Log.e("IMAGE_ACTIVITY_RESULT", "Taking image has failed");
            }
        }
        */
    }

    //---button onClick---
    public void onReturnButtonClick(View view){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}
