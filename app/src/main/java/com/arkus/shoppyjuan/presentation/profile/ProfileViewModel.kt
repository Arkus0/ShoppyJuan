package com.arkus.shoppyjuan.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.data.auth.AuthRepository
import com.arkus.shoppyjuan.data.auth.AuthResult
import com.arkus.shoppyjuan.data.location.LocationManager
import com.arkus.shoppyjuan.data.repository.FeedbackRepository
import com.arkus.shoppyjuan.domain.settings.AppLanguage
import com.arkus.shoppyjuan.domain.settings.AppTheme
import com.arkus.shoppyjuan.domain.settings.UserPreferencesManager
import com.arkus.shoppyjuan.domain.user.UserManager
import com.arkus.shoppyjuan.presentation.components.FeedbackType
import com.arkus.shoppyjuan.presentation.components.FeedbackRating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val error: String? = null,
    val feedbackSent: Boolean = false,
    // Preferences
    val language: AppLanguage = AppLanguage.SPANISH,
    val theme: AppTheme = AppTheme.SYSTEM,
    val searchRadiusKm: Int = 10,
    val locationEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userManager: UserManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val locationManager: LocationManager,
    private val feedbackRepository: FeedbackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            // Collect preferences
            launch {
                userPreferencesManager.language.collect { language ->
                    _uiState.update { it.copy(language = language) }
                }
            }
            launch {
                userPreferencesManager.theme.collect { theme ->
                    _uiState.update { it.copy(theme = theme) }
                }
            }
            launch {
                userPreferencesManager.searchRadius.collect { radius ->
                    _uiState.update { it.copy(searchRadiusKm = radius) }
                }
            }
            launch {
                userPreferencesManager.locationEnabled.collect { enabled ->
                    _uiState.update { it.copy(locationEnabled = enabled) }
                }
            }

            // Check location permission
            val hasPermission = locationManager.hasLocationPermission()
            _uiState.update { it.copy(hasLocationPermission = hasPermission) }
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesManager.updateLanguage(language)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesManager.updateTheme(theme)
        }
    }

    fun updateSearchRadius(radiusKm: Int) {
        viewModelScope.launch {
            userPreferencesManager.updateSearchRadius(radiusKm)
        }
    }

    fun updateLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateLocationEnabled(enabled)
            if (enabled) {
                // Try to get and save current location
                locationManager.updateAndSaveLocation()
            }
        }
    }

    fun refreshLocationPermission() {
        val hasPermission = locationManager.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
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

    fun sendFeedback(type: FeedbackType, rating: FeedbackRating?, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = _uiState.value.profile?.id

            val result = feedbackRepository.sendFeedback(type, rating, description, userId)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, feedbackSent = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "Error enviando feedback: ${e.message}") }
            }
        }
    }

    fun clearFeedbackSent() {
        _uiState.update { it.copy(feedbackSent = false) }
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
