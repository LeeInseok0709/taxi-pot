package com.example.taxipot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.feature.auth.login.LoginActivity
import com.example.taxipot.feature.pot.main.PotMainActivity

/** 앱 시작 화면: 로그인 기록이 있으면 홈, 없으면 로그인으로 보낸다 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val next = if (ApiClient.tokenManager.isLoggedIn()) {
            PotMainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}