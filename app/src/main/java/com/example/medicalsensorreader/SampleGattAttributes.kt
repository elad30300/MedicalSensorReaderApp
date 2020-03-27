package com.example.medicalsensorreader

import java.util.HashMap


/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    private val attributes = HashMap<String, String>()

    var NONIN_OXIMTERY_SERVICE = "46A970E0-0D5F-11E2-8B5E-0002A5D5C51B"    //    var USER_DATA_SERVICE = "0000181c-0000-1000-8000-00805f9b34fb"

    var NONIN_OXIMTERY_MEASURMENT = "0AAD7EA0-0D60-11E2-8E3C-0002A5D5C51B"
    var NONIN_RESPIRATION_RATE_MEASURMENT = "EC0A8F24-4D24-11E7-B114-B2F933D5FE66"
//    var LAST_NAME = "00002a90-0000-1000-8000-00805f9b34fb"
//    var FIRST_NAME = "00002a8a-0000-1000-8000-00805f9b34fb"
//    var USER_CONTROL_POINT = "00002a9f-0000-1000-8000-00805f9b34fb"
//    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
//    var FAN_CONTROL_SERVICE_UUID = "86c86302-fc10-1399-df43-fceb24618252"
//    var FAN_OPERATING_STATE = "705d627b-53e7-eda2-2649-5ecbd0bbfb85"

//    init {
//        // Sample Services.
//        attributes[USER_DATA_SERVICE] = "Heart Rate Service"
//        attributes["0000180a-0000-1000-8000-00805f9b34fb"] = "Device Information Service"
//        attributes["72f3d37d-c861-07ab-0341-f8cf302ca8b1"] = "DarkBlue Managed Service"
//        attributes["f23844d8-1f57-448b-7f44-e64ae0fe15cf"] = "Line Cook Steps Service"
//        attributes["430f9940-1ff5-ca8e-d843-27c7153258a4"] = "Line Cook 2.0 Steps Service"
//        attributes[FAN_CONTROL_SERVICE_UUID] = "Fan Control Service"
//
//        // Sample Characteristics.
//        attributes[FIRST_NAME] = "Heart Rate Measurement"
//        attributes["00002a29-0000-1000-8000-00805f9b34fb"] = "Manufacturer Name String"
//        attributes["abcbe138-a00c-6b8e-7d44-4b63a80170c3"] = "DarkBlue Company UUID Characteristic"
//        attributes["26b8a2dc-544d-008e-c343-5d8fac910c6e"] = "DarkBlue Major ID Characteristic"
//        attributes["0acea172-2a76-69a7-4e49-302ed371f6a8"] = "DarkBlue Minor ID Characteristic"
//        attributes["e205929f-e016-ecbc-024b-62f0658fdacd"] = "DarkBlue Measured Power Characteristic"
//        attributes["519fc39c-9a18-82b2-ea43-6a101ab1a8f9"] = "Cooking Step Characteristic"
//        attributes["52812a3d-247d-77b9-6740-75748c4621c5"] = "Cooking Step Characteristic"
//        attributes["f47e47ab-b25f-3a83-7b47-1f230b2b1a61"] = "Cooking Step Characteristic"
//        attributes[FAN_OPERATING_STATE] = "Fan Operating State Characteristic"
//    }

    @JvmStatic
    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes.get(uuid)
        return name ?: defaultName
    }
}