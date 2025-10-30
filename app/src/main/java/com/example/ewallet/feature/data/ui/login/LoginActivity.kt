package com.example.ewallet.feature.data.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.ewallet.R
import com.example.ewallet.feature.data.ui.otp.OtpActivity
import com.example.ewallet.databinding.ActivityMainBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.terms.text = HtmlCompat.fromHtml(
            getString(R.string.terms_html),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.terms.movementMethod = LinkMovementMethod.getInstance()
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, OtpActivity::class.java))
        }
    }
}