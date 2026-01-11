package com.arkus.shoppyjuan.data.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: UserInfo) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "AuthRepository"

    /**
     * Sign up with email and password
     */
    suspend fun signUp(email: String, password: String, name: String): AuthResult {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = mapOf("name" to name)
            }

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Error al crear la cuenta")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up", e)
            AuthResult.Error(e.message ?: "Error desconocido")
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
            AuthResult.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
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
     * Observe auth state changes
     */
    fun observeAuthState(): Flow<UserInfo?> = flow {
        // Initial state
        emit(supabase.auth.currentUserOrNull())

        // TODO: Listen to auth state changes when SDK supports it
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            AuthResult.Success(supabase.auth.currentUserOrNull()!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email", e)
            AuthResult.Error(e.message ?: "Error al enviar el email")
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(name: String?, avatarUrl: String?): AuthResult {
        return try {
            supabase.auth.modifyUser {
                data = buildMap {
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
            AuthResult.Error(e.message ?: "Error desconocido")
        }
    }
}
