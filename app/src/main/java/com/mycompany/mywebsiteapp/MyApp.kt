package com.mycompany.mywebsiteapp

import android.app.Application
import com.onesignal.OneSignal

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.initWithContext(this, "161c1e8a-1620-49f5-bdd4-3abb83a7911a")
    }
}
