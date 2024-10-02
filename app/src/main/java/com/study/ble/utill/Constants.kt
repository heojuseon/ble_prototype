package com.study.ble.utill

class Constants {
    companion object {
        //사용자 BLE UUID Service/Rx/Tx
        const val UART_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
        const val RX_CHARACTERISTIC = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"    //송신
        const val TX_CHARACTERISTIC = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"    //수신

    }
}