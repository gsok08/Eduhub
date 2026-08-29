package com.example.eduhub20.ui.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eduhub20.data.model.EduHubUser
import com.example.eduhub20.data.model.UserRole
import com.example.eduhub20.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSignUpMode: Boolean = false,
    val rememberMe: Boolean = false,
    val selectedRole: UserRole = UserRole.STUDENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val currentUser: EduHubUser? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("eduhub_auth_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val savedRemember = prefs.getBoolean("remember_me", false)
        if (savedRemember) {
            val savedId = prefs.getString("saved_user_id", null)
            val savedEmail = prefs.getString("saved_user_email", null)
            val savedName = prefs.getString("saved_user_name", null)
            val savedRoleStr = prefs.getString("saved_user_role", UserRole.STUDENT.name)
            val savedRole = try {
                UserRole.valueOf(savedRoleStr ?: UserRole.STUDENT.name)
            } catch (e: Exception) {
                UserRole.STUDENT
            }

            if (!savedId.isNullOrBlank() && !savedEmail.isNullOrBlank() && !savedName.isNullOrBlank()) {
                val restoredUser = EduHubUser(savedId, savedEmail, savedName, savedRole)
                AuthRepository.restoreUser(restoredUser)
                _uiState.update { it.copy(currentUser = restoredUser, rememberMe = true) }
            }
        }

        viewModelScope.launch {
            AuthRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleRememberMe(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
        if (!remember) {
            prefs.edit().clear().apply()
        }
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _uiState.update { it.copy(isSignUpMode = isSignUp, errorMessage = null, successMessage = null) }
    }

    fun setSelectedRole(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                errorMessage = null
            )
        }
    }

    fun showForgotPasswordDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = show,
                forgotPasswordEmail = if (show) it.email else "",
                errorMessage = null
            )
        }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email) }
    }

    fun submitLogin() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password.trim()

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (state.selectedRole == UserRole.LECTURER) {
                val res = AuthRepository.signInAsLecturer(email, password)
                res.fold(
                    onSuccess = { user ->
                        if (state.rememberMe) {
                            prefs.edit()
                                .putBoolean("remember_me", true)
                                .putString("saved_user_id", user.id)
                                .putString("saved_user_email", user.email)
                                .putString("saved_user_name", user.name)
                                .putString("saved_user_role", user.role.name)
                                .apply()
                        }
                        _uiState.update { it.copy(isLoading = false, currentUser = user) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Incorrect email or password. Please try again.") }
                    }
                )
            } else {
                if (state.isSignUpMode) {
                    val res = AuthRepository.signUpStudent(email, password)
                    res.fold(
                        onSuccess = { user ->
                            if (state.rememberMe) {
                                prefs.edit()
                                    .putBoolean("remember_me", true)
                                    .putString("saved_user_id", user.id)
                                    .putString("saved_user_email", user.email)
                                    .putString("saved_user_name", user.name)
                                    .putString("saved_user_role", user.role.name)
                                    .apply()
                            }
                            _uiState.update { it.copy(isLoading = false, currentUser = user) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Sign up failed. Please check your details and try again.") }
                        }
                    )
                } else {
                    val res = AuthRepository.signInAsStudent(email, password)
                    res.fold(
                        onSuccess = { user ->
                            if (state.rememberMe) {
                                prefs.edit()
                                    .putBoolean("remember_me", true)
                                    .putString("saved_user_id", user.id)
                                    .putString("saved_user_email", user.email)
                                    .putString("saved_user_name", user.name)
                                    .putString("saved_user_role", user.role.name)
                                    .apply()
                            }
                            _uiState.update { it.copy(isLoading = false, currentUser = user) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Incorrect email or password. Please try again.") }
                        }
                    )
                }
            }
        }
    }

    fun updateProfileName(newName: String) {
        viewModelScope.launch {
            AuthRepository.updateProfileName(newName)
            if (_uiState.value.rememberMe) {
                prefs.edit().putString("saved_user_name", newName.trim()).apply()
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email for password reset.") }
            return
        }
        viewModelScope.launch {
            AuthRepository.sendPasswordReset(email)
            _uiState.update {
                it.copy(
                    showForgotPasswordDialog = false,
                    successMessage = "Password reset instructions sent to $email"
                )
            }
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        AuthRepository.signOut()
        _uiState.update {
            it.copy(
                currentUser = null,
                email = "",
                password = "",
                rememberMe = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }
}