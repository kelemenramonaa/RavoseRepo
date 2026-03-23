package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SkinDiaryActivity : AppCompatActivity() {

    private lateinit var rvPhotos: RecyclerView
    private lateinit var db: AppDatabase
    private lateinit var tvPhotoCount: TextView
    private lateinit var emptyState: View
    private val photos = mutableListOf<SkinPhoto>()
    private lateinit var adapter: SkinPhotoAdapter
    private var currentUserEmail: String = ""

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                savePhotoToDb(imageBitmap)
            } else {
                Toast.makeText(this, "Hiba: A kamera nem küldött képet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_diary)
        supportActionBar?.hide()

        val prefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        currentUserEmail = prefs.getString("UserEmail", "") ?: ""

        db = AppDatabase.getDatabase(this)
        rvPhotos = findViewById(R.id.rvPhotos)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)
        emptyState = findViewById(R.id.emptyState)
        
        rvPhotos.layoutManager = GridLayoutManager(this, 2)
        adapter = SkinPhotoAdapter(photos)
        rvPhotos.adapter = adapter
        
        findViewById<ImageView>(R.id.btnBackDiary).setOnClickListener { finish() }
        findViewById<View>(R.id.fabAddPhoto).setOnClickListener { checkPermissionAndTakePhoto() }

        loadPhotos()
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            try {
                val list = db.skinPhotoDao().getAllPhotos(currentUserEmail)
                photos.clear()
                photos.addAll(list)
                
                updateUI(list.size)
                adapter.notifyDataSetChanged()
                Log.d("SkinDiary", "Betöltve: ${list.size} fotó")
            } catch (e: Exception) {
                Log.e("SkinDiary", "Hiba a betöltéskor", e)
            }
        }
    }

    private fun updateUI(count: Int) {
        if (count == 0) {
            emptyState.visibility = View.VISIBLE
            rvPhotos.visibility = View.GONE
            tvPhotoCount.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE
            tvPhotoCount.visibility = View.VISIBLE
            tvPhotoCount.text = "$count fotó"
        }
    }

    private fun checkPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 200)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(intent)
        }
    }

    private fun savePhotoToDb(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val fileName = "skin_${System.currentTimeMillis()}.jpg"
                val file = File(filesDir, fileName)
                
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                    }
                }

                if (file.exists()) {
                    val skinPhoto = SkinPhoto(
                        imagePath = file.absolutePath, 
                        date = System.currentTimeMillis(),
                        userEmail = currentUserEmail
                    )
                    db.skinPhotoDao().insertPhoto(skinPhoto)
                    Log.d("SkinDiary", "Mentve az adatbázisba: ${file.absolutePath}")
                    
                    Toast.makeText(this@SkinDiaryActivity, "Fotó elmentve!", Toast.LENGTH_SHORT).show()
                    loadPhotos()
                }
            } catch (e: Exception) {
                Log.e("SkinDiary", "Mentési hiba", e)
                Toast.makeText(this@SkinDiaryActivity, "Hiba a mentés során!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class SkinPhotoAdapter(private val items: List<SkinPhoto>) : RecyclerView.Adapter<SkinPhotoAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skin_photo, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val imageFile = File(item.imagePath)
            
            if (imageFile.exists()) {
                Glide.with(this@SkinDiaryActivity)
                    .load(imageFile)
                    .centerCrop()
                    .into(holder.ivPhoto)
            } else {
                holder.ivPhoto.setImageResource(R.drawable.ic_camera)
            }

            holder.tvDate.text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(item.date))
            
            holder.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    db.skinPhotoDao().deletePhoto(item)
                    if (imageFile.exists()) imageFile.delete()
                    loadPhotos()
                    Toast.makeText(this@SkinDiaryActivity, "Fotó törölve", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivPhoto: ImageView = v.findViewById(R.id.ivSkinPhoto)
            val tvDate: TextView = v.findViewById(R.id.tvPhotoDate)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeletePhoto)
        }
    }
}
