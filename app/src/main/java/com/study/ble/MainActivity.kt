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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()

        startDeviceControl()
    }

    private fun startDeviceControl() {
        //블루투스 활성화 성공시에 startActivity
        binding.toDeviceControl.setOnClickListener {
            //BLE 어뎁터 설정
            setBleAdapter()
        }
    }

    //블루투스 활성화 요청 콜백
    private var requestBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK){
            //granted
            Log.d("BLE!@!@", "Bluetooth가 활성화되었습니다")
            val intent = Intent(this, DeviceControlActivity::class.java)
            startActivity(intent)
        } else {
            //deny
            Log.d("BLE!@!@", "Bluetooth 활성화 거부")
            finish()
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value } // 모든 권한이 승인되었는지 확인
        if (allGranted) {
            Log.d("BLE!@!@", "모든 권한이 허용됨")
        } else {
            Log.d("BLE!@!@", "일부 권한이 거부됨")
            finish() // 권한 거부 시 앱 종료 (임시 조치)
        }
    }

    private fun setBleAdapter() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        Log.d("BLE!@!@", "bluetoothAdapter_info : $bluetoothAdapter")
        Log.d("BLE!@!@", "bluetoothAdapter_boolean : ${bluetoothAdapter?.isEnabled}")

        // 블루투스 활성화 상태 체크
        if (bluetoothAdapter?.isEnabled == false) {
            // 활성화가 안 되어 있을 경우
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)

        } else { // 활성화가 되어 있을 경우
            val intent = Intent(this, DeviceControlActivity::class.java)
            startActivity(intent)
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
}