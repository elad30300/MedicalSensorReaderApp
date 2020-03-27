package com.example.medicalsensorreader

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.example.medicalsensorreader.di.MainActivityComponent
import com.example.medicalsensorreader.services.BluetoothService
import com.example.medicalsensorreader.viewmodel.MainViewModel
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    private lateinit var mainActivityComponent: MainActivityComponent
    @Inject lateinit var mainViewModel: MainViewModel

//    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityComponent = (applicationContext as MyApplication).applicationComponent.mainActivityComponent().create()
        mainActivityComponent.inject(this)

        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        requestPermissions(
            PERMISSIONS_STORAGE,
            1234)

        checkIfBluetoothOn()

        startScan()
    }

    private fun startScan() {
        mainViewModel.startScan(
            "00:1C:05:FF:05:8E",
            { startScan() }
        ) { startScan() }
    }

    /* check if BT is on/off
     * on - do nothing
      off - popup to open BT*/
    private fun checkIfBluetoothOn() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            // Phone doesn't support BT
            Toast.makeText(this, "BT not supported exit app", Toast.LENGTH_SHORT).show()
            this.finishAndRemoveTask()
        } else {
            if (mBluetoothAdapter.isEnabled) {
                // BT is enabled
                val intent = Intent(this, BluetoothService::class.java)
                startService(intent)
            } else {
                // BT is disabled and need to be open
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val TAG = "MainActivity"
    }
}
