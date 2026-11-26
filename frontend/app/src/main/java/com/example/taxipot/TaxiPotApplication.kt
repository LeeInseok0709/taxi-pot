package com.example.taxipot

import android.app.Application
import com.example.taxipot.core.network.ApiClient

class TaxiPotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 저장된 토큰을 불러오고 Retrofit을 준비한다
        ApiClient.init(this)
    }
}
