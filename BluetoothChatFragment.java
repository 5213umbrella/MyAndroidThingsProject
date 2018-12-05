package com.example.cz.bluetoothuarttestthingsapp.bluetoothchat;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cz.bluetoothuarttestthingsapp.bluetoothchat.BluetoothChatService;
import com.example.cz.bluetoothuarttestthingsapp.bluetoothchat.Constants;
import com.example.cz.bluetoothuarttestthingsapp.bluetoothchat.DeviceListActivity;
import com.example.cz.bluetoothuarttestthingsapp.board.UartActivity;
import com.example.cz.bluetoothuarttestthingsapp.common.logger.Log;
import com.example.cz.bluetoothuarttestthingsapp.R;

import static android.widget.Toast.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.example.cz.bluetoothuarttestthingsapp.board.BoardDefaults;
import com.example.cz.bluetoothuarttestthingsapp.datahandle.TcpDataSend;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "蓝牙对话片段";
    private static final String UartTAG = "UART Infomation:";

    // UART相关的实例与变量
    private static final int BAUD_RATE_115200 = 115200;
    private static final int BAUD_RATE_9600 = 9600;
    private int baudRate;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    private static final int CHUNK_SIZE = 512;            //缓冲区大小

    private HandlerThread mInputThread;
    private Handler mInputHandler;
    private UartDevice mLoopbackDevice;

    public String recievedData;                           //UART收到的数据的缓冲字符串
    public String aiQinHexString;                         //爱琴仪器收到的数据的缓冲字符串

    int dataCount = 0;         //记录通过Uart接收到的数据条数

    //Intent request codes  连接请求常量,对应于menu中的选型
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int VERSION_INFO = 4;            //当前APP版本信息

    //Layout Views
    private ListView mConversationView;     //用于展示聊天记录的ListView
    private EditText mOutEditText;
    private Button mSendButton;             //发送按钮
    private Button mqttTestButton;
    private Button finishActivityButton;
    private static final int UPDATE_TITLE = 0;   //更新TextView的标志

    //For TCP Transmmssion
    private final String ServerIP = "172.20.29.75";    //手机热点下，通过ipconfig查看地址     淘宝服务器地址47.105.44.99
    private final int port = 10086;
    public TcpDataSend tcpDataSend;                     //创建使用TCP方式发送数据的对象

    //Name of the connected device 已连接设备的名字
    private String mConnectedDeviceName = null;

    //Array adapter for the conversation thread  用于传达聊天消息的adapter
    private ArrayAdapter<String> mConversationArrayAdapter;

    //String buffer for outgoing messages 用于存储发送出去消息的buffer
    private StringBuffer mOutStringBuffer;

    //Local Bluetooth adapter   蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;

    //Member object for the chat services  Chat Service实例对象
    private BluetoothChatService mChatService = null;

    private int count = 0;
    private MqttManager mqttManager;


