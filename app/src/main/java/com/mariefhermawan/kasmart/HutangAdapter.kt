package com.mariefhermawan.kasmart

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HutangAdapter(private var hutangList: List<dashboard.Hutang>) :
    RecyclerView.Adapter<HutangAdapter.HutangViewHolder>() {
    // Interface for item click listener
    var onItemClick: ((dashboard.Hutang) -> Unit)? = null
    var itemLongClickListener: ((dashboard.Hutang) -> Unit)? = null
    class HutangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val customerName: TextView = itemView.findViewById(R.id.customer_name)
        val debtAmount: TextView = itemView.findViewById(R.id.debt_amount)
        val dueDate: TextView = itemView.findViewById(R.id.due_date)
        val debtImage: ImageView = itemView.findViewById(R.id.debt_image)
        val status: TextView = itemView.findViewById(R.id.status)
        val indicator : View = itemView.findViewById(R.id.type_indicator)
        val cardHutang : View = itemView.findViewById(R.id.bg_hutang)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HutangViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_hutang, parent, false)
        return HutangViewHolder(view)
    }

    override fun onBindViewHolder(holder: HutangViewHolder, position: Int) {
        val hutang = hutangList[position]
        holder.customerName.text = hutang.penghutang
        holder.debtAmount.text = "Rp ${hutang.total_hutang}"
        holder.dueDate.text = "Hutang Sejak: ${hutang.tanggal_hutang}"
        holder.status.text = " ${hutang.status} "

        // Menghitung selisih hari dengan tanggal sekarang
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        try {
            val hutangDate = hutang.tanggal_hutang?.let { sdf.parse(it) }
            val currentDate = Date()
            if (hutangDate != null) {
                val diffInMillis = currentDate.time - hutangDate.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                holder.dueDate.text = "Sejak: ${hutang.tanggal_hutang} (${diffInDays} hari)"
            }
        } catch (e: Exception) {
            holder.dueDate.text = "Sejak: ${hutang.tanggal_hutang} (Error)"
        }

        // Mengatur warna status pembayaran
        val context = holder.itemView.context
        val drawable = ContextCompat.getDrawable(context, R.drawable.lc_penghutang)?.mutate()
        when (hutang.status.lowercase()) {
            "lunas" -> {
                holder.status.setBackgroundColor(Color.parseColor("#0CA5EA")) // Biru agak tua
                holder.indicator.setBackgroundColor(Color.parseColor("#0CA5EA")) // Biru tua
                holder.debtAmount.setTextColor(Color.parseColor("#0CA5EA")) // Biru tua
                drawable?.let {
                    DrawableCompat.setTint(it, Color.parseColor("#0CA5EA"))
                }
            }
            else -> {
                holder.status.setBackgroundColor(Color.parseColor("#EE463A")) // Merah
                holder.indicator.setBackgroundColor(Color.parseColor("#EE463A")) // Merah
                holder.debtAmount.setTextColor(Color.parseColor("#EE463A")) // Merah
                drawable?.let {
                    DrawableCompat.setTint(it, Color.parseColor("#EE463A"))
                }
            }
        }

        // Memuat drawable ke dalam ImageView menggunakan Glide
        Glide.with(context)
            .load(drawable)
            .into(holder.debtImage)

        // Set on click listener to navigate to item details
        holder.cardHutang.setOnClickListener {
            onItemClick?.invoke(hutang)
        }
        // Handling long click (klik lama)
        holder.cardHutang.setOnLongClickListener {
            itemLongClickListener?.invoke(hutang)
            true // Return true to indicate the long click is handled
        }
    }

    fun updateData(newhutang: List<dashboard.Hutang>) {
        hutangList = newhutang
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = hutangList.size
}
