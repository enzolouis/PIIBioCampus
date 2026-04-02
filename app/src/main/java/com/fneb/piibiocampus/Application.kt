package com.fneb.piibiocampus

import android.app.Application
import com.fneb.piibiocampus.network.NetworkMonitor

class App : Application() {

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor.getInstance(this)
    }
}