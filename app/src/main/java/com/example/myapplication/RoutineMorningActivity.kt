package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class RoutineMorningActivity : AppCompatActivity() {

    private lateinit var routineContainer: LinearLayout
    private lateinit var productDao: ProductDao
    private lateinit var tvSubtitle: TextView
    private lateinit var btnComplete: Button
    private lateinit var btnGenerate: Button
    
    private val PREFS_NAME = "RoutinePrefs"
    private val KEY_MORNING_ROUTINE = "MorningRoutine"
    private val KEY_MORNING_DONE = "MorningDone_"
    private val KEY_EVENING_DONE = "EveningDone_"
    private val KEY_LAST_DATE = "LastDate"
    private val KEY_STREAK = "StreakCount"
    
    private var currentRoutineTasks = mutableListOf<String>()
    private var currentConflictDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_morning)
        supportActionBar?.hide()

        productDao = AppDatabase.getDatabase(this).productDao()
        routineContainer = findViewById(R.id.routineContainer)
        tvSubtitle = findViewById(R.id.tvRoutineSubtitle)
        btnComplete = findViewById(R.id.btnCompleteRoutine)
        btnGenerate = findViewById(R.id.btnGenerateRoutine)

        loadSavedRoutine()

        btnGenerate.setOnClickListener {
            generateSmartRoutine()
        }

        btnComplete.setOnClickListener {
            completeRoutine()
        }

        findViewById<ImageButton>(R.id.btnSelectProduct).setOnClickListener {
            showProductSelector()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedRoutine()
    }

    private fun loadSavedRoutine() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MORNING_ROUTINE, null)
        if (saved != null && saved != "[]") {
            val jsonArray = JSONArray(saved)
            currentRoutineTasks.clear()
            for (i in 0 until jsonArray.length()) {
                currentRoutineTasks.add(jsonArray.getString(i))
            }

            val tasksSnapshot = currentRoutineTasks.toList()
            lifecycleScope.launch {
                val allProducts = productDao.getAllProductsOnce()
                var updated = false
                val newTasks = mutableListOf<String>()
                for (task in tasksSnapshot) {
                    val parts = task.split("|")
                    val productId = if (parts.size > 2) parts[2].toIntOrNull() else null
                    if (productId != null) {
                        val product = allProducts.find { it.id == productId }
                        if (product != null) {
                            val newTask = "${product.type}: ${product.name}|${product.imageUrl ?: ""}|${product.id}"
                            if (newTask != task) updated = true
                            newTasks.add(newTask)
                        } else {
                            newTasks.add(task)
                        }
                    } else {
                        newTasks.add(task)
                    }
                }
                if (updated) {
                    currentRoutineTasks = newTasks
                    saveRoutine()
                }
                refreshUI()
                performFullCompatibilityCheck(showAlert = false)
            }
            btnComplete.visibility = View.VISIBLE
        } else {
            btnComplete.visibility = View.GONE
            currentRoutineTasks.clear()
            refreshUI()
        }
    }

    private fun completeRoutine() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        
        prefs.edit().putBoolean(KEY_MORNING_DONE + today, true).apply()

        val eveningDone = prefs.getBoolean(KEY_EVENING_DONE + today, false)
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        
        if (eveningDone && lastDate != today) {
            val streak = prefs.getInt(KEY_STREAK, 0)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            
            val newStreak = if (lastDate == yesterday || lastDate == "") streak + 1 else 1
            prefs.edit()
                .putInt(KEY_STREAK, newStreak)
                .putString(KEY_LAST_DATE, today)
                .apply()
        }
        
        Toast.makeText(this, "Reggeli rutin kész! ✨", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun generateSmartRoutine() {
        lifecycleScope.launch {
            val allProducts = productDao.getAllProductsOnce().sortedByDescending { it.id }
            val morningSteps = listOf(
                Pair("Arctisztító", null),
                Pair("Tonik", null),
                Pair("Esszencia", null),
                Pair("Szérum", "Ascorbic Acid (C-vitamin)"),
                Pair("Szérum", null),
                Pair("Hidratáló", null),
                Pair("Fényvédő", null)
            )
            
            val generatedTasks = mutableListOf<String>()
            generatedTasks.add("Arcmosás: Langyos víz|")

            for (step in morningSteps) {
                val category = step.first
                val requiredIngredient = step.second
                
                val product = allProducts.find { p ->
                    p.type.contains(category, ignoreCase = true) && 
                    (requiredIngredient == null || p.ingredients?.contains(requiredIngredient, ignoreCase = true) == true)
                }
                
                if (product != null && generatedTasks.none { it.endsWith("|${product.id}") }) {
                    generatedTasks.add("${product.type}: ${product.name}|${product.imageUrl ?: ""}|${product.id}")
                }
            }

            currentRoutineTasks = generatedTasks
            saveRoutine()
            runOnUiThread {
                refreshUI()
                performFullCompatibilityCheck(showAlert = true)
                btnComplete.visibility = View.VISIBLE
                Toast.makeText(this@RoutineMorningActivity, "Rutin generálva!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProductSelector() {
        lifecycleScope.launch {
            val allProducts = productDao.getAllProductsOnce()
            val displayNames = allProducts.map { "${it.type}: ${it.name}" }.toTypedArray()

            if (displayNames.isEmpty()) {
                runOnUiThread { Toast.makeText(this@RoutineMorningActivity, "Még nincsenek termékeid!", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            runOnUiThread {
                AlertDialog.Builder(this@RoutineMorningActivity)
                    .setTitle("Új lépés beszúrása")
                    .setItems(displayNames) { _, which ->
                        val p = allProducts[which]
                        currentRoutineTasks.add("${p.type}: ${p.name}|${p.imageUrl ?: ""}|${p.id}")
                        saveRoutine()
                        refreshUI()
                        performFullCompatibilityCheck(showAlert = true)
                        btnComplete.visibility = View.VISIBLE
                    }
                    .show()
            }
        }
    }

    private fun refreshUI() {
        routineContainer.removeAllViews()
        tvSubtitle.text = "${currentRoutineTasks.size} lépés • ${currentRoutineTasks.size * 2} perc összesen"

        currentRoutineTasks.forEachIndexed { index, taskData ->
            val parts = taskData.split("|")
            val info = parts[0]
            val imageUrl = if (parts.size > 1) parts[1] else ""
            val productId = if (parts.size > 2) parts[2].toIntOrNull() else null
            
            val stepView = LayoutInflater.from(this).inflate(R.layout.item_routine_step, routineContainer, false)
            
            val tvNum = stepView.findViewById<TextView>(R.id.tvStepNumber)
            val tvCat = stepView.findViewById<TextView>(R.id.tvStepCategory)
            val tvBrand = stepView.findViewById<TextView>(R.id.tvProductBrand)
            val tvName = stepView.findViewById<TextView>(R.id.tvProductName)
            val ivImg = stepView.findViewById<ImageView>(R.id.ivProductImage)
            val btnDelete = stepView.findViewById<ImageButton>(R.id.btnDeleteStep)
            val line = stepView.findViewById<View>(R.id.timelineLine)
            val cardView = stepView.findViewById<View>(R.id.cardProduct)

            tvNum.text = (index + 1).toString()
            if (index == currentRoutineTasks.size - 1) line.visibility = View.GONE

            val infoParts = info.split(":")
            if (infoParts.size > 1) {
                tvCat.text = infoParts[0].trim()
                tvName.text = infoParts[1].trim()
                tvBrand.visibility = View.GONE
            } else {
                tvCat.text = "Lépés"
                tvName.text = info
                tvBrand.visibility = View.GONE
            }

            if (imageUrl.isNotEmpty()) {
                ivImg.setPadding(0, 0, 0, 0)
                ivImg.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this).load(imageUrl).into(ivImg)
            } else {
                ivImg.setImageResource(R.drawable.ic_product_bottle)
                ivImg.scaleType = ImageView.ScaleType.CENTER_INSIDE
                ivImg.setPadding(10, 10, 10, 10)
            }

            cardView?.setOnClickListener {
                productId?.let { id ->
                    val intent = Intent(this, AddProductActivity::class.java).apply {
                        putExtra("PRODUCT_ID", id)
                    }
                    startActivity(intent)
                }
            }

            btnDelete.setOnClickListener {
                currentRoutineTasks.removeAt(index)
                saveRoutine()
                refreshUI()
                performFullCompatibilityCheck(showAlert = false)
                if (currentRoutineTasks.isEmpty()) btnComplete.visibility = View.GONE
            }

            routineContainer.addView(stepView)
        }
    }

    private fun performFullCompatibilityCheck(showAlert: Boolean) {
        lifecycleScope.launch {
            val allProducts = productDao.getAllProductsOnce()
            val routineProductIds = currentRoutineTasks.mapNotNull { it.split("|").getOrNull(2)?.toIntOrNull() }
            val productsInRoutine = allProducts.filter { it.id in routineProductIds }

            val allIngredients = productsInRoutine.flatMap { it.ingredients?.split(",")?.map { i -> i.trim() } ?: emptyList() }
            val conflict = CompatibilityChecker.findConflicts(allIngredients)
            
            runOnUiThread {
                if (conflict != null) {
                    if (showAlert) showConflictDialog(conflict)
                } else {
                    if (currentConflictDialog?.isShowing == true) {
                        currentConflictDialog?.dismiss()
                        Toast.makeText(this@RoutineMorningActivity, "Kompatibilitási probléma megoldva! ✅", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showConflictDialog(conflict: CompatibilityChecker.Conflict) {
        if (currentConflictDialog?.isShowing == true) return
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_compatibility_warning, null)
        currentConflictDialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).setView(dialogView).create()
        currentConflictDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.tvConflictMessage).text = conflict.message
        dialogView.findViewById<Button>(R.id.btnGotIt).setOnClickListener { currentConflictDialog?.dismiss() }
        currentConflictDialog?.show()
    }

    private fun saveRoutine() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray(currentRoutineTasks)
        prefs.edit().putString(KEY_MORNING_ROUTINE, jsonArray.toString()).apply()
    }
}
