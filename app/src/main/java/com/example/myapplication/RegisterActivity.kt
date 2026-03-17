package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {
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

        val firstNameInput = findViewById<EditText>(R.id.regFirstName)
        val nicknameInput = findViewById<EditText>(R.id.regNickname)
        val registerButton = findViewById<Button>(R.id.btnRegisterSubmit)

        // Regisztráció gomb -> Főoldal
        registerButton.setOnClickListener {
            val nickname = nicknameInput.text.toString()
            val firstName = firstNameInput.text.toString()
            
            // Ha van becenév, azt használjuk, különben a keresztnevet
            val displayName = when {
                nickname.isNotEmpty() -> nickname
                firstName.isNotEmpty() -> firstName
                else -> "Anna"
            }

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_NAME", displayName)
            startActivity(intent)
            finish()
        }

        // Átlépés a bejelentkezéshez
        val loginLink = findViewById<TextView>(R.id.btnLoginLink)
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}