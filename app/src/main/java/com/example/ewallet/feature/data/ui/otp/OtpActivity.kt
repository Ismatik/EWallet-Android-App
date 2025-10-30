package com.example.ewallet.feature.data.ui.otp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ewallet.databinding.ActivityNextBinding

class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackToActivityMain.setOnClickListener {
            finish()
        }

    }

}