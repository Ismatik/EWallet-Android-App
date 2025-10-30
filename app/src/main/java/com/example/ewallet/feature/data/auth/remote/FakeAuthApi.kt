package com.example.ewallet.feature.data.auth.remote

import kotlinx.coroutines.delay

class FakeAuthApi : AuthApi {
    override suspend fun sendCode(phone: String): Boolean {
        delay(800)
        return phone.filter { it.isDigit() }.length >= 9
    }

    override suspend fun verifyCode(phone: String, code: String): Boolean {
        delay(800)
        return code == "123456"
    }
}
