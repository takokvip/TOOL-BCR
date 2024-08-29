package com.example.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class FifthActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var deviceNameTextView: TextView
    private lateinit var imeiTextView: TextView
    private lateinit var androidVersionTextView: TextView
    private lateinit var startHackButton: Button
    private lateinit var progressBar: ProgressBar

    private val overlayPermissionCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fifth)

        // Nhận dữ liệu từ Intent
        val username = intent.getStringExtra("username")

        greetingTextView = findViewById(R.id.greetingTextView)
        logoutButton = findViewById(R.id.logoutButton)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
        imeiTextView = findViewById(R.id.imeiTextView)
        androidVersionTextView = findViewById(R.id.androidVersionTextView)
        startHackButton = findViewById(R.id.startHackButton)
        progressBar = findViewById(R.id.progressBar)

        // Hiển thị lời chào
        greetingTextView.text = "Xin chào, $username"

        // Xử lý sự kiện nút Thoát
        logoutButton.setOnClickListener {
            val intent = Intent(this, FourthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Hiển thị thông tin thiết bị
        displayDeviceInfo()

        // Xử lý sự kiện khi nhấn nút Start Hack
        startHackButton.setOnClickListener {
            startHackProcess()
        }
    }

    private fun displayDeviceInfo() {
        // Hiển thị tên thiết bị
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        deviceNameTextView.text = "Tên máy: $deviceName"
        deviceNameTextView.setTextColor(ContextCompat.getColor(this, R.color.custom_green))

        // Hiển thị phiên bản Android
        val androidVersion = "Android ${Build.VERSION.RELEASE}"
        androidVersionTextView.text = "Phiên bản Android: $androidVersion"
        androidVersionTextView.setTextColor(ContextCompat.getColor(this, R.color.custom_green))

        // Hiển thị Android ID
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        imeiTextView.text = "Android ID: $androidId"
        imeiTextView.setTextColor(ContextCompat.getColor(this, R.color.custom_green))
    }

    private fun startHackProcess() {
        progressBar.visibility = ProgressBar.VISIBLE
        startHackButton.isEnabled = false

        // Sử dụng Handler để xử lý quá trình hack với các đoạn dừng ngẫu nhiên
        Handler(Looper.getMainLooper()).postDelayed({
            updateProgress(1, 36) {
                Handler(Looper.getMainLooper()).postDelayed({
                    updateProgress(36, 78) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            updateProgress(78, 100) {
                                showHackSuccessDialog()
                            }
                        }, (3000..5000).random().toLong()) // Dừng ngẫu nhiên 3-5 giây
                    }
                }, (3000..5000).random().toLong()) // Dừng ngẫu nhiên 3-5 giây
            }
        }, (3000..5000).random().toLong()) // Dừng ngẫu nhiên 3-5 giây
    }

    private fun updateProgress(start: Int, end: Int, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            for (i in start..end) {
                delay(50)
                progressBar.progress = i
            }
            onComplete()
        }
    }

    private fun showHackSuccessDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("HACK THÀNH CÔNG")
        builder.setMessage("Nhấn OK để tiếp tục")
        builder.setPositiveButton("OK") { dialog, _ ->
            if (Settings.canDrawOverlays(this)) {
                startFloatingWidgetService()
            } else {
                requestOverlayPermission()
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun startFloatingWidgetService() {
        val intent = Intent(this, FloatingWidgetService::class.java)
        startService(intent)

        // Quay về màn hình chính
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)

        // Sau 7 giây, mở lại app và chuyển sang Screen 6
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            val screen6Intent = Intent(this@FifthActivity, SixthActivity::class.java)
            startActivity(screen6Intent)
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, overlayPermissionCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            displayDeviceInfo()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayPermissionCode) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingWidgetService()
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền hệ thống cho ứng dụng để HACK", Toast.LENGTH_LONG).show()
            }
        }
    }
}
