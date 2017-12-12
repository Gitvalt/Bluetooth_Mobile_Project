package com.bluetoothgroup.k1967.pictureframecontroller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

;

/**
 * Created by Valtteri on 14.11.2017.
 */

public class BluetoothController {

    private static ArrayMap<String, BluetoothDevice> DetectedDevices;
    private BluetoothAdapter mmBluetoothAdapter;
    private Activity mmParent;
    private BroadcastReceiver mmBroadcastReceiver;

    private static final int ACTIVATE_BLUETOOTH_TAG = 738;
    private static final int BLUETOOTH_PERMISSION_TAG = 379;
    public static final int MAKE_DEVICE_DISCOVERABLE = 789;

    private static final Handler mHandler = new Handler();

    private static final int a_ScanningTimer_s = 20; //20 seconds
    public static final int a_ScanningTimer = (1000 * a_ScanningTimer_s);

    private static final String CREATING_B_SOCKET = "Creating Socket";
    public static final UUID myUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    
    /**
     *
     * 1. Enable Bluetooth
     * 2. Ask Permissions
     * 3. Register Service
     * 4. Find devices
     * 5. Pair with devices
     *
     */

    //---Constructor---
    public BluetoothController(Activity activity, @Nullable BroadcastReceiver broadC)
    {
        if(DetectedDevices == null) {
            DetectedDevices = new ArrayMap<>();
        }
        mmParent = activity;
        mmBroadcastReceiver = broadC;

        mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mmBluetoothAdapter == null)
        {
            throw new NullPointerException("This devices does not support bluetooth connections");
        }
    }


    //---Getters----
    public BluetoothAdapter getBluetoothAdapter() {
        return mmBluetoothAdapter;
    }

    public ArrayMap<String, BluetoothDevice> getDetectedDevices() {
        return DetectedDevices;
    }


    //---Enable Bluetooth---
    public void activateBluetooth(boolean setState){

        //turn on or off?
        if(setState)
        {
            //if bluetooth is wanted to be activated
            if (mmBluetoothAdapter.isEnabled())
            {
                Log.i("ENABLE_BLUETOOTH", "Bluetooth is already on!");
            }
            else
            {
                //Not on --> ask to be turned on --> get result in onActivityResult
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mmParent.startActivityForResult(enableBluetooth, ACTIVATE_BLUETOOTH_TAG);
                return;
            }
        }

        //off
        else
        {
            if (mmBluetoothAdapter.isEnabled())
            {
                //Bluetooth is on --> shutdown
                mmBluetoothAdapter.disable();
                mmParent.unregisterReceiver(mmBroadcastReceiver);
            }
            else
            {
                //if bluetooth is already turned off
                Log.i("ENABLE_BLUETOOTH", "Bluetooth is already off!");
            }
        }
    }


    //---Bluetooth Permissions---
    public boolean bluetoothPermissions()
    {

        Context mContext = mmParent.getApplicationContext();

        //check if app has permission to use Bluetooth (X >= Android 6.0)
        int selfPermission = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.BLUETOOTH);
        int adminPermission = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN);

        int fineLocation = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocation = PermissionChecker.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        int granted = PermissionChecker.PERMISSION_GRANTED;

        /**
         * If some of the necessary permissions are not granted, App will ask the user for them...
         */
        if(selfPermission != granted || adminPermission != granted || fineLocation != granted || coarseLocation != granted)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void askPermissions()
    {
        /**
         * Show alert dialog informing what permissions are required for this app to work
         */
        AlertDialog.Builder alert = new AlertDialog.Builder(mmParent.getWindow().getContext());
        alert.setTitle("Permissions have not been granted");
        alert.setMessage("Permissions for coarse/fine location and Bluetooth are required");
        alert.setCancelable(false);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                //Send the required permissions permissionRequest
                ActivityCompat.requestPermissions(mmParent, new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, BLUETOOTH_PERMISSION_TAG);
            }
        });

        alert.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close application
                System.exit(0);
            }
        });

        alert.show();
        //App continues in BroadcastReceiver
    }


    //---Register broadcasts---
    public void registerBluetoothService(){
        BroadcastReceiver receiver = mmBroadcastReceiver;
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        mmParent.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
    }


    //---Finding devices---
    public void findDevices()
    {
        try {
            mmBluetoothAdapter.cancelDiscovery();

            if (!mmBluetoothAdapter.isEnabled())
            {
                Log.e("FINDING_DEVICES", "Cant find devices. Bluetooth is Disabled!");
                activateBluetooth(true);
                return;
            }

            DetectedDevices.clear();
            addPairedDevicesToList();

            if (!bluetoothPermissions())
            {
                askPermissions();
            }
            else
            {

                registerBluetoothService();

                mmBluetoothAdapter.startDiscovery();

                Thread stopScanning = new Thread() {
                    @Override
                    public void run() {
                        if (mmBluetoothAdapter.isDiscovering()) {
                            mmBluetoothAdapter.cancelDiscovery();
                            Log.i("FINDING_DEVICES", "Cancelling device Discovery");
                        }
                    }
                };

                //activate the cancelling thread
                mHandler.postDelayed(stopScanning, a_ScanningTimer);
                Log.i("FINDING_DEVICES", "Started to scan surroundings for new bluetooth devices");
            }
        }
        catch (Exception error)
        {
            Log.e("FINDING_DEVICES", "Finding new devices has failed!", error);
        }
    }

    public void makeDiscoverable(int seconds)
    {
        //Discover this devices
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        mmParent.startActivityForResult(discoverableIntent, MAKE_DEVICE_DISCOVERABLE);
    }

    public void addDeviceToList(@NonNull BluetoothDevice mDevice)
    {
        try
        {
            DetectedDevices.put(mDevice.getAddress(), mDevice);
        }
        catch (Exception error)
        {
            Log.e("Adding Device", "Adding device to list has failed", error);
        }
    }

    public void removeDeviceFromList(@NonNull BluetoothDevice mDevice)
    {
        try
        {
            DetectedDevices.remove(mDevice.getAddress());
        }
        catch (Exception error)
        {
            Log.e("Removing Device", "Removing device from list has failed", error);
        }
    }

    public void clearDeviceList(){
        DetectedDevices.clear();
    }

    private void addPairedDevicesToList()
    {
        ArrayMap<String, BluetoothDevice> pairedDevices = getPairedDevices();

        if(pairedDevices == null){
            Log.w("Paired_devices", "No paired devices to add");
        }
        else
        {
            //put all found devices to main arraymap
            DetectedDevices.putAll(pairedDevices);
            Log.i("Paired_devices", "Devices added to main array");
        }
    }

    public ArrayMap getPairedDevices()
    {
        Set<BluetoothDevice> devices = mmBluetoothAdapter.getBondedDevices();
        ArrayMap<String, BluetoothDevice> tmp = new ArrayMap<>();

        if(devices.size() > 0)
        {

            for (BluetoothDevice device : devices) {
                tmp.put(device.getAddress(), device);
            }

            Log.i("Paired_Devices", "Found '" + tmp.size() + "' paired devices");
            return tmp;
        }
        else
        {
            Log.w("Paired_Devices", "No paired devices available");
            return null;
        }
    }


    //---Pairing with device---
    @NonNull
    private void PairDevices(BluetoothDevice mDevice)
    {
        try
        {
            if (mmBluetoothAdapter.getBondedDevices().contains(mDevice)) {
                Log.e("Bluetooth_Pairing", "Device has already been paired with");
            } else {
                mDevice.createBond();
                Log.i("Bluetooth_Pairing", "Pairing devices completed!");
            }
        }
        catch (Exception error)
        {
            Log.e("Bluetooth_Pairing", "Unkown error with pairing", error);
        }
    }

    @NonNull
    private void UnPairDevices(BluetoothDevice mDevice)
    {
        try {

            if(!mmBluetoothAdapter.getBondedDevices().contains(mDevice))
            {
                Log.e("Bluetooth_Pairing", "Device has no bond with this device");
            }

            Method m = mDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(mDevice, (Object[]) null);
            Log.i("Bluetooth_Pairing", "Unpairing devices completed!");
        }
        catch (Exception e)
        {
            Log.e("Bluetooth_Pairing", "Error in unpairing devices", e);
        }
    }


    //---Communicating with device---
    public void testConnection(@NonNull BluetoothDevice mdevice, @NonNull Handler handler) {
        final creatingConnectionThread thread = new creatingConnectionThread(mdevice);
        thread.run();

        /**
         * Handle messages:
         */
        if (thread.isConnected) {
            final handleSocket handleSocketThread = new handleSocket(thread.mmSocket, handler);
            handleSocketThread.getInputstream(DeviceActivity.MessageTypes.DataReceived);

            //send results to handler
            Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.TestingConnStatus.ordinal(), true);
            readMsg.sendToTarget();
        }
        else {
            Log.e("Bluetooth_conn", "Connection not created, cannot continue");

            //send results to handler
            Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.TestingConnStatus.ordinal(), false);
            readMsg.sendToTarget();
        }
    }

    public void sendImage(@NonNull BluetoothDevice mdevice, @NonNull Handler handler, @NonNull Bitmap image)
    {
        creatingConnectionThread connectionThread = new creatingConnectionThread(mdevice);
        connectionThread.run();

        if (connectionThread.isConnected)
        {
            final handleSocket handleSocketThread = new handleSocket(connectionThread.mmSocket, handler);
            boolean is_conn = handleSocketThread.testConnection();

            //was response successfully read?
            if(is_conn)
            {
                Log.i("Bluetooth_con", "Sending image to device");
                handleSocketThread.sendImage(image);
                handleSocketThread.getInputstream(DeviceActivity.MessageTypes.ImageReceived);
            }
            else
            {
                Log.e("Bluetooth_conn", "Could not read server response");

                //send results to handler
                Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 2, 0, false);
                readMsg.sendToTarget();
            }
        }
        else
        {
            Log.e("Bluetooth_conn", "Connection not created, cannot continue");

            //send results to handler
            Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 1,0, false);
            readMsg.sendToTarget();
        }
    }


    public void getImage(@NonNull BluetoothDevice mdevice, @NonNull Handler handler, View view)
    {
        creatingConnectionThread connectionThread = new creatingConnectionThread(mdevice);
        connectionThread.run();

        if (connectionThread.isConnected)
        {
            final handleSocket handleSocketThread = new handleSocket(connectionThread.mmSocket, handler);
            boolean is_conn = handleSocketThread.testConnection();

            //was response successfully read?
            if(is_conn)
            {
                Log.i("Bluetooth_con", "Sending image to device");
                handleSocketThread.getImage(view);
            }
            else
            {
                Log.e("Bluetooth_conn", "Could not read server response");

                //send results to handler
                Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 2, 0, false);
                readMsg.sendToTarget();
            }
        }
        else
        {
            Log.e("Bluetooth_conn", "Connection not created, cannot continue");

            //send results to handler
            Message readMsg = handler.obtainMessage(DeviceActivity.MessageTypes.ImageReceived.ordinal(), 1,0, false);
            readMsg.sendToTarget();
        }
    }

    //---Internal Classes---
    public class creatingConnectionThread extends Thread
    {
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;
        public boolean isConnected;
        private static final int maxSocketCycle = 5;
        private static final int maxConnCycle = 5;
        private static final int maxTimeMillis = (5)*(1000);

        public creatingConnectionThread(@NonNull BluetoothDevice bluetoothDevice)
        {
            mmDevice = bluetoothDevice;

            //creating bluetooth socket
            if(createSocket())
            {
                Log.i("SOCKET_CREATION", "Creating Socket Succeeded");
            }
            else
            {
                Log.e("SOCKET_CREATION", "Creating Socket failed");
            }

        }



        private boolean createSocket()
        {
            BluetoothSocket tmp;
            for (int i = 0; i < maxSocketCycle; i++)
            {
                try
                {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(myUUID);
                    mmSocket = tmp;
                    Log.i("CREATING_B_SOCKET", "Successfully created a bluetooth rfcomm socket");
                    return true;
                }
                catch (IOException error)
                {
                    Log.e("CREATING_B_SOCKET", "Cannot create bluetooth rfcomm socket. Sleep and try again.", error);
                    try
                    {
                        Thread.sleep(maxTimeMillis);
                        //for loop starts a new
                    }
                    catch (InterruptedException err)
                    {
                        Log.e("CREATING_B_SOCKET", "Cannot make thread to sleep", err);
                    }
                }
            }

            //if socket could not be created durning for loop, then function has failed
            return false;
        }

        public BluetoothSocket getSocket()
        {
            return mmSocket;
        }

        private void connectToDevice(@NonNull BluetoothSocket BlueSocket)
        {
            for(int i = 0; i < maxConnCycle; i++)
            {
                try
                {
                    BlueSocket.connect();
                    isConnected = true;
                    Log.i("Socket_Connection", "Connection has been created");
                    return;
                }
                catch (Exception error)
                {
                    Log.e("Socket_Connection", "Error in connecting to server", error);
                    try
                    {
                        Thread.sleep(maxTimeMillis);
                        //for loop starts a new
                    }
                    catch (InterruptedException err)
                    {
                        Log.e("Socket_Connection", "Cannot make thread to sleep", err);
                    }
                }
            }

            isConnected = false;
        }

        @Override
        public void run()
        {
            //just in case, stop discovery.
            mmBluetoothAdapter.cancelDiscovery();

            if(mmSocket == null)
            {
                Log.e(CREATING_B_SOCKET, "Cannot create connection. Socket is null", new NullPointerException("Socket is empty!"));
                isConnected = false;
                return;
            }
            else
            {
                connectToDevice(mmSocket);
                if(!isConnected)
                {
                    Log.e("Socket_Connection", "Still no connection", new Exception("No Connection"));
                }
            }
        }

        public void cancel()
        {
            if(mmSocket.isConnected())
            {
                try {
                    mmSocket.close();
                }
                catch (IOException error)
                {
                    Log.e("Socket_Connection", "Socket could not be closed", error);
                }
            }
        }
    }

    public class handleSocket extends Thread
    {

        private BluetoothSocket mmSocket;
        private InputStream mmInputStream;
        private OutputStream mmOutputStream;
        private Handler mmHandler;
        private byte[] buffer = new byte[2048];
        private static final String Bluetooth_handler = "Bluetooth_handler";

        public handleSocket(@NonNull BluetoothSocket socket, @NonNull Handler messageHandler)
        {

            mmSocket = socket;
            mmHandler = messageHandler;
            try {

                //if not connected
                if(!socket.isConnected())
                {
                    Log.w(Bluetooth_handler, "There is no connection to socket. Trying to create connection");
                    if(!connect())
                    {
                        Log.e(Bluetooth_handler, "Connecting to device failed");
                        return;
                    }
                }

                mmInputStream = socket.getInputStream();
                mmOutputStream = socket.getOutputStream();

            }
            catch (IOException ioError)
            {
                Log.e(Bluetooth_handler, "Fetching streams from socket has failed", ioError);
            }
        }


        @Override
        public void run()
        {
            mmBluetoothAdapter.cancelDiscovery();
        }

        public boolean connect(){
            try
            {
                mmSocket.connect();

                if(mmSocket.isConnected())
                {
                    Log.i("Socket_Connection", "Bluetooth socket has established connection with server");
                    return true;
                }
                else
                {
                    Log.e("Socket_Connection", "Bluetooth socket could not connect to server", new Exception("Connection not established"));
                    return false;
                }

            }
            catch (Exception error)
            {
                Log.e("Socket_Connection", "Error in connecting to server", error);
                return false;
            }
        }

        public boolean testConnection(){

                if(getInput() == null)
                {
                    return false;
                }
                else
                {
                    return true;
                }
        }

        public void closeConnection()
        {
            try
            {
                if(mmSocket.isConnected())
                {
                    mmSocket.close();
                    Log.i(Bluetooth_handler, "Connection to device closed");
                }
            }
            catch (IOException ioerror)
            {
                Log.e(Bluetooth_handler, "Connection could not be closed", ioerror);
            }
        }

        /**
         * listen for communication from the device
         */
        public void getInputstream(DeviceActivity.MessageTypes inqueryType) {
            try {

                if(mmSocket == null || mmInputStream == null || mmOutputStream == null)
                {
                    Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                    return;
                }

                buffer = new byte[2048];

                int response = mmInputStream.read(buffer);

                String responseStr = new String(buffer, 0, response);

                Log.i(Bluetooth_handler, "Received message: " + responseStr);
                Message readMsg = mmHandler.obtainMessage(DeviceActivity.MessageTypes.valueOf(inqueryType.name()).ordinal(), response, -1, buffer);
                readMsg.sendToTarget();

            }
            catch (IOException e)
            {
                Log.e(Bluetooth_handler, "Couldn't receive msg", e);
            }
        }

        public String getInput() {
            try {
                if (mmSocket == null || mmInputStream == null || mmOutputStream == null) {
                    Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                    return null;
                }

                buffer = new byte[2048];

                int response = mmInputStream.read(buffer);

                String responseStr = new String(buffer, 0, response);

                Log.i(Bluetooth_handler, "Received message: " + responseStr);
                return responseStr;
            }
            catch(IOException e)
            {
                Log.e(Bluetooth_handler, "Couldn't receive msg", e);
                return null;
            }
        }

        public void sendMessage(String message)
        {
            if(mmSocket == null || mmInputStream == null || mmOutputStream == null)
            {
                Log.e(Bluetooth_handler, "Cannot read input. Input, socker or output is empty");
                return;
            }

            try {
                byte[] send = message.getBytes();

                //send the information
                mmOutputStream.write(send);

            } catch (IOException e) {
                Log.e(Bluetooth_handler, "Couldn't send msg", e);
            }
        }

        /**
         * Client - Server picture upload process:
         * 1. Inform server that image is coming
         * 2. Wait for a moment
         * 3. Start sending image
         * 4. Wait for response that upload has been completed
         * @param image
         */
        public void getImage(View view)
        {
            if(mmSocket == null || mmInputStream == null || mmOutputStream == null)
            {
                Log.e(Bluetooth_handler, "Cannot read input. Input, socket or output is empty");
                return;
            }

            try
            {
                Context ctx = mmParent.getApplicationContext();
                Toast.makeText(ctx, "Connecting...",
                        Toast.LENGTH_SHORT).show();

                sendMessage("GetImage," + "200" + "," + "PNG");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                Log.v("asd", "filedir"+ctx.getFilesDir());
                String input = getInput();
                int img_bytes = Integer.parseInt(input);
                sendMessage("OK");
                int recieved_bytes = 0;
                String image_string = "";
                String responseStr = "";
                while (image_string.length() < img_bytes) {
                    try {
                        image_string = image_string + getInput();
                        //Log.v("getimg", image_string);
                    } catch (Exception e) {}
                }

                byte data[]= android.util.Base64.decode(image_string, android.util.Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.v("getimg", "image decoded?");
                MediaStore.Images.Media.insertImage(ctx.getContentResolver(), bmp ,"new image" , "image from server");
                ImageView currentimg;


                currentimg = mmParent.findViewById(R.id.currentImageView);
                currentimg.setImageBitmap(bmp);
                Toast.makeText(ctx, "Image saved to gallery",
                        Toast.LENGTH_LONG).show();

                //byte b[] = outputStream.toByteArray();
                //Log.v("asd", b.toString()+" len:"+b.length);
               // Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);




            } catch (Exception e) {
                Log.e(Bluetooth_handler, "GetImage Error", e);
            }
        }

        public void sendImage(@NonNull Bitmap image)
        {
            if(mmSocket == null || mmInputStream == null || mmOutputStream == null)
            {
                Log.e(Bluetooth_handler, "Cannot read input. Input, socket or output is empty");
                return;
            }

            try
            {
                //convert bitmap to bytearray
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] send = stream.toByteArray();

                Log.i("Sending picture", "Bytes to be sent: " + send.length);

                //inform server that image is being uploaded ("Picture", bytearray length, imagetype)
                sendMessage("Picture," + send.length + "," + "PNG");

                //should get "Ready for picture"
                String responseFromServer = getInput();

                try
                {
                    sleep(2000);
                } catch (Exception e){

                }
                //send the information
                mmOutputStream.write(send);

                Log.i("Sending picture", "Stopped sending...");

                //start listening for response
                getInputstream(DeviceActivity.MessageTypes.ResponseToMessage);

            } catch (IOException e) {
                Log.e(Bluetooth_handler, "Couldn't send msg", e);
            }
        }


        public void cancel()
        {
            mmOutputStream = null;
            mmInputStream = null;


            if(mmSocket.isConnected()){
                closeConnection();
            }

        }
    }

}
