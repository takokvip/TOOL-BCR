package com.example.myapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val buttonNext: Button = findViewById(R.id.buttonNext)
        buttonNext.setOnClickListener {
            // Chuyển sang ThirdActivity khi nhấn nút Next
            val intent = Intent(this, ThirdActivity::class.java)
            startActivity(intent)
        }
    }
}
