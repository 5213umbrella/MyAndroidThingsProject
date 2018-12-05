package com.example.cz.bluetoothuarttestthingsapp.bluetoothchat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.cz.bluetoothuarttestthingsapp.R;
import com.example.cz.bluetoothuarttestthingsapp.common.logger.Log;

public class ConnectWifiActivity extends Activity {

    //Tag for Log
    private static final String TAG = "ConnectWifiActivity";
    //Newly discovered wifi
    private ArrayAdapter<String> mNewWifiArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window  开启窗口
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.wifi_search_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);    //Some problem !!!!!!!

        // Initialize the button to perform device discovery  开始扫描
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        mNewWifiArrayAdapter = new ArrayAdapter<String>(this, R.layout.wifi_name);  //未配对设备

        // Find and set up the ListView for newly discovered devices  通过ListView展示未配对设备
        ListView wifiListView = (ListView) findViewById(R.id.wifi_list);
        wifiListView.setAdapter(mNewWifiArrayAdapter);
        wifiListView.setOnItemClickListener(mWifiClickListener);  //监听ListView的点击事件，代表选中某个设备

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (true) {

        }
    }

    /**
     * Start wifi discover with
     */
    private void doDiscovery() {
        Log.d(TAG, "doWifiDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);    //扫描的时候显示进度条
        setTitle("正在扫描设备");    //显示正在扫描设备

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

    }


    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mWifiClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

        }
    };



}
