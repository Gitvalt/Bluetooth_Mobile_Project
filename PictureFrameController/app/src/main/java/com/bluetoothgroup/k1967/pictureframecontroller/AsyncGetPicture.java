package com.bluetoothgroup.k1967.pictureframecontroller;

import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Valtteri on 8.12.2017.
 */

/**
 * AsyncGetPicture - Fetches a bitmap from the PictureFrame and then send's fetched bitmap to Handler
 */
public class AsyncGetPicture extends AsyncTask<Void, Void, Bitmap> {

    private BluetoothController mBluetoothController;
    private BluetoothDevice mDevice;
    private Handler mHandler;

    //Constructor
    public AsyncGetPicture(BluetoothController controller, BluetoothDevice device, Handler handler) {
        mBluetoothController = controller;
        mDevice = device;
        mHandler = handler;
    }


    @Override
    protected void onPreExecute() {
        Log.i("AsyncDownload", "Loading picture from server has begun");
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        Log.i("AsyncDownload", "Task Completed");
        Message readMsg = null;

        //if bitmap was created, then set messages arg1 as 1. Else set arg1 --> 0
        if(bitmap == null){
            readMsg = mHandler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 2, 0, null);

        } else {
            readMsg = mHandler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 1, 0, bitmap);
        }

        //send results to handler
        readMsg.sendToTarget();
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        //Bitmap shownPic = mBluetoothController.getCurrentlyShownPic(mDevice, mHandler);
        try {
            Bitmap image = mBluetoothController.getImage(mDevice, mHandler);
            return image;
        }
        catch (Exception error)
        {
            Log.e("ButtonClick", ""+error);
            return null;
        }
    }
}
