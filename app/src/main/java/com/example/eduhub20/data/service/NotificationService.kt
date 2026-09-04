package com.example.eduhub20.data.service

import com.example.eduhub20.data.model.ExamEntity
import com.example.eduhub20.data.model.ReminderEntity
import com.example.eduhub20.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class NotificationType {
    EXAM_COUNTDOWN,
    REMINDER,
    TASK
}

enum class NotificationSeverity {
    INFO,
    WARNING,
    URGENT
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val severity: NotificationSeverity,
    val timeRemainingText: String,
    val targetTimestamp: Long,
    val relatedId: String = "",
    val isDismissed: Boolean = false
)

object NotificationService {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun computeNotifications(
        exams: List<ExamEntity>,
        reminders: List<ReminderEntity>,
        tasks: List<TaskEntity> = emptyList(),
        dismissedIds: Set<String> = emptySet()
    ): List<AppNotification> {
        val list = mutableListOf<AppNotification>()

        // Normalize today to start of day for accurate day-based calculations
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // ── 1. Exam Countdown Notifications ─────────────────────────────────────
        // User requirements:
        // - 1 week left (7 days)
        // - 3 days left
        // - 1 day left (tomorrow)
        // Also supports today (0 days) and upcoming within 7 days
        for (exam in exams) {
            val examCal = Calendar.getInstance().apply {
                timeInMillis = exam.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = examCal.timeInMillis - todayStart.timeInMillis
            val daysLeft = (diffMillis / (1000L * 60 * 60 * 24)).toInt()

            if (daysLeft in 0..7) {
                val notifId = "exam_${exam.id}_${daysLeft}d"
                if (dismissedIds.contains(notifId)) continue

                val (title, severity, timeText, msg) = when {
                    daysLeft == 0 -> Quadruple(
                        "🔥 Exam Today: ${exam.name}",
                        NotificationSeverity.URGENT,
                        "Today",
                        "Best of luck! Your '${exam.name}' exam is today."
                    )
                    daysLeft == 1 -> Quadruple(
                        "🚨 Exam Tomorrow: ${exam.name}",
                        NotificationSeverity.URGENT,
                        "Tomorrow",
                        "Your exam '${exam.name}' is tomorrow (${dateFormat.format(Date(exam.date))}). Final revision time!"
                    )
                    daysLeft == 2 -> Quadruple(
                        "⚠️ Exam in 2 Days: ${exam.name}",
                        NotificationSeverity.WARNING,
                        "In 2 days",
                        "Only 2 days remaining until '${exam.name}'. Double-check your formulas and notes!"
                    )
                    daysLeft == 3 -> Quadruple(
                        "⚠️ Exam in 3 Days: ${exam.name}",
                        NotificationSeverity.WARNING,
                        "In 3 days",
                        "3 days left until '${exam.name}'. Review your key topics and past year papers."
                    )
                    daysLeft == 7 -> Quadruple(
                        "📅 Exam in 1 Week: ${exam.name}",
                        NotificationSeverity.INFO,
                        "In 7 days",
                        "Your exam '${exam.name}' is coming up in exactly 1 week on ${dateFormat.format(Date(exam.date))}."
                    )
                    else -> Quadruple(
                        "📅 Exam in $daysLeft Days: ${exam.name}",
                        NotificationSeverity.INFO,
                        "In $daysLeft days",
                        "'${exam.name}' is scheduled in $daysLeft days (${dateFormat.format(Date(exam.date))})."
                    )
                }

                list.add(
                    AppNotification(
                        id = notifId,
                        title = title,
                        message = msg,
                        type = NotificationType.EXAM_COUNTDOWN,
                        severity = severity,
                        timeRemainingText = timeText,
                        targetTimestamp = exam.date,
                        relatedId = exam.id
                    )
                )
            }
        }

        // ── 2. Reminder Notifications (Within 24 Hours) ─────────────────────────
        val currentMillis = System.currentTimeMillis()
        for (reminder in reminders) {
            val reminderTargetMillis = parseReminderEpoch(reminder.date, reminder.time)
            val diffMillis = reminderTargetMillis - currentMillis
            val hoursLeft = diffMillis / (1000L * 60 * 60)
            val minutesLeft = diffMillis / (1000L * 60)

            // Within the next 24 hours
            if (diffMillis in 0..(24 * 3600 * 1000L)) {
                val notifId = "reminder_${reminder.id}"
                if (dismissedIds.contains(notifId)) continue

                val (title, severity, timeText, msg) = when {
                    minutesLeft <= 60 -> Quadruple(
                        "⏰ Reminder Soon: ${reminder.name}",
                        NotificationSeverity.URGENT,
                        if (minutesLeft <= 1) "In 1 min" else "In $minutesLeft mins",
                        "Your reminder '${reminder.name}' is due at ${reminder.time}!"
                    )
                    hoursLeft <= 3 -> Quadruple(
                        "⏰ Reminder in ${hoursLeft}h: ${reminder.name}",
                        NotificationSeverity.WARNING,
                        "In ${hoursLeft}h",
                        "Scheduled for today at ${reminder.time}."
                    )
                    else -> Quadruple(
                        "⏰ Reminder Today: ${reminder.name}",
                        NotificationSeverity.INFO,
                        reminder.time,
                        "Don't forget: '${reminder.name}' at ${reminder.time}."
                    )
                }

                list.add(
                    AppNotification(
                        id = notifId,
                        title = title,
                        message = msg,
                        type = NotificationType.REMINDER,
                        severity = severity,
                        timeRemainingText = timeText,
                        targetTimestamp = reminderTargetMillis,
                        relatedId = reminder.id
                    )
                )
            } else if (diffMillis < 0 && diffMillis >= -(6 * 3600 * 1000L)) {
                // Scheduled earlier today (within last 6 hours)
                val notifId = "reminder_${reminder.id}_past"
                if (!dismissedIds.contains(notifId)) {
                    list.add(
                        AppNotification(
                            id = notifId,
                            title = "⏰ Past Reminder: ${reminder.name}",
                            message = "Was scheduled for ${reminder.time} today.",
                            type = NotificationType.REMINDER,
                            severity = NotificationSeverity.INFO,
                            timeRemainingText = "Today, ${reminder.time}",
                            targetTimestamp = reminderTargetMillis,
                            relatedId = reminder.id
                        )
                    )
                }
            }
        }

        // Sort by urgency: URGENT first, then WARNING, then INFO, then earliest target timestamp
        return list.sortedWith(
            compareByDescending<AppNotification> { it.severity.ordinal }
                .thenBy { it.targetTimestamp }
        )
    }

    private fun parseReminderEpoch(dateMillis: Long, timeStr: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        if (timeStr.isBlank()) return cal.timeInMillis

        try {
            val cleanTime = timeStr.trim().uppercase(Locale.getDefault())
            if (cleanTime.contains("AM") || cleanTime.contains("PM")) {
                val parsed = SimpleDateFormat("hh:mm a", Locale.getDefault()).parse(cleanTime)
                if (parsed != null) {
                    val pCal = Calendar.getInstance().apply { time = parsed }
                    cal.set(Calendar.HOUR_OF_DAY, pCal.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, pCal.get(Calendar.MINUTE))
                    cal.set(Calendar.SECOND, 0)
                    return cal.timeInMillis
                }
            } else if (cleanTime.contains(":")) {
                val parts = cleanTime.split(":")
                val h = parts[0].trim().toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.take(2)?.trim()?.toIntOrNull() ?: 0
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                return cal.timeInMillis
            }
        } catch (_: Exception) {}

        return cal.timeInMillis
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
