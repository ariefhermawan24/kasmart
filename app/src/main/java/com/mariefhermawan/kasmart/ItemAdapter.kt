package com.mariefhermawan.kasmart

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ItemAdapter(private var items: List<inventory.Barang> ) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    // Interface for item click listener
    var onItemClick: ((inventory.Barang) -> Unit)? = null
    var itemLongClickListener: ((inventory.Barang) -> Unit)? = null
    // Define color and drawable map for item types
    private val typeColors = mapOf(
        "snack" to "#FFB74D",
        "minuman" to "#42A5F5",
        "es krim" to "#BA68C8",
        "bumbu dapur" to "#FF7043",
        "sembako" to "#8D6E63",
        "galon" to "#29B6F6",
        "gas" to "#FF5252",
        "rokok" to "#757575",
        "obat-obatan" to "#66BB6A"
    )

    private val typeDrawables = mapOf(
        "snack" to R.drawable.snack,
        "minuman" to R.drawable.minuman,
        "es krim" to R.drawable.ice_cream,
        "bumbu dapur" to R.drawable.bumbu_dapur,
        "sembako" to R.drawable.sembako,
        "galon" to R.drawable.galon,
        "gas" to R.drawable.gas,
        "rokok" to R.drawable.rokok,
        "obat-obatan" to R.drawable.obat_obatan
    )

    // Create a ViewHolder to represent a single card item
    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.item_name)
        val itemId: TextView = view.findViewById(R.id.item_id)
        val itemPrice: TextView = view.findViewById(R.id.item_price)
        val itemStock: TextView = view.findViewById(R.id.item_stock)
        val itemImage: ImageView = view.findViewById(R.id.item_image)
        val typeIndicator: View = view.findViewById(R.id.type_indicator)
        val cardItem: View = view.findViewById(R.id.bg_card)
    }

    // Create a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_barang_card, parent, false)
        return ItemViewHolder(view)
    }

    // Bind data to each card item
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        holder.itemName.text = item.name
        holder.itemId.text = "ID: ${item.id}"
        holder.itemPrice.text = "Rp ${item.salePrice}"
        holder.itemStock.text = String.format("%02d", item.stock ?: 0) // Show "0" if stock is null

        // Set color based on item type
        val typeColor = typeColors[item.category?.toLowerCase() ?: ""] ?: "#000000"
        holder.typeIndicator.setBackgroundColor(Color.parseColor(typeColor))

        // Load image using Glide
        val drawableRes = typeDrawables[item.category?.toLowerCase() ?: ""]
        if (drawableRes != null) {
            Glide.with(holder.itemView.context)
                .load(drawableRes)
                .placeholder(R.drawable.box) // Placeholder while loading
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.box) // Default image if type is not found
        }

        // Change stock background color based on stock value
        val stockValue = item.stock ?: 0 // Default stock to 0 if it's null
        if (stockValue <= 5) {
            holder.itemStock.setBackgroundColor(Color.RED) // Red background for low stock
            holder.itemStock.setTextColor(Color.WHITE) // Optional: Change text color for contrast
        } else {
            holder.itemStock.setBackgroundColor(Color.BLACK) // Default background color
            holder.itemStock.setTextColor(Color.WHITE) // Optional: Keep text visible on black background
        }

        // Set on click listener to navigate to item details
        holder.cardItem.setOnClickListener {
            onItemClick?.invoke(item)
        }
        // Handling long click (klik lama)
        holder.cardItem.setOnLongClickListener {
            itemLongClickListener?.invoke(item)
            true // Return true to indicate the long click is handled
        }
    }

    fun updateData(newItems: List<inventory.Barang>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
