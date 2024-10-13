package com.study.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.study.ble.databinding.ActivityDeviceControlBinding

class DeviceControlActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceControlBinding
    private var bluetoothLeService: BluetoothLeService? = null

    private var deviceAddress: String? = ""

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val leDeviceListAdapter: LeDeviceListAdapter = LeDeviceListAdapter()
    private var isReceiverRegistered = false

    //ble_scanReceiver
    private val bleScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothLeService.BLE_SCAN_RESULT -> {
                    //test용
                    val deviceName = intent.getStringExtra("device_name")
                    val deviceAddress = intent.getStringExtra("device_address")
                    Log.d("BLE!@!@", "bleScanReceiver_deviceName : $deviceName")
                    Log.d("BLE!@!@", "bleScanReceiver_deviceAddress : $deviceAddress")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val result = intent.getParcelableExtra("result", ScanResult::class.java)
                        leDeviceListAdapter.addDevice(result)
                        leDeviceListAdapter.notifyDataSetChanged()
                        Log.d("BLE!@!@", "bleScanReceiver_result : $result")
                    } else {
                        val result: ScanResult? = intent.getParcelableExtra("result")
                        leDeviceListAdapter.addDevice(result)
                        leDeviceListAdapter.notifyDataSetChanged()
                        Log.d("BLE!@!@", "bleScanReceiver_result : $result")
                    }
                }
            }
        }
    }


    //서비스가 연결되어있을 경우 안되어있을경우
    private val serviceConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothLeService?.let { bluetooth ->
                //연결을 확인하고 장치에 연결하기 위해 서비스에서 기능을 호출
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

        //사용 가능한 GATT 서비스를 반복
        gattServices.forEach { gattService ->
            uuid = gattService?.uuid.toString()
            val gattCharacteristics = gattService?.characteristics

            //사용 가능한 특성을 반복
            gattCharacteristics?.forEach { gattCharacteristic ->
                uuid = gattCharacteristic.uuid.toString()
                //tx 특성만 뽑을경우
                if (uuid.equals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E".lowercase())) {
                    writeCharacteristic = gattCharacteristic
                }
            }
        }
        sayHello()
    }

    private fun sayHello() {
        writeCharacteristic?.let {
            if (it.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                bluetoothLeService?.writeCharacteristic(it)
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanControl()

        binding.scanList.adapter = leDeviceListAdapter

        listClickListener()

        //Gatt연결 및 연결 해제 이벤트를 수신하기 위한 서비스 시작
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun listClickListener() {
        binding.scanList.setOnItemClickListener { parent, view, position, id ->
            Log.d("BLE!@!@", "Clicked -> position: $position, id: $id")

            val device = leDeviceListAdapter.getDevice(position) as BluetoothDevice
            deviceAddress = device.address
            bluetoothLeService?.connect(deviceAddress)
        }
    }

    private fun scanControl() {
        binding.startScan.setOnClickListener {
            //기기가 꺼져있을경우 다시 start_scan 시 어뎁터 갱신
            leDeviceListAdapter.clearDevices()
            leDeviceListAdapter.notifyDataSetChanged()
            //스캔 시작 전에 스캔 브로드캐스트가 이미 등록되어있는지 확인
            if (!isReceiverRegistered) {
                registerReceiver(bleScanReceiver, bleScanIntentFilter())
                isReceiverRegistered = true
            }
            //스캔 시작
            bluetoothLeService?.startScan()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        registerReceiver(bleScanReceiver, bleScanIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(deviceAddress)
            Log.d("BLE!@!@", "Connect request result=$result")
        }
    }

    private fun bleScanIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.BLE_SCAN_RESULT)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)

        if (isReceiverRegistered) {
            unregisterReceiver(bleScanReceiver)
            isReceiverRegistered = false
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }


    private inner class LeDeviceListAdapter: BaseAdapter() {
        private val arrayDevices: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
        override fun getCount(): Int {
            return arrayDevices.size
        }

        override fun getItem(position: Int): Any {
            return arrayDevices[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun getDevice(position: Int): Any {
            return arrayDevices[position]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null){
                view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item, parent, false)
                //ViewHolder 생성 및 초기화
                viewHolder = ViewHolder()
                viewHolder.deviceName = view.findViewById(R.id.device_name)
                viewHolder.deviceAddress = view.findViewById(R.id.device_address)

                // View에 ViewHolder를 설정
                view.tag = viewHolder
            } else {    //기존 뷰 재사용
                view = convertView
                viewHolder = view.tag as ViewHolder
            }
            // 현재 position에 해당하는 BluetoothDevice 가져오기
            val device: BluetoothDevice = arrayDevices[position]

            //디바이스 정보가져올때 android version 12++ BLUETOOTH_CONNECT 권한 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {    //version 12++(BLUETOOTH_SCAN은 version 12이상 타겟팅)
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 디바이스 이름이 없으면 'Unknown Device'로 설정
                    val deviceName = device.name
                    val deviceAddress = device.address
                    // ViewHolder에 데이터를 설정
                    viewHolder.deviceName?.text = deviceName
                    viewHolder.deviceAddress?.text = deviceAddress

                } else {
                    Log.d("BLE!@!@", "BLUETOOTH_BLUETOOTH_CONNECT_V12 권한이 없습니다.")
                }
            } else {
                val deviceName = device.name
                val deviceAddress = device.address
                // ViewHolder에 데이터를 설정
                viewHolder.deviceName?.text = deviceName
                viewHolder.deviceAddress?.text = deviceAddress
            }
            return view
        }

        fun addDevice(result: ScanResult?) {
            result?.let {
                if (!arrayDevices.contains(it.device)){
                    arrayDevices.add(it.device)
                }
            }
        }

        fun clearDevices() {
            arrayDevices.clear()
        }

    }
    private class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? =null
    }
}