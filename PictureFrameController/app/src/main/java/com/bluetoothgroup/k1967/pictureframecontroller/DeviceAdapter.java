package com.bluetoothgroup.k1967.pictureframecontroller;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @class   DeviceAdapter   Display data in a RecyclerView element
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    /**
     * @member  deviceArrayMap   Map of found devices
     */
    private android.util.ArrayMap<String, BluetoothDevice> deviceArrayMap;
    public DeviceListener mmCallback;


    public Activity activity;
    private MainActivity mParent;


    //---Inteface for when device is selected---
    public interface DeviceListener {
        void OnDeviceSelect(BluetoothDevice selectedDevice, MainActivity.availableActions action);
    }


    /**
     *
     * @param devList
     * @param deviceListener
     * @param act
     */
    public DeviceAdapter(@NonNull BluetoothController controller, @NonNull DeviceListener deviceListener, @NonNull Activity act)
    {
        try
        {
            //get paired and detected devices from bluetooth-controller
            deviceArrayMap = controller.getDetectedDevices();

            //setup callback
            this.mmCallback = deviceListener;

            //setup activities
            activity = act;
            if (act.getClass() == MainActivity.class)
            {
                mParent = (MainActivity) activity;
            }
        }
        catch (Exception error)
        {
            Log.e("DeviceAdapter", "Creating deviceAdapter has failed", error);
        }
    }


    //---Create view holder---
    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return deviceArrayMap.size();
    }


    //---Handle content of recycler view list-element---
    @Override
    public void onBindViewHolder(DeviceAdapter.ViewHolder holder, int position)
    {
        String DeviceAddress = deviceArrayMap.keyAt(position);
        holder.deviceAddress.setText(DeviceAddress);

        int state = deviceArrayMap.valueAt(position).getBondState();

        switch (state){
            case BluetoothDevice.BOND_BONDING:
                holder.deviceStatus.setText("Bonding...");
                holder.deviceImage.setImageResource(R.drawable.bluetooth_searching_black);
                break;
            case BluetoothDevice.BOND_BONDED:
                holder.deviceStatus.setText("Bonded with device!");
                holder.deviceImage.setImageResource(R.drawable.bluetooth_connected_black);
                break;
            case BluetoothDevice.BOND_NONE:
                holder.deviceStatus.setText("Not bonded");
                holder.deviceImage.setImageResource(R.drawable.bluetooth_detected);
                break;
        }


    }


    //---Not implemented---
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView)
    {

    }


    //---Viewholder class---
    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView deviceImage;
        public TextView deviceAddress;
        public TextView deviceStatus;
        public TextView deviceName;

        public ViewHolder(final View itemView) {
            super(itemView);
            deviceImage = (ImageView)itemView.findViewById(R.id.logo);
            deviceName = (TextView)itemView.findViewById(R.id.Name);
            deviceAddress = (TextView)itemView.findViewById(R.id.Address);
            deviceStatus = (TextView)itemView.findViewById(R.id.Status);

            /**
             * On short click update the "selectMac"-textbox
             */
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BluetoothDevice selectedClickDevice = deviceArrayMap.valueAt(getAdapterPosition());
                    Snackbar.make(itemView, "Got click", Snackbar.LENGTH_LONG).show();
                    Log.i("DeviceAdapter", "Got short recyclerview itemclick");
                }

            });

            /**
             * On long click open the msgActivity
             */
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //we got a click
                    Log.w("Got CLICK", "We received a long click from you! At position " + getAdapterPosition());

                    BluetoothDevice selectedDevice = deviceArrayMap.valueAt(getAdapterPosition());

                    boolean continueActions = true;
                    MainActivity.availableActions action = null;


                    switch (selectedDevice.getBondState()){
                        case BluetoothDevice.BOND_BONDED:
                            //open device view
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            continueActions = false;
                            break;
                        case BluetoothDevice.BOND_NONE:
                            action = MainActivity.availableActions.Pair;
                            break;
                    }

                    if(continueActions)
                    {
                        Log.i("DeviceAdapter", "Got long recyclerview itemclick");
                        mmCallback.OnDeviceSelect(selectedDevice, action);
                    }
                    else
                    {
                        Log.w("Bluetooth", "Device is still bonding");
                    }
                    return false;
                }
            });

        }
    }

}
