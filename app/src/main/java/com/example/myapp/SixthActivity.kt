package com.example.myapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SixthActivity : AppCompatActivity() {

    private lateinit var userGameEditText: EditText
    private lateinit var selectedImageTextView: TextView
    private lateinit var sendInfoButton: Button
    private lateinit var uploadImageButton: Button
    private var selectedImageUri: Uri? = null

    private val BOT_TOKEN = "7342100125:AAFjwvqQRjVnAj78b8Am3N4I77jHitIZqOQ"
    private val CHAT_ID = "-4594590913"
    private val UPLOAD_URL = "https://lykorea.com/catak/upload.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sixth)

        userGameEditText = findViewById(R.id.usernameInput)
        selectedImageTextView = findViewById(R.id.selectedImageTextView)
        sendInfoButton = findViewById(R.id.sendInfoButton)
        uploadImageButton = findViewById(R.id.uploadImageButton)

        uploadImageButton.setOnClickListener {
            openGallery()
        }

        sendInfoButton.setOnClickListener {
            val username = userGameEditText.text.toString()
            if (username.isNotEmpty() && selectedImageUri != null) {
                uploadImageAndSendToTelegram(username, selectedImageUri!!)
            } else {
                Toast.makeText(this, "Vui lòng nhập User trang Game và chọn ảnh", Toast.LENGTH_SHORT).show()
            }
        }
        // Sau khi mở Screen6, đợi 5 giây rồi đóng FloatingWidgetService
        Handler(Looper.getMainLooper()).postDelayed({
            stopService(Intent(this, FloatingWidgetService::class.java))
        }, 3000)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageTextView.text = selectedImageUri?.lastPathSegment
        }
    }

    private fun uploadImageAndSendToTelegram(username: String, imageUri: Uri) {
        val file = createFileFromUri(imageUri)
        val fileName = generateFileName()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SixthActivity, "Tải ảnh lên thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "")
                    if (json.has("error")) {
                        runOnUiThread {
                            Toast.makeText(this@SixthActivity, "Lỗi: ${json.getString("error")}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val imageUrl = json.getString("url")
                        sendPhotoToTelegram(username, imageUrl)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SixthActivity, "Tải ảnh lên thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun sendPhotoToTelegram(username: String, imageUrl: String) {
        val caption = "Username: $username"
        val telegramUrl = "https://api.telegram.org/bot$BOT_TOKEN/sendPhoto?chat_id=$CHAT_ID&photo=$imageUrl&caption=${Uri.encode(caption)}"

        val request = Request.Builder()
            .url(telegramUrl)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SixthActivity, "Gửi thông tin thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@SixthActivity, "Gửi thông tin thành công!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SixthActivity, "Gửi thông tin thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun createFileFromUri(uri: Uri): File {
        val fileName = generateFileName()
        val tempFile = File(cacheDir, fileName)
        tempFile.createNewFile()

        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun generateFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "IMG_${sdf.format(Date())}.jpg"
    }
}
