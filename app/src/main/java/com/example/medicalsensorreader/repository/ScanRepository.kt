package com.example.medicalsensorreader.repository

import android.util.Log
import com.example.medicalsensorreader.bluetooth.BluetoothGattHandler


import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.medicalsensorreader.MessageParser
import com.example.medicalsensorreader.SampleGattAttributes
import com.example.medicalsensorreader.handlers.MessageHandlers
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val gattService: BluetoothGattHandler
) {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val rssiPair = MutableLiveData<Pair<String, Int>>()
    private val parser: MessageParser

    init {
        parser = MessageParser { address, characteristic, message ->
            processMessage(address, characteristic, message)
        }
    }

    private fun processMessage(address: String, characteristic: BluetoothGattCharacteristic, message: ByteArray) {
        when (characteristic.uuid.toString().toUpperCase()) {
            SampleGattAttributes.NONIN_OXIMTERY_MEASURMENT -> MessageHandlers.handleNoninOximteryMeasurmentCharacteristicMessage(address, message)
            SampleGattAttributes.NONIN_RESPIRATION_RATE_MEASURMENT -> MessageHandlers.handleNoninRespirationRateCharacteristicMessage(address, message)
        }
    }

    private fun onDeviceFirstInteraction(btName: String) {
        Log.d(TAG, "onDeviceFirstInteraction  with address $btName")
//        Device(btName.toUpperCase(), 0, ScanState.Scanning, false, 0, 0, forcedConnectionType.value!!, 0, deviceDao.getMaxOrderNum() + 1).run {
//            deviceDao.insertDevice(this)
//            connectingDeviceAddress.postValue(address)
//            logger.d(TAG, "New device inserted to DB, Address = $address")
//        }
    }

    fun startScan(btName: String, failCallback: () -> Unit, disconnectCallback: () -> Unit) {
        executor.execute {
            onDeviceFirstInteraction(btName)

            gattService.startScan(
                btName,
                object : BluetoothGattHandler.StateChangeListener {
                    override fun onDisconnectDeviceByUser(address: String) {
                        Log.i(TAG, "Disconnected device $address by user")
//                        executor.execute { deviceDao.deleteDevice(address) }
                    }

                    override fun onScanSuccessful(address: String) {
                        Log.i(TAG, "Scan for $address succeed")
                    }

                    override fun onRssiRead(address: String, rssi: Int) {
                        rssiPair.postValue(Pair(address, rssi))
                    }

                    override fun onCharacteristicRead(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                        Log.i(TAG, "onCharacteristicRead with ${device.address} and value ${value.toList()}")
                        executor.execute {
                            parser.handleMessage(characteristic, value, device.address)
                        }
                    }

                    override fun onServicesDiscovered(address: String) {
                        Log.d(TAG, "onServicesDiscovered called with address = $address")

//                        gattService.writeAsMonitor(phoneId.toByteArray(), address)
                    }

                    override fun onConnected(address: String) {
                        Log.d(TAG, "onConnected called with address = $address")
                    }

                    override fun onReconnected(address: String) {
                        Log.d(TAG, "onReconnected called with address = $address")
                    }

                    override fun onDisconnected(address: String) {
                        Log.d(TAG, "onDisconnected called with address = $address")
                        disconnectCallback()
                    }

                    override fun onScanStarted(address: String) {
                        Log.d(TAG, "scan started with address $address")
                    }

                    override fun onScanStopped(address: String) {
                        Log.d(TAG, "onScanStopped called")
                        failCallback()
                    }

                    override fun onScanFailed() {
                        Log.e(TAG, "onScanFailed called")
                        failCallback()
                    }
                }
            )
        }
    }


    companion object {
        private const val TAG = "ScanRepository"
        const val SYSTEM_EXIT = 123
    }
}
