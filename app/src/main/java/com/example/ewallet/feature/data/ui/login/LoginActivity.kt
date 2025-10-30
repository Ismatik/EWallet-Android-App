package com.example.ewallet.feature.data.ui.login

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
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

    private val viewModel: LoginViewModel by viewModels {
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

        binding.phone.doAfterTextChanged { text ->
            viewModel.onPhoneChanged(text?.toString().orEmpty())
        }
        binding.phone.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnLogin.isEnabled) {
                viewModel.sendCode()
                true
            } else {
                false
            }
        }
        binding.btnLogin.setOnClickListener {
            viewModel.sendCode()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
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
                viewModel.events.collect { event ->
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
}
