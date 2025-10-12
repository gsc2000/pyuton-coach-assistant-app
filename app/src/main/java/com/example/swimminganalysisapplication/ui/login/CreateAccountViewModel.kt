package com.example.swimminganalysisapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.UserCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.swimminganalysisapplication.ui.login.RegistrationState // ★インポート文を追加

class CreateAccountViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun onUserNameChange(newUsername: String) {
        _userName.value = newUsername
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun createAccount() {
        if (_userName.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _registrationState.value = RegistrationState.Error("すべてのフィールドを入力してください。")
            return
        }

        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                val userToCreate = UserCreate(
                    userName = _userName.value,
                    userEmail = _email.value,
                    userPassword = _password.value
                )
                repository.register(userToCreate)
                // 成功した場合
                _registrationState.value = RegistrationState.Success("アカウントを作成しました。")

            } catch (e: Exception) {
                // repository.registerが失敗するとApiExceptionを投げる
                _registrationState.value = RegistrationState.Error(e.message ?: "不明なエラーが発生しました。")
            }
        }
    }
}
