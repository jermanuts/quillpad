package org.qosp.notes.ui.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.flow.first
import org.qosp.notes.App
import org.qosp.notes.R
import org.qosp.notes.data.repo.ReminderRepository
import java.time.Instant
import java.time.ZonedDateTime

class ReminderManager(
    private val context: Context,
    private val reminderRepository: ReminderRepository,
) {
    private fun requestBroadcast(reminderId: Long, noteId: Long): PendingIntent {
        val notificationIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtras(
                bundleOf(
                    "noteId" to noteId,
                    "reminderId" to reminderId,
                )
            )
            action = ReminderReceiver.REMINDER_HAS_FIRED
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun schedule(reminderId: Long, dateTime: Long, noteId: Long) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        val broadcast = requestBroadcast(reminderId, noteId)
        alarmManager.cancel(broadcast)
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            dateTime * 1000, // convert seconds to millis
            broadcast
        )
    }

    fun cancel(reminderId: Long, noteId: Long) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java) ?: return
        alarmManager.cancel(requestBroadcast(reminderId, noteId))
    }

    suspend fun cancelAllRemindersForNote(noteId: Long) {
        val reminders = reminderRepository.getByNoteId(noteId).first()
        reminders.forEach { cancel(it.id, noteId) }
    }

    suspend fun rescheduleAll() {
        reminderRepository
            .getAll()
            .first()
            .forEach { reminder ->
                val reminderDate = Instant.ofEpochSecond(reminder.date)
                when {
                    reminderDate.isBefore(ZonedDateTime.now().toInstant()) -> reminderRepository.deleteById(reminder.id)
                    else -> schedule(reminder.id, reminder.date, reminder.noteId)
                }
            }
    }

    suspend fun sendNotification(reminderId: Long, noteId: Long) {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

        var notificationTitle = ""

        reminderRepository.getById(reminderId).first()?.let { notificationTitle = it.name }
        reminderRepository.deleteById(reminderId)

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.fragment_editor)
            .setArguments(
                bundleOf(
                    "noteId" to noteId,
                    "transitionName" to ""
                )
            )
            .createPendingIntent()

        val notification = NotificationCompat.Builder(context, App.REMINDERS_CHANNEL_ID)
            .setContentText(notificationTitle)
            .setContentTitle(context.getString(R.string.notification_reminder_fired))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }
}