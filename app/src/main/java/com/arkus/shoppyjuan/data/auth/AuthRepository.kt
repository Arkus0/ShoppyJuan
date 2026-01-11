package com.arkus.shoppyjuan.data.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: UserInfo) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "AuthRepository"

    /**
     * Observe authentication state changes
     */
    val authState: Flow<AuthState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    AuthState.Authenticated(user)
                } else {
                    AuthState.NotAuthenticated
                }
            }
            is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            is SessionStatus.LoadingFromStorage -> AuthState.Loading
            is SessionStatus.NetworkError -> AuthState.NotAuthenticated
        }
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUp(email: String, password: String, name: String): AuthResult {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject { put("name", name) }
            }

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Error al crear la cuenta")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up", e)
            AuthResult.Error(parseAuthError(e.message))
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Error al iniciar sesión")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in", e)
            AuthResult.Error(parseAuthError(e.message))
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Boolean {
        return try {
            supabase.auth.signOut()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            false
        }
    }

    /**
     * Get current user
     */
    fun getCurrentUser(): UserInfo? {
        return supabase.auth.currentUserOrNull()
    }

    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    /**
     * Observe auth state changes (legacy method for compatibility)
     */
    fun observeAuthState(): Flow<UserInfo?> = flow {
        emit(supabase.auth.currentUserOrNull())
        supabase.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> emit(supabase.auth.currentUserOrNull())
                else -> emit(null)
            }
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            AuthResult.Success(
                supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("No user")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email", e)
            AuthResult.Error(parseAuthError(e.message))
        }
    }

    /**
     * Update user password
     */
    suspend fun updatePassword(newPassword: String): AuthResult {
        return try {
            supabase.auth.modifyUser {
                password = newPassword
            }
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Error al actualizar la contraseña")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password", e)
            AuthResult.Error(parseAuthError(e.message))
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(name: String?, avatarUrl: String?): AuthResult {
        return try {
            supabase.auth.modifyUser {
                data = buildJsonObject {
                    name?.let { put("name", it) }
                    avatarUrl?.let { put("avatar_url", it) }
                }
            }

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Error al actualizar el perfil")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            AuthResult.Error(parseAuthError(e.message))
        }
    }

    /**
     * Get user display name from metadata
     */
    fun getUserDisplayName(user: UserInfo? = null): String {
        val targetUser = user ?: getCurrentUser()
        val metadata = targetUser?.userMetadata
        return metadata?.get("name")?.toString()?.removeSurrounding("\"")
            ?: targetUser?.email?.substringBefore("@")
            ?: "Usuario"
    }

    /**
     * Parse auth errors to user-friendly messages
     */
    private fun parseAuthError(message: String?): String {
        return when {
            message == null -> "Error desconocido"
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Email o contraseña incorrectos"
            message.contains("Email not confirmed", ignoreCase = true) ->
                "Por favor confirma tu email"
            message.contains("User already registered", ignoreCase = true) ->
                "Este email ya está registrado"
            message.contains("Password should be", ignoreCase = true) ->
                "La contraseña debe tener al menos 6 caracteres"
            message.contains("Invalid email", ignoreCase = true) ->
                "Email inválido"
            message.contains("network", ignoreCase = true) ->
                "Error de conexión. Verifica tu internet."
            else -> message
        }
    }
}
