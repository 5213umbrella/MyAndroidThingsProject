package com.example.cz.bluetoothuarttestthingsapp.bluetoothchat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.example.cz.bluetoothuarttestthingsapp.common.activities.SampleActivityBase;
import com.example.cz.bluetoothuarttestthingsapp.R;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "主活动";

    // Whether the Log Fragment is currently shown  log碎片是否可见
    //private boolean mLogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);    //显示主Activity  有两种布局，根据屏幕宽度决定

        bluetoothPermissions();    //询问地理位置权限

        //显示出屏幕右边的碎片
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();    //
            BluetoothChatFragment fragment = new BluetoothChatFragment();

            Intent intentFromBootActivity = getIntent();
            int baudRate = intentFromBootActivity.getIntExtra("BaudRate", 115200);   //Here 115200 is default value
            Bundle bundle = new Bundle();
            bundle.putInt("data", baudRate);
            fragment.setArguments(bundle);//数据传递到fragment中

            transaction.replace(R.id.sample_content_fragment, fragment);    //Replace the FrameLayout to fragment
            transaction.commit();      //此处开始调用BluetoothChatFragment里面的内容，执行其onCreate()
        }
    }

    // 定义获取基于地理位置的动态权限
    private void bluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //用户允许位置权限,允许之后才能开启活动
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
        else {
            Toast.makeText(MainActivity.this, "You Deny The Loaction Authority", Toast.LENGTH_LONG).show();
        }
    }


}


