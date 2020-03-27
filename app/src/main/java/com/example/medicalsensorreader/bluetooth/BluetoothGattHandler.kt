package com.example.medicalsensorreader.bluetooth

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.medicalsensorreader.SampleGattAttributes
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.concurrent.schedule

class BluetoothGattHandler @Inject constructor(val context: Application, val manager: BluetoothManager) {
    private var bluetoothScanner: BluetoothLeScanner? = manager.adapter?.bluetoothLeScanner
    private var gattConnections = mutableMapOf<String, BluetoothGattConnection?>()

    private lateinit var mStateChangeListener: StateChangeListener
    private var stopScanTimer: Timer? = null
    private var mIsScanning: Boolean = false
    private var writeExecutor = Executors.newSingleThreadExecutor()
    private var rssiExecutor = Executors.newCachedThreadPool()
    private val leScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            mIsScanning = false
            mStateChangeListener.onScanFailed()
        }

        @Synchronized
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScanTimer?.cancel()

            if (result.device.address !in gattConnections) {
                stopScan(true, result.device.address)
                Log.i(CONNECTION_TAG, "Found ble device, address: ${result.device.address}, trying to connect")
                gattConnections[result.device.address] = BluetoothGattConnection().apply {
                    bluetoothGatt = result.device.connectGatt(context, false, bluetoothGattCallback)
                }
            } else {
                Log.i(CONNECTION_TAG, "Device ${result.device.address} already connected")
                // stopScan(false, result.device.address)
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(CONNECTION_TAG, "Device connection state changed, new state: $newState")
            val address = gatt.device.address

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(CONNECTION_TAG, "Connected to device server, at address: $address")
                Log.d(CONNECTION_TAG, "Server is connected, at address: $address")

                Thread.sleep(600L)
                gatt.discoverServices()
                mStateChangeListener.onConnected(address)
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                gattConnections[address]?.let {
                    if (it.isReconnecting) {
                        mStateChangeListener.onReconnected(address)
                    }
                }

                readRssiOnInterval(address, gatt)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(CONNECTION_TAG, "Disconnected from device, address: $address, Disconnection status: $status")
                Log.d(CONNECTION_TAG, "Server is disconnected, address: $address, Disconnection status: $status")

                mStateChangeListener.onDisconnected(address)
                when (status) {
                    DISCONNECTED_BY_DEVICE, BLE_HCI_LOCAL_HOST_TERMINATED_CONNECTION -> {
                        Log.d(CONNECTION_TAG, "Disconnection reason: black box terminated connection")
                        gatt.close()
                        gattConnections.remove(address)
                    }
                    GATT_ERROR -> {
                        gatt.close()
                        gattConnections.remove(address)
                        startScan(address, mStateChangeListener)
                    }
                    DISCONNECTED_BY_USER -> {
                        gatt.close()
                        gattConnections.remove(address)
                        mStateChangeListener.onDisconnectDeviceByUser(address)
                    }
                    else -> {
                        Log.i(CONNECTION_TAG, "Trying to reconnect to device: $address")
                        gattConnections[address]?.reconnectWhenPossible()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(CONNECTION_TAG, "Services discovered: $status")
                gatt.requestMtu(250)
            } else {
                // TODO handle fail cases
                Log.w(CONNECTION_TAG, "onServicesDiscovered received: $status")
            }
        }

        private fun registerCharacteristicToNotification(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
            if ((characteristic.properties or BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                setCharacteristicNotification(gatt.device.address, characteristic, true)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.services.firstOrNull { x -> x.uuid == UUID.fromString(SampleGattAttributes.NONIN_OXIMTERY_SERVICE) }
                if (service != null) {
//                    val characteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.FIRST_NAME))

                    gattConnections[gatt.device.address]?.apply {
                        mNoninOximetryMeasurmentCharacteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.NONIN_OXIMTERY_MEASURMENT)).also {
                            registerCharacteristicToNotification(it, gatt)
                        }
                        mNoninRespirationRateCharacteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.NONIN_RESPIRATION_RATE_MEASURMENT)).also {
                            registerCharacteristicToNotification(it, gatt)
                        }
                    }

                    mStateChangeListener.onServicesDiscovered(gatt.device.address)

//                    if ((characteristic.properties or BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
//                        setCharacteristicNotification(gatt.device.address, characteristic, true)
//                    }

                    // Training module
                    /*when (forcedConnectionType) {
                        ConnectionType.Medic -> gattConnections[gatt.device.address]?.mWriteAppCharacteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.LAST_NAME))
                        ConnectionType.Trainer -> gattConnections[gatt.device.address]?.mWriteMonitorCharacteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.USER_CONTROL_POINT))
                        ConnectionType.Watcher -> Log.i(CONNECTION_TAG, "Granted read only permissions")
                    }*/
                }
            } else {
                Log.e(GENERAL_TAG, "Mtu change failed")
                gatt.requestMtu(250)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(GENERAL_TAG, "trying to write ${characteristic.value.toList()} to board, success = ${status == 0}")
            } else {
                Log.e(GENERAL_TAG, "Write failed: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(GENERAL_TAG, "trying to read ${characteristic.value.toList()} to board, success = ${status == 0}")
            } else {
                Log.e(GENERAL_TAG, "Read failed: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.d(GENERAL_TAG, "Characteristic changed, device = ${gatt.device.address}, characteristic = ${characteristic.value.toList()}")
            mStateChangeListener.onCharacteristicRead(gatt.device, characteristic, characteristic.value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            Log.v(GENERAL_TAG, "Rssi check, device: ${gatt?.device?.address}, strength: $rssi")
            gatt?.device?.let {
                mStateChangeListener.onRssiRead(it.address, rssi)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d(GENERAL_TAG, "onDescriptorWrite, status = $status")
            if (status == GATT_ERROR) {
                gatt?.writeDescriptor(descriptor)
            }
        }
    }

    fun disconnectDevice(address: String) {
        gattConnections[address]?.bluetoothGatt?.disconnect()
    }

    private fun readRssiOnInterval(address: String, gatt: BluetoothGatt) {
        rssiExecutor.execute {
            while (gattConnections[address]?.isConnected() == true) {
                gatt.readRemoteRssi()
                Thread.sleep(SIGNAL_CHECK_INTERVAL_MS)
            }

            Log.d(GENERAL_TAG, "Rssi check stopped")
        }
    }

    @Synchronized
    private fun write(address: String, characteristic: BluetoothGattCharacteristic, array: ByteArray) {
        var tryCounter = 0
        var writeResult: Boolean
        characteristic.value = array
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        Log.d(GENERAL_TAG, "trying to send ${array.toList()} to board at address $address")

        // Fail safe writing
        writeExecutor.execute {
            do {
                gattConnections[address]?.bluetoothGatt?.writeCharacteristic(characteristic).also {
                    writeResult = it ?: false
                }

                tryCounter++

                if (!writeResult) {
                    Thread.sleep(WAIT_TIME_BETWEEN_WRITES)
                }

                Log.i(GENERAL_TAG, "write result: $writeResult, tries Counter: $tryCounter, device: $address")
            } while (!writeResult && tryCounter <= MAX_TRIES)
        }
    }

//    fun writeAsApp(address: String, array: ByteArray) {
//        gattConnections[address]?.mWriteAppCharacteristic?.let { write(address, it, array) }
//    }

    // TrainingModule
    // TODO make it suitable for mci
//    fun writeAsMonitor(array: ByteArray, address: String) {
//        // TODO get address as parameter when merging mci with training module
//        gattConnections[address]?.mWriteMonitorCharacteristic?.let { characteristic ->
//            write(address, characteristic, array)
//        } ?: Log.e(GENERAL_TAG, "Unable to send data, USER_CONTROL_POINT characteristic is null")
//    }

    // Watcher
//    fun disableWriting(address: String) {
//        gattConnections[address]?.let {
//            // it.mWriteMonitorCharacteristic = null
//            it.mWriteAppCharacteristic = null
//        }
//    }

    fun stopScan(isSuccessful: Boolean, address: String? = null) {
        Log.i(CONNECTION_TAG, "Stop scan has been called with isSuccessful = $isSuccessful and address = $address")
        mIsScanning = false

        bluetoothScanner?.stopScan(leScanCallback)

        address?.let {
            if (isSuccessful) {
                mStateChangeListener.onScanSuccessful(it)
            } else {
                Log.d(CONNECTION_TAG, "Device $address not found")
                mStateChangeListener.onScanStopped(it)
            }
        }
    }

    fun startScan(btName: String, listener: StateChangeListener) {
        Log.i(CONNECTION_TAG, "Scan result: Looking for $btName")
        mStateChangeListener = listener

        Log.d(CONNECTION_TAG, "Server is connected = ${gattConnections[btName]?.isConnected()}")
        gattConnections[btName]?.bluetoothGatt?.close()
        Log.d(CONNECTION_TAG, "Server is connected after = ${gattConnections[btName]?.isConnected()}")
        gattConnections.remove(btName)

        stopScanTimer = Timer("Stop scan", false).apply {
            schedule(SCAN_TIME) {
                stopScan(false, btName)
            }
        }

        if (!mIsScanning) {
            mIsScanning = true

            mStateChangeListener.onScanStarted(btName)
            bluetoothScanner?.startScan(listOf(ScanFilter.Builder().setDeviceAddress(btName).build()), ScanSettings.Builder().setNumOfMatches(
                ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), leScanCallback)
        }
    }

    fun close() {
        gattConnections.apply {
            values.forEach {
                it?.bluetoothGatt?.close()
            }
            clear()
        }

        Log.i(CONNECTION_TAG, "Disconnected from all devices")
    }

    @Synchronized
    private fun setCharacteristicNotification(address: String, characteristic: BluetoothGattCharacteristic, enable: Boolean) {
        gattConnections[address]?.bluetoothGatt?.setCharacteristicNotification(characteristic, enable).also {
            Log.d(GENERAL_TAG, "setCharacteristicNotification result = $it, device: $address, characteristic: ${characteristic.uuid}")
        } ?: Log.e(GENERAL_TAG, "Couldn't set characteristic")

        characteristic.descriptors.forEach {
            val descriptor = it
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            writeExecutor.execute {
                var tryCounter = 0
                var writeResult = false

                do {
                    tryCounter++

                    gattConnections[address]?.bluetoothGatt?.writeDescriptor(descriptor)?.also {
                        Log.i(GENERAL_TAG, "descriptor write result: $writeResult, tries Counter: $tryCounter, device $address")
                        writeResult = it

                        if (!it) {
                            Thread.sleep(WAIT_TIME_BETWEEN_WRITES)
                        }
                    } ?: break
                } while (!writeResult && tryCounter <= MAX_TRIES)
            }
        }


//            val descriptor = characteristic.getDescriptor(
//                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
//            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//
//            writeExecutor.execute {
//                var tryCounter = 0
//                var writeResult = false
//
//                do {
//                    tryCounter++
//
//                    gattConnections[address]?.bluetoothGatt?.writeDescriptor(descriptor)?.also {
//                        Log.i(GENERAL_TAG, "descriptor write result: $writeResult, tries Counter: $tryCounter, device $address")
//                        writeResult = it
//
//                        if (!it) {
//                            Thread.sleep(WAIT_TIME_BETWEEN_WRITES)
//                        }
//                    } ?: break
//                } while (!writeResult && tryCounter <= MAX_TRIES)
//            }
    }


    fun isReconnected(address: String) = gattConnections[address]?.isReconnecting

    fun onSynchronized(address: String) {
        Log.d(CONNECTION_TAG, "Changing connection priority to balanced")
        gattConnections[address]?.bluetoothGatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
    }

    inner class BluetoothGattConnection {
        var bluetoothGatt: BluetoothGatt? = null
        var mNoninOximetryMeasurmentCharacteristic: BluetoothGattCharacteristic? = null
        var mNoninRespirationRateCharacteristic: BluetoothGattCharacteristic? = null
        var isReconnecting = false

        fun reconnectWhenPossible() {
            bluetoothGatt?.connect()
            isReconnecting = true
        }

        fun isConnected(): Boolean = manager.getConnectionState(bluetoothGatt?.device, BluetoothProfile.GATT_SERVER) == BluetoothProfile.STATE_CONNECTED
    }

    companion object {
        private const val CONNECTION_TAG = "BluetoothConnection"
        private const val GENERAL_TAG = "BluetoothGattHandler"
        private const val SCAN_TIME: Long = 10000L
        private const val MAX_TRIES = 20
        private const val WAIT_TIME_BETWEEN_WRITES = 150L
        private const val SIGNAL_CHECK_INTERVAL_MS = 5000L
        private const val DISCONNECTED_BY_DEVICE = 19
        private const val DISCONNECTED_BY_USER = 0
        private const val GATT_ERROR = 133
        private const val BLE_HCI_LOCAL_HOST_TERMINATED_CONNECTION = 22
        private const val BLE_HCI_CONNECTION_TIMEOUT = 8
    }

    // TODO Cancel remove callbacks not working
    // TODO Rename / class abstract?
    interface StateChangeListener {
        fun onCharacteristicRead(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic, value: ByteArray)
        fun onServicesDiscovered(address: String)
        fun onConnected(address: String)
        fun onDisconnected(address: String)
        fun onScanStarted(address: String)
        fun onScanStopped(address: String)
        fun onScanSuccessful(address: String)
        fun onScanFailed()
        fun onRssiRead(address: String, rssi: Int)
        fun onReconnected(address: String)
        fun onDisconnectDeviceByUser(address: String)
    }
}