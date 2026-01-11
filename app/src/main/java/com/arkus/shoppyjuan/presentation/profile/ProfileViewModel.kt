package com.arkus.shoppyjuan.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.data.auth.AuthRepository
import com.arkus.shoppyjuan.data.auth.AuthResult
import com.arkus.shoppyjuan.domain.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val signedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userManager: UserManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userManager.refreshUser()
                val currentUser = userManager.currentUser.value

                if (currentUser != null) {
                    val profile = UserProfile(
                        id = currentUser.id,
                        name = currentUser.name,
                        email = currentUser.email,
                        avatarUrl = currentUser.avatarUrl
                    )
                    _uiState.update {
                        it.copy(profile = profile, isLoading = false, error = null)
                    }
                } else {
                    // Fallback to AuthRepository directly
                    val user = authRepository.getCurrentUser()
                    if (user != null) {
                        val profile = UserProfile(
                            id = user.id,
                            name = authRepository.getUserDisplayName(user),
                            email = user.email ?: "",
                            avatarUrl = user.userMetadata?.get("avatar_url")?.toString()?.removeSurrounding("\"")
                        )
                        _uiState.update {
                            it.copy(profile = profile, isLoading = false, error = null)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, signedOut = true)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun startChangingPassword() {
        _uiState.update { it.copy(isChangingPassword = true, passwordChangeSuccess = false) }
    }

    fun cancelChangingPassword() {
        _uiState.update { it.copy(isChangingPassword = false) }
    }

    fun updateProfile(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when (val result = authRepository.updateProfile(name = name, avatarUrl = null)) {
                    is AuthResult.Success -> {
                        userManager.setUser(result.user)
                        val updatedProfile = _uiState.value.profile?.copy(name = name)
                        _uiState.update {
                            it.copy(
                                profile = updatedProfile,
                                isEditing = false,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(error = result.message, isLoading = false)
                        }
                    }
                    AuthResult.Loading -> { /* Ignore */ }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // First verify current password by attempting re-authentication
                val email = _uiState.value.profile?.email ?: return@launch

                when (val signInResult = authRepository.signIn(email, currentPassword)) {
                    is AuthResult.Success -> {
                        // Current password is correct, now update to new password
                        when (val updateResult = authRepository.updatePassword(newPassword)) {
                            is AuthResult.Success -> {
                                _uiState.update {
                                    it.copy(
                                        isChangingPassword = false,
                                        passwordChangeSuccess = true,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            }
                            is AuthResult.Error -> {
                                _uiState.update {
                                    it.copy(error = updateResult.message, isLoading = false)
                                }
                            }
                            AuthResult.Loading -> { /* Ignore */ }
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(error = "Contrasena actual incorrecta", isLoading = false)
                        }
                    }
                    AuthResult.Loading -> { /* Ignore */ }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val email = _uiState.value.profile?.email ?: return@launch

                when (val result = authRepository.sendPasswordResetEmail(email)) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isChangingPassword = false,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(error = result.message, isLoading = false)
                        }
                    }
                    AuthResult.Loading -> { /* Ignore */ }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = authRepository.signOut()
                if (success) {
                    userManager.clearUser()
                    _uiState.update {
                        it.copy(
                            profile = null,
                            signedOut = true,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Error al cerrar sesion", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearPasswordChangeSuccess() {
        _uiState.update { it.copy(passwordChangeSuccess = false) }
    }
}
