package com.study.ble

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.study.ble.DeviceControlActivity.Companion.EXTRAS_DEVICE_ADDRESS
import com.study.ble.DeviceControlActivity.Companion.EXTRAS_DEVICE_NAME
import com.study.ble.utill.SafeAppGattAttribute
import java.util.UUID

class DeviceControlActivity : AppCompatActivity() {
    private var bluetoothLeService: BluetoothLeService? = null

    private var deviceAddress: String? = ""

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    companion object{
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }

    //서비스가 연결되어있을 경우 안되어있을경우
    private val serviceConnection: ServiceConnection = object: ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
//            bluetoothLeService?.connect(deviceAddress)  //BLE 기기에 연결
            bluetoothLeService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e("BLE!@!@", "Unable to initialize Bluetooth")
                    finish()
                }
                else {
                    bluetooth.connect(deviceAddress)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
        }

    }


    /**
     * BroadcastReceiver는 BluetoothLeService 로 부터 연결 상태와 데이터들을 받아오는 역할
     * 등록 후에 BluetoothService 에 정의되어 있는 connect 함수를 호출해 장치와 연결
     * 여기서 connect 함수는 BluetoothLeService의 ACTION_GATT_CONNECTED... 등등 변수 선언
     */
    var connected: Boolean = false
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("BLE!@!@", "gattUpdateReceiver: ${intent?.action.toString()}")
            when (intent?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {   //연결 성공
                    connected = true
                    Log.d("BLE!@!@", "BLE : Connected to device")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {    //연결 실패
                    connected = false
                    Log.d("BLE!@!@", "BLE : Disconnected to device")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> { //gatt service 발견
                    // Show all the supported services and characteristics on the user interface.
                    // BLE 제공되는 서비스(and 특성)들 가져오는 함수
                    displayGattServices(bluetoothLeService?.getSupportedGattServices())
                    Log.d("BLE!@!@", "BLE : GATT_SERVICES_DISCOVERED")
                }
            }
        }
    }

    /**
     * BLE 특성읽기
     * BluetoothGattService의 리스트를 받아와서 그 서비스와 해당하는 특성 들을 화면에 표시하기 위한 작업 수행
     * 지원되는 GATT를 반복하는 방법을 보여줍니다.
     */
//    //tx 특성만 뽑을경우
//    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null) return
        var uuid: String?
        val gattServiceData: MutableList<HashMap<String, String>> = mutableListOf()
        val gattCharacteristicData: MutableList<ArrayList<HashMap<String, String>>> = mutableListOf()
        val mGattCharacteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()

        //사용 가능한 GATT 서비스를 반복
        gattServices.forEach { gattService ->
            val currenServiceData = HashMap<String, String>()
            uuid = gattService?.uuid.toString()
            Log.d("BLE!@!@", "gatt_service_uuid: $uuid")
            currenServiceData["name"] = SafeAppGattAttribute.lookup(uuid, "unknownService")
            currenServiceData["uuid"] = uuid!!
            Log.d("BLE!@!@", "gatt_service_uuid_currenServiceData: $uuid")
            gattServiceData += currenServiceData

            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
            val gattCharacteristics = gattService?.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            //사용 가능한 특성을 반복
            gattCharacteristics?.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()

//                //tx 특성만 뽑을경우
//                if (uuid.equals("6E400003-B5A3-F393-E0A9-E50E24DCCA9E".lowercase())) {
//                    txCharacteristic = gattCharacteristic
//                }

                Log.d("BLE!@!@", "gatt_charas_uuid: $uuid")
                currentCharaData["name"] = SafeAppGattAttribute.lookup(uuid, "unknowncharas")
                currentCharaData["uuid"] = uuid!!
                gattCharacteristicGroupData += currentCharaData
                Log.d("BLE!@!@", "gatt_charas_uuid_currentCharaData: $uuid")
            }
            mGattCharacteristics += charas
            Log.d("BLE!@!@", "mGattCharacteristics: $mGattCharacteristics")
            gattCharacteristicData += gattCharacteristicGroupData
            Log.d("BLE!@!@", "gattCharacteristicData: $gattCharacteristicData")
        }
//        //tx 특성만 뽑을경우
//        Log.d("BLE!@!@", "txCharacteristic : ${txCharacteristic?.uuid}")

//        gattServices.forEach { gattService ->
//            val serviceUuid = gattService.uuid.toString()
//            Log.d("BLE!@!@", "Service discovered: $serviceUuid")
//
//            // 각 서비스에 대해 characteristic들을 가져옴
//            val gattCharacteristics = gattService.characteristics
//            gattCharacteristics.forEach { characteristic ->
//                val characteristicUuid = characteristic.uuid.toString()
//                Log.d("BLE!@!@", "Characteristic discovered: $characteristicUuid")
//
//                when (characteristic.uuid) {
//                    Constants.TX_CHARACTERISTIC -> {
//                        // TX 특성 (데이터 전송 처리)
//                        writeCharacteristic = characteristic
//                        Log.d("BLE!@!@", "TX Characteristic found")
//                    }
//                    Constants.RX_CHARACTERISTIC -> {
//                        // RX 특성 (데이터(알림) 수신 처리)
//                        notifyCharacteristic = characteristic
//                        enableNotifications(characteristic)
//                        Log.d("BLE!@!@", "RX Characteristic found")
//                    }
//                }
//            }
//        }
    }
    private fun sendData(data: String) {
        writeCharacteristic?.let {
            if (it.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0){
                bluetoothLeService?.writeCharacteristic(it, data)
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        val deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        Log.d("BLE!@!@", "getDeviceName: $deviceName")
        Log.d("BLE!@!@", "getDeviceAddress: $deviceAddress")

        //연결 및 연결 해제 이벤트를 수신하기 위한 서비스 시작
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(deviceAddress)
            Log.d("BLE!@!@", "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }
}