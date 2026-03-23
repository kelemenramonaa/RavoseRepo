package com.example.myapplication

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var photoCard: CardView
    private lateinit var photoOverlay: View
    private lateinit var productDao: ProductDao
    private lateinit var editBrand: EditText
    private lateinit var editName: EditText
    private lateinit var editNote: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var tvIngredientsSelector: TextView
    private lateinit var layoutExpiry: View
    private lateinit var tvExpiryDisplay: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnSave: Button
    
    private var currentImageUrl: String? = null
    private var selectedExpiryDate: String? = null
    private var editingProductId: Int? = null
    private var existingProduct: Product? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val activeIngredientsList = listOf(
        "Retinol", "Retinal (Retinaldehyde)", "Tretinoin", "Bakuchiol",
        "Ascorbic Acid (C-vitamin)", "Sodium Ascorbyl Phosphate", "Tetrahexyldecyl Ascorbate",
        "Niacinamide (B3-vitamin)", "Panthenol (B5-vitamin)", "Tocopherol (E-vitamin)",
        "Hyaluronic Acid", "Sodium Hyaluronate", "Glycolic Acid", "Lactic Acid",
        "Mandelic Acid", "Salicylic Acid", "Gluconolactone", "Lactobionic Acid",
        "Azelaic Acid", "Tranexamic Acid", "Alpha Arbutin", "Kojic Acid",
        "Ferulic Acid", "Resveratrol", "Ubiquinone (Q10)",
        "Matrixyl 3000 (Palmitoyl Tripeptide-1, Palmitoyl Tetrapeptide-7)",
        "Argireline (Acetyl Hexapeptide-8)", "Copper Tripeptide-1",
        "Ceramide NP / AP / EOP", "Centella Asiatica (Asiaticoside, Madecassoside)",
        "Allantoin", "Bisabolol", "Caffeine", "Benzoyl Peroxide", "Sulfur", "Adapalene"
    )

    private var selectedItems = BooleanArray(activeIngredientsList.size)
    private val customIngredients = mutableListOf<String>()

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.extras?.get("data") as? Bitmap
                }
                
                imageBitmap?.let {
                    imageView.setImageBitmap(it)
                    imageView.setPadding(0, 0, 0, 0)
                    imageView.clearColorFilter()
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    photoOverlay.visibility = View.GONE
                    
                    saveImageToInternalStorage(it)
                    processImageWithMLKit(it)
                }
            }
        }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.close()
            currentImageUrl = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Hiba a kép mentésekor", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        supportActionBar?.hide()

        val db = AppDatabase.getDatabase(this)
        productDao = db.productDao()

        initViews()
        setupTypeSpinner()
        
        val productId = intent.getIntExtra("PRODUCT_ID", -1)
        if (productId != -1) {
            editingProductId = productId
            tvTitle.text = "Termék szerkesztése"
            btnSave.text = "Módosítások mentése"
            loadProductData(productId)
        }

        tvIngredientsSelector.setOnClickListener {
            showMultiSelectIngredientsDialog()
        }

        layoutExpiry.setOnClickListener {
            showDatePickerDialog()
        }

        photoCard.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnAddCustomIngredient).setOnClickListener {
            showCustomIngredientDialog()
        }

        btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun initViews() {
        photoCard = findViewById(R.id.photoCard)
        imageView = findViewById(R.id.productImageView)
        photoOverlay = findViewById(R.id.photoOverlay)
        editBrand = findViewById(R.id.editProductBrand)
        editName = findViewById(R.id.editProductName)
        editNote = findViewById(R.id.editNote)
        spinnerType = findViewById(R.id.spinnerType)
        tvIngredientsSelector = findViewById(R.id.tvIngredientsSelector)
        layoutExpiry = findViewById(R.id.layoutExpiry)
        tvExpiryDisplay = findViewById(R.id.tvExpiryDisplay)
        tvTitle = findViewById(R.id.tvToolbarTitle)
        btnSave = findViewById(R.id.btnSaveProduct)
    }

    private fun setupTypeSpinner() {
        val categories = listOf(
            "Arctisztító", "Olajos lemosó", "Tonik", "Hámlasztó", "Esszencia", 
            "Ampulla", "Szérum", "Hidratáló", "Arcolaj", "Fényvédő", 
            "Szemkörnyékápoló", "Maszk", "Célzott kezelés", "Ajakápoló"
        ).sorted()
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter
    }

    private fun loadProductData(id: Int) {
        lifecycleScope.launch {
            val product = productDao.getProductById(id)
            product?.let { p ->
                existingProduct = p
                runOnUiThread {
                    editBrand.setText(p.brand)
                    editName.setText(p.name)
                    editNote.setText(p.note)
                    
                    @Suppress("UNCHECKED_CAST")
                    val adapter = spinnerType.adapter as ArrayAdapter<String>
                    val position = adapter.getPosition(p.type)
                    if (position >= 0) spinnerType.setSelection(position)
                    
                    selectedExpiryDate = p.expiryDate
                    tvExpiryDisplay.text = p.expiryDate ?: "Nincs megadva"
                    
                    p.ingredients?.let { ingredientsStr ->
                        tvIngredientsSelector.text = ingredientsStr
                        val items = ingredientsStr.split(", ").map { it.trim() }
                        items.forEach { item ->
                            val index = activeIngredientsList.indexOf(item)
                            if (index >= 0) {
                                selectedItems[index] = true
                            } else {
                                if (!customIngredients.contains(item)) {
                                    customIngredients.add(item)
                                }
                            }
                        }
                    }
                    
                    currentImageUrl = p.imageUrl
                    if (!p.imageUrl.isNullOrEmpty()) {
                        imageView.setPadding(0, 0, 0, 0)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        photoOverlay.visibility = View.GONE
                        Glide.with(this@AddProductActivity).load(p.imageUrl).into(imageView)
                    }
                }
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        if (selectedExpiryDate != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.parse(selectedExpiryDate!!)?.let { calendar.time = it }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            selectedExpiryDate = date
            tvExpiryDisplay.text = date
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun saveProduct() {
        val brand = editBrand.text.toString().trim()
        val name = editName.text.toString().trim()
        val type = spinnerType.selectedItem.toString()
        val note = editNote.text.toString().trim()
        var ingredientsString = tvIngredientsSelector.text.toString()
        
        if (ingredientsString == "Válassz összetevőket...") {
            ingredientsString = ""
        }

        if (name.isNotEmpty()) {
            lifecycleScope.launch {
                val productToSave = if (editingProductId != null) {
                    existingProduct?.copy(
                        brand = brand,
                        name = name,
                        type = type,
                        note = note,
                        ingredients = ingredientsString.ifEmpty { null },
                        expiryDate = selectedExpiryDate,
                        imageUrl = currentImageUrl
                    )
                } else {
                    Product(
                        brand = brand,
                        name = name,
                        imageUrl = currentImageUrl,
                        ingredients = ingredientsString.ifEmpty { null },
                        type = type,
                        note = note,
                        expiryDate = selectedExpiryDate
                    )
                }

                productToSave?.let {
                    if (editingProductId != null) {
                        productDao.updateProduct(it)
                    } else {
                        productDao.insertProduct(it)
                    }

                    selectedExpiryDate?.let { date ->
                        scheduleExpiryNotification(name, date)
                    }

                    runOnUiThread {
                        Toast.makeText(this@AddProductActivity, 
                            if (editingProductId != null) "Termék módosítva!" else "Termék mentve!", 
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Kérjük, töltse ki a termék nevét!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleExpiryNotification(productName: String, expiryDateStr: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDate = sdf.parse(expiryDateStr) ?: return
            
            val calendar = Calendar.getInstance()
            calendar.time = expiryDate
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                val intent = Intent(this, NotificationReceiver::class.java).apply {
                    putExtra("title", "Lejáró termék!")
                    putExtra("message", "A(z) $productName egy hét múlva lejár.")
                    putExtra("type", "expiry")
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    productName.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = getSystemService(AlarmManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun showMultiSelectIngredientsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Válassz összetevőket")
        builder.setMultiChoiceItems(activeIngredientsList.toTypedArray(), selectedItems) { _, which, isChecked ->
            selectedItems[which] = isChecked
        }
        builder.setPositiveButton("Kész") { _, _ ->
            updateIngredientsText()
        }
        builder.setNegativeButton("Mégse", null)
        builder.show()
    }

    private fun updateIngredientsText() {
        val allSelected = mutableListOf<String>()
        activeIngredientsList.forEachIndexed { index, name ->
            if (selectedItems[index]) allSelected.add(name)
        }
        allSelected.addAll(customIngredients)
        
        if (allSelected.isEmpty()) {
            tvIngredientsSelector.text = "Válassz összetevőket..."
        } else {
            tvIngredientsSelector.text = allSelected.joinToString(", ")
        }
    }

    private fun showCustomIngredientDialog() {
        val input = EditText(this)
        input.hint = "pl. Squalane"
        AlertDialog.Builder(this)
            .setTitle("Egyéni összetevő")
            .setMessage("Írd be az összetevő nevét:")
            .setView(input)
            .setPositiveButton("Hozzáadás") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (!customIngredients.contains(name)) {
                        customIngredients.add(name)
                        updateIngredientsText()
                        Toast.makeText(this, "$name hozzáadva", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun processImageWithMLKit(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                if (resultText.isNotEmpty()) {
                    val lines = resultText.split("\n").filter { it.trim().length > 2 }
                    if (lines.isNotEmpty()) {
                        editBrand.setText(lines[0].trim())
                        if (lines.size > 1) {
                            editName.setText(lines.drop(1).take(2).joinToString(" "))
                        }
                    }
                    checkIngredientsInText(resultText)
                    updateIngredientsText()
                    Toast.makeText(this, "Adatok felismerve!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkIngredientsInText(text: String) {
        activeIngredientsList.forEachIndexed { index, name ->
            val searchTerms = name.split("(", ")", "/").map { it.trim() }.filter { it.length > 3 }
            if (searchTerms.any { text.contains(it, ignoreCase = true) }) {
                selectedItems[index] = true
            }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                takePictureLauncher.launch(takePictureIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Kamera hiba: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
