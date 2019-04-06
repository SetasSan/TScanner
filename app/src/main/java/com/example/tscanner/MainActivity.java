package com.example.tscanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.Manifest;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity{

    private TextView mTextMessage;
    private WifiP2pManager manager;
    public static final String TAG = "MainActivity";
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver broadcastReceiver;
    IntentFilter mIntentFilter;
    private Handler mHandler;
    private int mInterval = 5000; // 5 seconds by default, can be changed later

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private int _selectedWindow = R.id.navigation_home;
    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @TargetApi(Build.VERSION_CODES.O)
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            _selectedWindow = item.getItemId();
            switch (item.getItemId()) {
                case R.id.navigation_home:

                    mTextMessage.setText("Loading: wifi");

                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText("Loading: bluetooth");
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText("Loading: "+R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.editText);
        mTextMessage.setMovementMethod(new ScrollingMovementMethod());
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        }

        StartWifiDiscovery();
        mBluetoothAdapter.startDiscovery();
        broadcastReceiver.ScanP2pWIFI();
        mHandler = new Handler();
        mStatusChecker.run();
    }

    private void StartWifiDiscovery(){
        manager=(WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel=manager.initialize(this,getMainLooper(),null);


        Log.d(TAG, "onCreate: manager "+manager);

        broadcastReceiver = new WiFiDirectBroadcastReceiver(manager, channel);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mIntentFilter.addAction(BluetoothDevice.EXTRA_DEVICE);
        mIntentFilter.addAction(BluetoothDevice.EXTRA_CLASS);


        registerReceiver(broadcastReceiver, mIntentFilter);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void RenderWifiStatus(){
        List<DateObjectContainer<ScanResult>> wifiList = broadcastReceiver.wifiList;
        StringBuilder sb = new StringBuilder();
        if(wifiList == null){
            return;
        }
        for (int i=0; i<wifiList.size(); i++){
            ScanResult scanresult = wifiList.get(i).Device;
            sb.append("RSSI: "+scanresult.level+"\n");
            sb.append("SSID: "+scanresult.SSID+"\n");
            sb.append("BSSID: "+scanresult.BSSID+"\n");
            sb.append("Frequency: "+scanresult.frequency+"\n");
            sb.append("Capability: "+scanresult.capabilities+"\n");
            sb.append("friendly name: "+ scanresult.operatorFriendlyName+"\n");
            sb.append("isPasspointNetwork: "+scanresult.isPasspointNetwork()+"\n");
            sb.append("is80211mcResponder: "+scanresult.is80211mcResponder()+"\n");
            sb.append("Found at: "+ wifiList.get(i).Time.toString() +"\n");
            sb.append("------------------------------\n");
        }
        if(!mTextMessage.getText().equals(sb) && !sb.equals("")){
            mTextMessage.setText(sb);
        }
    }

    private void RenderBluetoothDevices(){
        List<DateObjectContainer<BluetoothDevice>> devices = broadcastReceiver.bluetoothDevices;
        StringBuilder sb = new StringBuilder();
        if(devices == null) return;
        for (int i=0; i<devices.size(); i++){
            BluetoothDevice scanresult = devices.get(i).Device;
            sb.append("Name: "+scanresult.getName()+"\n");
            sb.append("SSID: "+scanresult.getAddress()+"\n");
            sb.append("Found at: "+ devices.get(i).Time.toString() +"\n");
            sb.append("------------------------------\n");
        }
        if(!mTextMessage.getText().equals(sb) && !sb.equals("")){
            mTextMessage.setText(sb);
        }
    }

    private void RenderP2pWifiDevices(){
        List<DateObjectContainer<WifiP2pDevice>> devices = broadcastReceiver.p2pWifiDevices;
        StringBuilder sb = new StringBuilder();
        if(devices == null) return;
        for (int i=0; i<devices.size(); i++){
            WifiP2pDevice scanresult = devices.get(i).Device;
            sb.append("Name: "+scanresult.deviceName+"\n");
            sb.append("SSID: "+scanresult.deviceAddress+"\n");
            sb.append("primaryDeviceType: "+scanresult.primaryDeviceType+"\n");
            sb.append("secondaryDeviceType: "+scanresult.secondaryDeviceType+"\n");
            sb.append("status: "+scanresult.status+"\n");
            sb.append("Found at: "+ devices.get(i).Time.toString() +"\n");
            sb.append("------------------------------\n");
        }
        if(!mTextMessage.getText().equals(sb) && !sb.equals("")){
            mTextMessage.setText(sb);
        }
    }

    Runnable mStatusChecker = new Runnable() {
        @TargetApi(Build.VERSION_CODES.O)
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void run() {
            try {
                switch(_selectedWindow){
                    case R.id.navigation_home:
                        RenderWifiStatus();
                        return;
                    case R.id.navigation_dashboard:
                        RenderBluetoothDevices();
                        return;
                    case R.id.navigation_notifications:
                        RenderP2pWifiDevices();
                        return;
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };
}
