package com.example.medicalsensorreader

import android.bluetooth.BluetoothGattCharacteristic

class MessageParser(private val listener: (address: String, characteristic: BluetoothGattCharacteristic, value: ByteArray) -> Unit) {
    /**
     * Recursively iterate through the whole Byte Array until there is nothing left to read
     */
    fun handleMessage(characteristic: BluetoothGattCharacteristic, array: ByteArray, address: String) {
        // If array is empty then there is no new messages to parse
        if (array.isEmpty()) {
            return
        }

        listener(address, characteristic, array)
    }
}