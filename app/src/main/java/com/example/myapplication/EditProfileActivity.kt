package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    private val PREFS_NAME = "ProfilePrefs"
    private var currentUserEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("IsLoggedIn", false)
        
        if (!isLoggedIn) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        supportActionBar?.hide()

        currentUserEmail = prefs.getString("UserEmail", "") ?: ""

        val mainView = findViewById<android.view.View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        val editSurname = findViewById<EditText>(R.id.editSurname)
        val editFirstName = findViewById<EditText>(R.id.editFirstName)
        val editNickname = findViewById<EditText>(R.id.editNickname)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val editPasswordConfirm = findViewById<EditText>(R.id.editPasswordConfirm)
        val btnSaveData = findViewById<Button>(R.id.btnSaveData)

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                db.userDao().getUserByEmail(currentUserEmail)
            }
            
            user?.let { u ->
                runOnUiThread {
                    editSurname.setText(u.surname)
                    editFirstName.setText(u.firstName)
                    editNickname.setText(u.nickname)
                    editPassword.setText(u.password)
                    editPasswordConfirm.setText(u.password)
                }
            }
        }

        btnSaveData.setOnClickListener {
            val surname = editSurname.text.toString().trim()
            val firstName = editFirstName.text.toString().trim()
            val nickname = editNickname.text.toString().trim()
            val pass = editPassword.text.toString()
            val confirm = editPasswordConfirm.text.toString()

            if (pass != confirm) {
                Toast.makeText(this, "A jelszavak nem egyeznek!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (firstName.isEmpty()) {
                Toast.makeText(this, "A keresztnév kötelező!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val displayName = if (nickname.isNotEmpty()) nickname else "$surname $firstName".trim()

            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    db.userDao().getUserByEmail(currentUserEmail)
                }
                
                user?.let { u ->
                    val updatedUser = u.copy(
                        surname = surname,
                        firstName = firstName,
                        nickname = nickname,
                        password = pass,
                        displayName = displayName
                    )
                    
                    withContext(Dispatchers.IO) {
                        db.userDao().updateUser(updatedUser)
                    }

                    prefs.edit()
                        .putString("UserName", displayName)
                        .apply()

                    runOnUiThread {
                        Toast.makeText(this@EditProfileActivity, "Adatok elmentve!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
}
