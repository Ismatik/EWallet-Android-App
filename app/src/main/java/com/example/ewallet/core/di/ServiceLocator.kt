package com.example.ewallet.core.di

import com.example.ewallet.feature.data.domain.repository.AuthRepository
import com.example.ewallet.feature.data.auth.repository.AuthRepositoryImpl
import com.example.ewallet.feature.data.auth.remote.FakeAuthApi
import com.example.ewallet.feature.data.domain.usecase.SendCodeUseCase
import com.example.ewallet.feature.data.domain.usecase.VerifyCodeUseCase

object ServiceLocator {
    private val authApi by lazy { FakeAuthApi() }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi) }

    val sendCodeUseCase by lazy { SendCodeUseCase(authRepository) }
    val verifyCodeUseCase by lazy { VerifyCodeUseCase(authRepository) }
}
