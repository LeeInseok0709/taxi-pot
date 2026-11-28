package com.example.taxipot.core.ui

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.NotificationApi
import kotlinx.coroutines.launch

/**
 * 종 아이콘의 안 읽은 알림 점.
 * 화면이 다시 보일 때(onResume)마다 NotificationBadge.refresh(this, binding.viewBellDot) 한 줄이면 된다.
 */
object NotificationBadge {

    private val api by lazy { ApiClient.create(NotificationApi::class.java) }

    fun refresh(owner: LifecycleOwner, dot: View) {
        owner.lifecycleScope.launch {
            val result = safeApiCall { api.getUnreadCount() }
            // 실패하면 점을 숨김 (잘못된 표시보다 안 보이는 게 나음)
            dot.visibility = if (result is ApiResult.Success && result.data.unreadCount > 0) View.VISIBLE else View.GONE
        }
    }
}
