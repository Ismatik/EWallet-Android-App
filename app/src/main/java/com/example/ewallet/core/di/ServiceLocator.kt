package com.example.ewallet.core.di

import com.example.ewallet.feature.data.domain.repository.AuthRepository
import com.example.ewallet.feature.data.auth.repository.AuthRepositoryImpl
import com.example.ewallet.feature.data.auth.remote.FakeAuthApi
import com.example.ewallet.feature.data.domain.usecase.SendCodeUseCase
import com.example.ewallet.feature.data.domain.usecase.VerifyCodeUseCase
import com.example.ewallet.feature.data.auth.remote.AuthApi

object ServiceLocator {

    // swap to real implementation later
    private val authApi: AuthApi by lazy { FakeAuthApi() }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi)
    }

    val sendCodeUseCase: SendCodeUseCase by lazy {
        SendCodeUseCase(authRepository)
    }

    val verifyCodeUseCase: VerifyCodeUseCase by lazy {
        VerifyCodeUseCase(authRepository)
    }
}