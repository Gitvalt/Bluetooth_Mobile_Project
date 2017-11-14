package com.bluetoothgroup.k1967.pictureframecontroller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private ImageController mImageController;
    private static final int IMAGECAPTURE_TAG = 664;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
