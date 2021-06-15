package com.doma.ble_project;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String ID_NAME = "ID_NAME";
    public String lastData = "";
    public Date lastDate;

    private static String ID;
    private TextView mConnectionState;
    //private TextView mDataField,textView1,textView2,textView3,textView4,textView5,textView6;
    private MediaPlayer mp = new MediaPlayer();
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private static BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private static BluetoothGattCharacteristic mWriteCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Button buttonSend, buttonStart;
    private EditText editText;
    private ListView listView;
    private ArrayList<String> arrayList = new ArrayList();
    private ListAdapter listAdapter;

    String DestinationIDdata = "0";
    static String DestinationIDnum = "0";

    private static final String secretKey = "abcd!@#$%^";
    private static String ReSend_buf = "";
    static String[] ReSend_split, Rcv_Buf = new String[100];
    private int MaxPacket = 5;
    private static int Max_Try_Resend = 10;
    private static int[] check_ack = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int Error_Num = 0;
    //private static int[] check_sum = {1,1,1}  ;


    //ryan tfLite --------------------------------------------------

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 2000; //time
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 2000; //time
    private static final float DETECTION_THRESHOLD = 0.50f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 3;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 300;
    private static final String LABEL_FILENAME = "file:///android_asset/label.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/model_5class0517.tflite";

    // UI elements.
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = StartActivity.class.getSimpleName();

    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private final ReentrantLock tfLiteLock = new ReentrantLock();

    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;

    private final Interpreter.Options tfLiteOptions = new Interpreter.Options();
    private MappedByteBuffer tfLiteModel;
    private Interpreter tfLite;

    private Button
            startButton,
            stopButton;

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //ryan tfLite --------------------------------------------------


    //重送機制 Resend mechanism
    static class ResendTimer {

        private final Timer timer = new Timer();
        private final int seconds, packet_num;
        private int i;
        private String[] Tx_buffer; //存資料
        private String Tx_data = "";  //送出的封包
        private int error_num;


        public ResendTimer(int seconds, String data, int packet_num, int error_num) {
            this.seconds = seconds;
            this.Tx_buffer = data.split("-&-");
            this.packet_num = packet_num;
            this.error_num = error_num;
        }

        public void start() {
            Date now = new Date();
            timer.schedule(new RemindTask(), 2000, seconds * 1000);

        }

        class RemindTask extends TimerTask {
            int runtimer = Max_Try_Resend;

            public void run() {
                Log.d("TAG", "Error_Num    " + Error_Num + " check_ack[Error_Num]     " + check_ack[Error_Num]);
                Log.d("TAG", "error_num    " + error_num + " check_ack[error_num]     " + check_ack[error_num]);
                if (runtimer > 0 && check_ack[error_num] == 1) {
                    for (i = 1; i <= packet_num; i++) {
                        Tx_data = 'A' + ID + DestinationIDnum + i + packet_num + error_num + Tx_buffer[i - 1];
                        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic, Tx_data);
                        System.out.println("重送的 Tx_Buf = " + Tx_data);
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Try " + runtimer + " times left!!  ");
                    runtimer--;
                } else {
                    System.out.println("Stop trying Resend!!! ");
                    timer.cancel(); //call System.exit is the same to stop PGM
                    //System.exit(0);   //Stops the AWT thread (and everything else)
                }
            }
        }

        public static void Resend(String data, int packet_num, int error_num) throws Exception {
            System.out.println("開始時間:" + new java.util.Date());
            ResendTimer resendTimer = new ResendTimer(1, data, packet_num, error_num);
            resendTimer.start();
        }
    }


    // Count UTF-8 bytes
    public int CountBytes(char str) {
        String data = String.valueOf(str);
        int bytes = 0;
        try {
            bytes = data.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    //encrypt function
    public static String encrypt(String plainText) {
        String encryption = "";
        /*try {
            plainText =  new String(plainText.getBytes("UTF-8"),"iso-8859-1");
        } catch (Exception e) {
        }*/
        char[] cipher = new char[plainText.length()];
        for (int i = 0, j = 0; i < plainText.length(); i++, j++) {
            if (j == secretKey.length())
                j = 0;
            cipher[i] = (char) (plainText.charAt(i) ^ secretKey.charAt(j));
            encryption += cipher[i];
            //String strCipher= Integer.toHexString(cipher[i]);
            /*if(strCipher.length() == 1){
                encryption+="0"+strCipher;
            }else{
                encryption+=strCipher;
            }*/
        }
        return encryption;
    }

    //decrypt function
    public static String decrypt(String encryption) {
        String decoding = "";
        char[] decryption = new char[encryption.length()];
        for (int i = 0, j = 0; i < encryption.length(); i++, j++) {
            if (j == secretKey.length())
                j = 0;
            decryption[i] = (char) (encryption.charAt(i) ^ secretKey.charAt(j));

            decoding += decryption[i];
           /* char n=(char)(int)Integer.valueOf(encryption.substring(i*2,i*2+2),16);
            decryption[i]=(char)(n^secretKey.charAt(j));*/
        }

        /*try {
            decoding = new String(String.valueOf(decryption).getBytes("iso-8859-1"),"UTF-8");
        } catch (Exception e) {
        }*/
        return decoding;
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d("TAG", "action:    ACTION_GATT_CONNECTED");
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d("TAG", "action:    ACTION_GATT_DISCONNECTED");
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d("TAG", "action:    ACTION_GATT_SERVICES_DISCOVERED");
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                Log.d("TAG", "Collecting data");

            }

        }
    };

    private void clearUI() {

        //mDataField.setText(R.string.no_data);

    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ID = intent.getStringExtra(ID_NAME);
        Log.d("TAG", "ID:    " + ID);
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = findViewById(R.id.connection_state);
        //mDataField = (TextView) findViewById(R.id.data_value);

        editText = findViewById(R.id.editText);

        listView = findViewById(R.id.list_of_messages);           //找到ListView
        listAdapter = new ListAdapter(this, arrayList);  //使用ListAdapter來顯示你輸入的文字
        listView.setAdapter(listAdapter);                       //將ListAdapter設定至ListView裡面 //Data和Listview的介面卡:ListAdapter

        if (ID.equals("0")) {
            mp = MediaPlayer.create(this, R.raw.notifymusic);
        } else {
            mp = MediaPlayer.create(this, R.raw.notifymusic2);
        }

        buttonSend = findViewById(R.id.button_send);
        buttonStart = findViewById(R.id.button_start);
        onClick();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class); //創建畫面的同時，也建立 BluetoothLeService 類別 (衍生自 service 類別)，這裡利用一個技巧，
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);                 //讓服務在背景執行，然後將畫面與服務 bind 在一起。


        //ryan tfLite --------------------------------------------------

        // Load the labels for the model, but only display those that don't start
        // with an underscore.
        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        Log.i(LOG_TAG, "Reading labels from: " + actualLabelFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                Log.d("label", line);

                labels.add(line);
                if (line.charAt(0) != '_') {

                    displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        // Set up an object to smooth recognition results to increase accuracy.
        recognizeCommands =
                new RecognizeCommands(
                        labels,
                        AVERAGE_WINDOW_DURATION_MS,
                        DETECTION_THRESHOLD,
                        SUPPRESSION_MS,
                        MINIMUM_COUNT,
                        MINIMUM_TIME_BETWEEN_SAMPLES_MS);

        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLiteModel = loadModelFile(getAssets(), actualModelFilename);
            Log.d("LOG_TAG", "load model");
            recreateInterpreter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Start the recording and recognition threads.
        requestMicrophonePermission();

        startButton = findViewById(R.id.button_start_record);
        stopButton = findViewById(R.id.button_stop_record);

        stopButton.setEnabled(false);
        startButton.setEnabled(true);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButton.setEnabled(true);
                startButton.setEnabled(false);
                startRecording();
                startRecognition();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                stopRecording();
                stopRecognition();
            }
        });

        //ryan tfLite --------------------------------------------------

    }


    public void enableNotify() {
        //final BluetoothGattCharacteristic characteristic_TX ;
        final BluetoothGattCharacteristic characteristic;
        if (mGattCharacteristics.size() == 3) {
            characteristic = mGattCharacteristics.get(2).get(1); //RX  //correct get(2).get(0)
            final int charaProp = characteristic.getProperties();
            Log.d("TAG", "charaProp:" + charaProp);
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {  //16
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                mBluetoothLeService.setMTU(216);
                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();

            }
//            //TX characteristic 12 //RX characteristic 16
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {  //RX characteristic
//                mNotifyCharacteristic = characteristic;
//                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
//                mBluetoothLeService.setMTU(216);
//                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
//
//            }


//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                // If there is an active notification on a characteristic, clear
//                // it first so it doesn't update the data field on the user interface.
//                if (mNotifyCharacteristic != null) {
//                    mBluetoothLeService.setCharacteristicNotification(
//                            mNotifyCharacteristic, false);
//                    mNotifyCharacteristic = null;
//                }
//                mBluetoothLeService.readCharacteristic(characteristic);
//            }
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                mNotifyCharacteristic = characteristic;
//                mBluetoothLeService.setCharacteristicNotification(
//                        characteristic, true);
//                Toast.makeText(getApplicationContext(), "NNN", Toast.LENGTH_SHORT).show();
//            }

        }
    }


    //send button onclick function
    public void onClick() {

        buttonStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                enableNotify();

                editText.setVisibility(View.VISIBLE);
                buttonSend.setVisibility(View.VISIBLE);
            }
        });

        buttonSend.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {

                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(2).get(0); //correct get(2).get(0)->RX characteristic->phone send data to nordic //origin .get(2).get(1) -> charaProp ==16
                Log.d("TAG", "characteristic=        " + characteristic);
                final int charaProp = characteristic.getProperties();
                Log.d("TAG", "charaProp :   " + charaProp);
                String TX_buf;
                //origin set charaProp == 12   //correct 12
                if (charaProp == 12) {
                    int countbyte = 0, i, packetNum;
                    mWriteCharacteristic = characteristic;
                    Log.d("TAG", "mWriteCharacteristic:      " + mWriteCharacteristic);
                    if (editText.getText().toString().length() != 0) {

                        lastData = "";

                        try {
                            countbyte = editText.getText().toString().getBytes("UTF-8").length;
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        Log.d("TAG", "Data bytes (UTF-8) :" + countbyte + " bytes");


                        for (i = 0; i < editText.getText().toString().length(); i++) {
                            final char lastDatabuf = editText.getText().toString().charAt(i);
                            lastData = lastData + String.valueOf(lastDatabuf);
                        }
                        //Log.d("TAG","Data length :"+TX_buf.length());
                        Log.d("TAG", "lastData buffer:" + lastData);
                        //Log.d("TAG","Rcv_Buf :" + Rcv_Buf[0] );

                        ReSend_buf = "";
                        for (i = 0, countbyte = 0, packetNum = 1; i < editText.getText().toString().length() && packetNum <= MaxPacket; i++) {
                            countbyte += CountBytes(editText.getText().toString().charAt(i));
                            if (countbyte > 14) {
                                if (packetNum + 1 > MaxPacket)
                                    break;
                                ReSend_buf += "-&-";
                                countbyte = CountBytes(editText.getText().toString().charAt(i));
                                packetNum += 1;
                            }
                            ReSend_buf += editText.getText().toString().charAt(i);
                        }
                        ReSend_split = ReSend_buf.split("-&-");
                        Log.d("TAG", "Total Packets = " + packetNum);
                        Log.d("TAG", "ReSend_buf = " + ReSend_buf);


                        for (i = 1; i <= packetNum; i++) {
                            TX_buf = 'A' + ID + DestinationIDnum + i + packetNum + Error_Num + ReSend_split[i - 1];
                            //mBluetoothLeService.writeCharacteristic(mWriteCharacteristic,encrypt(TX_buf)); //origin : (mWriteCharacteristic,TX_buf);
                            mBluetoothLeService.writeCharacteristic(mWriteCharacteristic, TX_buf);
                            Log.d("TAG", "Tx_Buf = " + TX_buf);
                            //Log.d("TAG","encrypt Tx_Buf = "+ encrypt(TX_buf));
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }



                        /*TX_buf='A'+ID+DestinationIDnum+i+packetNum+'0'+ReSend_buf;
                        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic,TX_buf);*/

                        //Log.d("TAG","Encrypt  buffer :  " + encrypt(lastData));
                        //Log.d("TAG","Encrypt length :"+encrypt(TX_buf).length());
                        //Log.d("TAG","Decrypt  buffer :  " + decrypt(encrypt(lastData)));


                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        Date date = new Date(System.currentTimeMillis());
                        arrayList.add("me&" + simpleDateFormat.format(date) + "&" + editText.getText().toString() + "&" + ID + "&" + DestinationIDnum + "&" + "noRead");
                        listView.setAdapter(listAdapter);

                        //檢查有沒有收到ACK  check the ACK is received or not, 1 = no ack
                        check_ack[Error_Num] = 1;
                        lastDate = date;
                        Log.d("TAG", "check_ack[Error_Num] == 1");
                        if (check_ack[Error_Num] == 1) {
                            try {                                                       //try 區塊用來監控預先認定會出現例外的程式碼
                                ResendTimer.Resend(ReSend_buf, packetNum, Error_Num);
                            } catch (Exception e) {
                                e.printStackTrace();                                    //catch 區塊則是用來放置當例外真的在 try 區塊出現時，所設計並處理例外的程式碼，處理完後直接處理catch{}後的code
                            }
                        }
                        Log.d("TAG", "Error_Num = " + Error_Num);
                        if (Error_Num < 9) {
                            Error_Num += 1;
                        } else {
                            Error_Num = 0;
                        }

                        editText.setText(null);
                    } else {
                        Toast.makeText(getApplicationContext(), "No data input", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    //show the data received
    private void displayData(String data) {
        final char fromID;
        final char myID;
        final char packet_num;
        final char packet_total;
        final char errorNUM;
        int i;

        String packetData, Rcv_Data = "";  //origin
        String ACK_buf;  //origin
//        data = decrypt(data) ;
//        Log.d("TAG","Decrypt DATA :        " + data);
        Log.d("TAG", "displayData:  " + data);
        Log.d("TAG", "ID:  " + ID);
        Log.d("TAG", "data.charAt(0):  " + data.charAt(0));
        Log.d("TAG", "data.charAt(1):  " + data.charAt(1));
        Log.d("TAG", "data.charAt(2):  " + data.charAt(2));
        Log.d("TAG", "data.charAt(3):  " + data.charAt(3));
        Log.d("TAG", "data.length():  " + data.length());
        if (data != null) {
            if (data.length() == 5 && data.charAt(0) == ID.charAt(0) && data.charAt(1) == 'A' && data.charAt(2) == 'C' && data.charAt(3) == 'K') {
                check_ack[Error_Num - 1] = data.charAt(1); //暫時debug一直重送的問題 //應該要在送完後立刻讀取ack 不要等到Error_Num +1 了才讀取
                Log.d("TAG", "data!=null ; check_ack[Error_Num-1](ACK) :  " + check_ack[Error_Num - 1]);
            }

            //   if(data.length()>6)
            //Log.d("TAG","data[0]:  "+data.charAt(0));
            if (data.charAt(0) == 'A') {

                fromID = data.charAt(1);
                myID = data.charAt(2);
                packet_num = data.charAt(3);
                packet_total = data.charAt(4);
                errorNUM = data.charAt(5);
                Log.d("TAG", "fromID :" + fromID);
                Log.d("TAG", "myID :" + myID);
                Log.d("TAG", "packet_num :" + packet_num);
                Log.d("TAG", "packet_total :" + packet_total);
                Log.d("TAG", "errorNUM :" + errorNUM);

                if (myID == ID.charAt(0)) {


                    packetData = "";

                    Log.d("TAG", "ID packetData.L :        " + packetData.length());
                    Log.d("TAG", "PacketdataLength:   " + data.length());

                    for (i = 0; i < data.length() - 6; i++) {
                        final char packetdatabuf = data.charAt(6 + i);
                        packetData = packetData + String.valueOf(packetdatabuf);
                        //Log.d("TAG","packetdatabuf:"+packetdatabuf);
                    }

                    Log.d("TAG", "packetData:" + packetData);
                    Log.d("TAG", "PacketdataLength:   " + packetData.length());


                    packetData = packetData.substring(0, packetData.length() - 1);

                    if (packetData.equals("ACK")) {

                        Log.d("TAG", "ACK CHECK");

                        //lastData=arrayList.get(arrayList.size()-1);             //origin //

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        //Date lastDate = new Date(System.currentTimeMillis());  //origin //


                        //arrayList.add(arrayList.size()-1,lastData + "&" + "read");
                        //arrayList.remove(arrayList.size()-1);  //origin has
                        arrayList.add("me&" + simpleDateFormat.format(lastDate) + "&" + lastData + "&" + ID + "&" + fromID + "&" + "Read");

                        listView.setAdapter(listAdapter);
                        lastData = "";

                    } else if (packet_num < packet_total) {
                        Log.d("TAG", "packet_num < packet_total");
                        //收到的訊息暫時存在Buf
                        Rcv_Buf[packet_num - '0'] = packetData;

                    } else {

                        if (packet_num == packet_total) {
                            Log.d("TAG", "packet_num == packet_total");
                            Rcv_Buf[packet_num - '0'] = packetData;              // confirm it is in correct position(int)
                            Log.d("TAG", "String Rcv_Data: " + Rcv_Buf[packet_num - '0']);
                        }

                        //把存在Buf的訊息寫進String Rcv_Data
                        Log.d("TAG", "把存在Buf的訊息寫進String Rcv_Data " + packet_total);

                        //packet_total is char need to conver to int so it needs to -'0'
                        for (i = 0; i < packet_total - '0'; i++) {
                            Rcv_Data += Rcv_Buf[i + 1];                           //first is Rcv_Buf[1] not Rcv_Buf[0] according to the above code
                            Log.d("TAG", "Rcv_Buf[i]: " + Rcv_Buf[i + 1] + String.valueOf(i) + packet_total);
                        }
                        Log.d("TAG", "String Rcv_Data: " + Rcv_Data);
                        Log.d("TAG", "arrayList.add date...");
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

                        Date date = new Date(System.currentTimeMillis());

                        arrayList.add("another&" + simpleDateFormat.format(date) + "&" + Rcv_Data + "&" + ID + "&" + fromID + "&" + "noRead");
                        Log.d("TAG", "arrayList  " + arrayList);
                        listView.setAdapter(listAdapter);

                        mp.start();
                        //send ACK
                        Log.d("TAG", "send ACK");
                        ACK_buf = 'A' + ID + fromID + packet_num + packet_total + errorNUM + "ACK";
                        Log.d("TAG", "ACK_buf: " + ACK_buf);
                        //mBluetoothLeService.writeCharacteristic(mWriteCharacteristic, encrypt(ACK_buf));   //origin has
                        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic, ACK_buf);     //new add 3/9
                        Log.d("TAG", "ACK_buf: ");
                    }

                }
            }

        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        Log.d("TAG", "displayGattServices    mGattCharacteristics" + mGattCharacteristics);

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, MyGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, MyGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        //mGattServicesList.setAdapter(gattServiceAdapter);

    }

    //雖然已經將服務與畫面連結起來了，不過如果藍芽服務有任何訊息要通知 UI 畫面，該怎麼辦？
    //此時，畫面需要攔截藍芽服務的訊息，並顯示在 UI 畫面上。因此，我們必須向系統註冊一個 callback 函式用來處理服務的訊息，
    //底下程式碼說明了先設定好欲攔截的訊息，然後將 callback 函式 (mGattUpdateReceiver) 向系統註冊：
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);           //connected to a GATT server.連接一個GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED); //discovered GATT services.查找GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);           //received data from the device.  This can be a result of read or notification operations.從服務中接受數據
        return intentFilter;
    }


    public void buttonConnectOnClick(View view) {

        mBluetoothLeService.connect(mDeviceAddress);
        Log.d("TAG", "mDeviceAddress  :   " + mDeviceAddress);
    }

    public void buttonDisconnectOnClick(View view) {
        mBluetoothLeService.disconnect();
    }


    public void buttonIDDestinationOnClick(View view) {

        final View item = LayoutInflater.from(DeviceControlActivity.this).inflate(R.layout.alertdialog_layout, null);
        new AlertDialog.Builder(DeviceControlActivity.this)
                .setTitle("請輸入Destination ID")
                .setView(item)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = item.findViewById(R.id.edit_text);
                        if (editText.getText().toString().length() != 0) {
                            DestinationIDnum = editText.getText().toString();
                            Log.d("TAG", "DestinationIDnum  :   " + DestinationIDnum);
                            if (DestinationIDnum.equals("1") || DestinationIDnum.equals("2") || DestinationIDnum.equals("3") || DestinationIDnum.equals("4") || DestinationIDnum.equals("5")
                                    || DestinationIDnum.equals("6") || DestinationIDnum.equals("7") || DestinationIDnum.equals("8") || DestinationIDnum.equals("9") || DestinationIDnum.equals("0")) {
                                DestinationIDdata = editText.getText().toString();
                                Log.d("TAG", "DestinationIDdata:" + DestinationIDdata);
                            } else {
                                Toast.makeText(getApplicationContext(), "Destination ID is between 1-9 :", Toast.LENGTH_SHORT).show();
                            }


                        }
                        if (TextUtils.isEmpty(DestinationIDdata)) {
                            Toast.makeText(getApplicationContext(), "NO Destination ID input  \n ID :" + DestinationIDdata, Toast.LENGTH_SHORT).show();
                        } else {

                            // Toast.makeText(getApplicationContext(), "ID :" + data, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    //ryan tfLite --------------------------------------------------

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[] {android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Log.d("Permissions","REQUEST_RECORD_AUDIO");
            //startRecording();
            //startRecognition();
        }
    }

    public synchronized void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    public synchronized void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }

    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {

            //Log.d("record","recording");

            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    public synchronized void startRecognition() {
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }

    public synchronized void stopRecognition() {
        if (recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }

    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[1][RECORDING_LENGTH];
        final float[][] outputScores = new float[1][labels.size()];



        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            long startTime = new Date().getTime();
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[0][i] = inputBuffer[i] / 32767.0f;
            }

            Object[] inputArray = {floatInputBuffer};


            Map<Integer, Object> outputMap = new HashMap<>();
            outputMap.put(0, outputScores);

            // Run the model.
            tfLiteLock.lock();
            try {

                tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

            } finally {
                tfLiteLock.unlock();
            }

            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result =
                    recognizeCommands.processLatestResults(outputScores[0], currentTime);
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {

                            String showtext = null;

                            // If we do have a new command, highlight the right list entry.
                            if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
                                int labelIndex = -1;
                                for (int i = 0; i < labels.size(); ++i) {
                                    if (labels.get(i).equals(result.foundCommand)) {
                                        labelIndex = i;
                                    }
                                }

                                for(int i = 0;i < labels.size();i++)
                                {
                                    Log.d(String.valueOf(i), "scores: " + String.valueOf(outputScores[0][i]));
                                }
                                Log.d("labelIndex",String.valueOf(labelIndex));

                                //new label
                                switch (labelIndex - 2) {
                                    case 0:
                                        showtext = "applause";
                                        break;
                                    case 1:
                                        showtext = "cat";
                                        break;
                                    case 2:
                                        showtext = "dog_barking";
                                        break;
                                    case 3:
                                        showtext = "hammer";
                                        break;
                                    case 4:
                                        showtext = "laughter";
                                        break;
                                }


                                if(showtext != null){
                                    Toast.makeText(DeviceControlActivity.this, showtext, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        Log.v(LOG_TAG, "End recognition");
    }

    private void recreateInterpreter() {
        tfLiteLock.lock();
        try {
            if (tfLite != null) {
                tfLite.close();
                tfLite = null;
            }
            //tfLite = new Interpreter(tfLiteModel, tfLiteOptions);
            tfLite = new Interpreter(tfLiteModel, tfLiteOptions);

            tfLite.resizeInput(0, new int[] {RECORDING_LENGTH, 1});
            //tfLite.resizeInput(1, new int[] {1});
        } finally {
            tfLiteLock.unlock();
        }
    }


    //ryan tfLite --------------------------------------------------
}