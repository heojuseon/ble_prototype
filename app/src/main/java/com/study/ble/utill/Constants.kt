//package com.study.ble.utill
//
//import java.util.UUID
//
//class Constants {
//    companion object {
//        //사용자 BLE UUID Service/Rx/Tx
//        const val UART_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
//        val RX_CHARACTERISTIC: UUID? = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")    //데이터 수신 -> UUID_DATA_NOTIFY
//        val TX_CHARACTERISTIC: UUID? = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")    //데이터 전송 -> UUID_DATA_WRITE
//
//        //BLE 통신에서 정해진 UUID를 가지고 있는 표준 Descriptor
//        //특성(characteristic)에서 Notify나 Indicate를 활성화하거나 비활성화할 때 사용
//        val CLIENT_CHARACTERISTIC_CONFIG: UUID? = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//    }
//}