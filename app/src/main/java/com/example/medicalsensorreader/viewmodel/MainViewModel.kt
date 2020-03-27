package com.example.medicalsensorreader.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.medicalsensorreader.di.MainActivityScope
import com.example.medicalsensorreader.repository.ScanRepository
import javax.inject.Inject

@MainActivityScope
class MainViewModel @Inject constructor (
    val scanRepository: ScanRepository
) : ViewModel() {

    fun startScan(btName: String, failCallback: () -> Unit, disconnectCallback: () -> Unit) {
        scanRepository.startScan(btName, failCallback, disconnectCallback)
        Log.i(TAG, "Start scan $btName")
    }

    companion object {
        private const val TAG = "MainViewModel"
//        private const val LAST_TIME_MEASURED_NOT_VALID_ERROR_TAG = "time-measured-not-valid"
//        private const val LAST_TIME_MEASURED_NOT_VALID_ERROR_MESSAGE = "measured time is not valid (less than 0)"
//        private val DEVICE_ID_NOT_ACQUIRED = null
    }
}