package com.bluetoothgroup.k1967.pictureframecontroller;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

public class PictureManagerActivity extends AppCompatActivity {

    private static final int GET_GALLERY_IMAGE = 691;
    private static final int GET_CAMERA_IMAGE = 240;

    private BluetoothController mmBluetoothController;
    private BluetoothDevice mmDevice;
    private Bitmap mmSelectedImage;

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg)
        {
            DeviceActivity.MessageTypes messageType = DeviceActivity.MessageTypes.values()[msg.what];
            int response = msg.arg1;

            Log.i("Handler", "Got message! " + msg);
            Log.i("Handler_msg", "Got response of '" + messageType + "'");

            switch (messageType)
            {
                case ImageReceived:
                    //-1 == message from server
                    switch (msg.arg2)
                    {
                        //error! something failed
                        case 0:
                            if(msg.arg1 == 2)
                            {
                                Log.e("Handler", "Could not read server init response");
                            }
                            else if(msg.arg1 == 1)
                            {
                                Log.e("Handler", "Connection could not be created");
                            }
                            else
                            {
                                Log.e("Handler", "undefined error");
                            }
                            break;

                        //got msg
                        case -1:
                            byte[] buffer = new byte[2048];
                            buffer = (byte[])msg.obj;
                            String responseStr = new String(buffer, 0, response);
                            Log.i("Handler", "Data received from server: " + responseStr);
                            break;
                    }
                    break;


                default:
                    Log.i("Handler", "Unkown messagetype");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_manager);

        mmBluetoothController = new BluetoothController(this, mReceiver);
        String address = getIntent().getExtras().getString("DeviceAddress");

        if(address == null)
        {
            throw new NullPointerException("Device address is empty. Cant send imaged to undefined device");
        }
        else
        {
            mmDevice = mmBluetoothController.getDetectedDevices().get(address);
        }

    }


    //--onclick function---
    public void onCameraButtonClick(View view)
    {
        try {
            Log.i("ButtonClick", "onCameraButton clicked!");
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, GET_CAMERA_IMAGE);
            }
        }
        catch (Exception error)
        {
            Log.e("ButtonClick", "Getting image from gallery has failed", error);
        }
    }

    public void onGalleryButtonClick(View view)
    {
        try {
            Log.i("ButtonClick", "onGalleryButton clicked!");
            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/");
            startActivityForResult(galleryIntent, GET_GALLERY_IMAGE);
        }
        catch (Exception error)
        {
            Log.e("ButtonClick", "Getting image from gallery has failed", error);
        }
    }


    public void GetImageButton(View view)
    {
        try {
            mmBluetoothController.getImage(mmDevice, handler, view);
        }
        catch (Exception error)
        {
            Log.e("ButtonClick", ""+error);
        }
    }

    public void onSendToDeviceButtonClick(View view)
    {
        if(mmSelectedImage != null)
        {
            mmBluetoothController.sendImage(mmDevice, handler, mmSelectedImage);
        }
        else
        {
            Log.e("Send_Image", "Cannot send image. None defined for sending", new NullPointerException("No image selected!"));
        }
    }

    //---On activity response---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView receivedImage = (ImageView) findViewById(R.id.capturedImageView);

        switch (requestCode)
        {
            case GET_GALLERY_IMAGE:
                if(resultCode == RESULT_OK)
                {
                    try
                    {
                        Uri imageURI = data.getData();
                        InputStream imageStream = getContentResolver().openInputStream(imageURI);
                        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        int width = selectedImage.getWidth(), height = selectedImage.getHeight();

                        if(width > 1280 && height > 960){
                            imageStream = getContentResolver().openInputStream(imageURI);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 4;

                            selectedImage = BitmapFactory.decodeStream(imageStream, new Rect(0,0,0,0), options);
                            receivedImage.setImageBitmap(selectedImage);
                            mmSelectedImage = selectedImage;
                        }
                        else
                        {

                            receivedImage.setImageBitmap(selectedImage);
                            mmSelectedImage = selectedImage;
                        }
                    }
                    catch (Exception error)
                    {
                        Log.i("Fetching image", "Fetching image from gallery has failed!", error);
                    }
                }
                break;

            case GET_CAMERA_IMAGE:
                if(resultCode == RESULT_OK){
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    receivedImage.setImageBitmap(imageBitmap);
                }
                break;

            default:
                //unknown response
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
        }
    };


}
