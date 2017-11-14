package com.bluetoothgroup.k1967.pictureframecontroller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class DeviceActivity extends AppCompatActivity {

    private ImageController mImageController;
    private ImageView previewImage;
    private static final int IMAGECAPTURE_TAG = 664;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        mImageController = new ImageController(this);
        //previewImage = (ImageView)findViewById(R.id.previewView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
    }

    public void OnClick_GalleryButton(View view)
    {

    }

    public void OnClick_CameraButton(View view)
    {
        mImageController.checkCameraHardware();
        mImageController.takeAPicture();
    }

}
