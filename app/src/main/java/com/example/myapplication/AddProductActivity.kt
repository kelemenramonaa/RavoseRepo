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

    private lateinit var productDao: ProductDao
    private lateinit var photoCard: View
    private lateinit var imageView: ImageView
    private lateinit var photoOverlay: View
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
    private var currentUserEmail: String = ""

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Kamera engedély szükséges a fotózáshoz!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        supportActionBar?.hide()

        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        currentUserEmail = profilePrefs.getString("UserEmail", "") ?: ""

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

        tvIngredientsSelector.setOnClickListener { showMultiSelectIngredientsDialog() }
        layoutExpiry.setOnClickListener { showDatePickerDialog() }
        photoCard.setOnClickListener { checkCameraPermissionAndLaunch() }
        
        findViewById<TextView>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnAddCustomIngredient).setOnClickListener { showCustomIngredientDialog() }
        btnSave.setOnClickListener { saveProduct() }
    }

    private fun initViews() {
        photoCard = findViewById(R.id.photoCard)
        imageView = findViewById(R.id.productImageView)
        photoOverlay = findViewById(R.id.photoOverlay)
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
        val types = arrayOf("Arctisztító", "Olajos lemosó", "Tonik", "Esszencia", "Szérum", "Hidratáló", "Fényvédő", "Hámlasztó", "Retinoid", "Peptid", "Niacinamide", "Arcolaj", "Maszk", "Egyéb")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        try {
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            currentImageUrl = file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadProductData(id: Int) {
        lifecycleScope.launch {
            val product = productDao.getProductById(id)
            product?.let { p ->
                existingProduct = p
                runOnUiThread {
                    editName.setText(p.name)
                    editNote.setText(p.note)
                    
                    val adapter = spinnerType.adapter as? ArrayAdapter<String>
                    val position = adapter?.getPosition(p.type) ?: -1
                    if (position >= 0) spinnerType.setSelection(position)
                    
                    selectedExpiryDate = p.expiryDate
                    tvExpiryDisplay.text = p.expiryDate ?: "Nincs megadva"
                    p.ingredients?.let { ingredientsStr ->
                        tvIngredientsSelector.text = ingredientsStr
                        val items = ingredientsStr.split(", ").map { it.trim() }
                        items.forEach { item ->
                            val index = activeIngredientsList.indexOf(item)
                            if (index >= 0) selectedItems[index] = true
                            else if (!customIngredients.contains(item)) customIngredients.add(item)
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

    private fun processImageWithMLKit(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedText = visionText.text
                if (detectedText.isNotEmpty()) {
                    if (editName.text.isEmpty()) {
                        val firstLine = detectedText.split("\n").firstOrNull()
                        editName.setText(firstLine)
                    }
                }
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            selectedExpiryDate = date
            tvExpiryDisplay.text = date
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun saveProduct() {
        val name = editName.text.toString().trim()
        val type = spinnerType.selectedItem.toString()
        val note = editNote.text.toString().trim()
        val ingredientsString = tvIngredientsSelector.text.toString().let {
            if (it == "Válassz összetevőket...") "" else it
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Kérjük, töltse ki a termék nevét!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val productToSave = if (editingProductId != null) {
                existingProduct?.copy(name = name, type = type, note = note, ingredients = ingredientsString.ifEmpty { null }, expiryDate = selectedExpiryDate, imageUrl = currentImageUrl)
            } else {
                Product(name = name, imageUrl = currentImageUrl, ingredients = ingredientsString.ifEmpty { null }, type = type, note = note, expiryDate = selectedExpiryDate, userEmail = currentUserEmail)
            }

            productToSave?.let {
                if (editingProductId != null) productDao.updateProduct(it)
                else productDao.insertProduct(it)

                selectedExpiryDate?.let { date -> scheduleExpiryNotification(name, date) }

                runOnUiThread {
                    Toast.makeText(this@AddProductActivity, "Mentve!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun scheduleExpiryNotification(productName: String, expiryDateStr: String) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDate = sdf.parse(expiryDateStr) ?: return
            val calendar = Calendar.getInstance().apply {
                time = expiryDate
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                val intent = Intent(this, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_SHOW_NOTIFICATION
                    putExtra("title", "Lejáró termék!")
                    putExtra("message", "A(z) $productName egy hét múlva lejár.")
                    putExtra("type", "expiry")
                }
                val pendingIntent = PendingIntent.getBroadcast(this, productName.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarmManager = getSystemService(AlarmManager::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager?.canScheduleExactAlarms() == true) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showCustomIngredientDialog() {
        val input = EditText(this)
        input.hint = "Pl. Cica, Probiotikumok..."
        AlertDialog.Builder(this)
            .setTitle("Egyedi összetevő hozzáadása")
            .setView(input)
            .setPositiveButton("Hozzáadás") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && !customIngredients.contains(text)) {
                    customIngredients.add(text)
                    updateIngredientsText()
                }
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun showMultiSelectIngredientsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Válassz összetevőket")
            .setMultiChoiceItems(activeIngredientsList.toTypedArray(), selectedItems) { _, which, isChecked -> selectedItems[which] = isChecked }
            .setPositiveButton("Kész") { _, _ -> updateIngredientsText() }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun updateIngredientsText() {
        val allSelected = activeIngredientsList.filterIndexed { index, _ -> selectedItems[index] }.toMutableList()
        allSelected.addAll(customIngredients)
        tvIngredientsSelector.text = if (allSelected.isEmpty()) "Válassz összetevőket..." else allSelected.joinToString(", ")
    }
}
