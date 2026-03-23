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

class EditProfileActivity : AppCompatActivity() {

    private val PREFS_NAME = "ProfilePrefs"
    private val KEY_SURNAME = "UserSurname"
    private val KEY_FIRST_NAME = "UserFirstName"
    private val KEY_NICKNAME = "UserNickname"
    private val KEY_PASSWORD = "UserPassword"
    private val KEY_USER_NAME = "UserName" // A profil fejlécében megjelenő név

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        supportActionBar?.hide()

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

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val editSurname = findViewById<EditText>(R.id.editSurname)
        val editFirstName = findViewById<EditText>(R.id.editFirstName)
        val editNickname = findViewById<EditText>(R.id.editNickname)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val editPasswordConfirm = findViewById<EditText>(R.id.editPasswordConfirm)
        val btnSaveData = findViewById<Button>(R.id.btnSaveData)

        // Adatok betöltése
        editSurname.setText(prefs.getString(KEY_SURNAME, ""))
        editFirstName.setText(prefs.getString(KEY_FIRST_NAME, ""))
        editNickname.setText(prefs.getString(KEY_NICKNAME, ""))
        editPassword.setText(prefs.getString(KEY_PASSWORD, ""))
        editPasswordConfirm.setText(prefs.getString(KEY_PASSWORD, ""))

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

            if (surname.isEmpty() && firstName.isEmpty() && nickname.isEmpty()) {
                Toast.makeText(this, "Kérjük, adj meg legalább egy nevet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveAllData(surname, firstName, nickname, pass)
            Toast.makeText(this, "Adatok elmentve!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveAllData(surname: String, firstName: String, nickname: String, password: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Becenév az elsődleges, ha nincs, akkor a Vezetéknév + Keresztnév
        val displayName = when {
            nickname.isNotEmpty() -> nickname
            surname.isNotEmpty() || firstName.isNotEmpty() -> "$surname $firstName".trim()
            else -> "Felhasználó"
        }
        
        prefs.edit()
            .putString(KEY_SURNAME, surname)
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_USER_NAME, displayName)
            .apply()
    }
}