package com.mycompany.mywebsiteapp

import android.app.Application
import com.onesignal.OneSignal

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.initWithContext(this, "YOUR_ONESIGNAL_APP_ID")
    }
}
