package com.goride

import android.app.Application
import com.goride.data.api.RetrofitClient
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class GoRideApp : Application() {
    override fun onCreate() {
        super.onCreate()

        MapLibre.getInstance(
            this,
            "",
            WellKnownTileServer.MapLibre
        )

        RetrofitClient.init(this)
    }
}