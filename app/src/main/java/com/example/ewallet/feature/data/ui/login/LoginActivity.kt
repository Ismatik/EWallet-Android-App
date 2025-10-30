package com.example.ewallet.feature.data.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ewallet.R
import com.example.ewallet.core.di.ServiceLocator
import com.example.ewallet.databinding.ActivityMainBinding
import com.example.ewallet.feature.data.ui.otp.OtpActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isFormattingPhone = false

    private val loginViewModel: LoginViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel(ServiceLocator.sendCodeUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.terms.text = HtmlCompat.fromHtml(
            getString(R.string.terms_html),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.terms.movementMethod = LinkMovementMethod.getInstance()

        binding.phone.addTextChangedListener(phoneTextWatcher)
        binding.phone.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                loginViewModel.sendCode()
                true
            } else {
                false
            }
        }
        binding.btnLogin.setOnClickListener {
            loginViewModel.sendCode()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.uiState.collect { state ->
                    binding.btnLogin.isEnabled = state.isValid && !state.isLoading
                    binding.btnLogin.text = if (state.isLoading) {
                        getString(R.string.login_sending)
                    } else {
                        getString(R.string.continue_label)
                    }
                    binding.phone.isEnabled = !state.isLoading
                    binding.phoneTil.isErrorEnabled = !state.error.isNullOrBlank()
                    binding.phoneTil.error = state.error
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.events.collect { event ->
                    when (event) {
                        is LoginViewModel.Event.CodeSent -> {
                            startActivity(OtpActivity.createIntent(this@LoginActivity, event.phone))
                        }

                        is LoginViewModel.Event.Error -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private val phoneTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (isFormattingPhone) return

            val digitsRaw = s?.toString().orEmpty().filter { it.isDigit() }
            val digits = digitsRaw.take(MAX_PHONE_LENGTH)
            val formatted = formatPhoneInput(digits)

            if (formatted != s?.toString().orEmpty() || digitsRaw.length != digits.length) {
                isFormattingPhone = true
                binding.phone.setText(formatted)
                binding.phone.setSelection(formatted.length)
                isFormattingPhone = false
            }

            loginViewModel.onPhoneChanged(digits)
        }
    }

    private fun formatPhoneInput(digits: String): String {
        if (digits.isEmpty()) return ""

        val groups = intArrayOf(3, 2, 2, 2)
        val builder = StringBuilder()
        var index = 0
        for (group in groups) {
            if (index >= digits.length) break
            val end = (index + group).coerceAtMost(digits.length)
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(digits.substring(index, end))
            index = end
        }
        if (index < digits.length) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(digits.substring(index))
        }
        return builder.toString()
    }
    companion object {
        private const val MAX_PHONE_LENGTH = 9
    }
}
