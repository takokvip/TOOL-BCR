package com.example.myapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class FourthActivity : AppCompatActivity() {

    private var telegramLink: String? = null
    private var userList: JSONArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fourth)

        val inputUsername: EditText = findViewById(R.id.inputUsername)
        val inputPassword: EditText = findViewById(R.id.inputPassword)
        val buttonLogin: Button = findViewById(R.id.buttonLogin)
        val buttonContactTelegram: Button = findViewById(R.id.buttonContactTelegram)

        // Lấy link Telegram và danh sách user từ JSON
        fetchUserList()

        // Sử dụng TelegramLinkHelper để lấy link Telegram
        val telegramLinkHelper = TelegramLinkHelper()
        telegramLinkHelper.fetchTelegramLink { link ->
            telegramLink = link
        }

        // Xử lý sự kiện cho nút LOGIN
        buttonLogin.setOnClickListener {
            val username = inputUsername.text.toString()
            val password = inputPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (checkCredentials(username, password)) {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                    // Pass the username to Screen 5
                    val intent = Intent(this, FifthActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Sai username hoặc password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập Username và Password", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý sự kiện cho nút Liên Hệ Telegram
        buttonContactTelegram.setOnClickListener {
            telegramLink?.let { link ->
                val username = extractUsernameFromLink(link)
                if (username != null) {
                    openTelegram(username)
                } else {
                    Toast.makeText(this, "Không thể lấy username từ link Telegram", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Không thể lấy link Telegram", Toast.LENGTH_SHORT).show()
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

    private fun fetchUserList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://lykorea.com/addtak/user.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(json)
                    userList = jsonObject.getJSONArray("users")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FourthActivity, "Lỗi mạng: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FourthActivity, "Không thể lấy danh sách người dùng: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkCredentials(username: String, password: String): Boolean {
        userList?.let { users ->
            for (i in 0 until users.length()) {
                val user = users.getJSONObject(i)
                if (user.getString("username") == username && user.getString("password") == password) {
                    return true
                }
            }
        }
        return false
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
}

// Class mới cho việc lấy link Telegram
class TelegramLinkHelper {

    fun fetchTelegramLink(onLinkFetched: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var telegramLink: String? = null
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onLinkFetched(telegramLink)
                }
            }
        }
    }
}
