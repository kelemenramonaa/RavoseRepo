package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import java.util.Calendar

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        supportActionBar?.hide()

        val userName = intent.getStringExtra("USER_NAME") ?: "Anna"
        findViewById<TextView>(R.id.welcomeText).text = "Szia, $userName"

        setDailyTip()

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

        findViewById<LinearLayout>(R.id.morningRoutineCard).setOnClickListener {
            startActivity(Intent(this, RoutineMorningActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.eveningRoutineCard).setOnClickListener {
            startActivity(Intent(this, RoutineEveningActivity::class.java))
        }

        findViewById<TextView>(R.id.navProducts).setOnClickListener {
            val intent = Intent(this, ProductsActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }
        
        findViewById<TextView>(R.id.navStats).setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }
    }

    private fun setDailyTip() {
        val tips = listOf(
            "Ne feledd a fényvédőt! A nap sugarai felhős időben is érik a bőrödet.",
            "Hidratálj belülről is! Igyál meg ma legalább 2 liter vizet.",
            "A dupla tisztítás az esti rutin alapja a tiszta pórusokért.",
            "C-vitamin szérumot reggel használj az antioxidáns védelemért.",
            "A retinol segít a bőr megújulásában, de csak este használd!",
            "Cseréld le a párnahuzatodat hetente a pattanások megelőzése érdekében.",
            "A rendszeresség a kulcs: ne hagyd ki az esti rutint fáradtan sem!"
        )
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        findViewById<TextView>(R.id.dailyTipText).text = tips[dayOfYear % tips.size]
    }
}