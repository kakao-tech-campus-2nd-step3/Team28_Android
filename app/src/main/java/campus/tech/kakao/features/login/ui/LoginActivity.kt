package campus.tech.kakao.features.login.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import campus.tech.kakao.BuildConfig
import campus.tech.kakao.databinding.ActivityLoginBinding
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authService: AuthorizationService
    private lateinit var getAuthResponse: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AuthorizationService 인스턴스 생성
        authService = AuthorizationService(this)

        // ActivityResult 등록 (여기서 수행)
        getAuthResponse =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val authResponse = AuthorizationResponse.fromIntent(result.data!!)
                    val authException = AuthorizationException.fromIntent(result.data!!)
                    if (authResponse != null) {
                        // 성공적으로 Authorization Code를 받음
                        handleAuthResponseIntent(result.data!!)
                        Log.d("testt", "Authorization Code: ${authResponse.authorizationCode}")
                    } else {
                        // 인증 실패
                        Log.e("testt", "Authorization failed: ${authException?.localizedMessage}")
                    }
                } else {
                    Log.e("testt", "Result canceled or data is null")
                }
            }

        // GitHub 로그인 버튼 클릭 리스너 설정
        binding.loginGithub.setOnClickListener {
            loginWithGithub()
        }
    }

    private fun loginWithGithub() {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        val openAuthPageIntent = authService.getAuthorizationRequestIntent(getAuthRequest(), customTabsIntent)
        getAuthResponse.launch(openAuthPageIntent)
    }

    private fun getAuthRequest(): AuthorizationRequest {
        val redirectUri = AuthConfig.CALLBACK_URL.toUri()
        return AuthorizationRequest
            .Builder(
                serviceConfiguration,
                AuthConfig.CLIENT_ID,
                AuthConfig.RESPONSE_TYPE,
                redirectUri,
            ).setScope(AuthConfig.SCOPE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // AuthorizationService가 더 이상 필요하지 않으면 해제
        authService.dispose()
    }

    companion object AuthConfig {
        const val AUTH_URI = "https://github.com/login/oauth/authorize"
        const val TOKEN_URI = "https://github.com/login/oauth/access_token"
        const val END_SESSION_URI = "https://github.com/logout"
        const val RESPONSE_TYPE = ResponseTypeValues.CODE
        const val SCOPE = "user,repo"
        const val CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
        const val CLIENT_SECRET = BuildConfig.GITHUB_CLIENT_SECRET
        const val CALLBACK_URL = "digitalcard://github.com/callback"
        const val LOGOUT_CALLBACK_URL = "digitalcard://github.com/logout_callback"
    }

    private val serviceConfiguration =
        AuthorizationServiceConfiguration(
            Uri.parse(AuthConfig.AUTH_URI),
            Uri.parse(AuthConfig.TOKEN_URI),
            null, // registration endpoint
            Uri.parse(AuthConfig.END_SESSION_URI),
        )

    private fun handleAuthResponseIntent(intent: Intent) {
        val exception = AuthorizationException.fromIntent(intent)
        val tokenExchangeRequest =
            AuthorizationResponse
                .fromIntent(intent)
                ?.createTokenExchangeRequest()

        when {
            exception != null -> onAuthCodeFailed(exception)
            tokenExchangeRequest != null -> onAuthCodeReceived(tokenExchangeRequest)
        }
    }

    private fun onAuthCodeReceived(tokenRequest: TokenRequest) {
        // Authorization Code를 Access Token으로 교환
        performTokenRequest(
            authService,
            tokenRequest,
            onComplete = {
                Log.d("testt", "Access token received: ${TokenStorage.getAccessToken(this)}")
                // Access Token을 사용하여 GitHub API 호출 가능
            },
            onError = {
                Log.e("testt", "Failed to get access token")
            },
        )
    }

    fun performTokenRequest(
        authService: AuthorizationService,
        tokenRequest: TokenRequest,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ) {
        authService.performTokenRequest(tokenRequest, getClientAuthentication()) { response, ex ->
            when {
                response != null -> {
                    // Access Token을 성공적으로 받음
                    TokenStorage.saveAccessToken(this, response.accessToken.orEmpty())
                    TokenStorage.saveRefreshToken(this, response.refreshToken.orEmpty())
                    onComplete()
                }
                else -> {
                    // 오류 처리
                    Log.e("testt", "Token request failed: ${ex?.localizedMessage}")
                    onError()
                }
            }
        }
    }

    private fun getClientAuthentication(): ClientAuthentication = ClientSecretPost(AuthConfig.CLIENT_SECRET)

    private fun onAuthCodeFailed(exception: AuthorizationException) {
        Log.e("testt", "Authorization Code request failed: ${exception.localizedMessage}")
    }
}
