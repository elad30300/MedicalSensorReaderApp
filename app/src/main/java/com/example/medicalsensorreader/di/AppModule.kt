package com.example.medicalsensorreader.di

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(val context: Application) {

    @Singleton
    @Provides
    fun provideApplicationContext() = context

    @Singleton
    @Provides
    fun provideBluetoothManager(app: Application): BluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

}