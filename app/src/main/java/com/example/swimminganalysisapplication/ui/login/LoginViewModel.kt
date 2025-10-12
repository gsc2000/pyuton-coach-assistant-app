package com.example.swimminganalysisapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.UserLogin // ★ インポートを追加
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: SwimmingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun onEmailChange(email: String) {
        _email.value = email
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // ★★★ 修正点：UserLoginオブジェクトを生成して渡す ★★★
                val userLogin = UserLogin(
                    userEmail = _email.value.trim(),
                    userPassword = _password.value.trim()
                )

                val tokenResponse = repository.login(userLogin)
                // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

                if (tokenResponse != null) {
                    userPreferences.saveAuthToken(tokenResponse.accessToken)
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("トークンの取得に失敗しました。")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "不明なエラーが発生しました。")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}