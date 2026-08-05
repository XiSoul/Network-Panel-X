package com.example.networkpanelx

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupProtocolTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun webDavCreatesListsAndRestoresVersionedBackup() = runBlocking {
        var uploadedPath = ""
        var uploadedJson = ""
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.method) {
                "PUT" -> {
                    uploadedPath = request.path.orEmpty()
                    uploadedJson = request.body.readUtf8()
                    MockResponse().setResponseCode(201)
                }

                "PROPFIND" -> MockResponse().setResponseCode(207)
                    .setHeader("Content-Type", "application/xml")
                    .setBody(
                        """<?xml version="1.0" encoding="utf-8"?>
                        <d:multistatus xmlns:d="DAV:">
                          <d:response><d:href>/dav/backups/network-panel-x-20260101-000000-000-old.json</d:href><d:propstat><d:prop><d:getlastmodified>Thu, 01 Jan 2026 00:00:00 GMT</d:getlastmodified><d:getcontentlength>100</d:getcontentlength></d:prop></d:propstat></d:response>
                          <d:response><d:href>$uploadedPath</d:href><d:propstat><d:prop><d:getlastmodified>Wed, 05 Aug 2026 03:00:00 GMT</d:getlastmodified><d:getcontentlength>${uploadedJson.length}</d:getcontentlength></d:prop></d:propstat></d:response>
                        </d:multistatus>""".trimIndent(),
                    )

                "GET" -> MockResponse().setResponseCode(200).setBody(uploadedJson)
                else -> MockResponse().setResponseCode(405)
            }
        }
        server.start()
        val directory = server.url("/dav/backups/").toString()
        val document = JSONObject().put("schemaVersion", 1).put("links", org.json.JSONArray())

        val uploaded = WebDavBackup.upload(directory, "user", "password", document)
        val versions = WebDavBackup.list(directory, "user", "password")
        val restored = WebDavBackup.download(uploaded.remoteId, "user", "password")

        assertTrue(uploaded.fileName.startsWith("network-panel-x-"))
        assertTrue(uploaded.fileName.endsWith(".json"))
        assertEquals(2, versions.size)
        assertEquals(uploaded.fileName, versions.first().fileName)
        assertEquals(1, restored.getInt("schemaVersion"))
    }

    @Test
    fun s3ListsAllPagesAndRestoresSelectedObject() = runBlocking {
        var uploadedKey = ""
        var uploadedJson = ""
        var continuationSeen = false
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                assertTrue(request.getHeader("Authorization").orEmpty().startsWith("AWS4-HMAC-SHA256"))
                val url = requireNotNull(request.requestUrl)
                return when {
                    request.method == "PUT" -> {
                        uploadedKey = request.path.orEmpty().substringAfter("/bucket/")
                        uploadedJson = request.body.readUtf8()
                        MockResponse().setResponseCode(200)
                    }

                    url.queryParameter("list-type") == "2" && url.queryParameter("continuation-token") == null -> {
                        MockResponse().setResponseCode(200).setBody(
                            """<ListBucketResult><IsTruncated>true</IsTruncated><Contents><Key>backups/network-panel-x-20260101-000000-000-old.json</Key><LastModified>2026-01-01T00:00:00Z</LastModified><Size>100</Size></Contents><NextContinuationToken>page-2</NextContinuationToken></ListBucketResult>""",
                        )
                    }

                    url.queryParameter("continuation-token") == "page-2" -> {
                        continuationSeen = true
                        MockResponse().setResponseCode(200).setBody(
                            """<ListBucketResult><IsTruncated>false</IsTruncated><Contents><Key>$uploadedKey</Key><LastModified>2026-08-05T03:00:00Z</LastModified><Size>${uploadedJson.length}</Size></Contents></ListBucketResult>""",
                        )
                    }

                    request.method == "GET" && request.path.orEmpty().substringBefore('?').endsWith(uploadedKey) -> {
                        MockResponse().setResponseCode(200).setBody(uploadedJson)
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
        val connection = BackupConnection(
            provider = "s3",
            s3Endpoint = server.url("/").toString(),
            s3Region = "us-east-1",
            s3Bucket = "bucket",
            s3AccessKey = "access-key",
            s3SecretKey = "secret-key",
            s3ObjectPrefix = "backups",
        )
        val document = JSONObject().put("schemaVersion", 1).put("links", org.json.JSONArray())

        val uploaded = S3Backup.upload(connection, document)
        val versions = S3Backup.list(connection)
        val restored = S3Backup.download(connection, uploaded.remoteId)

        assertTrue(continuationSeen)
        assertEquals(2, versions.size)
        assertEquals(uploaded.remoteId, versions.first().remoteId)
        assertEquals(1, restored.getInt("schemaVersion"))
    }
}
