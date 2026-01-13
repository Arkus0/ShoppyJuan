package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.presentation.components.FeedbackType
import com.arkus.shoppyjuan.presentation.components.FeedbackRating
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FeedbackDto(
    val type: String,
    val description: String,
    val rating: String?,
    val app_version: String,
    val user_id: String?
)

@Singleton
class FeedbackRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun sendFeedback(
        type: FeedbackType,
        rating: FeedbackRating?,
        description: String,
        userId: String?,
        appVersion: String = "1.0.0"
    ): Result<Unit> {
        return try {
            val feedback = FeedbackDto(
                type = type.name,
                description = description,
                rating = rating?.name,
                app_version = appVersion,
                user_id = userId
            )

            supabase.postgrest.from("feedback").insert(feedback)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
