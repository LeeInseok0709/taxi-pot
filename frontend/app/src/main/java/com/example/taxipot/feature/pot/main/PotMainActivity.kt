package com.example.taxipot.feature.pot.main

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.taxipot.BuildConfig
import com.example.taxipot.databinding.ActivityPotMainBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapLifeCycleCallback
import java.security.MessageDigest

class PotMainActivity : ComponentActivity() {

    // XML 화면(activity_pot_main.xml)을 코드에서 쓰기 위한 변수
    private lateinit var binding: ActivityPotMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵에 내 키 알려주기 (지도 쓰기 전에 꼭 먼저)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) 카카오 콘솔에 등록할 키 해시를 Logcat에 출력 (등록 끝나면 이 줄 삭제)
        logKeyHash()

        // 3) XML 화면 붙이기
        binding = ActivityPotMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4) 지도 시작
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                Log.d("KakaoMap", "지도 준비 완료!")
            }
        })
    }

    // 화면이 다시 보일 때 / 가려질 때 지도도 같이 켜고 멈춤
    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }

    // 키 해시 출력 함수 (카카오 콘솔 등록이 끝나면 함수째 삭제)
    private fun logKeyHash() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners?.forEach { sig ->
            val md = MessageDigest.getInstance("SHA")
            md.update(sig.toByteArray())
            Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.NO_WRAP))
        }
    }
}