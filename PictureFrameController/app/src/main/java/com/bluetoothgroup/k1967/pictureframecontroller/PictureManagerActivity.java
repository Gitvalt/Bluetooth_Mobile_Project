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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureManagerActivity extends AppCompatActivity {

    private static final int GET_GALLERY_IMAGE = 691;
    private static final int GET_CAMERA_IMAGE = 240;

    private BluetoothController mmBluetoothController;
    private BluetoothDevice mmDevice;

    public static final String photoFolder  = "/PictureFrameController/photos/";
    public static final String photoFolderTmp  = "/PictureFrameController/photos/tmp/";
    public static final String photoRootFolder  = "/PictureFrameController";

    /**
     * mmSelectedImage - Image from gallery or camera
     * mmFetchedImage - Image from PictureFrame-device
     */
    private Bitmap mmSelectedImage;
    private Bitmap mmFetchedImage;

    //Uri we get from camera
    private Uri cameraPictureURI;

    //image Uri from galleria or camera
    private Uri selectedPhoto;

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DeviceActivity.MessageTypes messageType = DeviceActivity.MessageTypes.values()[msg.what];
            int response = msg.arg1;

            Log.i("Handler", "Got message! " + msg);
            Log.i("Handler_msg", "Got response of '" + messageType + "'");

            switch (messageType) {
                // TODO: 12.12.2017 Tarkista, että tiedoston tallennus oikeasti onnistui 
                case ImageReceived:

                    ProgressBar progressBar = (ProgressBar)findViewById(R.id.current_progressBar);
                    progressBar.setVisibility(View.INVISIBLE);

                    ImageView picHolder = (ImageView) findViewById(R.id.currentImageView);
                    switch (response) {
                        case 1:

                            Log.i("Download Handler", "Image received successfully");
                            Bitmap image = (Bitmap) msg.obj;
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                            File file = new File(photoFolderTmp, timeStamp);
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(file);
                                image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            }
                            catch (Exception e)
                            {
                                Log.e("SaveFetched", "Saving fetched image to tmp folder has failed", e);
                            }

                            //finally try to close fileoutputstream
                            finally {
                                try {
                                    if (fos != null) {
                                        fos.close();
                                    }
                                }
                                catch (Exception err)
                                {
                                    Log.e("SaveFetched", "Could not close fileoutputstream",err);
                                }
                            }

                            try {
                                Bitmap savedImage = BitmapFactory.decodeStream(new FileInputStream(file));
                                picHolder.setImageBitmap(savedImage);
                                mmFetchedImage = savedImage;
                                selectedPhoto = Uri.parse(file.getAbsolutePath());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
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
                                getCurrentlyShownPicture();
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

        selectedPhoto = null;
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

            //create directory for photos if it does not exist
            createFileDir();

            //Creates a filename and where photos will be saved
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File file = new File(Environment.getExternalStorageDirectory(), photoFolder + "photo_" + timeStamp + ".png");
            Uri imageUri = Uri.fromFile(file);

            //Save uri to photo for later
            cameraPictureURI = imageUri;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

            //Set "loading progressbar" to visible
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

    @Deprecated
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

    // TODO: 9.12.2017 Kun painetaan serverikuvaa, päivitetään
    // TODO: 9.12.2017 Kaikenlaiset messagheboxist 
    public void onSendToDeviceButtonClick(View view) {
        if (mmSelectedImage != null) {
            mmBluetoothController.sendImage(mmDevice, handler, mmSelectedImage);
        } else {
            Log.e("Send_Image", "Cannot send image. None defined for sending", new NullPointerException("No image selected!"));
        }
    }


    /**
     * Save fetched image to storage
     * @param view
     */
    public void saveToStorageButtonClick(View view){

    }

    //---Picture function---

    private void saveToStorage(@NonNull Bitmap targetIMG, Uri targetUri){

    }

    /**
     * This function fetches the currently shown picture from the PictureFrame Device.
     */
    public void getCurrentlyShownPicture() {

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.current_progressBar);
        progressBar.setVisibility(View.VISIBLE);

        AsyncGetPicture fetcher = new AsyncGetPicture(mmBluetoothController, mmDevice, handler);
        Toast.makeText(getApplicationContext(), "Fetching currently shown picture from the pictureframe", Toast.LENGTH_LONG).show();
        fetcher.execute();
        Log.i("Get Image", "getCurrentlyShownPic completed!");
    }

    /**
     * Create photos directory to $photoFolder, if it does not exist
     */
    public void createFileDir(){

        boolean permission = mmBluetoothController.bluetoothPermissions();

        //If necessary permissions have not been given
        if(permission == false)
        {
            mmBluetoothController.askPermissions(getParent());
        }

        String path = String.valueOf(Environment.getExternalStorageDirectory() + photoRootFolder);

        try {
            File tmp = new File(path);
            File folder = new File(tmp.getAbsolutePath(), "/photos");
            File folder_tmp = new File(tmp.getAbsolutePath(), "/photos/tmp");


            boolean success = true;
            boolean success_2 = true;

            if (!folder.exists()) {
                success = folder.mkdir();
            }
            else {
                Log.i("FileDir", "Folders already exists");
            }

            if(!folder_tmp.exists()){
                success_2 = folder_tmp.mkdir();
            } else {
                Log.i("FileDir", "Tmp Folders already exists");
            }

            if (success && success_2) {
                Log.i("FileDir", "Folder's has been created");
            } else {
                Log.e("FileDir", "Could not create folder");
            }

        }
        catch (Exception err){
            Log.e("FileDir", "Could not create folder", err);
        }
    }

    /**
     * Resizes the inserted photo to smaller size so that it can be displayed on phone.
     * @param photo
     */
    public Bitmap resizePhoto(@NonNull Uri imageURI){

        try {
            Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), imageURI);
            Bitmap resizedPhoto;
            int width = photo.getWidth(), height = photo.getHeight();

            if (width > 1280 && height > 960) {
                InputStream imageStream = getContentResolver().openInputStream(imageURI);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;

                resizedPhoto = BitmapFactory.decodeStream(imageStream, new Rect(0, 0, 0, 0), options);
                return resizedPhoto;
            } else {
                return photo;
            }
        }
        catch (Exception err){
            Log.e("ResizeError", "Error caught while resizing photo", err);
            return null;
        }
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
                        Bitmap selectedImage = resizePhoto(imageURI);

                        receivedImage.setImageBitmap(selectedImage);
                        mmSelectedImage = selectedImage;
                        selectedPhoto = cameraPictureURI;

                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.captured_progressBar);
                        progressBar.setVisibility(View.INVISIBLE);

                    } catch (Exception error) {
                        Log.i("Fetching image", "Fetching image from gallery has failed!", error);
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.captured_progressBar);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
                break;

            case GET_CAMERA_IMAGE:

                ProgressBar progressBar = (ProgressBar) findViewById(R.id.captured_progressBar);
                progressBar.setVisibility(View.INVISIBLE);

                if (resultCode == RESULT_OK) {

                    if (cameraPictureURI == null) {
                        Log.e("CameraPicture", "Could not find the URI to taken photo");
                        return;
                    }


                    //Taken picture normally is too large to be display --> resize picture
                    Bitmap resizedPic = resizePhoto(cameraPictureURI);

                    if (resizedPic == null) {
                        Toast.makeText(getApplicationContext(), "Image could not be resized", Toast.LENGTH_SHORT).show();
                        break;
                    } else {
                        receivedImage.setImageBitmap(resizedPic);
                        mmSelectedImage = resizedPic;
                    }

                    cameraPictureURI = null;
                    selectedPhoto = cameraPictureURI;

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
