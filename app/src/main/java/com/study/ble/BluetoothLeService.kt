package com.study.ble

import android.Manifest
import android.annotation.SuppressLint
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
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.study.ble.utill.SafeAppGattAttribute
import com.study.ble.utill.SafeAppGattAttribute.RX_CHARACTERISTIC
import java.lang.IllegalArgumentException
import java.util.UUID

class BluetoothLeService: Service() {
//    val STATE_DISCONNECTED = 0
//    val STATE_CONNECTING = 1
//    val STATE_CONNECTED = 2

    private var connectionState = STATE_DISCONNECTED
    private var bluetoothGatt: BluetoothGatt? = null
    var deviceAddress: String? = ""

//    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    fun initialize(): Boolean {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e("BLE!@!@", "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    companion object{
        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "EXTRA_DATA"

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
    }


    /**
     * GATT 콜백 선언
     * 활동이 서비스에 연결할 기기와 서비스를 알려준 후 장치에 연결되면 서비스는 장치의 GATT 서버에 접속해야
     * BLE 기기 이 연결에서는 BluetoothGattCallback이(가) 있어야 연결 상태, 서비스 검색, 특성에 대한 알림 특성 알림을 제공
     */
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            var intentAction = ""
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // successfully connected to the GATT Server
                    intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_CONNECTED
                    Log.d("BLE!@!@", "successfully connected to the GATT Server")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                this@BluetoothLeService,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //BLE 기기에서 정보를 쿼리
                            bluetoothGatt?.discoverServices()
                            Log.d("BLE!@!@", "discoverServices_qurey")
                        } else return
                    } else {
                        //버전 11 이하
                        bluetoothGatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // disconnected from the GATT Server
                    intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_DISCONNECTED
                    Log.d("BLE!@!@", "disconnected from the GATT Server")
                }
            }
        }

        //discover services : 서비스 발견시 실행
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.d("BLE!@!@", "onServicesDiscovered_GATT_SUCCESS")
            } else {
                Log.d("BLE!@!@", "onServicesDiscovered_GATT_FAIL: $status")
            }
//            when(status) {
//                BluetoothGatt.GATT_SUCCESS -> {
//                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
//                }
//            }
        }

        // 특성의 결과를 읽어온다.
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.d("BLE!@!@", "onCharacteristicRead")
            if (status == BluetoothGatt.GATT_SUCCESS){  //읽기 성공시
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)   //읽은 데이터를 사용 가능하다고 알린다.
                Log.d("BLE!@!@", "onCharacteristicRead_Success")
            } else {
                Log.d("BLE!@!@", "onCharacteristicRead_Failed")
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

        /**
         * GATT를 사용하는 장치와 연결되었을 때 데이터 수신
         * 만약 characteristic의 UUID가 RX_CHARACTERISTIC 같다면, 해당 특성의 값이 수신 데이터를 나타낸다는 것을 의미
         */
        when (characteristic.uuid) {
            RX_CHARACTERISTIC -> {
                val flag = characteristic.properties
                val format = when (flag and 0x01) {
                    0x01 -> {
                        //0x01 이면 특성값이 : FORMAT_UINT16
                        Log.d("BLE!@!@", "RX_CHARACTERISTIC format UINT16.")
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    }
                    else -> {
                        Log.d("BLE!@!@", "RX_CHARACTERISTIC format UINT8.")
                        BluetoothGattCharacteristic.FORMAT_UINT8
                    }
                }
            }
        }

//        characteristic.let {
//            when (characteristic.uuid) {
//                Constants.RX_CHARACTERISTIC -> {
//                    val data: String = getString(0)
//                    intent.putExtra(EXTRA_DATA, data)
//                }
//                else -> Log.d("BLE!@!@", "broadcastUpdate: ")
//            }
//        }
//        sendBroadcast(intent)
    }

    /**
     * 서버가 GATT 서버에 연결하거나 연결을 끊을 때 새로운 상태의 활동 전달
     */
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        Log.d("BLE!@!@", "broadcastUpdate")
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
        bluetoothAdapter?.let { adapter ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED) {
                        //권한 설정되어있는 경우 로직
                        val device = adapter.getRemoteDevice(address)
                        // connect to the GATT server on the device
                        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                    } else {
                        Log.d("BLE!@!@", "Gatt 서버 연결시 권한 거부")
                    }
                } else {
                    //11 이하 버전
                    val device = adapter.getRemoteDevice(address)
                    // connect to the GATT server on the device
                    bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                }
                Log.d("BLE!@!@", "connect to the GATT server on the device_Success")
                return true
            } catch (e: IllegalArgumentException) {
                Log.d("BLE!@!@", "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.d("BLE!@!@", "BluetoothAdapter not initialized")
            return false
        }

//        bluetoothGatt?.let {
//            if (address.equals(deviceAddress)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    if (ContextCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.BLUETOOTH_CONNECT
//                        ) == PackageManager.PERMISSION_GRANTED
//                    ) {
//                        return if (it.connect()){
//                            connectionState = STATE_CONNECTING
//                            Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
//                            true
//                        } else{
//                            Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
//                            false
//                        }
//                    }
//                } else {
//                    //12 이하
//                    return if (it.connect()){
//                        connectionState = STATE_CONNECTING
//                        Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
//                        true
//                    } else {
//                        Log.d("BLE!@!@", "bluetoothGatt_connect: $connectionState")
//                        false
//                    }
//                }
//            }
//        }
//
//        val device = bluetoothAdapter.getRemoteDevice(address)
//        bluetoothGatt = device?.connectGatt(this, false, bluetoothGattCallback)
//        deviceAddress = address
//        connectionState = STATE_CONNECTING
//        Log.d("BLE!@!@", "bluetoothGatt_connect2: $connectionState")
//        return true
    }


    /**
     * 서비스가 검색되면 서비스는 getServices()(으)로 보고된 데이터를 가져옵니다.
     * BLE 장치에서 제공되는 서비스들을 받아올 수 있도록 해주는 메소드
     */
    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
//        return if (bluetoothGatt == null) {
//            emptyList()
//        } else {
//            bluetoothGatt!!.services
//        }
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

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(txCharacteristic: BluetoothGattCharacteristic, data: String) {
        txCharacteristic.let { characteristic ->
            val len: Byte = 0x01  // 데이터 길이
            val cmd: Byte = 0xA1.toByte()  // 전송할 CMD 값

            // 보낼 데이터를 ByteArray로 생성 (len + cmd)
            val byteArrayValue = byteArrayOf(len, cmd)
            // 데이터를 설정하는 방식 변경
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    byteArrayValue,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            } else {
                bluetoothGatt?.writeCharacteristic(txCharacteristic)
            }

            if (result == BluetoothStatusCodes.SUCCESS){
                // 성공적으로 데이터 전송됨
                Log.d("BLE!@!@", "Data written successfully")
            }
            else {
                Log.d("BLE!@!@", "Failed to write data")
            }
        }
    }
}