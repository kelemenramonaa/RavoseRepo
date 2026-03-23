package com.example.myapplication

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private val PREFS_NAME = "ProfilePrefs"
    private val KEY_SKIN_TYPE = "SkinType"
    private val KEY_MORNING_NOTIF = "MorningNotif"
    private val KEY_EVENING_NOTIF = "EveningNotif"
    private val KEY_MORNING_TIME = "MorningTime"
    private val KEY_EVENING_TIME = "EveningTime"
    private val KEY_USER_NAME = "UserName"
    private val KEY_USER_EMAIL = "UserEmail"
    private val KEY_PROFILE_IMAGE_URI = "ProfileImageUri"

    private var selectedSkinType: String? = "Zsíros"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.notif_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.notif_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val internalUri = saveImageToInternalStorage(it)
            if (internalUri != null) {
                savePreference(KEY_PROFILE_IMAGE_URI, internalUri.toString())
                loadProfileImage(internalUri.toString())
                Toast.makeText(this, getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.profile_photo_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        supportActionBar?.hide()

        val mainView = findViewById<View>(R.id.main)
        val bottomNavCard = findViewById<View>(R.id.bottomNavCard)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            bottomNavCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 20
            }
            insets
        }

        checkNotificationPermission()
        updateProfileUI()

        val btnDry = findViewById<MaterialButton>(R.id.btnDry)
        val btnOily = findViewById<MaterialButton>(R.id.btnOily)
        val btnCombo = findViewById<MaterialButton>(R.id.btnCombo)
        val btnSensitive = findViewById<MaterialButton>(R.id.btnSensitive)
        val skinButtons = listOf(btnDry, btnOily, btnCombo, btnSensitive)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        selectedSkinType = prefs.getString(KEY_SKIN_TYPE, "Zsíros")
        
        skinButtons.forEach { button ->
            button.setOnClickListener {
                updateSkinTypeSelection(button, skinButtons)
                savePreference(KEY_SKIN_TYPE, button.text.toString())
            }
        }

        val currentButton = skinButtons.find { it.text.toString() == selectedSkinType } ?: btnOily
        updateSkinTypeSelection(currentButton, skinButtons)

        val switchMorning = findViewById<SwitchMaterial>(R.id.switchMorning)
        val switchEvening = findViewById<SwitchMaterial>(R.id.switchEvening)
        val tvMorningTime = findViewById<TextView>(R.id.tvMorningTime)
        val tvEveningTime = findViewById<TextView>(R.id.tvEveningTime)

        switchMorning.isChecked = prefs.getBoolean(KEY_MORNING_NOTIF, true)
        switchEvening.isChecked = prefs.getBoolean(KEY_EVENING_NOTIF, true)

        val morningTime = prefs.getString(KEY_MORNING_TIME, "08:00") ?: "08:00"
        val eveningTime = prefs.getString(KEY_EVENING_TIME, "20:00") ?: "20:00"
        tvMorningTime.text = morningTime
        tvEveningTime.text = eveningTime

        tvMorningTime.setOnClickListener {
            showTimePicker(morningTime) { selectedTime ->
                tvMorningTime.text = selectedTime
                savePreference(KEY_MORNING_TIME, selectedTime)
                if (switchMorning.isChecked) {
                    val parts = selectedTime.split(":")
                    scheduleNotification("morning", parts[0].toInt(), parts[1].toInt())
                }
            }
        }

        tvEveningTime.setOnClickListener {
            showTimePicker(eveningTime) { selectedTime ->
                tvEveningTime.text = selectedTime
                savePreference(KEY_EVENING_TIME, selectedTime)
                if (switchEvening.isChecked) {
                    val parts = selectedTime.split(":")
                    scheduleNotification("evening", parts[0].toInt(), parts[1].toInt())
                }
            }
        }

        switchMorning.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_MORNING_NOTIF, isChecked)
            if (isChecked) {
                val parts = tvMorningTime.text.toString().split(":")
                scheduleNotification("morning", parts[0].toInt(), parts[1].toInt())
            } else {
                cancelNotification("morning")
            }
        }

        switchEvening.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_EVENING_NOTIF, isChecked)
            if (isChecked) {
                val parts = tvEveningTime.text.toString().split(":")
                scheduleNotification("evening", parts[0].toInt(), parts[1].toInt())
            } else {
                cancelNotification("evening")
            }
        }

        findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnChangePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            val p = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putBoolean("IsLoggedIn", false).putBoolean("RememberMe", false).apply()
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        val userName = prefs.getString(KEY_USER_NAME, "Anna") ?: "Anna"
        setupNavigation(userName)
    }

    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        }, hour, minute, true).show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm_title))
            .setMessage(getString(R.string.delete_confirm_msg))
            .setPositiveButton(getString(R.string.delete_btn)) { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton(getString(R.string.cancel_btn), null)
            .show()
    }

    private fun deleteUserAccount() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_USER_EMAIL, null)

        if (email == null) {
            Toast.makeText(this, getString(R.string.error_email_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ProfileActivity)
                val user = db.userDao().getUserByEmail(email)
                if (user != null) {
                    db.userDao().deleteUser(user)
                    prefs.edit().clear().apply()
                    Toast.makeText(this@ProfileActivity, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@ProfileActivity, getString(R.string.error_user_not_found), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, getString(R.string.error_general, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupNavigation(userName: String) {
        val navHome = findViewById<LinearLayout>(R.id.navHomeLayout)
        val navProducts = findViewById<LinearLayout>(R.id.navProductsLayout)
        val navStats = findViewById<LinearLayout>(R.id.navStatsLayout)
        val navProfile = findViewById<LinearLayout>(R.id.navProfileLayout)

        updateNavUI(navProfile, true)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }
        
        navProducts.setOnClickListener {
            val intent = Intent(this, ProductsActivity::class.java) 
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }

        navStats.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }
    }

    private fun updateNavUI(layout: LinearLayout, isActive: Boolean) {
        val icon = layout.getChildAt(0) as ImageView
        val text = layout.getChildAt(1) as TextView
        val color = if (isActive) R.color.deep_pink else R.color.slate_400
        
        icon.setColorFilter(ContextCompat.getColor(this, color))
        text.setTextColor(ContextCompat.getColor(this, color))
        text.paint.isFakeBoldText = isActive
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleNotification(type: String, hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userName = prefs.getString(KEY_USER_NAME, "Szépségem")
        
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("type", type)
            if (type == "morning") {
                putExtra("title", getString(R.string.notif_morning_title, userName))
                putExtra("message", getString(R.string.notif_morning_msg))
            } else {
                putExtra("title", getString(R.string.notif_evening_title, userName))
                putExtra("message", getString(R.string.notif_evening_msg))
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            if (type == "morning") 100 else 101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelNotification(type: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            if (type == "morning") 100 else 101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
    }

    private fun updateProfileUI() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tvName = findViewById<TextView>(R.id.profileName)
        val tvEmail = findViewById<TextView>(R.id.profileEmail)

        val userName = prefs.getString(KEY_USER_NAME, null) ?: intent.getStringExtra("USER_NAME") ?: "Kovács Elena"
        val userEmail = prefs.getString(KEY_USER_EMAIL, null) ?: intent.getStringExtra("USER_EMAIL") ?: "elena.kovacs@pelda.hu"

        tvName.text = userName
        tvEmail.text = userEmail
        
        if (prefs.getString(KEY_USER_EMAIL, null) == null && intent.hasExtra("USER_EMAIL")) {
            prefs.edit().putString(KEY_USER_NAME, userName).putString(KEY_USER_EMAIL, userEmail).apply()
        }

        val savedUri = prefs.getString(KEY_PROFILE_IMAGE_URI, null)
        loadProfileImage(savedUri)
    }

    private fun loadProfileImage(uriString: String?) {
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        if (uriString != null) {
            Glide.with(this)
                .load(Uri.parse(uriString))
                .circleCrop()
                .into(profileImage)
            profileImage.setPadding(0, 0, 0, 0)
        } else {
            profileImage.setImageResource(R.drawable.ic_person)
            profileImage.setPadding(60, 60, 60, 60)
        }
    }

    private fun savePreference(key: String, value: Any) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
        }
        editor.apply()
    }

    private fun updateSkinTypeSelection(selectedButton: MaterialButton, allButtons: List<MaterialButton>) {
        selectedSkinType = selectedButton.text.toString()
        val pinkColor = Color.parseColor("#F2C2C2")
        val grayText = Color.parseColor("#64748B")
        val strokeColor = Color.parseColor("#F1F5F9")

        allButtons.forEach { button ->
            if (button == selectedButton) {
                button.backgroundTintList = ColorStateList.valueOf(pinkColor)
                button.setTextColor(Color.WHITE)
                button.strokeWidth = 0
            } else {
                button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                button.setTextColor(grayText)
                button.strokeWidth = 2
                button.setStrokeColor(ColorStateList.valueOf(strokeColor))
            }
        }
    }
}
