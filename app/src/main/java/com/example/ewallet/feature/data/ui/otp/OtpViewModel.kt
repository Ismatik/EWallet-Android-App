package com.example.ewallet.feature.data.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ewallet.core.util.Result
import com.example.ewallet.feature.data.domain.usecase.SendCodeUseCase
import com.example.ewallet.feature.data.domain.usecase.VerifyCodeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpViewModel(
    private val phone: String,
    private val sendCode: SendCodeUseCase,
    private val verifyCode: VerifyCodeUseCase
) : ViewModel() {

    data class UiState(
        val code: String = "",
        val isComplete: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val remainingSeconds: Int = RESEND_INTERVAL,
        val isResendAvailable: Boolean = false,
        val isResendLoading: Boolean = false,
    )

    sealed interface Event {
        data object Verified : Event
        data object CodeResent : Event
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun onCodeChanged(code: String) {
        _uiState.update {
            it.copy(code = code, isComplete = code.length == CODE_LENGTH, error = null)
        }
    }

    fun submit() {
        val state = _uiState.value
        val code = state.code
        if (code.length != CODE_LENGTH) {
            _uiState.update { it.copy(error = INVALID_CODE_ERROR) }
            return
        }
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = verifyCode(phone, code)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    stopTimer()
                    _events.emit(Event.Verified)
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = res.message ?: GENERIC_ERROR) }
                }
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (!state.isResendAvailable || state.isResendLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResendLoading = true, error = null) }
            when (val res = sendCode(phone)) {
                is Result.Success -> {
                    _uiState.update { it.copy(code = "", isComplete = false, isResendLoading = false) }
                    startTimer()
                    _events.emit(Event.CodeResent)
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isResendLoading = false,
                            error = res.message ?: GENERIC_ERROR,
                            isResendAvailable = true
                        )
                    }
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                remainingSeconds = RESEND_INTERVAL,
                isResendAvailable = false,
                isResendLoading = false
            )
        }
        timerJob = viewModelScope.launch {
            var remaining = RESEND_INTERVAL
            while (remaining > 0) {
                delay(ONE_SECOND)
                remaining -= 1
                _uiState.update {
                    it.copy(
                        remainingSeconds = remaining,
                        isResendAvailable = remaining == 0
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isResendAvailable = false, remainingSeconds = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val CODE_LENGTH = 6
        private const val RESEND_INTERVAL = 60
        private const val ONE_SECOND = 1000L
        private const val INVALID_CODE_ERROR = "Введите 6-значный код"
        private const val GENERIC_ERROR = "Что-то пошло не так"
    }
}



