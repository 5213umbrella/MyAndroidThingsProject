package com.example.cz.bluetoothuarttestthingsapp.bluetoothchat;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.WebMessagePort;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager {

    public static final String TAG = "MQTTManager";

    private String host = "tcp://120.79.151.208:1883";
    private String userName = "admin";
    private String passWord = "public";
    private String clientId = "";

    //Set the connections
    private static MqttManager mqttManager = null;
    private MqttClient client;
    private MqttConnectOptions connectOptions;

    //构造方法
    public MqttManager(Context context){
        clientId = MqttClient.generateClientId();
    }

    public MqttManager getInstance(Context context){
        if(mqttManager == null){
            mqttManager = new MqttManager(context);
        }else{
            return mqttManager;
        }
        return null;
    }

    //Connect to the server
    public void connect(){
        try{
            client = new MqttClient(host, clientId, new MemoryPersistence());
            connectOptions = new MqttConnectOptions();
            connectOptions.setUserName(userName);
            connectOptions.setPassword(passWord.toCharArray());
            client.setCallback(mqttCallback);
            client.connect(connectOptions);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    //Subscribe topic in server
    public void subscribe(String topic, int qos){
        if(client != null){
            int[] Qos = {qos};
            String[] topic1 = {topic};
            try {
                client.subscribe(topic1, Qos);
                Log.d(TAG,"订阅topic : "+ topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    //Publish messages to server
    public void publish(String topic, String msg, boolean isRetained, int qos) {
        try {
            if (client != null) {
                MqttMessage message = new MqttMessage();
                message.setQos(qos);
                message.setRetained(isRetained);
                message.setPayload(msg.getBytes());
                client.publish(topic, message);
            }
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    //Callback function about the connection
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.i(TAG,"Connection lost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message){
            Log.i(TAG,"Received topic : " + topic);

            String payload = new String(message.getPayload());
            Log.i(TAG,"Received msg : " + payload);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.i(TAG,"deliveryComplete");
        }
    };

}
