package com.example.medicalsensorreader

import androidx.lifecycle.LifecycleService
import dagger.android.AndroidInjection

abstract class DaggerLifecycleService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)
    }
}