package com.bluetoothgroup.k1967.pictureframecontroller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

public class PictureManagerActivity extends AppCompatActivity {

    private static final int GET_GALLERY_IMAGE = 691;
    private static final int GET_CAMERA_IMAGE = 240;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_manager);
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

    public void onSendToDeviceButtonClick(View view)
    {

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
                        }
                        else
                        {
                            receivedImage.setImageBitmap(selectedImage);
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
}
