package com.prumo.feature.auth

import com.prumo.core.model.SessionToken
import com.prumo.core.model.SessionUser
import com.prumo.core.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates state`() = runTest {
        val vm = LoginViewModel(FakeAuthRepository())
        vm.onEmailChange("a@a.com")
        vm.onPasswordChange("123")
        vm.login()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.success)
    }
}

private class FakeAuthRepository : AuthRepository {
    override suspend fun login(email: String, password: String): SessionToken = SessionToken(
        accessToken = "token",
        refreshToken = "refresh",
        expiresAtEpochSeconds = 999999,
        user = SessionUser("u", email, "User", "operacional", "tenant", emptyList())
    )

    override suspend fun logout() {}
    override suspend fun currentSession(): SessionToken? = null
    override suspend fun clearSession() {}
}