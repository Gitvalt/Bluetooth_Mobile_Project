package com.bluetoothgroup.k1967.pictureframecontroller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class PictureActivity extends AppCompatActivity {

    private ImageController mImageController;
    private static final int IMAGECAPTURE_TAG = 664;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        mImageController = new ImageController(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMAGECAPTURE_TAG)
        {
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
        }
    }

}
