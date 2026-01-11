package com.arkus.shoppyjuan

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShoppyJuanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-level components here
    }
}
