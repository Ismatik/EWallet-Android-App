package com.example.ewallet.feature.data.auth.remote

interface AuthApi {
    suspend fun sendCode(phone: String): Boolean
    suspend fun verifyCode(phone: String, code: String): Boolean
}
