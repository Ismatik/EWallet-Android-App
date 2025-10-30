package com.example.ewallet.feature.data.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ewallet.core.util.Result
import com.example.ewallet.feature.data.domain.usecase.SendCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val sendCode: SendCodeUseCase) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val isValid: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isCodeSent: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPhoneChanged(value: String) {
        _uiState.update {
            it.copy(
                phone = value,
                isValid = isValidPhone(value),
                error = null
            )
        }
    }

    fun sendCode() {
        val phone = _uiState.value.phone
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(error = "Введите корректный номер") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = sendCode(phone)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isCodeSent = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = res.message) }
            }
        }
    }

    private fun isValidPhone(input: String): Boolean {
        val digits = input.filter { it.isDigit() }
        return digits.length >= 10
    }
}
