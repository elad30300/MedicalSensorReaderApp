package com.example.medicalsensorreader.handlers

import android.util.Log

object MessageHandlers {

    val TAG = "MessageHandlers"

//    val meesageHandlers = mapOf<String, (address: String, message: ByteArray) -> Unit>(
//        SampleGattAttributes.NONIN_OXIMTERY_MEASURMENT to { address, message ->
//            handleNoninOximteryMeasurmentCharacteristicMessage(address, message)
//        },
//        SampleGattAttributes.NONIN_RESPIRATION_RATE_MEASURMENT to { address, message ->
//            handleNoninRespirationRateCharacteristicMessage(address, message)
//        }
//    )

    fun handleNoninOximteryMeasurmentCharacteristicMessage(address: String, message: ByteArray) {
        val saturation = message[7].toInt() and 0xff
        val pulseRate =  ((message[8].toInt() and 0xff ) shl 8) or ((message[9].toInt()) and 0xff)
        Log.i(TAG, "nonin oximetry message, saturation: ${saturation}, hr: ${pulseRate}")
    }

    fun handleNoninRespirationRateCharacteristicMessage(address: String, message: ByteArray) {
        val respiratoryRate = message[4].toInt() and 0xff
        Log.i(TAG, "nonin respiratory rate message, respiratory: $respiratoryRate")
    }
}