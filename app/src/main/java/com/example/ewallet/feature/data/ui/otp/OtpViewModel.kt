package com.example.ewallet.feature.data.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ewallet.core.util.Result
import com.example.ewallet.feature.data.domain.usecase.VerifyCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpViewModel(
    private val phone: String,
    private val verifyCode: VerifyCodeUseCase
) : ViewModel() {

    data class UiState(
        val code: String = "",
        val isComplete: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isVerified: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onCodeChanged(code: String) {
        _uiState.update { it.copy(code = code, isComplete = code.length == 6, error = null) }
    }

    fun submit() {
        val code = _uiState.value.code
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Введите 6-значный код") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = verifyCode(phone, code)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isVerified = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = res.message) }
            }
        }
    }
}
