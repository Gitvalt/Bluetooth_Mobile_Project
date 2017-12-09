package com.bluetoothgroup.k1967.pictureframecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.InputStream;

public class PictureManagerActivity extends AppCompatActivity {

    private static final int GET_GALLERY_IMAGE = 691;
    private static final int GET_CAMERA_IMAGE = 240;

    private BluetoothController mmBluetoothController;
    private BluetoothDevice mmDevice;
    private Bitmap mmSelectedImage;

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DeviceActivity.MessageTypes messageType = DeviceActivity.MessageTypes.values()[msg.what];
            int response = msg.arg1;

            Log.i("Handler", "Got message! " + msg);
            Log.i("Handler_msg", "Got response of '" + messageType + "'");

            switch (messageType) {
                case ImageReceived:

                    ProgressBar progressBar = (ProgressBar)findViewById(R.id.current_progressBar);
                    progressBar.setVisibility(View.INVISIBLE);

                    ImageView picHolder = (ImageView) findViewById(R.id.currentImageView);
                    switch (response) {
                        case 1:

                            Log.i("Download Handler", "Image received successfully");
                            Bitmap image = (Bitmap) msg.obj;
                            picHolder.setImageBitmap(image);
                            Toast.makeText(getApplicationContext(), "Fetching image has succeeded", Toast.LENGTH_LONG).show();
                            break;

                        case 2:
                            Log.e("Download Handler", "Image could not be created");
                            picHolder.setImageBitmap(null);
                            Toast.makeText(getApplicationContext(), "Loading currently shown picture has failed", Toast.LENGTH_LONG).show();
                            break;

                        default:
                            Log.e("Download Handler", "Unknown download picture handler message!");
                            picHolder.setImageBitmap(null);
                            break;
                    }

                    break;

                //When image is sent from the phone to pictureframe
                case ImageSent:
                    //-1 == message from server
                    switch (msg.arg2) {
                        //error! something failed
                        case 0:

                            String toastMSG = "";

                            if (msg.arg1 == 2) {
                                Log.e("Handler", "Could not read server init response");
                                toastMSG = "Could not read response from server";
                            } else if (msg.arg1 == 1) {
                                Log.e("Handler", "Connection could not be created");
                                toastMSG = "Connection could not be created";
                            } else if(msg.arg1 == 3){
                                Log.e("Handler", "Picture successfully sent!");
                                toastMSG = "Picture successfully sent";
                            } else {
                                Log.e("Handler", "undefined error");
                                toastMSG = "Unknown error";
                            }

                            Toast.makeText(getApplicationContext(), toastMSG, Toast.LENGTH_SHORT).show();

                            break;

                        //got msg
                        case -1:
                            byte[] buffer = new byte[2048];
                            buffer = (byte[]) msg.obj;
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

        if (address == null) {
            throw new NullPointerException("Device address is empty. Cant send imaged to undefined device");
        } else {
            mmDevice = mmBluetoothController.getDetectedDevices().get(address);
        }

        getCurrentlyShownPicture();

    }


    //--onclick function---
    public void onCameraButtonClick(View view) {
        try {
            Log.i("ButtonClick", "onCameraButton clicked!");
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            ProgressBar progressBar = (ProgressBar)findViewById(R.id.captured_progressBar);
            progressBar.setVisibility(View.VISIBLE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, GET_CAMERA_IMAGE);
            }
        } catch (Exception error) {
            Log.e("ButtonClick", "Getting image from gallery has failed", error);
        }
    }

    public void onGalleryButtonClick(View view) {
        try {
            Log.i("ButtonClick", "onGalleryButton clicked!");
            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setType("image/");

            ProgressBar progressBar = (ProgressBar)findViewById(R.id.captured_progressBar);
            progressBar.setVisibility(View.VISIBLE);

            startActivityForResult(galleryIntent, GET_GALLERY_IMAGE);
        } catch (Exception error) {
            Log.e("ButtonClick", "Getting image from gallery has failed", error);
        }
    }

    public void onReturnButtonClick(View view) {
        Intent deviceActivity = new Intent(this, DeviceActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("deviceName", mmDevice.getName());
        bundle.putString("deviceAddress", mmDevice.getAddress());
        deviceActivity.putExtras(bundle);

        startActivity(deviceActivity);
    }

    // TODO: 9.12.2017 Latausikonit
    // TODO: 9.12.2017 Kun painetaan serverikuvaa, päivitetään
    // TODO: 9.12.2017 Kaikenlaiset messagheboxist 
    public void onSendToDeviceButtonClick(View view) {
        if (mmSelectedImage != null) {
            mmBluetoothController.sendImage(mmDevice, handler, mmSelectedImage);
        } else {
            Log.e("Send_Image", "Cannot send image. None defined for sending", new NullPointerException("No image selected!"));
        }
    }

    //---Picture function---
    public void getCurrentlyShownPicture() {

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.current_progressBar);
        progressBar.setVisibility(View.VISIBLE);

        AsyncGetPicture fetcher = new AsyncGetPicture(mmBluetoothController, mmDevice, handler);
        Toast.makeText(getApplicationContext(), "Fetching currently shown picture from the pictureframe", Toast.LENGTH_LONG).show();
        fetcher.execute();
        Log.i("Get Image", "getCurrentlyShownPic completed!");
    }


    //---On activity response---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView receivedImage = (ImageView) findViewById(R.id.capturedImageView);

        switch (requestCode) {
            case GET_GALLERY_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri imageURI = data.getData();
                        InputStream imageStream = getContentResolver().openInputStream(imageURI);
                        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        int width = selectedImage.getWidth(), height = selectedImage.getHeight();

                        if (width > 1280 && height > 960) {
                            imageStream = getContentResolver().openInputStream(imageURI);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 4;

                            selectedImage = BitmapFactory.decodeStream(imageStream, new Rect(0, 0, 0, 0), options);
                            receivedImage.setImageBitmap(selectedImage);
                            mmSelectedImage = selectedImage;
                        } else {
                            receivedImage.setImageBitmap(selectedImage);
                            mmSelectedImage = selectedImage;
                        }

                        ProgressBar progressBar = (ProgressBar)findViewById(R.id.captured_progressBar);
                        progressBar.setVisibility(View.INVISIBLE);

                    } catch (Exception error) {
                        Log.i("Fetching image", "Fetching image from gallery has failed!", error);
                        ProgressBar progressBar = (ProgressBar)findViewById(R.id.captured_progressBar);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
                break;

            case GET_CAMERA_IMAGE:

                ProgressBar progressBar = (ProgressBar)findViewById(R.id.captured_progressBar);
                progressBar.setVisibility(View.INVISIBLE);

                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    receivedImage.setImageBitmap(imageBitmap);
                    mmSelectedImage = imageBitmap;

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
