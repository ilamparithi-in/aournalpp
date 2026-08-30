package dev.ilamparithi.aournalpp.backup.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val tokenType: String,
    val scope: String?,
    val userEmail: String? = null
)

/**
 * Manages Google OAuth2 Authorization Code Flow with PKCE (Proof Key for Code Exchange)
 * and automatic token refreshing.
 */
object GoogleOAuthManager {

    private const val TAG = "GoogleOAuthManager"

    // Default public client ID for Aournal++ Google Drive integration
    // Users can also supply their own client ID and client secret if desired.
    const val DEFAULT_CLIENT_ID = "619728257049-7m1a8u5km5bkg0t1qg4km1j79jflq00l.apps.googleusercontent.com"
    const val REDIRECT_URI = "dev.ilamparithi.aournalpp:/oauth2redirect"
    private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    private const val USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo"
    private const val SCOPES = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Transient memory for ongoing PKCE authorization request
    private var pendingCodeVerifier: String? = null
    private var pendingClientId: String? = null
    private var pendingClientSecret: String? = null

    /**
     * Starts the browser-based OAuth2 authorization code flow with PKCE.
     */
    fun startOAuthFlow(
        context: Context,
        customClientId: String? = null,
        customClientSecret: String? = null
    ) {
        val clientId = customClientId?.ifBlank { DEFAULT_CLIENT_ID } ?: DEFAULT_CLIENT_ID
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        pendingCodeVerifier = codeVerifier
        pendingClientId = clientId
        pendingClientSecret = customClientSecret

        val state = generateCodeVerifier().substring(0, 16)

        val authUri = Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        try {
            customTabsIntent.launchUrl(context, authUri)
        } catch (e: Exception) {
            Log.w(TAG, "Custom tabs launch failed, falling back to standard intent", e)
            val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Handles incoming redirect intent containing authorization code.
     */
    suspend fun handleRedirectUri(uri: Uri): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val code = uri.getQueryParameter("code") ?: error("No authorization code found in redirect URI")
            val verifier = pendingCodeVerifier ?: error("No pending PKCE code verifier found")
            val clientId = pendingClientId ?: DEFAULT_CLIENT_ID
            val clientSecret = pendingClientSecret

            val formBuilder = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", verifier)

            if (!clientSecret.isNullOrBlank()) {
                formBuilder.add("client_secret", clientSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty token response from Google")

            if (!response.isSuccessful) {
                error("Token exchange failed (${response.code}): $body")
            }

            val json = JSONObject(body)
            val accessToken = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", null)
            val expiresIn = json.optLong("expires_in", 3600L)
            val tokenType = json.optString("token_type", "Bearer")
            val scope = json.optString("scope", null)

            // Fetch user info email
            val email = fetchUserEmail(accessToken)

            pendingCodeVerifier = null
            pendingClientId = null
            pendingClientSecret = null

            OAuthTokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresInSeconds = expiresIn,
                tokenType = tokenType,
                scope = scope,
                userEmail = email
            )
        }
    }

    /**
     * Refreshes an expired access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(
        refreshToken: String,
        customClientId: String? = null,
        customClientSecret: String? = null
    ): Result<OAuthTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = customClientId?.ifBlank { DEFAULT_CLIENT_ID } ?: DEFAULT_CLIENT_ID

            val formBuilder = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)

            if (!customClientSecret.isNullOrBlank()) {
                formBuilder.add("client_secret", customClientSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty refresh response from Google")

            if (!response.isSuccessful) {
                error("Token refresh failed (${response.code}): $body")
            }

            val json = JSONObject(body)
            val newAccessToken = json.getString("access_token")
            val expiresIn = json.optLong("expires_in", 3600L)
            val newRefreshToken = json.optString("refresh_token", refreshToken)

            val email = fetchUserEmail(newAccessToken)

            OAuthTokenResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresInSeconds = expiresIn,
                tokenType = "Bearer",
                scope = null,
                userEmail = email
            )
        }
    }

    private fun fetchUserEmail(accessToken: String): String? {
        return try {
            val request = Request.Builder()
                .url(USERINFO_ENDPOINT)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                json.optString("email", null)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
