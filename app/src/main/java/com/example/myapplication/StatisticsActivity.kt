package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private val PREFS_NAME = "RoutinePrefs"
    private val KEY_STREAK = "StreakCount"
    private val KEY_MORNING_DONE = "MorningDone_"
    private val KEY_EVENING_DONE = "EveningDone_"
    private val KEY_MORNING_ROUTINE = "MorningRoutine"
    private val KEY_EVENING_ROUTINE = "EveningRoutine"

    private var currentMode = "WEEKLY" // WEEKLY, MONTHLY, YEARLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_statistics)
        
        supportActionBar?.hide()

        val mainView = findViewById<View>(R.id.main)
        val bottomNavCard = findViewById<View>(R.id.bottomNavCard)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            bottomNavCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 20
            }
            insets
        }

        val userName = intent.getStringExtra("USER_NAME") ?: "Anna"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        setupTabs()
        updateUI()
        setupNavigation(userName)
    }

    private fun setupNavigation(userName: String) {
        val navHome = findViewById<LinearLayout>(R.id.navHomeLayout)
        val navProducts = findViewById<LinearLayout>(R.id.navProductsLayout)
        val navStats = findViewById<LinearLayout>(R.id.navStatsLayout)
        val navProfile = findViewById<LinearLayout>(R.id.navProfileLayout)

        updateNavUI(navStats, true)

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

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
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

    override fun onResume() {
        super.onResume()
        updateUI() 
    }

    private fun setupTabs() {
        val tabWeekly = findViewById<TextView>(R.id.tabWeekly)
        val tabMonthly = findViewById<TextView>(R.id.tabMonthly)
        val tabYearly = findViewById<TextView>(R.id.tabYearly)

        val tabs = listOf(tabWeekly, tabMonthly, tabYearly)

        tabs.forEach { tab ->
            tab.setOnClickListener {
                tabs.forEach { t ->
                    t.background = null
                    t.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
                    t.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                tab.setBackgroundResource(R.drawable.edittext_rounded_white)
                tab.setTextColor(ContextCompat.getColor(this, R.color.slate_800))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)

                currentMode = when (tab.id) {
                    R.id.tabWeekly -> "WEEKLY"
                    R.id.tabMonthly -> "MONTHLY"
                    R.id.tabYearly -> "YEARLY"
                    else -> "WEEKLY"
                }
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        
        val morningRoutine = prefs.getString(KEY_MORNING_ROUTINE, "[]")
        val eveningRoutine = prefs.getString(KEY_EVENING_ROUTINE, "[]")
        val morningTasks = JSONArray(morningRoutine)
        val eveningTasks = JSONArray(eveningRoutine)
        val mStepCount = morningTasks.length()
        val eStepCount = eveningTasks.length()

        val streak = prefs.getInt(KEY_STREAK, 0)
        findViewById<TextView>(R.id.tvStreakCountLarge).text = streak.toString()

        var grandTotalSteps = 0
        val tempCal = Calendar.getInstance()
        for (i in 0 until 365) {
            val dStr = sdf.format(tempCal.time)
            if (prefs.getBoolean(KEY_MORNING_DONE + dStr, false)) grandTotalSteps += mStepCount
            if (prefs.getBoolean(KEY_EVENING_DONE + dStr, false)) grandTotalSteps += eStepCount
            tempCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        findViewById<TextView>(R.id.tvTotalDone).text = grandTotalSteps.toString()

        val container = findViewById<LinearLayout>(R.id.categoryStatsContainer)
        container.removeAllViews()

        val categoryMap = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance()
        val daysToLookBack = when(currentMode) {
            "WEEKLY" -> ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            "MONTHLY" -> cal.get(Calendar.DAY_OF_MONTH)
            "YEARLY" -> cal.get(Calendar.DAY_OF_YEAR)
            else -> 7
        }

        var actualRoutinesDone = 0
        var totalStepsInPeriod = 0
        val statsCal = Calendar.getInstance()
        
        for (i in 0 until daysToLookBack) {
            val dStr = sdf.format(statsCal.time)
            val morningDone = prefs.getBoolean(KEY_MORNING_DONE + dStr, false)
            val eveningDone = prefs.getBoolean(KEY_EVENING_DONE + dStr, false)

            if (morningDone) {
                actualRoutinesDone++
                totalStepsInPeriod += mStepCount
                for (j in 0 until mStepCount) {
                    val cat = morningTasks.getString(j).split(":")[0].trim()
                    categoryMap[cat] = categoryMap.getOrDefault(cat, 0) + 1
                }
            }
            if (eveningDone) {
                actualRoutinesDone++
                totalStepsInPeriod += eStepCount
                for (j in 0 until eStepCount) {
                    val cat = eveningTasks.getString(j).split(":")[0].trim()
                    categoryMap[cat] = categoryMap.getOrDefault(cat, 0) + 1
                }
            }
            statsCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        if (categoryMap.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Még nincs adat az időszakra."
            tv.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, 40, 0, 40)
            container.addView(tv)
        } else {
            categoryMap.toList().sortedByDescending { it.second }.forEach { (cat, count) ->
                addCategoryRow(container, cat, count, daysToLookBack)
            }
        }

        val maxPossibleSteps = daysToLookBack * (mStepCount + eStepCount)
        val percent = if (maxPossibleSteps > 0) (totalStepsInPeriod * 100) / maxPossibleSteps else 0
        findViewById<TextView>(R.id.tvWeeklyPercent).text = "$percent%"
        
        val averageStepsPerDay = if (daysToLookBack > 0) totalStepsInPeriod.toFloat() / daysToLookBack.toFloat() else 0f
        findViewById<TextView>(R.id.tvWeeklyAverage).text = String.format(Locale.getDefault(), "%.1f", averageStepsPerDay)

        findViewById<TextView>(R.id.tvWeeklyRange).text = when(currentMode) {
            "WEEKLY" -> "Ezen a héten"
            "MONTHLY" -> "Ebben a hónapban"
            "YEARLY" -> "Ebben az évben"
            else -> ""
        }
        findViewById<TextView>(R.id.tvAverageLabel).text = when(currentMode) {
            "WEEKLY" -> "Heti átlag (lépés/nap)"
            "MONTHLY" -> "Havi átlag (lépés/nap)"
            "YEARLY" -> "Éves átlag (lépés/nap)"
            else -> "Átlag"
        }

        val insightText = if (percent == 0) "Vágj bele a mai rutinodba, a bőröd hálás lesz érte!"
                         else if (percent > 80) "Fantasztikus! Majdnem minden lépést elvégeztél. Csak így tovább!"
                         else if (percent > 50) "Jól haladsz! Minden elvégzett rutin egy lépés a célod felé."
                         else "Minden lépés számít! Folytasd bátran, megéri a törődést."
        findViewById<TextView>(R.id.tvInsightText).text = insightText
    }

    private fun addCategoryRow(container: LinearLayout, category: String, count: Int, totalDays: Int) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        val rowParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rowParams.setMargins(0, 0, 0, 24)
        row.layoutParams = rowParams

        val header = RelativeLayout(this)
        val tvCat = TextView(this)
        tvCat.text = category
        tvCat.setTextColor(ContextCompat.getColor(this, R.color.slate_800))
        tvCat.textSize = 14f
        tvCat.setTypeface(null, android.graphics.Typeface.BOLD)

        val tvCount = TextView(this)
        tvCount.text = "$count alkalom"
        tvCount.setTextColor(ContextCompat.getColor(this, R.color.slate_400))
        tvCount.textSize = 12f
        val countParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        countParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        tvCount.layoutParams = countParams

        header.addView(tvCat)
        header.addView(tvCount)
        row.addView(header)

        val frame = FrameLayout(this)
        val frameParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (8 * resources.displayMetrics.density).toInt())
        frameParams.setMargins(0, 12, 0, 0)
        frame.layoutParams = frameParams

        val progressBg = View(this)
        progressBg.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        progressBg.setBackgroundResource(R.drawable.edittext_rounded_white)
        progressBg.backgroundTintList = ContextCompat.getColorStateList(this, R.color.stat_inactive_gray)
        frame.addView(progressBg)

        val maxPossible = totalDays * 2
        val ratio = if (maxPossible > 0) count.toFloat() / maxPossible.toFloat() else 0f
        val screenWidth = resources.displayMetrics.widthPixels - (40 * resources.displayMetrics.density)
        val actualWidth = (screenWidth * ratio.coerceIn(0f, 1f)).toInt()

        if (actualWidth > 0) {
            val coloredBar = View(this)
            coloredBar.layoutParams = FrameLayout.LayoutParams(actualWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            coloredBar.setBackgroundResource(R.drawable.edittext_rounded_white)
            coloredBar.backgroundTintList = ContextCompat.getColorStateList(this, R.color.powder_pink)
            frame.addView(coloredBar)
        }

        row.addView(frame)
        container.addView(row)
    }
}
