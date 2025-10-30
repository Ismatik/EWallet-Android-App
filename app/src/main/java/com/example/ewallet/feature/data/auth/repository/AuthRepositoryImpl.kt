package com.example.ewallet.feature.data.auth.repository

import com.example.ewallet.core.util.Result
import com.example.ewallet.feature.data.auth.remote.AuthApi
import com.example.ewallet.feature.data.domain.repository.AuthRepository

class AuthRepositoryImpl(private val api: AuthApi) : AuthRepository {
    override suspend fun sendCode(phone: String): Result<Unit> = try {
        if (api.sendCode(phone)) Result.Success(Unit) else Result.Error("Неверный номер")
    } catch (t: Throwable) {
        Result.Error("Не удалось отправить код", t)
    }

    override suspend fun verifyCode(phone: String, code: String): Result<Unit> = try {
        if (api.verifyCode(phone, code)) Result.Success(Unit) else Result.Error("Код неверный")
    } catch (t: Throwable) {
        Result.Error("Не удалось подтвердить код", t)
    }
}