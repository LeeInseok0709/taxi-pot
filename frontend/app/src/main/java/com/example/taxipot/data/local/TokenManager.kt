package com.example.taxipot.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.tokenDataStore by preferencesDataStore(name = "auth_tokens")

/**
 * 액세스·리프레시 토큰 저장소.
 * 매 요청마다 DataStore를 읽지 않도록 메모리에도 들고 있는다.
 */
class TokenManager(context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
    }

    @Volatile private var cachedAccess: String? = null
    @Volatile private var cachedRefresh: String? = null

    val accessToken: String? get() = cachedAccess
    val refreshToken: String? get() = cachedRefresh

    /** 자동 로그인 판단용 */
    fun isLoggedIn(): Boolean = cachedRefresh != null

    /** 앱 시작 시 1회 */
    suspend fun load() {
        val prefs = appContext.tokenDataStore.data.first()
        cachedAccess = prefs[Keys.ACCESS]
        cachedRefresh = prefs[Keys.REFRESH]
    }

    /** 로그인·가입·재발급 성공 시 */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        cachedAccess = accessToken
        cachedRefresh = refreshToken
        appContext.tokenDataStore.edit {
            it[Keys.ACCESS] = accessToken
            it[Keys.REFRESH] = refreshToken
        }
    }

    /** 로그아웃·탈퇴·토큰 무효 시 */
    suspend fun clear() {
        cachedAccess = null
        cachedRefresh = null
        appContext.tokenDataStore.edit { it.clear() }
    }
}
