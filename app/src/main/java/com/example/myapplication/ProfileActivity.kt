package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        supportActionBar?.hide()

        val mainView = findViewById<android.view.View>(R.id.main)
        val bottomNav = findViewById<LinearLayout>(R.id.bottomNav)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            bottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }

        // Név és email beállítása (Példa adatok, később adatbázisból jöhet)
        val userName = intent.getStringExtra("USER_NAME") ?: "Anna"
        findViewById<TextView>(R.id.profileName).text = userName
        findViewById<TextView>(R.id.profileEmail).text = "$userName@email.hu".lowercase().replace(" ", "")

        // Kijelentkezés
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Navigáció
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }
        
        findViewById<TextView>(R.id.navProducts).setOnClickListener {
            val intent = Intent(this, ProductsActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.navCalendar).setOnClickListener {
            // Naptár oldal következik
        }
    }
}