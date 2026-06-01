package com.goride

import android.app.Application
import com.goride.data.api.RetrofitClient

class GoRideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}