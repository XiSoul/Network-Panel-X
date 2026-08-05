package com.example.networkpanelx

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

const val DEFAULT_CLOUD_API_URL = "http://39.98.88.224:50087"

data class CloudSession(
    val apiBaseUrl: String,
    val username: String,
    val token: String,
)

data class CloudTrafficStats(
    val period: String,
    val consumedBytes: Long,
    val taskCount: Long,
)

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val consumedBytes: Long,
    val taskCount: Long,
)

class CloudSessionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "cloud_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): CloudSession? {
        val apiBaseUrl = preferences.getString("api_base_url", "").orEmpty()
        val username = preferences.getString("username", "").orEmpty()
        val token = preferences.getString("token", "").orEmpty()
        return if (apiBaseUrl.isBlank() || username.isBlank() || token.isBlank()) null else {
            CloudSession(apiBaseUrl, username, token)
        }
    }

    fun save(session: CloudSession) {
        preferences.edit()
            .putString("api_base_url", session.apiBaseUrl)
            .putString("username", session.username)
            .putString("token", session.token)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}

object CloudApi {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun register(apiBaseUrl: String, username: String, password: String): CloudSession =
        authenticate(apiBaseUrl, "register", username, password)

    suspend fun login(apiBaseUrl: String, username: String, password: String): CloudSession =
        authenticate(apiBaseUrl, "login", username, password)

    suspend fun syncTraffic(session: CloudSession, consumedBytes: Long, taskCount: Long) {
        requestJson(
            session,
            "POST",
            "/v1/traffic/sync",
            JSONObject().put("consumedBytes", consumedBytes).put("taskCount", taskCount),
        )
    }

    suspend fun personalStats(session: CloudSession, period: String): CloudTrafficStats {
        val json = requestJson(session, "GET", "/v1/stats/me?period=$period")
        return CloudTrafficStats(
            period = json.optString("period", period),
            consumedBytes = json.optLong("consumedBytes"),
            taskCount = json.optLong("taskCount"),
        )
    }

    suspend fun leaderboard(session: CloudSession, period: String): List<LeaderboardEntry> {
        val json = requestJson(session, "GET", "/v1/leaderboard?period=$period")
        val entries = json.optJSONArray("entries") ?: JSONArray()
        return List(entries.length()) { index ->
            val entry = entries.getJSONObject(index)
            LeaderboardEntry(
                rank = entry.optInt("rank", index + 1),
                username = entry.optString("username"),
                consumedBytes = entry.optLong("consumedBytes"),
                taskCount = entry.optLong("taskCount"),
            )
        }
    }

    private suspend fun authenticate(apiBaseUrl: String, endpoint: String, username: String, password: String): CloudSession {
        val baseUrl = normalizeBaseUrl(apiBaseUrl)
        val json = requestJson(
            session = null,
            method = "POST",
            path = "/v1/auth/$endpoint",
            body = JSONObject().put("username", username).put("password", password),
            baseUrl = baseUrl,
        )
        val user = json.optJSONObject("user") ?: throw IllegalStateException("服务器未返回用户信息")
        val token = json.optString("token")
        if (token.isBlank()) throw IllegalStateException("服务器未返回登录令牌")
        return CloudSession(baseUrl, user.optString("username", username), token)
    }

    private suspend fun requestJson(
        session: CloudSession?,
        method: String,
        path: String,
        body: JSONObject? = null,
        baseUrl: String = session?.apiBaseUrl.orEmpty(),
    ): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${normalizeBaseUrl(baseUrl)}$path")
            .header("Accept", "application/json")
            .apply {
                session?.let { header("Authorization", "Bearer ${it.token}") }
                when (method) {
                    "POST" -> post((body ?: JSONObject()).toString().toRequestBody(jsonMediaType))
                    else -> get()
                }
            }
            .build()
        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(payload) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) throw IllegalStateException(json.optString("error", "服务器返回 ${response.code}"))
            json
        }
    }

    private fun normalizeBaseUrl(value: String): String {
        val url = value.trim().trimEnd('/')
        require(url.startsWith("https://") || url.startsWith("http://")) { "API 地址必须以 http:// 或 https:// 开头" }
        return url
    }
}

object WebDavBackup {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun upload(url: String, username: String, password: String, document: JSONObject) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.trim())
            .put(document.toString().toRequestBody(jsonMediaType))
            .header("Authorization", basicAuthorization(username, password))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("WebDAV 上传失败：${response.code}")
        }
    }

    suspend fun download(url: String, username: String, password: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url.trim()).header("Authorization", basicAuthorization(username, password)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("WebDAV 下载失败：${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun basicAuthorization(username: String, password: String): String {
        val encoded = Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encoded"
    }
}
