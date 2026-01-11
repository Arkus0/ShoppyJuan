package com.arkus.shoppyjuan.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    CUSTOM
}

@Serializable
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    fun toSpanish(): String = when (this) {
        MONDAY -> "Lunes"
        TUESDAY -> "Martes"
        WEDNESDAY -> "Miercoles"
        THURSDAY -> "Jueves"
        FRIDAY -> "Viernes"
        SATURDAY -> "Sabado"
        SUNDAY -> "Domingo"
    }

    fun toCalendarDay(): Int = when (this) {
        SUNDAY -> java.util.Calendar.SUNDAY
        MONDAY -> java.util.Calendar.MONDAY
        TUESDAY -> java.util.Calendar.TUESDAY
        WEDNESDAY -> java.util.Calendar.WEDNESDAY
        THURSDAY -> java.util.Calendar.THURSDAY
        FRIDAY -> java.util.Calendar.FRIDAY
        SATURDAY -> java.util.Calendar.SATURDAY
    }
}

@Serializable
data class RecurrenceSettings(
    val isEnabled: Boolean = false,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.WEEKLY,
    val selectedDays: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY),
    val customIntervalDays: Int = 7,
    val resetOnRecurrence: Boolean = true, // Uncheck all items when recurring
    val notifyBeforeRecurrence: Boolean = true,
    val notifyHoursBefore: Int = 2 // Hours before to send reminder
) {
    fun getDisplayText(): String {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> "Diario"
            RecurrenceFrequency.WEEKLY -> {
                if (selectedDays.size == 1) {
                    "Cada ${selectedDays.first().toSpanish()}"
                } else {
                    "Semanal (${selectedDays.size} dias)"
                }
            }
            RecurrenceFrequency.BIWEEKLY -> "Cada 2 semanas"
            RecurrenceFrequency.MONTHLY -> "Mensual"
            RecurrenceFrequency.CUSTOM -> "Cada $customIntervalDays dias"
        }
    }

    fun getNextOccurrence(): Long {
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis

        when (frequency) {
            RecurrenceFrequency.DAILY -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            RecurrenceFrequency.WEEKLY, RecurrenceFrequency.BIWEEKLY -> {
                val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val sortedDays = selectedDays.map { it.toCalendarDay() }.sorted()

                // Find next occurrence day
                val nextDay = sortedDays.find { it > currentDay }
                    ?: sortedDays.first()

                var daysToAdd = if (nextDay > currentDay) {
                    nextDay - currentDay
                } else {
                    7 - currentDay + nextDay
                }

                if (frequency == RecurrenceFrequency.BIWEEKLY && nextDay <= currentDay) {
                    daysToAdd += 7
                }

                calendar.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
            }
            RecurrenceFrequency.MONTHLY -> {
                calendar.add(java.util.Calendar.MONTH, 1)
            }
            RecurrenceFrequency.CUSTOM -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, customIntervalDays)
            }
        }

        // Set to beginning of day
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}
