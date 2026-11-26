package com.example.taxipot.feature.notification

import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.core.ui.PagedListViewModel
import com.example.taxipot.data.remote.api.NotificationApi
import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.notification.NotificationResponse
import kotlinx.coroutines.launch

class NotificationViewModel : PagedListViewModel<NotificationResponse>() {

    private val api = ApiClient.create(NotificationApi::class.java)

    override suspend fun fetch(page: Int, size: Int): ApiResult<PageResponse<NotificationResponse>> =
        safeApiCall { api.getNotifications(page = page, size = size) }

    /** 누른 알림 읽음 처리 (화면은 바로 바꾸고 서버에도 알림) */
    fun markRead(id: Long) {
        updateItems { items -> items.map { if (it.id == id) it.copy(isRead = true) else it } }
        viewModelScope.launch { safeApiCall { api.markRead(id) } }
    }

    fun markAllRead() {
        updateItems { items -> items.map { it.copy(isRead = true) } }
        viewModelScope.launch { safeApiCall { api.markAllRead() } }
    }
}
