package com.example.ewallet.feature.data.domain.repository

import com.example.ewallet.core.util.Result

interface AuthRepository {
    suspend fun sendCode(phone: String): Result<Unit>
    suspend fun verifyCode(phone: String, code: String): Result<Unit>
}