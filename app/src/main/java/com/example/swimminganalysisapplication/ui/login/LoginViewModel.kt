package com.example.swimminganalysisapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.storage.UserPreferences // ★ インポートパスを修正
import com.example.swimminganalysisapplication.data.remote.model.UserLogin
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
                // UserLoginオブジェクトを作成してリポジトリに渡す
                val userLogin = UserLogin(userEmail = _email.value, userPassword = _password.value)
                val token = repository.login(userLogin)

                if (token != null) {
                    // 成功時：トークンを保存
                    userPreferences.saveAuthToken(token.accessToken)
                    _loginState.value = LoginState.Success("Login successful")
                } else {
                    // 失敗時：リポジトリがnullを返した場合
                    _loginState.value = LoginState.Error("ログインに失敗しました。メールアドレスまたはパスワードを確認してください。")
                }
            } catch (e: Exception) {
                // 失敗時：ネットワークエラーなど
                _loginState.value = LoginState.Error(e.message ?: "不明なエラーが発生しました。")
            }
        }
    }
}

// LoginScreenが期待するUIの状態を定義
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val message: String) : LoginState()
    data class Error(val message: String) : LoginState()
}