package com.arkus.shoppyjuan.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    // TODO: Inject AuthRepository when available
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
                // TODO: Load profile from AuthRepository
                // For now, use mock data
                val mockProfile = UserProfile(
                    id = "current_user_id",
                    name = "Usuario",
                    email = "usuario@example.com"
                )
                _uiState.update {
                    it.copy(profile = mockProfile, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateProfile(name: String) {
        viewModelScope.launch {
            try {
                // TODO: Update profile via AuthRepository
                val updatedProfile = _uiState.value.profile?.copy(name = name)
                _uiState.update {
                    it.copy(profile = updatedProfile, isEditing = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                // TODO: Call AuthRepository.signOut()
                _uiState.update { it.copy(profile = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
