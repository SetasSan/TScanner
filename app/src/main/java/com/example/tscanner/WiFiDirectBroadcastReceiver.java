package com.example.tscanner;


import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.util.Calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;


    public List<DateObjectContainer<ScanResult>> wifiList = new ArrayList<>();

    public List<DateObjectContainer<BluetoothDevice>> bluetoothDevices = new ArrayList<>();
    public List<DateObjectContainer<WifiP2pDevice>> p2pWifiDevices = new ArrayList<>();

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
    }

    public void ScanP2pWIFI(){
        this.mManager.requestPeers(this.mChannel, new WifiP2pManager.PeerListListener() {
            @TargetApi(Build.VERSION_CODES.O)
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                Log.e("P2P-","Available");
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList())
                {
                    if(!CheckIfContains(device.deviceAddress, p2pWifiDevices)){
                        DateObjectContainer<WifiP2pDevice> element = new DateObjectContainer<>();
                        element.Device = device;
                        element.MAC = device.deviceAddress;
                        element.Time = Calendar.getInstance().getTime();
                        p2pWifiDevices.add(element);
                    }

                }
            }
        });
    }

      @TargetApi(Build.VERSION_CODES.O)
      @RequiresApi(api = Build.VERSION_CODES.M)
      @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //editText.setText("Channel Received");
            }

            @Override
            public void onFailure(int reasonCode) {
                //editText.setText("Channel Not Received: "+reasonCode);
            }
        });



        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                //mActivity.setIsWifiP2pEnabled(true);
                Log.e("WifiDIRECT-","Enabled");
            } else {
                //mActivity.setIsWifiP2pEnabled(false);
                Log.e("WifiDIRECT-","Disabled");
                // mActivity.resetData();

            }
            Log.d(MainActivity.TAG, "P2P state changed - " + state);

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (mManager != null) {
                Log.e("Peer Changed:", "Detected Peer");
                WifiManager wManager;
                wManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

                for (ScanResult dev:wManager.getScanResults()) {
                    DateObjectContainer<ScanResult> element = new DateObjectContainer<>();
                    element.Device = dev;
                    element.MAC = dev.BSSID;
                    element.Time = Calendar.getInstance().getTime();
                    if(!CheckIfContains(dev.BSSID, wifiList)) {
                        wifiList.add(element);
                    }
                }
            }
            // Call WifiP2pManager.requestPeers() to get a list of current peers

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (mManager == null) {
                return;
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//
//
//
//            Log.e("P2P found:"+device.deviceAddress, "Detected Peer");
        } else if(BluetoothDevice.ACTION_FOUND.equals(action)){
            Log.e("Bluetooth peer found:", "Detected Peer");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(!CheckIfContains(device.getAddress(), bluetoothDevices)){
                DateObjectContainer<BluetoothDevice> element = new DateObjectContainer<>();
                element.Device = device;
                element.MAC = device.getAddress();
                element.Time = Calendar.getInstance().getTime();
                bluetoothDevices.add(element);
            }
        }else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION))
        {

        }

    }

    public <T> boolean CheckIfContains(String Mac, List<DateObjectContainer<T>> list){
        boolean found = false;
        for (DateObjectContainer<T> var: list) {
            if(var.MAC.equals(Mac)){
                return true;
            }
        }
        return found;
    }

}
