package com.goride

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.goride.data.api.RetrofitClient
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class GoRideApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        MapLibre.getInstance(
            this,
            "",
            WellKnownTileServer.MapLibre
        )

        RetrofitClient.init(this)
    }
}