package com.example.taxipot.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 어느 화면에서든 받아야 하는 공통 이벤트 */
sealed interface AppEvent {
    /** 토큰 무효(TOKEN_REVOKED)·재발급 실패 → 로그인 화면으로 */
    data object ForceLogout : AppEvent

    /** 409 UNPAID_DEBT_EXISTS → 미수금 화면으로 */
    data object UnpaidDebt : AppEvent
}

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
