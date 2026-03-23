package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private val PREFS_NAME = "ProfilePrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        val mainView = findViewById<android.view.View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val surnameInput = findViewById<EditText>(R.id.regSurname)
        val firstNameInput = findViewById<EditText>(R.id.regFirstName)
        val nicknameInput = findViewById<EditText>(R.id.regNickname)
        val emailInput = findViewById<EditText>(R.id.regEmail)
        val passwordInput = findViewById<EditText>(R.id.regPass)
        val confirmInput = findViewById<EditText>(R.id.regPassConfirm)
        val registerButton = findViewById<Button>(R.id.btnRegisterSubmit)
        val rememberMeCheck = findViewById<CheckBox>(R.id.regRememberMe)

        val db = AppDatabase.getDatabase(this)

        registerButton.setOnClickListener {
            val surname = surnameInput.text.toString().trim()
            val firstName = firstNameInput.text.toString().trim()
            val nickname = nicknameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()
            val isRememberMe = rememberMeCheck.isChecked

            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty()) {
                Toast.makeText(this, "Kérjük, töltsd ki a kötelező mezőket!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "A jelszavak nem egyeznek!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val displayName = if (nickname.isNotEmpty()) nickname else "$surname $firstName".trim()

            lifecycleScope.launch {
                try {
                    val existingUser = withContext(Dispatchers.IO) {
                        db.userDao().getUserByEmail(email)
                    }

                    if (existingUser != null) {
                        Toast.makeText(this@RegisterActivity, "Ez az e-mail cím már foglalt!", Toast.LENGTH_SHORT).show()
                    } else {
                        val newUser = User(
                            email = email,
                            surname = surname,
                            firstName = firstName,
                            nickname = nickname,
                            password = password,
                            displayName = displayName
                        )

                        withContext(Dispatchers.IO) {
                            db.userDao().insertUser(newUser)
                        }

                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("IsLoggedIn", true)
                            .putBoolean("RememberMe", isRememberMe)
                            .putString("UserEmail", email)
                            .putString("UserName", displayName)
                            .apply()

                        Toast.makeText(this@RegisterActivity, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
                        intent.putExtra("USER_NAME", displayName)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Hiba történt: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val loginLink = findViewById<TextView>(R.id.btnLoginLink)
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
