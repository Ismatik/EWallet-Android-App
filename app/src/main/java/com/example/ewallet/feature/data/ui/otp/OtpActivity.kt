package com.example.ewallet.feature.data.ui.otp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ewallet.R
import com.example.ewallet.core.di.ServiceLocator
import com.example.ewallet.databinding.ActivityNextBinding
import kotlinx.coroutines.launch
import java.util.Locale

class OtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNextBinding
    private lateinit var otpInputs: List<EditText>
    private var isUpdatingInputs = false

    private val phone: String by lazy(LazyThreadSafetyMode.NONE) {
        intent.getStringExtra(EXTRA_PHONE).orEmpty()
    }

    private val viewModel: OtpViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OtpViewModel::class.java)) {
                    return OtpViewModel(
                        phone = phone,
                        sendCode = ServiceLocator.sendCodeUseCase,
                        verifyCode = ServiceLocator.verifyCodeUseCase
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (phone.isBlank()) {
            finish()
            return
        }

        binding = ActivityNextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otpInputs = listOf(
            binding.otp1,
            binding.otp2,
            binding.otp3,
            binding.otp4,
            binding.otp5,
            binding.otp6
        )

        binding.tvSubtitle.text = getString(R.string.verify_subtitle, formatPhoneForDisplay(phone))

        binding.btnBackToActivityMain.setOnClickListener { finish() }
        binding.btnVerify.setOnClickListener {
            hideKeyboard()
            viewModel.submit()
        }
        binding.tvResend.setOnClickListener { viewModel.resendCode() }

        setupOtpInputs()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    syncInputsWithState(state.code)
                    binding.btnVerify.isEnabled = state.isComplete && !state.isLoading
                    binding.btnVerify.text = if (state.isLoading) {
                        getString(R.string.verify_button_loading)
                    } else {
                        getString(R.string.verify_button)
                    }
                    binding.tvError.isVisible = !state.error.isNullOrBlank()
                    binding.tvError.text = state.error
                    updateResendState(state)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is OtpViewModel.Event.Verified -> {
                            Toast.makeText(
                                this@OtpActivity,
                                getString(R.string.verify_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            hideKeyboard()
                            setResult(RESULT_OK)
                            finish()
                        }

                        is OtpViewModel.Event.CodeResent -> {
                            Toast.makeText(
                                this@OtpActivity,
                                getString(R.string.otp_resend_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupOtpInputs() {
        otpInputs.forEachIndexed { index, editText ->
            editText.doAfterTextChanged { text ->
                if (isUpdatingInputs) return@doAfterTextChanged

                val value = text?.toString().orEmpty()
                if (value.length > 1) {
                    val lastChar = value.last().toString()
                    isUpdatingInputs = true
                    editText.setText(lastChar)
                    editText.setSelection(lastChar.length)
                    isUpdatingInputs = false
                }

                if (value.isNotEmpty() && index < otpInputs.lastIndex) {
                    otpInputs[index + 1].requestFocus()
                }

                dispatchCodeChanged()
            }

            editText.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && editText.text.isNullOrEmpty() && index > 0) {
                    val previous = otpInputs[index - 1]
                    isUpdatingInputs = true
                    previous.setText("")
                    isUpdatingInputs = false
                    previous.requestFocus()
                    previous.setSelection(previous.text?.length ?: 0)
                    dispatchCodeChanged()
                    true
                } else {
                    false
                }
            }
        }

        otpInputs.last().setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (viewModel.uiState.value.isComplete) {
                    hideKeyboard()
                    viewModel.submit()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun syncInputsWithState(code: String) {
        val current = otpInputs.joinToString(separator = "") { it.text?.toString().orEmpty() }
        if (current == code) return

        isUpdatingInputs = true
        otpInputs.forEachIndexed { index, editText ->
            val char = code.getOrNull(index)?.toString() ?: ""
            editText.setText(char)
            editText.setSelection(editText.text?.length ?: 0)
        }
        isUpdatingInputs = false

        val nextIndex = code.length.coerceAtMost(otpInputs.lastIndex)
        otpInputs[nextIndex].requestFocus()
    }

    private fun dispatchCodeChanged() {
        if (isUpdatingInputs) return
        val code = otpInputs.joinToString(separator = "") { it.text?.toString().orEmpty() }
        viewModel.onCodeChanged(code)
    }

    private fun updateResendState(state: OtpViewModel.UiState) {
        val isAvailable = state.isResendAvailable && !state.isResendLoading
        val text = when {
            state.isResendLoading -> getString(R.string.verify_resend_loading)
            state.isResendAvailable -> getString(R.string.verify_resend_available)
            else -> getString(R.string.verify_resend_in, formatRemainingTime(state.remainingSeconds))
        }
        binding.tvResend.text = text
        binding.tvResend.isEnabled = isAvailable
        binding.tvResend.isClickable = isAvailable
        binding.tvResend.alpha = if (isAvailable) 1f else 0.6f
        binding.tvResend.setTextColor(getColorCompat(if (isAvailable) R.color.brandGreen else R.color.textSecondary))
    }

    private fun formatRemainingTime(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val secs = safeSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }

    private fun formatPhoneForDisplay(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return raw
        val grouped = digits.chunked(3).joinToString(" ")
        return "+$grouped"
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: binding.root
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getColorCompat(@ColorRes colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    companion object {
        const val EXTRA_PHONE = "extra_phone"

        fun createIntent(context: Context, phone: String): Intent {
            return Intent(context, OtpActivity::class.java).putExtra(EXTRA_PHONE, phone)
        }
    }
}
