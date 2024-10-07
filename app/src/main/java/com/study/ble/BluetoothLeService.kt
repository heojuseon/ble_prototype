package com.study.ble

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.study.ble.utill.Constants

class BluetoothLeService: Service() {
    val STATE_DISCONNECTED = 0
    val STATE_CONNECTING = 1
    val STATE_CONNECTED = 2

    var connectionState = STATE_DISCONNECTED
    var bluetoothGatt: BluetoothGatt? = null
    var deviceAddress: String? = ""

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    companion object{
        val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "EXTRA_DATA"
    }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            var intentAction = ""
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(intentAction)
                }
            }
        }

        //discover services : 서비스 발견시 실행
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            when(status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                }
            }
        }

        // 특성의 결과를 읽어온다.
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS){  //읽기 성공시
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)   //읽은 데이터를 사용 가능하다고 알린다.
            }
        }

        //특성의 값이 바뀔때
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        characteristic.let {
            when (characteristic.uuid) {
                Constants.RX_CHARACTERISTIC -> {
                    val data: String = getString(0)
                    intent.putExtra(EXTRA_DATA, data)
                }
                else -> Log.d("BLE!@!@", "broadcastUpdate: ")
            }
        }
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class LocalBinder: Binder() {
       fun getService() : BluetoothLeService {
           return this@BluetoothLeService
       }
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    //블루투스 디바이스 GATT 서버 연결
    fun connect(address: String?): Boolean {
        bluetoothGatt?.let {
            if (address.equals(deviceAddress)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        return if (it.connect()){
                            connectionState = STATE_CONNECTING
                            Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
                            true
                        } else{
                            Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
                            false
                        }
                    }
                } else {
                    //12 이하
                    return if (it.connect()){
                        connectionState = STATE_CONNECTING
                        Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
                        true
                    } else {
                        Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
                        false
                    }
                }
            }
        }

        val device = bluetoothAdapter.getRemoteDevice(address)
        bluetoothGatt = device?.connectGatt(this, false, bluetoothGattCallback)
        deviceAddress = address
        connectionState = STATE_CONNECTING
        Log.d("BLE!@!@", "bluetoothGatt_connect2: $connectionState")
        return true
    }

    //BLE 장치에서 제공되는 서비스들을 받아올 수 있도록 해주는 메소드
    fun getSupportedGattServices(): List<BluetoothGattService> {
        return if (bluetoothGatt == null) {
            emptyList()
        } else {
            bluetoothGatt!!.services
        }
    }

    //알람 수신
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic?, enabled: Boolean) {
        bluetoothGatt?.let { gatt ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.setCharacteristicNotification(characteristic, enabled)
                    val descriptor = characteristic?.getDescriptor(Constants.CLIENT_CHARACTERISTIC_CONFIG)
                    descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    return
                }
            } else {
                gatt.setCharacteristicNotification(characteristic, enabled)
            }
        }
    }

    //Gatt 연결 닫기
    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }
    private fun close() {
        bluetoothGatt?.let { gatt ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.close()
                    bluetoothGatt = null
                } else {
                    Log.d("BLE!@!@", "gatt_close_Error")
                }
            } else {
                gatt.close()
                bluetoothGatt = null
            }
        }
    }
}