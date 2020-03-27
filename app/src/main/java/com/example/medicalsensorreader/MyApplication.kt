package com.example.medicalsensorreader

import android.app.Application
import com.example.medicalsensorreader.di.AppModule
import com.example.medicalsensorreader.di.DaggerApplicationComponent

class MyApplication : Application() {
    val applicationComponent = DaggerApplicationComponent.builder()
        .appModule(AppModule(this))
        .build()
}