package com.example.taxipot

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.CommonApi
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        testApi() // TODO: 연결 확인 후 삭제
    }

    /** 서버 연결 테스트: Logcat에서 'ApiTest'로 검색 */
    private fun testApi() {
        lifecycleScope.launch {
            val api = ApiClient.create(CommonApi::class.java)
            when (val result = safeApiCall { api.getUniversities() }) {
                is ApiResult.Success -> Log.d("ApiTest", "성공: 학교 ${result.data.size}개 ${result.data}")
                is ApiResult.Failure -> Log.e("ApiTest", "서버 에러: ${result.httpStatus} ${result.code} ${result.message}")
                is ApiResult.NetworkError -> Log.e("ApiTest", "연결 실패", result.throwable)
            }
        }
    }
}
