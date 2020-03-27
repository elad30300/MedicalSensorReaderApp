package com.example.medicalsensorreader.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.example.medicalsensorreader.DaggerLifecycleService
import com.example.medicalsensorreader.R
import com.example.medicalsensorreader.repository.ScanRepository
import javax.inject.Inject

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothService : DaggerLifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var scanRepository: ScanRepository

    override fun onCreate() {
        super.onCreate()

//        scanRepository.getConnectingDevice().observe(this, Observer {
//            when (it?.state) {
//                ScanState.Ready,
//                ScanState.Disconnected -> setNotificationText(R.string.state_waiting_for_device)
//                ScanState.Scanning -> setNotificationText(R.string.state_scanning)
//                ScanState.Connected -> setNotificationText(R.string.state_connected)
//                ScanState.Synchronizing -> setNotificationText(R.string.state_synchronizing)
//                ScanState.Synchronized -> setNotificationText(R.string.state_synchronized)
//            }
//        })

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

//        val notificationIntent = Intent(this, ScanFragment::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.state_init))
//            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.mipmap.ic_launcher)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder.build()
        )
    }

    private fun setNotificationText(resId: Int) {
        notificationBuilder.setContentText(getString(resId))
        updateNotification()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )

        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    companion object {
        const val NOTIFICATION_ID = 1337
        const val NOTIFICATION_CHANNEL_ID = "codezero"
        const val NOTIFICATION_CHANNEL_NAME = "codezero"
    }
}
