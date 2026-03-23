package com.example.myapplication

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ProductAdapter(
    private var items: List<Any> = emptyList(),
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CATEGORY = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_CATEGORY else TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CATEGORY) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
            CategoryViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = holder.itemView.context
        if (holder is CategoryViewHolder) {
            holder.title.text = items[position] as String
        } else if (holder is ProductViewHolder) {
            val product = items[position] as Product
            holder.nameText.text = product.name
            holder.brandText.visibility = View.GONE

            if (!product.expiryDate.isNullOrEmpty()) {
                holder.expiryText.visibility = View.VISIBLE
                holder.expiryText.text = context.getString(R.string.expiry_label, product.expiryDate)
                checkExpiryStatus(product.expiryDate, holder.expiryText)
            } else {
                holder.expiryText.visibility = View.GONE
            }

            if (product.imageUrl.isNullOrEmpty()) {
                holder.productImage.setImageResource(R.drawable.ic_product_bottle)
                holder.productImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
                holder.productImage.setPadding(20, 20, 20, 20)
            } else {
                holder.productImage.setPadding(0, 0, 0, 0)
                holder.productImage.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(context)
                    .load(product.imageUrl)
                    .into(holder.productImage)
            }

            // Click on the whole item to edit
            holder.itemView.setOnClickListener {
                val intent = Intent(context, AddProductActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                context.startActivity(intent)
            }

            holder.deleteButton.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(context.getString(R.string.delete_product_confirm_msg, product.name))
                    .setPositiveButton(R.string.delete_btn) { _, _ ->
                        onDeleteClick(product)
                    }
                    .setNegativeButton(R.string.cancel_btn, null)
                    .show()
            }
        }
    }

    private fun checkExpiryStatus(expiryDateStr: String, textView: TextView) {
        try {
            val context = textView.context
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDate = sdf.parse(expiryDateStr) ?: return
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time

            if (expiryDate.before(today)) {
                textView.setTextColor(ContextCompat.getColor(context, R.color.expired_red))
                textView.text = context.getString(R.string.expired_status, expiryDateStr)
            } else {
                val calendar = Calendar.getInstance(); calendar.time = today; calendar.add(Calendar.DAY_OF_YEAR, 30)
                if (expiryDate.before(calendar.time)) {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.warning_orange))
                } else {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.slate_400))
                }
            }
        } catch (e: Exception) {}
    }

    override fun getItemCount() = items.size

    fun submitList(newList: List<Any>) {
        items = newList
        notifyDataSetChanged()
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tvProductName)
        val brandText: TextView = view.findViewById(R.id.tvProductBrand)
        val productImage: ImageView = view.findViewById(R.id.ivProductImage)
        val expiryText: TextView = view.findViewById(R.id.tvExpiryDate)
        val deleteButton: ImageView = view.findViewById(R.id.btnDeleteProduct)
    }

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvCategoryHeader)
    }
}
