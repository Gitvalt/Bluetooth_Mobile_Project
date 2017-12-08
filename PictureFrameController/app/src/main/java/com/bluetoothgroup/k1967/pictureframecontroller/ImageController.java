package com.bluetoothgroup.k1967.pictureframecontroller;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by K1967 on 14.11.2017.
 */

public class ImageController {

    private Activity mmActivity;

    private static final int IMAGECAPTURE_TAG = 664;

    public ImageController(Activity parent) {
        mmActivity = parent;
    }

    //---saving and loading---
    public void saveImage() throws IOException {

    }

    private void addToGallery(String Photopath) {
        try {
            Intent mediascanner = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = new File(Photopath);
            Uri contentURI = Uri.fromFile(file);
            mediascanner.setData(contentURI);
        } catch (Exception error) {
            Log.e("IMAGE", "Unsuspected error in adding picture to gallery", error);
        }
    }

    //---Camera---

    public boolean checkCameraHardware() {
        if (mmActivity.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            Log.e("CAMERA_HARDWARE", "Camera hardware is not available");
            return false;
        }
    }

    public void takeAPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mmActivity.getPackageManager()) != null) {
            mmActivity.startActivityForResult(takePictureIntent, IMAGECAPTURE_TAG);
        }

    }

    //---Gallery---
}
