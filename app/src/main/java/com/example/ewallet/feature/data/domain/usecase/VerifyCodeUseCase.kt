package com.example.ewallet.feature.data.domain.usecase

import com.example.ewallet.feature.data.domain.repository.AuthRepository
import com.example.ewallet.core.util.Result

class VerifyCodeUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(phone: String, code: String): Result<Unit> =
        repo.verifyCode(phone, code)
}
