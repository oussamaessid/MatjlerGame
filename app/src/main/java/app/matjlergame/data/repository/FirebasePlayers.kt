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

class FirebaseRequestException(message: String, val status: String) : Exception(message)

/**
 * Firestore REST client without Firebase Auth, so it works on the free Spark plan.
 * Access is controlled by firebase/firestore.rules; reuses the game's HTTP/JSON stack.
 */
class FirebasePlayers(context: Context) {
    private val assets = context.assets
    private val packageName = context.packageName
    private val certificate by lazy { runCatching { signingSha1(context) }.getOrNull() }
    val configured = assets.list("")?.contains("google-services.json") == true
    private val config by lazy {
        if (!configured) throw FirebaseRequestException("MISSING_APP_CONFIGURATION", "FAILED_PRECONDITION")
        val json = JSONObject(assets.open("google-services.json").bufferedReader().use { it.readText() })
        val clients = json.getJSONArray("client")
        val client = (0 until clients.length()).map { clients.getJSONObject(it) }.firstOrNull {
            it.getJSONObject("client_info").getJSONObject("android_client_info").getString("package_name") == "app.matjlergame"
        } ?: throw FirebaseRequestException("MISSING_APP_CONFIGURATION", "FAILED_PRECONDITION")
        Pair(json.getJSONObject("project_info").getString("project_id"),
            client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
    }
    private val database get() = "projects/${config.first}/databases/(default)"

    private fun post(url: String, body: String): String {
        val connection = URL("$url?key=${URLEncoder.encode(config.second, "UTF-8")}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            // Lets an Android-restricted API key accept these REST calls.
            connection.setRequestProperty("X-Android-Package", packageName)
            certificate?.let { connection.setRequestProperty("X-Android-Cert", it) }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val error = runCatching { JSONArray(raw).getJSONObject(0) }.getOrNull()
                    ?: runCatching { JSONObject(raw) }.getOrNull()
                val status = error?.optJSONObject("error")?.optString("status")?.takeIf { it.isNotBlank() } ?: "UNAVAILABLE"
                throw FirebaseRequestException(error?.optJSONObject("error")?.optString("message")?.take(180) ?: "HTTP_$code", status)
            }
            return raw
        } finally { connection.disconnect() }
    }

    /** Atomically replaces each document and stamps updatedAt with the server time. */
    suspend fun write(documents: Map<String, JSONObject>) = withContext(Dispatchers.IO) {
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
    suspend fun query(parentPath: String, structuredQuery: JSONObject): List<JSONObject> = withContext(Dispatchers.IO) {
        val raw = post("https://firestore.googleapis.com/v1/$database/documents/$parentPath:runQuery",
            JSONObject().put("structuredQuery", structuredQuery).toString())
        val rows = JSONArray(raw)
        (0 until rows.length()).mapNotNull { rows.getJSONObject(it).optJSONObject("document") }
    }

    fun documentName(path: String) = "$database/documents/$path"

    private fun signingSha1(context: Context): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        else pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        val signature = signatures?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
    }
}
