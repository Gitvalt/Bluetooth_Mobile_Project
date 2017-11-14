package com.bluetoothgroup.k1967.pictureframecontroller;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by K1967 on 14.11.2017.
 */

public class ImageController {

    private Activity mmActivity;

    private static final int IMAGECAPTURE_TAG = 664;
    
    public ImageController(Activity parent)
    {
        mmActivity = parent;
    }

    //---Camera---

    public boolean checkCameraHardware()
    {
        if(mmActivity.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            return true;
        }
        else
        {
            Log.e("CAMERA_HARDWARE", "Camera hardware is not available");
            return false;
        }
    }

    public void takeAPicture()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mmActivity.getPackageManager()) != null) {
            mmActivity.startActivityForResult(takePictureIntent, IMAGECAPTURE_TAG);
        }

    }

    //---Gallery---
}
