package com.example.taxipot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.fcm.PushRoute
import com.example.taxipot.feature.auth.login.LoginActivity
import com.example.taxipot.feature.pot.main.PotMainActivity

/**
 * 앱 시작 화면 (UI 없음): 로그인 기록이 있으면 홈, 없으면 로그인.
 * 푸시를 눌러 열렸으면 홈을 깔고 그 위에 알림 대상 화면(정산 내역·평가·채팅 등)을 연다.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ApiClient.tokenManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 이미 홈이 열려 있으면 새로 만들지 않고 앞으로
        val home = Intent(this, PotMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val target = PushRoute.targetIntent(this, intent)
        if (target == null || target.component?.className == PotMainActivity::class.java.name) {
            startActivity(home)
        } else {
            startActivities(arrayOf(home, target)) // 대상 화면에서 뒤로가기 → 홈
        }
        finish()
    }
}
