package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private var items: List<Any>,
    private val onItemClick: (Product) -> Unit = {},
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is String) {
            holder.headerText.text = item
            holder.headerText.textSize = 18f
            holder.headerText.setPadding(32, 48, 16, 16)
            holder.headerText.setTextColor(holder.itemView.context.getColor(R.color.slate_800))
        } else if (holder is ProductViewHolder && item is Product) {
            holder.nameText.text = item.name
            holder.brandText.text = item.type

            if (!item.expiryDate.isNullOrEmpty()) {
                holder.expiryText.visibility = View.VISIBLE
                holder.expiryText.text = "Lejár: ${item.expiryDate}"
            } else {
                holder.expiryText.visibility = View.GONE
            }

            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_product_bottle)
                    .into(holder.productImage)
            } else {
                holder.productImage.setImageResource(R.drawable.ic_product_bottle)
            }

            holder.deleteButton.setOnClickListener { onDeleteClick(item) }
            holder.itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerText: TextView = view.findViewById(android.R.id.text1)
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tvProductName)
        val brandText: TextView = view.findViewById(R.id.tvProductBrand)
        val expiryText: TextView = view.findViewById(R.id.tvExpiryDate)
        val productImage: ImageView = view.findViewById(R.id.ivProductImage)
        val deleteButton: ImageView = view.findViewById(R.id.btnDeleteProduct)
    }
}
