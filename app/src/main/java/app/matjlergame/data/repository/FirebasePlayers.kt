package app.matjlergame.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

class FirebaseRequestException(message: String, val status: String, val reason: String? = null) : Exception(message)

/** Transport contract also allows testing network failures without changing the real backend. */
interface PlayerCloud {
    val configured: Boolean
    suspend fun write(documents: Map<String, JSONObject>)
    suspend fun query(parentPath: String, structuredQuery: JSONObject): List<JSONObject>
    /** Number of documents matching the query, without downloading them. */
    suspend fun count(parentPath: String, structuredQuery: JSONObject): Long = query(parentPath, structuredQuery).size.toLong()
    /** One document by path, or null when it does not exist. */
    suspend fun read(path: String): JSONObject? = null
    /** Full Firestore name of a document, as used by cursors and __name__ filters. */
    fun documentName(path: String): String = path
}

/**
 * Firestore REST client without Firebase Auth, so it works on the free Spark plan.
 * Access is controlled by firebase/firestore.rules; reuses the game's HTTP/JSON stack.
 */
class FirebasePlayers(context: Context) : PlayerCloud {
    private val assets = context.assets
    private val packageName = context.packageName
    private val certificate by lazy { runCatching { signingSha1(context) }.getOrNull() }
    private val hasAsset = assets.list("")?.contains("google-services.json") == true
    // Built-in config: builds from a fresh checkout (google-services.json is git-ignored) keep the leaderboard.
    override val configured = true
    private val config by lazy {
        if (!hasAsset) return@lazy Pair(PROJECT_ID, API_KEY)
        val json = JSONObject(assets.open("google-services.json").bufferedReader().use { it.readText() })
        val clients = json.getJSONArray("client")
        val client = (0 until clients.length()).map { clients.getJSONObject(it) }.firstOrNull {
            it.getJSONObject("client_info").getJSONObject("android_client_info").getString("package_name") == "app.matjlergame"
        } ?: throw FirebaseRequestException("MISSING_APP_CONFIGURATION", "FAILED_PRECONDITION")
        Pair(json.getJSONObject("project_info").getString("project_id"),
            client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
    }
    private val database get() = "projects/${config.first}/databases/(default)"

    /** body = null sends a GET; a missing document then returns null instead of throwing. */
    private fun request(url: String, body: String?): String? {
        val connection = URL("$url?key=${URLEncoder.encode(config.second, "UTF-8")}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            // Lets an Android-restricted API key accept these REST calls.
            connection.setRequestProperty("X-Android-Package", packageName)
            certificate?.let { connection.setRequestProperty("X-Android-Cert", it) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (body == null && code == 404) return null
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val error = runCatching { JSONArray(raw).getJSONObject(0) }.getOrNull()
                    ?: runCatching { JSONObject(raw) }.getOrNull()
                val status = error?.optJSONObject("error")?.optString("status")?.takeIf { it.isNotBlank() } ?: "UNAVAILABLE"
                val payload = error?.optJSONObject("error")
                val details = payload?.optJSONArray("details")
                val reason = (0 until (details?.length() ?: 0)).mapNotNull {
                    details?.optJSONObject(it)?.optString("reason")?.takeIf { value -> value.isNotBlank() }
                }.firstOrNull()
                android.util.Log.w("FirebasePlayers", "HTTP $code $status reason=${reason ?: "unspecified"}")
                throw FirebaseRequestException(payload?.optString("message")?.take(180) ?: "HTTP_$code", status, reason)
            }
            return raw
        } finally { connection.disconnect() }
    }
    private fun post(url: String, body: String): String = request(url, body)!!

    /** Atomically replaces each document and stamps updatedAt with the server time. */
    override suspend fun write(documents: Map<String, JSONObject>) = withContext(Dispatchers.IO) {
        val writes = JSONArray()
        documents.forEach { (path, fields) ->
            writes.put(JSONObject()
                .put("update", JSONObject().put("name", "$database/documents/$path").put("fields", fields))
                .put("updateTransforms", JSONArray().put(JSONObject()
                    .put("fieldPath", "updatedAt").put("setToServerValue", "REQUEST_TIME"))))
        }
        post("https://firestore.googleapis.com/v1/$database/documents:commit", JSONObject().put("writes", writes).toString())
        Unit
    }

    /** Runs a structured query on parentPath's subcollection and returns the documents found. */
    override suspend fun query(parentPath: String, structuredQuery: JSONObject): List<JSONObject> = withContext(Dispatchers.IO) {
        val raw = post("https://firestore.googleapis.com/v1/$database/documents/$parentPath:runQuery",
            JSONObject().put("structuredQuery", structuredQuery).toString())
        val rows = JSONArray(raw)
        (0 until rows.length()).mapNotNull { rows.getJSONObject(it).optJSONObject("document") }
    }

    /** COUNT aggregation: billed one read per 1000 matching documents. */
    override suspend fun count(parentPath: String, structuredQuery: JSONObject): Long = withContext(Dispatchers.IO) {
        val raw = post("https://firestore.googleapis.com/v1/$database/documents/$parentPath:runAggregationQuery",
            JSONObject().put("structuredAggregationQuery", JSONObject().put("structuredQuery", structuredQuery)
                .put("aggregations", JSONArray().put(JSONObject().put("alias", "n").put("count", JSONObject())))).toString())
        JSONArray(raw).getJSONObject(0).getJSONObject("result").getJSONObject("aggregateFields")
            .getJSONObject("n").getString("integerValue").toLong()
    }

    override suspend fun read(path: String): JSONObject? = withContext(Dispatchers.IO) {
        request("https://firestore.googleapis.com/v1/$database/documents/$path", null)?.let { JSONObject(it) }
    }

    override fun documentName(path: String) = "$database/documents/$path"

    private fun signingSha1(context: Context): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        else pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        val signature = signatures?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
    }

    private companion object {
        // Same values as app/google-services.json (project equal-to-81b8f); public in every APK anyway.
        const val PROJECT_ID = "equal-to-81b8f"
        const val API_KEY = "AIzaSyAG6PSBr1eUll_VBKLgiBgzTGtPGk8Nf0g"
    }
}
