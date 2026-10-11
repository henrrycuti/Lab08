package com.codelabs.lab08

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class TaskReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val db = Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "task_db"
        ).build()

        val pending = runBlocking { db.taskDao().getAllTasks() }.count { !it.isCompleted }
        db.close()

        if (pending > 0) {
            showReminder(context, pending)
        }
        return Result.success()
    }
}

@SuppressLint("MissingPermission")
fun showReminder(context: Context, pending: Int) {
    val channelId = "task_reminders"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Recordatorios de tareas",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return

    val text = if (pending == 1) "Tienes 1 tarea pendiente" else "Tienes $pending tareas pendientes"

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Mis tareas")
        .setContentText(text)
        .setAutoCancel(true)
        .build()

    manager.notify(1, notification)
}