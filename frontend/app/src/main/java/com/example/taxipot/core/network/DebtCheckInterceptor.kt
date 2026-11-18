package com.example.taxipot.core.network

import com.example.taxipot.core.event.AppEvent
import com.example.taxipot.core.event.AppEventBus
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response

/** 409 UNPAID_DEBT_EXISTS는 어느 API에서든 올 수 있어서 여기서 한 번에 감지한다 */
class DebtCheckInterceptor(private val gson: Gson) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 409 && response.peekErrorCode(gson) == "UNPAID_DEBT_EXISTS") {
            AppEventBus.emit(AppEvent.UnpaidDebt)
        }
        return response
    }
}
