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
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
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

data class BackupConnection(
    val provider: String = "webdav",
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val s3Endpoint: String = "",
    val s3Region: String = "us-east-1",
    val s3Bucket: String = "",
    val s3AccessKey: String = "",
    val s3SecretKey: String = "",
    val s3ObjectPath: String = "network-panel-x/backup.json",
)

class BackupConnectionStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "backup_connection",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): BackupConnection = BackupConnection(
        provider = preferences.getString("provider", "webdav").orEmpty(),
        webDavUrl = preferences.getString("webdav_url", "").orEmpty(),
        webDavUsername = preferences.getString("webdav_username", "").orEmpty(),
        webDavPassword = preferences.getString("webdav_password", "").orEmpty(),
        s3Endpoint = preferences.getString("s3_endpoint", "").orEmpty(),
        s3Region = preferences.getString("s3_region", "us-east-1").orEmpty(),
        s3Bucket = preferences.getString("s3_bucket", "").orEmpty(),
        s3AccessKey = preferences.getString("s3_access_key", "").orEmpty(),
        s3SecretKey = preferences.getString("s3_secret_key", "").orEmpty(),
        s3ObjectPath = preferences.getString("s3_object_path", "network-panel-x/backup.json").orEmpty(),
    )

    fun save(connection: BackupConnection) {
        preferences.edit()
            .putString("provider", connection.provider)
            .putString("webdav_url", connection.webDavUrl)
            .putString("webdav_username", connection.webDavUsername)
            .putString("webdav_password", connection.webDavPassword)
            .putString("s3_endpoint", connection.s3Endpoint)
            .putString("s3_region", connection.s3Region)
            .putString("s3_bucket", connection.s3Bucket)
            .putString("s3_access_key", connection.s3AccessKey)
            .putString("s3_secret_key", connection.s3SecretKey)
            .putString("s3_object_path", connection.s3ObjectPath)
            .apply()
    }
}

object S3Backup {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

    suspend fun upload(connection: BackupConnection, document: JSONObject) = request(connection, "PUT", document.toString().toByteArray(Charsets.UTF_8))

    suspend fun download(connection: BackupConnection): JSONObject = JSONObject(request(connection, "GET", null))

    private suspend fun request(connection: BackupConnection, method: String, body: ByteArray?): String = withContext(Dispatchers.IO) {
        val endpoint = URI(connection.s3Endpoint.trim().trimEnd('/'))
        require(endpoint.scheme == "http" || endpoint.scheme == "https") { "S3 Endpoint 必须以 http:// 或 https:// 开头" }
        require(endpoint.host.isNullOrBlank().not()) { "S3 Endpoint 无效" }
        require(connection.s3Region.isNotBlank() && connection.s3Bucket.isNotBlank()) { "请填写 S3 区域和存储桶" }
        require(connection.s3AccessKey.isNotBlank() && connection.s3SecretKey.isNotBlank()) { "请填写 S3 Access Key 和 Secret Key" }
        val objectPath = connection.s3ObjectPath.trim().trim('/').ifBlank { "network-panel-x/backup.json" }
        val basePath = endpoint.rawPath.orEmpty().trimEnd('/')
        val encodedPath = "/${percentEncode(connection.s3Bucket)}/${objectPath.split('/').filter { it.isNotBlank() }.joinToString("/") { percentEncode(it) }}"
        val canonicalUri = if (basePath.isBlank()) encodedPath else "$basePath$encodedPath"
        val host = endpoint.host + if (endpoint.port > 0) ":${endpoint.port}" else ""
        val now = Instant.now()
        val amzDate = timestampFormatter.format(now)
        val shortDate = dateFormatter.format(now)
        val payload = body ?: ByteArray(0)
        val payloadHash = sha256Hex(payload)
        val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
        val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
        val canonicalRequest = "$method\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val scope = "$shortDate/${connection.s3Region}/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$scope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"
        val signingKey = hmac(hmac(hmac(hmac("AWS4${connection.s3SecretKey}".toByteArray(Charsets.UTF_8), shortDate), connection.s3Region), "s3"), "aws4_request")
        val signature = hmacHex(signingKey, stringToSign)
        val url = "${endpoint.scheme}://$host$canonicalUri"
        val requestBuilder = Request.Builder().url(url)
            .header("Host", host)
            .header("x-amz-date", amzDate)
            .header("x-amz-content-sha256", payloadHash)
            .header("Authorization", "AWS4-HMAC-SHA256 Credential=${connection.s3AccessKey}/$scope, SignedHeaders=$signedHeaders, Signature=$signature")
        if (method == "PUT") requestBuilder.put(payload.toRequestBody("application/json; charset=utf-8".toMediaType())) else requestBuilder.get()
        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("S3 ${if (method == "PUT") "上传" else "下载"}失败：${response.code}")
            responseBody
        }
    }

    private fun percentEncode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val char = byte.toInt().and(0xff).toChar()
            if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '-' || char == '_' || char == '.' || char == '~') append(char)
            else append("%${byte.toInt().and(0xff).toString(16).uppercase().padStart(2, '0')}")
        }
    }

    private fun sha256Hex(value: ByteArray): String = value.toSha256().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun ByteArray.toSha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)
    private fun hmac(key: ByteArray, value: String): ByteArray = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(value.toByteArray(Charsets.UTF_8))
    private fun hmacHex(key: ByteArray, value: String): String = hmac(key, value).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
