package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductsActivity : AppCompatActivity() {

    private lateinit var productDao: ProductDao
    private lateinit var productAdapter: ProductAdapter
    private lateinit var rvMain: RecyclerView
    private lateinit var tvNoResults: TextView
    private var loadJob: Job? = null
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_products)
        
        supportActionBar?.hide()

        val db = AppDatabase.getDatabase(this)
        productDao = db.productDao()

        rvMain = findViewById(R.id.rvMain)
        tvNoResults = findViewById(R.id.tvNoResults)
        
        setupRecyclerView()
        
        val userName = intent.getStringExtra("USER_NAME") ?: "Anna"
        setupNavigation(userName)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            findViewById<View>(R.id.bottomNavCard).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 20
            }
            insets
        }

        findViewById<EditText>(R.id.etSearch).addTextChangedListener { text ->
            currentQuery = text.toString().trim()
            updateList()
        }

        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        updateList()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            lifecycleScope.launch {
                productDao.deleteProduct(product)
                Toast.makeText(this@ProductsActivity, "Termék törölve", Toast.LENGTH_SHORT).show()
                updateList()
            }
        }
        rvMain.layoutManager = LinearLayoutManager(this)
        rvMain.adapter = productAdapter
    }

    private fun updateList() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            delay(200)
            productDao.getAllProducts().collectLatest { allProducts ->
                val filtered = if (currentQuery.isEmpty()) {
                    allProducts
                } else {
                    allProducts.filter { it.name.contains(currentQuery, ignoreCase = true) }
                }

                if (filtered.isEmpty()) {
                    productAdapter.submitList(emptyList())
                    tvNoResults.visibility = View.VISIBLE
                } else {
                    tvNoResults.visibility = View.GONE
                    // Csoportosítás kategóriák szerint
                    val groupedItems = mutableListOf<Any>()
                    val groupedMap = filtered.groupBy { it.type }
                    
                    // Rendezés kategória szerint (opcionális)
                    val sortedCategories = groupedMap.keys.sorted()
                    
                    for (category in sortedCategories) {
                        groupedItems.add(category) // Header
                        groupedItems.addAll(groupedMap[category] ?: emptyList()) // Termékek
                    }
                    productAdapter.submitList(groupedItems)
                }
            }
        }
    }

    private fun setupNavigation(userName: String) {
        val navHome = findViewById<LinearLayout>(R.id.navHomeLayout)
        val navStats = findViewById<LinearLayout>(R.id.navStatsLayout)
        val navProfile = findViewById<LinearLayout>(R.id.navProfileLayout)

        findViewById<ImageView>(R.id.ivNavProducts).setColorFilter(ContextCompat.getColor(this, R.color.deep_pink))
        findViewById<TextView>(R.id.tvNavProducts).apply {
            setTextColor(ContextCompat.getColor(this@ProductsActivity, R.color.deep_pink))
            paint.isFakeBoldText = true
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).putExtra("USER_NAME", userName))
            finish()
        }
        navStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java).putExtra("USER_NAME", userName))
            finish()
        }
        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_NAME", userName))
            finish()
        }
    }
}
