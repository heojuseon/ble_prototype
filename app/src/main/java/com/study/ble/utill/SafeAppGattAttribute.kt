package com.study.ble.utill

import java.util.UUID

object SafeAppGattAttribute {
    private val attributes: HashMap<String, String> = HashMap()

//    const val UART_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
//    const val RX_CHARACTERISTIC = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"  //데이터 수신 -> UUID_DATA_NOTIFY
//    const val TX_CHARACTERISTIC = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"    //데이터 전송 -> UUID_DATA_WRITE

    val UART_SERVICE: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val RX_CHARACTERISTIC: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")  //데이터 수신 -> UUID_DATA_NOTIFY
    val TX_CHARACTERISTIC: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")   //데이터 전송 -> UUID_DATA_WRITE

    init {
        //service
        attributes[UART_SERVICE.toString()] = "UART_SERVICE"
        //characteristics
        attributes[RX_CHARACTERISTIC.toString()] = "RX_CHARACTERISTIC"
        attributes[TX_CHARACTERISTIC.toString()] = "TX_CHARACTERISTIC"
    }

    fun lookup(uuid: String?, defaultName: String): String {
        return attributes[uuid] ?: defaultName
    }
}