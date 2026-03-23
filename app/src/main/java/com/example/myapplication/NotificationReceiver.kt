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
import java.util.*

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val type = intent.getStringExtra("type") ?: "morning"

        Log.d("NotificationReceiver", "OnReceive: action=$action, type=$type")

        if (action == Intent.ACTION_BOOT_COMPLETED) {

            restoreNotifications(context)
        } else {

            val title = intent.getStringExtra("title") ?: "Ravose"
            val message = intent.getStringExtra("message") ?: "Ideje a bőrápolási rutinnak!"

            showNotification(context, title, message, type)


            if (type == "morning" || type == "evening") {
                rescheduleNextDay(context, type, title, message)
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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Napi rutin emlékeztetők"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val requestCode = when (type) {
            "morning" -> 100
            "evening" -> 101
            "expiry" -> 102
            else -> 103
        }
        
        val notificationId = when (type) {
            "morning" -> 1
            "evening" -> 2
            "expiry" -> 3
            else -> 4
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_flower)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun rescheduleNextDay(context: Context, type: String, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        
        val timeKey = if (type == "morning") "MorningTime" else "EveningTime"
        val timeStr = prefs.getString(timeKey, if (type == "morning") "08:00" else "20:00") ?: "08:00"
        val parts = timeStr.split(":")
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun restoreNotifications(context: Context) {
        val prefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("UserName", "Szépségem")
        
        // Reggeli rutin
        if (prefs.getBoolean("MorningNotif", true)) {
            val time = prefs.getString("MorningTime", "08:00") ?: "08:00"
            val parts = time.split(":")
            scheduleOnce(context, "morning", parts[0].toInt(), parts[1].toInt(), 
                "Jó reggelt, $userName!", "Ideje a reggeli rutinnak! ✨")
        }

        // Esti rutin
        if (prefs.getBoolean("EveningNotif", true)) {
            val time = prefs.getString("EveningTime", "20:00") ?: "20:00"
            val parts = time.split(":")
            scheduleOnce(context, "evening", parts[0].toInt(), parts[1].toInt(), 
                "Szép estét, $userName!", "Ne feledd az esti rutint! 🌙")
        }

    }

    private fun scheduleOnce(context: Context, type: String, hour: Int, minute: Int, title: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }
}
