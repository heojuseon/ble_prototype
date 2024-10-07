package com.study.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.study.ble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothScanner = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val leDeviceListAdapter: LeDeviceListAdapter = LeDeviceListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //listView 어뎁터 연결
        binding.scanList.adapter = leDeviceListAdapter
        //BLE 어뎁터 설정
        setBleAdapter()

        listClickListener()
    }

    private fun listClickListener() {
        binding.scanList.setOnItemClickListener { parent, view, position, id ->
            Log.d("BLE!@!@", "Clicked -> position: $position, id: $id")

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val device = leDeviceListAdapter.getDevice(position) as BluetoothDevice
                    val intent = Intent(this, DeviceControlActivity::class.java)
                    Log.d("BLE!@!@", "clicked_deviceName: ${device.name}")
                    Log.d("BLE!@!@", "clicked_deviceAddress: ${device.address}")
                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
                    startActivity(intent)
                }
            } else {
                val device = leDeviceListAdapter.getDevice(position) as BluetoothDevice
                val intent = Intent(this, DeviceControlActivity::class.java)
                Log.d("BLE!@!@", "clicked_deviceName: ${device.name}")
                Log.d("BLE!@!@", "clicked_deviceAddress: ${device.address}")
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
                startActivity(intent)
            }
        }
    }

    //블루투스 활성화 요청 콜백
    private var requestBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK){
            //granted
            Log.d("BLE!@!@", "Bluetooth가 활성화되었습니다")
            //활성화시 퍼미션 체크
            checkPermission()
            //권한 승인 시 scanning
            Log.d("BLE!@!@", "Scanning_Start")
        } else {
            //deny
            Log.d("BLE!@!@", "Bluetooth 활성화 거부")
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permissionName = entry.key
            val isGranted = entry.value
            if (isGranted){
                //권한 승인 시 scanning
                Log.d("BLE!@!@", "Permission : $permissionName 권한 허용됨")
                startScan()

            } else {
                Log.d("BLE!@!@", "Permission : $permissionName 권한 거부됨")
            }
        }

    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d("BLE!@!@", "Scanning...")
            //스캔 결과값 받아올 콜백 메소드
            //어뎁터에 연결하여 디바이스 정보 뿌려주는 로직(우선 리스트에 담아서 로그로 확인작업)
            leDeviceListAdapter.addDevice(result)
            leDeviceListAdapter.notifyDataSetChanged()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("BLE!@!@", "Scan_failed")
        }
    }

    private fun startScan() {
        //권한이 되어있는지 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){    //version 12++(BLUETOOTH_SCAN은 version 12이상 타겟팅)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED){
                Log.d("BLE!@!@", "Start_Scan_v12")
                if (!scanning) {
                    //스캔 시간 지나면 중지
                    handler.postDelayed({
                        scanning = false
                        bluetoothScanner?.stopScan(leScanCallback)
                    }, SCAN_PERIOD)
                    scanning = true
                    bluetoothScanner?.startScan(leScanCallback)
                } else {
                    scanning = false
                    bluetoothScanner?.stopScan(leScanCallback)
                }
            } else {
                Log.d("BLE!@!@", "BLUETOOTH_SCAN_V12 권한이 없습니다.")
                checkPermission()
            }
        } else {    //version 12++ 이외
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BLE!@!@", "Start_Scan_v11 이하")
                if (!scanning) {
                    //스캔 시간 지나면 중지
                    handler.postDelayed({
                        scanning = false
                        bluetoothScanner?.stopScan(leScanCallback)
                    }, SCAN_PERIOD)
                    scanning = true
                    bluetoothScanner?.startScan(leScanCallback)
                } else {
                    scanning = false
                    bluetoothScanner?.stopScan(leScanCallback)
                }
            } else {
                Log.d("BLE!@!@", "BLUETOOTH_SCAN_V11 이하 권한이 없습니다.")
                checkPermission()
            }
        }
    }

    private fun setBleAdapter() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.d("BLE!@!@", "블루투스를 지원하지 않는 기기 입니다.")
        } else {
            //스캐너 초기화
            bluetoothScanner = bluetoothAdapter?.bluetoothLeScanner
        }
        Log.d("BLE!@!@", "bluetoothAdapter_info : $bluetoothAdapter")
        Log.d("BLE!@!@", "bluetoothAdapter_boolean : ${bluetoothAdapter?.isEnabled}")
        //블루투스 활성화
        if (bluetoothAdapter?.isEnabled == false){  //활성화가 안되어있을경우
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        } else { //활성화가 되어있을경우
            checkPermission()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {   //android 12 이상
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {    //android 11 이하
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        //ble 를 지원하지 않으면 종료
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
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
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceAddress = device.address
                    // ViewHolder에 데이터를 설정
                    viewHolder.deviceName?.text = deviceName
                    viewHolder.deviceAddress?.text = deviceAddress

                } else {
                    Log.d("BLE!@!@", "BLUETOOTH_BLUETOOTH_CONNECT_V12 권한이 없습니다.")
                    checkPermission()
                }
            } else {
                // 디바이스 이름이 없으면 'Unknown Device'로 설정
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address
                // ViewHolder에 데이터를 설정
                viewHolder.deviceName?.text = deviceName
                viewHolder.deviceAddress?.text = deviceAddress
                if (deviceName != "Unknown Device"){
                    Log.d("BLE!@!@", "device_info_name: $deviceName")
                }

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

    }

    private class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? =null
    }
}