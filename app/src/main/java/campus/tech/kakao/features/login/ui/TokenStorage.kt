package campus.tech.kakao.features.login.ui

import android.content.Context
import android.content.SharedPreferences

object TokenStorage {
    private const val PREF_NAME = "token_storage"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun getSharedPreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Access Token 저장
    fun saveAccessToken(
        context: Context,
        accessToken: String,
    ) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.apply()
    }

    // Refresh Token 저장
    fun saveRefreshToken(
        context: Context,
        refreshToken: String?,
    ) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    // Access Token 가져오기
    fun getAccessToken(context: Context): String? = getSharedPreferences(context).getString(KEY_ACCESS_TOKEN, null)

    // Refresh Token 가져오기
    fun getRefreshToken(context: Context): String? = getSharedPreferences(context).getString(KEY_REFRESH_TOKEN, null)

    // 토큰 삭제
    fun clearTokens(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(KEY_ACCESS_TOKEN)
        editor.remove(KEY_REFRESH_TOKEN)
        editor.apply()
    }
}