/*-----------------------------------------Next is the main code--------------------------------------------*/

    //创建BluetoothChatFragment之后首先执行的方法
    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);                //Set the menu
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器

        // 如果adapter为null，则设备不支持蓝牙
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            makeText(activity, "Your Device Do Not Support Bluetooth...", LENGTH_LONG).show();
            activity.finish();
        }

        Bundle bundle = this.getArguments();
        if(bundle!=null){
            this.baudRate = bundle.getInt("data");
        }
    }

    //Do onStart() after onCreate();
    @Override
    public void onStart() {
        super.onStart();
        //如果蓝牙未开启，询问开启权限，之后执行setupChat()方法。
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        //如果蓝牙可用则直接调用setupChat();
        } else if (mChatService == null) {
            setupChat();
        }
    }

    //onCreateView返回的就是fragment要显示的view。碎片的启动代码
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }


    //onViewCreated在onCreateView执行完后立即执行
    //将控件与实例对象关联
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);

        //Other widget added
        finishActivityButton = (Button)view.findViewById(R.id.finishActivity_Button);
        mqttTestButton = (Button)view.findViewById(R.id.mqttTest);
    }

    //Fragment销毁时执行
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null) {
            mChatService.stop();    //销毁活动时停止ChatService
        }

        //关闭此Fragment时关闭UART资源
        Log.d(UartTAG, "Loopback Destroyed");

        // Terminate the worker thread
        if (mInputThread != null) {
            mInputThread.quitSafely();
        }

        //尝试关闭UART硬件资源
        try {
            closeUart();
        } catch (IOException e) {
            Log.e(UartTAG, "Error closing UART device:", e);
        }
    }


    //当从其他界面再次返回该活动时，判断chatService是否可用，不可用则重新开启chatService
    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was not enabled during onStart(),
        // so we were paused to enable it, onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    /**
     * Open UART, Open TCP, Open Bluetooth Service, Button Registration, Connect the widget with the Instance
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");
        //----------------------------------------Init the conversation----------------------------------------------------

        // Initialize the array adapter for the conversation thread   初始化对话界面的adapter
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);  //将回话消息的Layout传入ArrayAdapter
        mConversationView.setAdapter(mConversationArrayAdapter);   //用Adapter更新UI,Put the ArrayAdapter into the ListView

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);


        //----------------------------------------Register the Buttons----------------------------------------------------

        //通过按钮将EditText的数据发送到移动端
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send the message put in the edit text widget by users
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);   //数据来源！！由EditText控件得到数据
                    String message = textView.getText().toString();
                    sendMessage(message);     //Message is the data to be send by Bluetooth
                }

                if (recievedData != null){
                    sendMessage(recievedData);
                    recievedData = "none";
                }
            }
        });


        //Send Command Button
        finishActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        //----------------------------------------Open USART Function----------------------------------------

        // Create a background looper thread for I/O
        mInputThread = new HandlerThread("InputThread");
        mInputThread.start();
        mInputHandler = new Handler(mInputThread.getLooper());
        // Attempt to access the UART device
        try {
            openUart(BoardDefaults.getUartName(), baudRate);    //Open the UART with a setting baudRate
            // Read any initially buffered data
            mInputHandler.post(mTransferUartRunnable);
        } catch (IOException e) {
            Log.e(TAG, "无法打开串口", e);
        }


        mqttTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Other code
                        Message message = new Message();  //创建Message
                        message.what = 1;  //为字段赋值
                        mqttHandler.sendMessage(message);  //将message对象发送到handler
                    }
                }).start();
            }
        });

        //--------------------------------------Start TCP Connection---------------------------------------


        //tcpDataSend = new TcpDataSend(ServerIP, port);


        //--------------------------------------Open Bluetooth ChatService------------------------------

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);     //此处把mHandler传入BluetoothChatService并与其中的mHandler挂钩

        // Initialize the buffer for outgoing messages   初始化数据发送缓冲区
        mOutStringBuffer = new StringBuffer("");

        //---------------------------------    MQTT Connection    ---------------------------------------
        mqttManager = new MqttManager(getContext());
        mqttManager.connect();
        mqttManager.subscribe("/cz",0);
        mqttManager.publish("/cz","Hello MQTT",false,0);

    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).  让设备保持300S内能被搜索到
     */
    private void ensureDiscoverable() {
        /*以下为原本的程序
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        } */
        Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);    //请求搜索
        discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);     //可被搜索300s
        startActivity(discoverable);
    }


    @SuppressLint("HandlerLeak")
    private Handler mqttHandler = new Handler(){    //全局实例handler
        public void handleMessage(Message msg){  //重写handleMessage方法
            switch (msg.what){
                case 1:
                    mqttManager.publish("/cz", msg.getData().getString("recievedData"),false,0);
                    Toast.makeText(getActivity(), "Send", Toast.LENGTH_LONG).show();
                    break;

                default:
                    break;
            }
        }
    };


    /**
     * Sends a message.  发送数据函数
     * @param message A string of text to send.  message为需要发送的数据
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            makeText(getActivity(), R.string.not_connected, LENGTH_SHORT).show();
            return;
        }

        // Check the data validation
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] dataSendOut = message.getBytes();   //获取发送消息的字节数
            mChatService.write(dataSendOut);           //通过ChatService的write()发送数据

            // Reset out string buffer to zero and clear the edit text field  置零数据发送buffer
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }


    /**
     * The action listener for the EditText widget, to listen for the return key
     * 需要注意的是 setOnEditorActionListener这个方法，并不是在我们点击EditText的时候触发，
     * 也不是在我们对EditText进行编辑时触发，而是在我们编辑完之后点击软键盘上的回车键才会触发
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);         //通过键盘的回车也可以发消息？
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return ;
        }
        //ActionBar位于Activity的顶部，可用来显示activity的标题、Icon、Actions和一些用于交互的View
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return ;
        }
        actionBar.setSubtitle(resId);       //设置ActionBar的副标题
    }

    /**
     * Updates the status on the ActionBar.
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService  此mHandler在BluetoothService中
     * 从蓝牙服务中取回信息的handler   重点！！！！！！！！但实际的数据发送与解析仍在BluetoothService类中
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @SuppressLint("StringFormatInvalid")
        @Override
        //处理Handler中的数据
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();   //不大懂

            //根据what字段的内容来判断
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:     //状态改变时
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:      //Connected
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));     //在副标题显示已连接的设备名称
                            mConversationArrayAdapter.clear();              //重新连接设备之后，清除回话列表的内容
                            break;
                        case BluetoothChatService.STATE_CONNECTING:         //Connecting
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:             //Listening
                        case BluetoothChatService.STATE_NONE:               //Not connect
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:                               //正在写Data时
                    byte[] writeBuf = (byte[]) msg.obj;                     //通过obj字段来传递写的数据
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Send:  " + writeMessage);   //把要写的消息加入adapter以便更新
                    break;
                case Constants.MESSAGE_READ:                                //正在读数据时
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);    //读入的数据存入readMessage

                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);  //将接收到的数据加入Adapter中
                    mConversationArrayAdapter.add("Receive:  " + readMessage);      //将接收到的数据加入Adapter中更新回话列表
                    break;
                case Constants.MESSAGE_DEVICE_NAME:    //设备名称相关

                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);  //获取已连接设备的名称
                    if (null != activity) {
                        makeText(activity, "Connected to " + mConnectedDeviceName, LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        makeText(activity, msg.getData().getString(Constants.TOAST), LENGTH_SHORT).show();
                    }
                    break;
                //Other constants added
                case Constants.SEND_UART_DATA:
                    sendUartDataToDevice();
            }
        }
    };

    /*
    将获取到的UART数据发送到平板端
     */
    public void sendUartDataToDevice(){
        if (recievedData != null){
            sendMessage(recievedData);
            recievedData = "none";
        }
    }


    //对应于右上角的menu目录的选择
    public void onActivityResult(int requestCode, int resultCode, Intent connectIntent) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:                             //返回为安全连接模式
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(connectIntent, true);              //连接设备（安全模式）
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:                           //返回消息为非安全连接模式
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(connectIntent, false);             //连接设备（非安全模式）
                }
                break;
            case REQUEST_ENABLE_BT:                                         //返回消息为使能搜索
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session, Init the bluetooth
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");   //蓝牙不可用
                    makeText(getActivity(), R.string.bt_not_enabled_leaving, LENGTH_SHORT).show();
                    getActivity().finish();
                }

        }
    }


    //创建右上角目录
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);                      //此处的布局文件可以添加更多的目录选项
    }

    /**
     * Establish connection with other device  与其他设备建立连接，重点！！
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)   选择安全模式或者非安全模式
     */
    private void connectDevice(Intent data, boolean secure){
        // Get the device MAC address  获取设备MAC地址
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);   //通过蓝牙适配器使用设备的MAC地址产生一个设备对象
        // Attempt to connect to the device
        mChatService.connect(device, secure);   //通过设备对象进行连接
    }


    //判断Menu的哪个条目被选择了
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            //选择了安全连接模式
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);   //打开DeviceListActivity活动
                return true;            }

            //选择了非安全连接模式
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);  //打开DeviceListActivity活动
                return true;
            }

            //选择了使设备可见
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }

            //选择了版本信息
            case R.id.versionInfo: {
                showDialog();       //显示弹窗
                return true;
            }
        }
        return false;
    }




