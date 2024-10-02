package com.study.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.study.ble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //BLE 어뎁터 설정
        setBleAdapter()
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
            } else {
                Log.d("BLE!@!@", "Permission : $permissionName 권한 거부됨")
            }
        }

    }

    private fun setBleAdapter() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.d("BLE!@!@", "블루투스를 지원하지 않는 기기 입니다.")
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
}