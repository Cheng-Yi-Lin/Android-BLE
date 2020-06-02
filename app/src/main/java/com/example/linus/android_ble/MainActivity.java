package com.example.linus.android_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    public UUID UUID_SERV = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");            // device service
    public UUID UUID_DATA = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");            // device description
    public UUID CLIENT_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //定義手機的UUID

    static final int REQUEST_LOCATION_PERMISSION = 1;                                         // Location permission for bluetooth
    static final int REQUEST_ENABLE_BIT = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt BTGatt;

    ArrayList<String> devices = new ArrayList<String>();
    ArrayAdapter<String> adapter1;
    String devAddr = null;
    String devName = null;

    Button EnBLE, ScanBLE, ConnectBLE, Exit;
    Spinner spinner;

    private Receiver readth = null;

    private boolean mScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // device shoud have BLE function
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Device not support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }


        // 確任位置權限
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }


        EnBLE = (Button)findViewById(R.id.ENbluetooth);
        ScanBLE = (Button)findViewById(R.id.ScanBluetooth);
        ConnectBLE = (Button)findViewById(R.id.ConnectBluetooth);
        Exit = (Button)findViewById(R.id.Exit);
        spinner = (Spinner)findViewById(R.id.DEVList);
        adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, devices);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter1);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id){
                devName = ((String)devices.get(position)).split("\\|")[0];
                devAddr = ((String) devices.get(position)).split("\\|")[1];
                adapterView.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this,
                        "已選擇藍牙裝置："+ adapterView.getSelectedItem().toString(), Toast.LENGTH_LONG).show();
            }
            public void onNothingSelected(AdapterView<?> arg0) {

                Toast.makeText(MainActivity.this," 無選擇任何裝置", Toast.LENGTH_LONG).show();
            }
        });


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        EnBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BIT);
                }
                else{
                    bluetoothAdapter.disable();
                    Toast.makeText(MainActivity.this, "藍牙功能已關閉", Toast.LENGTH_LONG).show();
                }
            }
        });


        ScanBLE.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(mScanning==true){
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
                mScanning = true;
                bluetoothAdapter.startLeScan(leScanCallback);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    }
                },5000);

            }
        });


        ConnectBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(BTGatt != null){
                    disconnect();
                    BTGatt = null;
                }
                else{
                    BluetoothDevice devName = bluetoothAdapter.getRemoteDevice(devAddr);
                    BTGatt = devName.connectGatt(getApplicationContext(), false, GattCallback);
                    connected();
                }
//                try{
//                    connected();
//                }catch (IOException e){
//                    Log.e("test", "Connected failed");
//                }
            }
        });


        Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseThread();

                osDestroy();
            }
        });
    }


    private void disconnect(){
        spinner.setClickable(true);
        if(BTGatt != null){
            BTGatt.disconnect();
            BTGatt = null;
        }
    }


    private void connected(){
        readth = new Receiver();                                                                    // use thread to read data
        readth.start();
        spinner.setClickable(false);
    }

    private class Receiver extends Thread{
    MyHandler handler = new MyHandler();

    public Receiver(){
    }
    @Override
        public void run(){
        super.run();
        /*
                    code
                 */
        }
    }

    public void pauseThread(){
        Log.d("test", "pause Thread");
        Receiver.currentThread().interrupt();
        Receiver.currentThread().equals(null);
    }

    public void startThread(){
        Log.d("test", "stop Thread");
        Receiver.currentThread().currentThread();
    }


    public BluetoothGattCallback GattCallback;
    {
        GattCallback = new BluetoothGattCallback() {

            public void SetupSensorStep(BluetoothGatt gatt){
                BluetoothGattCharacteristic characteristic;
                BluetoothGattDescriptor descriptor;
                characteristic = gatt.getService(UUID_SERV).getCharacteristic(UUID_DATA);     // get device service and charateristic
                gatt.setCharacteristicNotification(characteristic, true);                   // read data when charateristic was changed
//                gatt.readCharacteristic(characteristic);                                                                                                                             // see onCharacteristicRead()
                descriptor = characteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR);         // Enable the data change notification bit
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }

            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic, int status){
                if(status == BluetoothGatt.GATT_SUCCESS){
                    Log.e("test","讀取成功"+characteristic.getValue());
                }
            }

            public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState){
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.e("test","Connected to  GATT Server");
                            Toast.makeText(MainActivity.this, "Connected to GATT Server", Toast.LENGTH_LONG).show();
                            gatt.discoverServices();
                        } else {
                            Log.e("test","Disconnected from GATT Server");
                            Toast.makeText(MainActivity.this, "Disconnected from GATT Server", Toast.LENGTH_LONG).show();
                            gatt.disconnect();
                            gatt.close();
                        }
                    }
                });

            }

            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.e("test","Discover & Config GATT Services");
//                Toast.makeText(MainActivity.this, "Discover & Config GATT Services", Toast.LENGTH_LONG).show();
                SetupSensorStep(gatt);
            }

            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                if(UUID_DATA.equals(characteristic.getUuid())) {
                    byte[] data = characteristic.getValue();                                        // get the value and put them in to [ ]
                    /*
                                        code
                                       */
                }
            }
        };
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String str = bluetoothDevice.getName() + "|" + bluetoothDevice.getAddress();
                            if(devices.indexOf(str) == -1){
                                devices.add(str);
                                adapter1.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_LOCATION_PERMISSION){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){                          // 拒絕給予權限將會無法使用藍牙scan功能，直接退出程式
                Toast.makeText(this, "Please agree to the request ",Toast.LENGTH_SHORT).show();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BIT){
            if(resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this,
                        "Bluetooth ON", Toast.LENGTH_LONG).show();
            }
            else if (resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this,
                        "Deny to turn Bluetooth ON", Toast.LENGTH_LONG).show();
            }
        }
    }


    public void osDestroy(){
        if(BTGatt != null){
            BTGatt.disconnect();
            BTGatt.close();
        }
        super.onDestroy();
    }


    public class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            /*
                        msg.what
                        code
                        */
        }
    }
}
