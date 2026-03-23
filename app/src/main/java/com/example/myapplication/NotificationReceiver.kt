package com.example.myapplication

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.myapplication.SHOW_NOTIFICATION"
        const val ACTION_UPDATE_NOTIFICATIONS = "UPDATE_NOTIFICATIONS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, 
            "android.intent.action.QUICKBOOT_POWERON", 
            ACTION_UPDATE_NOTIFICATIONS -> {
                restoreNotifications(context)
            }
            ACTION_SHOW_NOTIFICATION -> {
                val type = intent.getStringExtra("type")?.trim() ?: "morning"
                val title = intent.getStringExtra("title")?.trim() ?: "Ravose"
                val message = intent.getStringExtra("message")?.trim() ?: "Ideje a bőrápolási rutinnak!"
                
                showNotification(context, title, message, type)

                if (type == "morning" || type == "evening") {
                    rescheduleNextDay(context, type, title, message)
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ravose_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ravose emlékeztetők",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Napi rutin és lejárati emlékeztetők"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val requestCode = when(type) {
            "morning" -> 100
            "evening" -> 101
            "expiry" -> 102
            else -> 103
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = when(type) {
            "morning" -> 1
            "evening" -> 2
            "expiry" -> title.hashCode()
            else -> 3
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_spa)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Failed to send notification", e)
        }
    }

    private fun restoreNotifications(context: Context) {
        val prefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("UserEmail", "") ?: ""
        val userName = prefs.getString("UserName", "Szépségem")
        
        if (userEmail.isEmpty()) return

        if (prefs.getBoolean("${userEmail}_MorningNotif", true)) {
            val time = prefs.getString("${userEmail}_MorningTime", "08:00") ?: "08:00"
            scheduleByString(context, "morning", time, "Jó reggelt, $userName!", "Ideje a reggeli rutinnak! ✨")
        }

        if (prefs.getBoolean("${userEmail}_EveningNotif", true)) {
            val time = prefs.getString("${userEmail}_EveningTime", "20:00") ?: "20:00"
            scheduleByString(context, "evening", time, "Szép estét, $userName!", "Ne feledd az esti rutint! 🌙")
        }
    }

    private fun scheduleByString(context: Context, type: String, timeStr: String, title: String, message: String) {
        val calendar = Calendar.getInstance()
        try {
            if (timeStr.contains(" ")) {
                val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                val date = sdf.parse(timeStr)
                if (date != null) calendar.time = date
            } else {
                val parts = timeStr.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            scheduleAlarm(context, type, calendar.timeInMillis, title, message)
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Parse error for time: $timeStr", e)
        }
    }

    private fun scheduleAlarm(context: Context, type: String, timeInMillis: Long, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION
            putExtra("type", type)
            putExtra("title", title)
            putExtra("message", message)
        }

        val requestCode = if (type == "morning") 100 else 101
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Alarm failed", e)
        }
    }

    private fun rescheduleNextDay(context: Context, type: String, title: String, message: String) {
        val prefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("UserEmail", "") ?: ""
        if (userEmail.isEmpty()) return
        
        val timeKey = if (type == "morning") "${userEmail}_MorningTime" else "${userEmail}_EveningTime"
        val timeStr = prefs.getString(timeKey, if (type == "morning") "08:00" else "20:00") ?: "08:00"
        
        val hourMinute = if (timeStr.contains(" ")) timeStr.split(" ")[1] else timeStr
        scheduleByString(context, type, hourMinute, title, message)
    }
}
