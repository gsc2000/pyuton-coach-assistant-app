package com.example.swimminganalysisapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.UserCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateAccountViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _birthday = MutableStateFlow("") // 誕生日用のStateFlow
    val birthday: StateFlow<String> = _birthday.asStateFlow()

    private val _createAccountState = MutableStateFlow<CreateAccountState>(CreateAccountState.Idle)
    val createAccountState: StateFlow<CreateAccountState> = _createAccountState.asStateFlow()

    fun onUsernameChange(username: String) {
        _username.value = username
    }

    fun onEmailChange(email: String) {
        _email.value = email
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun onBirthdayChange(birthday: String) { // 誕生日用のイベントハンドラ
        _birthday.value = birthday
    }

    fun createAccount() {
        viewModelScope.launch {
            _createAccountState.value = CreateAccountState.Loading
            try {
                // ★★★ 修正点：すべての入力値から前後の空白を削除 ★★★
                val trimmedUsername = _username.value.trim()
                val trimmedEmail = _email.value.trim()
                val trimmedPassword = _password.value.trim()
                val trimmedBirthday = _birthday.value.trim()
                // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

                val userCreate = UserCreate(
                    userName = trimmedUsername,
                    userEmail = trimmedEmail,
                    userPassword = trimmedPassword,
                    userBirthday = trimmedBirthday.takeIf { it.isNotBlank() } // 空でなければセット
                )
                val createdUser = repository.register(userCreate)
                if (createdUser != null) {
                    _createAccountState.value = CreateAccountState.Success("アカウントが正常に作成されました。")
                } else {
                    _createAccountState.value = CreateAccountState.Error("アカウントの作成に失敗しました。")
                }
            } catch (e: Exception) {
                _createAccountState.value = CreateAccountState.Error(e.message ?: "不明なエラーが発生しました。")
            }
        }
    }
}

sealed class CreateAccountState {
    object Idle : CreateAccountState()
    object Loading : CreateAccountState()
    data class Success(val message: String) : CreateAccountState()
    data class Error(val message: String) : CreateAccountState()
}