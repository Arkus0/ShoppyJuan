package com.arkus.shoppyjuan.domain.user

import com.arkus.shoppyjuan.data.auth.AuthRepository
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current user's information
 */
data class CurrentUser(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null
) {
    companion object {
        fun fromUserInfo(userInfo: UserInfo): CurrentUser {
            val metadata = userInfo.userMetadata
            return CurrentUser(
                id = userInfo.id,
                email = userInfo.email ?: "",
                name = metadata?.get("name")?.toString()?.removeSurrounding("\"")
                    ?: userInfo.email?.substringBefore("@")
                    ?: "Usuario",
                avatarUrl = metadata?.get("avatar_url")?.toString()?.removeSurrounding("\"")
            )
        }
    }
}

/**
 * Centralized manager for user information
 * Provides current user data across the application
 */
@Singleton
class UserManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val _currentUser = MutableStateFlow<CurrentUser?>(null)
    val currentUser: StateFlow<CurrentUser?> = _currentUser.asStateFlow()

    /**
     * Check if user is currently signed in
     */
    val isSignedIn: Boolean
        get() = authRepository.isSignedIn()

    /**
     * Get current user ID or empty string if not signed in
     */
    val currentUserId: String
        get() = _currentUser.value?.id ?: authRepository.getCurrentUser()?.id ?: ""

    /**
     * Get current user name
     */
    val currentUserName: String
        get() = _currentUser.value?.name ?: "Usuario"

    /**
     * Get current user email
     */
    val currentUserEmail: String
        get() = _currentUser.value?.email ?: ""

    init {
        // Initialize with current user if available
        refreshUser()
    }

    /**
     * Refresh current user from auth repository
     */
    fun refreshUser() {
        authRepository.getCurrentUser()?.let { userInfo ->
            _currentUser.value = CurrentUser.fromUserInfo(userInfo)
        } ?: run {
            _currentUser.value = null
        }
    }

    /**
     * Update current user after sign in/sign up
     */
    fun setUser(userInfo: UserInfo) {
        _currentUser.value = CurrentUser.fromUserInfo(userInfo)
    }

    /**
     * Clear current user on sign out
     */
    fun clearUser() {
        _currentUser.value = null
    }

    /**
     * Observe auth state changes
     */
    fun observeAuthState(): Flow<UserInfo?> = authRepository.observeAuthState()

    /**
     * Get display name for a user ID
     * For now returns a formatted version; in future could lookup from a users table
     */
    fun getDisplayName(userId: String): String {
        return if (userId == currentUserId) {
            currentUserName
        } else {
            "Usuario ${userId.take(4)}"
        }
    }

    /**
     * Get avatar initial for a user
     */
    fun getAvatarInitial(userId: String? = null): String {
        val name = if (userId == null || userId == currentUserId) {
            currentUserName
        } else {
            "Usuario ${userId.take(4)}"
        }
        return name.take(1).uppercase()
    }

    /**
     * Generate avatar color based on user ID
     */
    fun getAvatarColor(userId: String): Int {
        val hash = userId.hashCode()
        val colors = listOf(
            0xFF6366F1.toInt(), // Indigo
            0xFF8B5CF6.toInt(), // Violet
            0xFFEC4899.toInt(), // Pink
            0xFFEF4444.toInt(), // Red
            0xFFF97316.toInt(), // Orange
            0xFFF59E0B.toInt(), // Amber
            0xFF10B981.toInt(), // Emerald
            0xFF14B8A6.toInt(), // Teal
            0xFF06B6D4.toInt(), // Cyan
            0xFF3B82F6.toInt()  // Blue
        )
        return colors[kotlin.math.abs(hash) % colors.size]
    }
}
