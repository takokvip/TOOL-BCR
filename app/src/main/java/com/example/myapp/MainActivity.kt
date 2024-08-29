package com.example.myapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private var telegramLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kiểm tra kết nối internet
        if (!isInternetAvailable()) {
            showNoInternetDialog()
            return
        }

        // Lấy URL Telegram từ JSON
        fetchTelegramLink()

        // Kiểm tra và yêu cầu quyền nếu cần thiết
        checkAndRequestPermissions()

        val videoView: VideoView = findViewById(R.id.videoView)
        val uri: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.intro)
        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            // Bắt đầu phát video
            videoView.start()

            // Khi video kết thúc, chuyển sang SecondActivity
            mediaPlayer.setOnCompletionListener {
                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)
                finish()  // Để ngăn người dùng quay lại màn hình intro
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Lỗi kết nối")
            .setMessage("Không có mạng, vui lòng kết nối mạng để sử dụng")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()  // Đóng ứng dụng nếu không có mạng
            }
            .setCancelable(false)
            .show()
    }

    private fun fetchTelegramLink() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://lykorea.com/addtak/telegram.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(json)
                    telegramLink = jsonObject.getString("telegram")
                } else {
                    withContext(Dispatchers.Main) {
                        telegramLink = "Không thể lấy link Telegram"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    telegramLink = "Không thể lấy link Telegram"
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Kiểm tra từng quyền
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (!Settings.canDrawOverlays(this)) {
            // SYSTEM_ALERT_WINDOW được yêu cầu đặc biệt
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
        }

        // Nếu có quyền nào chưa được cấp, yêu cầu người dùng cấp quyền
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    // Xử lý kết quả yêu cầu quyền
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val deniedPermissions = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                // Hiển thị thông báo nếu quyền bị từ chối
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Yêu cầu cấp quyền")
            .setMessage("Yêu cầu cấp quyền để sử dụng. Hoặc liên hệ Telegram: $telegramLink")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()  // Đóng ứng dụng nếu không được cấp quyền
            }
            .setCancelable(false)
            .show()
    }
}
