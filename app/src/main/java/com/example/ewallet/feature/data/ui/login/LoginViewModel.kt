package com.example.ewallet.feature.data.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ewallet.core.util.Result
import com.example.ewallet.feature.data.domain.usecase.SendCodeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val sendCode: SendCodeUseCase) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val isValid: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    sealed interface Event {
        data class CodeSent(val phone: String) : Event
        data class Error(val message: String) : Event
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    fun onPhoneChanged(value: String) {
        val normalized = normalizePhone(value)
        _uiState.update {
            it.copy(
                phone = normalized,
                isValid = isValidPhone(normalized),
                error = null
            )
        }
    }

    fun sendCode() {
        val currentState = _uiState.value
        val digits = normalizePhone(currentState.phone)
        if (!isValidPhone(digits)) {
            _uiState.update { it.copy(error = INVALID_PHONE_ERROR) }
            return
        }
        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val phoneWithCountry = withCountryCode(digits)
            when (val res = sendCode(phoneWithCountry)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(Event.CodeSent(phoneWithCountry))
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(Event.Error(res.message ?: GENERIC_ERROR))
                }
            }
        }
    }

    private fun normalizePhone(input: String): String = input.filter { it.isDigit() }

    private fun withCountryCode(digits: String): String =
        if (digits.isEmpty()) digits else COUNTRY_CODE_PREFIX + digits

    private fun isValidPhone(phone: String): Boolean = phone.length >= MIN_PHONE_LENGTH

    companion object {
        private const val MIN_PHONE_LENGTH = 9
        private const val COUNTRY_CODE_PREFIX = "+992"
        private const val INVALID_PHONE_ERROR = "Введите корректный номер"
        private const val GENERIC_ERROR = "Не удалось отправить код"
    }
}

