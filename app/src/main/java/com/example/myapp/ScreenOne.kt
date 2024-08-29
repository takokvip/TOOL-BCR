package com.example.myapp

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import androidx.core.content.FileProvider
import androidx.appcompat.app.AlertDialog
import android.content.pm.PackageManager
import android.view.WindowManager

class ScreenOneActivity : AppCompatActivity() {

    private lateinit var checkStatusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_one)

        checkStatusTextView = findViewById(R.id.checkStatusTextView)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        // Giữ màn hình luôn sáng
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        performChecks()
    }

    private fun performChecks() {
        Handler(Looper.getMainLooper()).postDelayed({
            checkForUpdate()
        }, 2000)
    }

    private fun checkForUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://lykorea.com/cw_update/version.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(json)

                    val latestVersionCode = jsonObject.getInt("version_code")
                    val apkUrl = jsonObject.getString("apk_url")
                    val currentVersionCode = getCurrentVersionCode()

                    withContext(Dispatchers.Main) {
                        if (latestVersionCode > currentVersionCode) {
                            promptUpdate(apkUrl)
                        } else {
                            checkStatusTextView.text = "Phiên bản TOOL hiện tại mới nhất."
                            startProgressBar()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        checkStatusTextView.text = "Không thể kết nối đến server để kiểm tra phiên bản."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScreenOneActivity, "Có lỗi xảy ra: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun promptUpdate(apkUrl: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cập nhật mới")
        builder.setMessage("Phiên bản mới đã sẵn sàng, bạn có muốn tải về và cài đặt ngay không?")
        builder.setPositiveButton("Có") { dialog, _ ->
            dialog.dismiss()
            downloadAndInstallApk(apkUrl)
        }
        builder.setNegativeButton("Không") { dialog, _ ->
            dialog.dismiss()
            startProgressBar() // Tiếp tục nếu người dùng từ chối cập nhật
        }
        builder.show()
    }

    private fun downloadAndInstallApk(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Tool Tak Update")
            .setDescription("Đang tải phiên bản mới")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Tool_Tak_Update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val query = DownloadManager.Query().setFilterById(downloadId)
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(object : Runnable {
            override fun run() {
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusColumnIndex != -1) {
                        val status = cursor.getInt(statusColumnIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriColumnIndex != -1) {
                                val uriString = cursor.getString(uriColumnIndex)
                                installApk(Uri.parse(uriString))
                            }
                        } else {
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
                cursor?.close()
            }
        }, 1000)
    }

    private fun installApk(uri: Uri) {
        val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Tool_Tak_Update.apk")
        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
        finish()
    }

    private fun getCurrentVersionCode(): Int {
        return packageManager.getPackageInfo(packageName, 0).versionCode
    }

    private fun startProgressBar() {
        progressBar.progress = 0

        Handler(Looper.getMainLooper()).postDelayed({
            updateProgress(1, 36) {
                Handler(Looper.getMainLooper()).postDelayed({
                    updateProgress(36, 78) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateProgress(78, 100) {
                                val intent = Intent(this@ScreenOneActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }, 5000)
                    }
                }, 5000)
            }
        }, 5000)
    }

    private fun updateProgress(start: Int, end: Int, onComplete: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        var progress = start
        val updateInterval = 100L

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (progress <= end) {
                    progressBar.progress = progress
                    progressText.text = "Đang kiểm tra... $progress%"
                    progress++
                    handler.postDelayed(this, updateInterval)
                } else {
                    onComplete()
                }
            }
        }, updateInterval)
    }
}
