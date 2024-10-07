package com.study.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.study.ble.DeviceControlActivity.Companion.EXTRAS_DEVICE_ADDRESS
import com.study.ble.DeviceControlActivity.Companion.EXTRAS_DEVICE_NAME
import com.study.ble.utill.Constants
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
            bluetoothLeService?.connect(deviceAddress)
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
                    bluetoothLeService?.let {
                        displayGattServices(it.getSupportedGattServices())
                    }
                }
            }
        }

    }

    /**
     * BLE 특성읽기
     * BluetoothGattService의 리스트를 받아와서 그 서비스와 해당하는 특성 들을 화면에 표시하기 위한 작업 수행
     */
    private fun displayGattServices(gattServices: List<BluetoothGattService>) {
        gattServices.forEach { gattService ->
            val serviceUuid = gattService.uuid.toString()
            Log.d("BLE!@!@", "Service discovered: $serviceUuid")

            // 각 서비스에 대해 characteristic들을 가져옴
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { characteristic ->
                val characteristicUuid = characteristic.uuid.toString()
                Log.d("BLE!@!@", "Characteristic discovered: $characteristicUuid")

                when (characteristic.uuid) {
                    Constants.TX_CHARACTERISTIC -> {
                        // TX 특성 (데이터 전송 처리)
                        writeCharacteristic = characteristic
                        Log.d("BLE!@!@", "TX Characteristic found")
                    }
                    Constants.RX_CHARACTERISTIC -> {
                        // RX 특성 (데이터(알림) 수신 처리)
                        notifyCharacteristic = characteristic
                        enableNotifications(characteristic)
                        Log.d("BLE!@!@", "RX Characteristic found")
                    }
                }
            }
        }
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic?) {
        if (characteristic != null) {
            bluetoothLeService?.setCharacteristicNotification(characteristic, true)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        val deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        Log.d("BLE!@!@", "getDeviceName: $deviceName")
        Log.d("BLE!@!@", "getDeviceAddress: $deviceAddress")

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

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }
}