//-----------------------------       UART       ----------------------------------------

    private Runnable mTransferUartRunnable = new Runnable() {
        @Override
        public void run() {
            transferUartData();
        }
    };

    /**
     * Callback invoked when UART receives new incoming data.
     */
    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            // Queue up a data transfer
            transferUartData();                       //Transfer uart data from device to Mobile by Bluetooth
            //Continue listening for more interrupts
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };



    /**
     * 设置UART参数并进行注册
     * @param name Name of the UART peripheral device to open.
     * @param baudRate Data transfer rate. Should be a standard UART baud, such as 9600, 19200, 38400, 57600, 115200, etc.
     * @throws IOException if an error occurs opening the UART port.
     */
    private void openUart(String name, int baudRate) throws IOException {
        mLoopbackDevice = PeripheralManager.getInstance().openUartDevice(name);
        // Configure the UART
        mLoopbackDevice.setBaudrate(baudRate);
        mLoopbackDevice.setDataSize(DATA_BITS);
        mLoopbackDevice.setParity(UartDevice.PARITY_NONE);     //奇偶校验
        mLoopbackDevice.setStopBits(STOP_BITS);

        mLoopbackDevice.registerUartDeviceCallback(mInputHandler, mCallback);
    }

    /**
     * Close Uart
     */
    private void closeUart() throws IOException {   //在销毁Fragment的时候调用此方法
        if (mLoopbackDevice != null) {
            mLoopbackDevice.unregisterUartDeviceCallback(mCallback);
            try {
                mLoopbackDevice.close();
            } finally {
                mLoopbackDevice = null;
            }
        }
    }

    /**
     * Loop over the contents of the UART RX buffer, transferring each  one back to the TX buffer to create
     * a loopback service. Potentially long-running operation. Call from a worker thread.
     */
    private void transferUartData() {
        // Loop until there is no more data in the RX buffer.
        if (mLoopbackDevice != null) try {
            byte[] buffer = new byte[CHUNK_SIZE];       //Set the buffer size according to the CHUNK_SIZE

            int read;
            while ((read = mLoopbackDevice.read(buffer, buffer.length)) > 0) {

                mLoopbackDevice.write(buffer, read);            //实现向串口写数据

                dataCount++;      //数据发送条数计数器

                recievedData = new String(buffer).trim();       //接收到的数据
                //aiQinHexString = str2HexStr(recievedData);    //转换爱琴专用的十六进制数据信息
                //Log.d("Hex Type:" , aiQinHexString);

                //用于判断接收到的字符串是否是“SKULL”，从而判断是否是名希设备的输出，从而对应发出控制命令
                if (buffer[0] == 'S' &&  buffer[1] == 'K' && buffer[2] == 'U'){
                    sendMingXiCommand(7);
                }

                //tcpDataSend.sendData(recievedData);             //Send data via TCP protocol

                Log.d("UART Receive：", recievedData);

                //Other code
                Message mqttMessage = new Message();  //创建Message
                mqttMessage.what = 1;  //为字段赋值
                Bundle bundle = new Bundle();
                bundle.putString("recievedData", recievedData);
                mqttMessage.setData(bundle);
                mqttHandler.sendMessage(mqttMessage);  //将message对象发送到handler

                //通过message发送字段消息，字段为SEND_UART_DATA
                Message msg = new Message();
                msg.what = Constants.SEND_UART_DATA;
                mHandler.sendMessage(msg);   //交给mHandler进行处理
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to transfer data over UART", e);
        }
    }


    //用于爱琴的字符串转16进制解析，暂时失败
public static String str2HexStr(String str){
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int  bit;
        for (int i = 0; i < bs.length; i++){
            bit = (bs[i] & 0x0f0) >>4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
        }
        return sb.toString().trim();
}


    /**
     * 实现由串口发送一个字符串出去
     * @param data   The data to be sent via the uart Port
     *    如发送命令writeDataToUart("MingXi\r\n");
     */
    public void writeDataToUart(String data){
        try{
            int dataLength = data.length();
            byte [] Data = data.getBytes();
            mLoopbackDevice.write(Data, dataLength);
        }catch (IOException e){
            Log.w(TAG, "Unable to transfer data over UART", e);
        }
    }


    /**
     * 发送重庆名希的控制命令的专用函数
     * @param a
     */
    public void sendMingXiCommand(int a){

        Log.d("Send Command","Start sending MingXi Command.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int a = 0; a < 1000; a++){
                    try {
                        writeDataToUart("mingxi\r\n");
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        //Log.d("发送命令","开始发送");
    }


    /**
     * 关于版本号信息的弹窗
     */
    private void showDialog(){
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("VersionInfo")
                .setMessage("Version Number: 1.0.5")
                .setIcon(R.drawable.dialog)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {    //添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getActivity(), "OK", Toast.LENGTH_SHORT).show();
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {     //添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getActivity(), "Cancel", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Reserved", new DialogInterface.OnClickListener() {   //添加普通按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getActivity(), "......", Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog.show();
    }


}
