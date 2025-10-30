package com.example.ewallet.feature.data.domain.usecase

import com.example.ewallet.feature.data.domain.repository.AuthRepository
import com.example.ewallet.core.util.Result

class SendCodeUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(phone: String): Result<Unit> = repo.sendCode(phone)
}
