package com.example.cz.bluetoothuarttestthingsapp.bluetoothchat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.cz.bluetoothuarttestthingsapp.R;

public class BootActivity extends Activity {


    Button deviceChooseButton_MingXi;
    Button deviceChooseButton_AiQin;
    Button finishActivity;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boot_activity);

        deviceChooseButton_MingXi = (Button)findViewById(R.id.deviceChooseButton_MingXi);
        deviceChooseButton_AiQin = (Button)findViewById(R.id.deviceChooseButton_AiQin);
        finishActivity = (Button)findViewById(R.id.finishActivity_Button);

        intent = new Intent(BootActivity.this, MainActivity.class);

        deviceChooseButton_MingXi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("BaudRate", 115200);
                startActivity(intent);
            }
        });

        deviceChooseButton_AiQin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("BaudRate", 9600);
                startActivity(intent);
            }
        });

        finishActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }





}
