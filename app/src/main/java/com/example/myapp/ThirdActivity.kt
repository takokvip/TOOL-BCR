package com.example.myapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class ThirdActivity : AppCompatActivity() {

    private var telegramLink: String? = null
    private var urlLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)

        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val verifyButton: Button = findViewById(R.id.verifyButton)
        val contactTelegramButton: Button = findViewById(R.id.contactTelegramButton)
        val linkUrlButton: Button = findViewById(R.id.LinkUrlButton)
        val buttonBack: Button = findViewById(R.id.buttonBack)

        // Lấy link Telegram và URL từ JSON
        fetchTelegramLink()
        fetchUrlLink()

        // Xử lý sự kiện cho nút Xác Minh Tài Khoản
        verifyButton.setOnClickListener {
            val username = usernameInput.text.toString()
            if (username.isNotEmpty()) {
                // Hiển thị ProgressDialog
                val progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Đang xác minh tài khoản: $username...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                // Giả lập tiến trình 1-100%
                CoroutineScope(Dispatchers.Main).launch {
                    for (i in 1..100) {
                        progressDialog.setMessage("Đang xác minh tài khoản: $username... $i%")
                        delay(30) // Giả lập thời gian thực thi
                    }
                    progressDialog.dismiss()

                    // Sau khi hoàn tất, chuyển sang Screen 4
                    val intent = Intent(this@ThirdActivity, FourthActivity::class.java)
                    startActivity(intent)
                }
            } else {
                // Hiển thị dialog lỗi nếu không có username
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Lỗi")
                    .setMessage("Vui lòng nhập Username")
                    .setPositiveButton("OK", null)
                    .create()
                dialog.show()
            }
        }

        // Xử lý sự kiện cho nút Liên Hệ Telegram
        contactTelegramButton.setOnClickListener {
            telegramLink?.let { link ->
                val username = extractUsernameFromLink(link)
                if (username != null) {
                    openTelegram(username)
                } else {
                    Toast.makeText(this, "Không thể lấy username từ link Telegram", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Không thể lấy link Telegram", Toast.LENGTH_SHORT).show()
        }

        // Xử lý sự kiện cho nút Back để quay lại màn hình trước đó
        buttonBack.setOnClickListener {
            finish() // Quay trở về màn hình trước đó (SecondActivity)
        }

        // Xử lý sự kiện cho nút Link URL
        linkUrlButton.setOnClickListener {
            urlLink?.let { link ->
                openUrl(link)
            } ?: Toast.makeText(this, "Không thể lấy URL", Toast.LENGTH_SHORT).show()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard(v)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: android.view.View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showErrorDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Lỗi")
        builder.setMessage("Vui lòng nhập Username")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        // Tô màu thông báo lỗi
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.holo_red_dark))
    }

    private fun showProgressDialog(username: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Xác minh tài khoản")
        progressDialog.setMessage("Đang xác minh $username...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.progress = 0
        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            for (i in 1..100) {
                delay(50)  // Thời gian giữa các bước tiến (0.05 giây)
                progressDialog.progress = i
            }
            progressDialog.dismiss()
            Toast.makeText(this@ThirdActivity, "Xác minh thành công!", Toast.LENGTH_SHORT).show()
        }
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
                        Toast.makeText(this@ThirdActivity, "Lỗi mạng: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ThirdActivity, "Không thể lấy link Telegram: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchUrlLink() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://lykorea.com/addtak/url.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(json)
                    urlLink = jsonObject.getString("url")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ThirdActivity, "Lỗi mạng: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ThirdActivity, "Không thể lấy URL: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun extractUsernameFromLink(link: String): String? {
        val pattern = Pattern.compile("https://t.me/([a-zA-Z0-9_]+)")
        val matcher = pattern.matcher(link)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    private fun openTelegram(username: String) {
        val deeplink = "tg://resolve?domain=$username"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplink))

        // Kiểm tra nếu ứng dụng Telegram được cài đặt
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Nếu không có Telegram, mở link trong trình duyệt
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))
            startActivity(webIntent)
        }
    }

    private fun openUrl(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}
