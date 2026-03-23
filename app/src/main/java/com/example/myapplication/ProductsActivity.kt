package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
    private var currentUserEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("IsLoggedIn", false)
        
        if (!isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_products)
        supportActionBar?.hide()

        currentUserEmail = prefs.getString("UserEmail", "") ?: ""
        val db = AppDatabase.getDatabase(this)
        productDao = db.productDao()

        rvMain = findViewById(R.id.rvMain)
        tvNoResults = findViewById(R.id.tvNoResults)
        
        setupRecyclerView()
        
        val userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("UserName", "Anna") ?: "Anna"
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@ProductsActivity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        })

        updateList()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList(), { product ->
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
            }
            startActivity(intent)
        }, { product ->
            lifecycleScope.launch {
                productDao.deleteProduct(product)
                Toast.makeText(this@ProductsActivity, "Termék törölve", Toast.LENGTH_SHORT).show()
                updateList()
            }
        })
        rvMain.layoutManager = LinearLayoutManager(this)
        rvMain.adapter = productAdapter
    }

    private fun updateList() {
        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            delay(200)
            productDao.getAllProducts(currentUserEmail).collectLatest { allProducts ->
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
                    val groupedItems = mutableListOf<Any>()
                    val groupedMap = filtered.groupBy { it.type }
                    val sortedCategories = groupedMap.keys.sorted()
                    
                    for (category in sortedCategories) {
                        groupedItems.add(category)
                        groupedItems.addAll(groupedMap[category] ?: emptyList())
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
