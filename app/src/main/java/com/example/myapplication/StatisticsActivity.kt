package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

class StatisticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)
        
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

        val userName = intent.getStringExtra("USER_NAME") ?: "Anna"

        // Vissza gomb
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
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

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish()
        }
    }
}