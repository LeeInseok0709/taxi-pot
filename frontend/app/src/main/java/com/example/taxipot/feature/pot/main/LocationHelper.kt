package com.example.taxipot.feature.pot.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * 내 위치 도구 모음
 * - 위치 권한이 있는지 확인
 * - 휴대폰 위치(GPS)가 켜져 있는지 확인
 * - 현재 위치 한 번 찾기
 * - 두 지점 사이 거리(m) 계산 (집결 50m 확인에 사용 예정)
 */
object LocationHelper {

    // 위치를 이 시간(10초) 안에 못 찾으면 마지막으로 알던 위치를 사용
    private const val TIMEOUT_MS = 10_000L

    /** 위치 권한(정확한 위치 또는 대략적인 위치)이 있는지 */
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /** 휴대폰 설정에서 위치가 켜져 있는지 */
    fun isLocationOn(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    /**
     * 현재 위치를 한 번 찾아서 onResult 로 돌려줌 (못 찾으면 null)
     * GPS와 네트워크(와이파이·기지국) 위치를 동시에 요청해서 먼저 오는 것을 사용
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
        if (!hasPermission(context)) {
            onResult(null)
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            onResult(null)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var finished = false
        lateinit var listener: LocationListener

        // 결과는 딱 한 번만 돌려주고, 위치 요청은 바로 멈춤
        fun finish(location: Location?) {
            if (finished) return
            finished = true
            lm.removeUpdates(listener)
            handler.removeCallbacksAndMessages(null)
            onResult(location)
        }

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                finish(location)
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        providers.forEach { provider ->
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }

        // 10초 안에 못 찾으면 마지막으로 알던 위치 중 가장 최근 것을 사용
        handler.postDelayed({
            val last = providers
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
            finish(last)
        }, TIMEOUT_MS)
    }

    /** 두 지점 사이 거리 (미터) */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }
}