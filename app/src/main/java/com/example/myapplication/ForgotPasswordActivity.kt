package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        supportActionBar?.hide()

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val btnReset = findViewById<Button>(R.id.btnResetPassword)
        val resetSection = findViewById<LinearLayout>(R.id.resetSection)
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordInput)
        val btnSave = findViewById<Button>(R.id.btnSaveNewPassword)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        val db = AppDatabase.getDatabase(this)
        var targetUser: User? = null

        btnBack.setOnClickListener { finish() }

        btnReset.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Kérjük, add meg az e-mail címedet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserByEmail(email)
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        targetUser = user
                        resetSection.visibility = View.VISIBLE
                        btnReset.visibility = View.GONE
                        emailInput.isEnabled = false
                        Toast.makeText(this@ForgotPasswordActivity, "E-mail cím ellenőrizve. Adj meg egy új jelszót!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Nincs ilyen e-mail címmel regisztrált felhasználó!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val newPass = newPasswordInput.text.toString()
            if (newPass.length < 4) {
                Toast.makeText(this, "A jelszónak legalább 4 karakterből kell állnia!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            targetUser?.let { user ->
                val updatedUser = user.copy(password = newPass)
                CoroutineScope(Dispatchers.IO).launch {
                    db.userDao().updateUser(updatedUser)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ForgotPasswordActivity, "Jelszó sikeresen megváltoztatva!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
}