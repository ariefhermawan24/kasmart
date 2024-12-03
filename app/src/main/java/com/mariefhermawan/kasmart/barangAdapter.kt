package com.mariefhermawan.kasmart

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mariefhermawan.kasmart.inventory.Barang

class barangAdapter(
    private var barangList: List<Barang>,
    private val onTotalUpdated: (Int) -> Unit // Callback untuk menghitung total harga
) : RecyclerView.Adapter<barangAdapter.ItemViewHolder>() {

    // Menyimpan barang yang dipilih dan jumlahnya
    private val selectedItems = mutableMapOf<String, Int>()
    // Menyimpan jumlah berdasarkan ID barang
    private val itemQuantities = mutableMapOf<String, Int>()

    // ViewHolder class
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNamaBarang: TextView = itemView.findViewById(R.id.text_nama_barang)
        val textcategoryBarang: TextView = itemView.findViewById(R.id.text_id_barang)
        val textHarga: TextView = itemView.findViewById(R.id.text_harga)
        val editTextJumlah: EditText = itemView.findViewById(R.id.edit_text_jumlah)
        val checkBoxBarang: CheckBox = itemView.findViewById(R.id.checkbox_barang)
    }

    // Inflate item layout and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_barang, parent, false)
        return ItemViewHolder(view)
    }

    // Bind data to ViewHolder
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val barang = barangList[position]
        val barangId = barang.id ?: return

        // Set text untuk TextView
        holder.textNamaBarang.text = barang.name ?: "Tidak Diketahui"
        holder.textcategoryBarang.text = barang.category ?: "Tidak Diketahui"
        holder.textHarga.text = "Rp ${barang.salePrice ?: 0}"

        // Mengatur nilai EditText berdasarkan data di Map
        val quantity = itemQuantities[barangId] ?: 1 // Default 1 jika tidak ada data
        holder.editTextJumlah.setText(quantity.toString())

        // Mengembalikan status checkbox
        holder.checkBoxBarang.isChecked = selectedItems.containsKey(barangId)

        // Listener untuk CheckBox
        holder.checkBoxBarang.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val quantity = holder.editTextJumlah.text.toString().toIntOrNull() ?: 1
                selectedItems[barangId] = quantity
            } else {
                selectedItems.remove(barangId)
            }
            updateTotal()
        }

        // TextWatcher untuk mengganti angka 1
        holder.editTextJumlah.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false // Untuk mencegah recursive update

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                val input = s.toString()

                // Jika input kosong, set angka menjadi 1
                if (input.isEmpty()) {
                    isUpdating = true
                    holder.editTextJumlah.setText("1")
                    holder.editTextJumlah.setSelection(1) // Set kursor setelah angka 1
                    isUpdating = false
                } else if (input.startsWith("1") && input.length > 1) {
                    // Hapus angka 1 saat input pertama kali diubah
                    isUpdating = true
                    holder.editTextJumlah.setText(input.replaceFirst("1", ""))
                    holder.editTextJumlah.setSelection(input.length - 1) // Menjaga posisi kursor
                    isUpdating = false
                }

                // Perbarui quantity di Map
                val quantity = input.toIntOrNull() ?: 1
                itemQuantities[barangId] = quantity
                if (holder.checkBoxBarang.isChecked) {
                    holder.checkBoxBarang.isChecked = false
                    selectedItems[barangId] = quantity
                    holder.checkBoxBarang.isChecked = true
                }
                updateTotal()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener untuk mengatur nilai saat kehilangan fokus
        holder.editTextJumlah.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val quantity = holder.editTextJumlah.text.toString().toIntOrNull() ?: 1
                if (holder.editTextJumlah.text.toString().isEmpty()) {
                    holder.editTextJumlah.setText("1") // Set angka 1 jika kosong
                }
                itemQuantities[barangId] = quantity

                if (holder.checkBoxBarang.isChecked) {
                    selectedItems[barangId] = quantity
                }
                updateTotal()
            }
        }
    }

    // Return item count
    override fun getItemCount(): Int = barangList.size

    // Fungsi untuk menghitung total harga
    private fun updateTotal() {
        val total = selectedItems.entries.sumOf { (barangId, quantity) ->
            val barang = barangList.find { it.id == barangId }
            (barang?.salePrice ?: 0) * quantity
        }
        onTotalUpdated(total) // Kirim total ke fragment/activity
    }

    fun getSelectedItems(): Map<String, Triple<String, Pair<String, Int>, Triple<Int, Int, Int>>> {
        return selectedItems.mapValues { (barangId, quantity) ->
            // Mencari barang berdasarkan ID
            val barang = barangList.find { it.id == barangId }

            // Mengambil informasi dari objek barang atau memberikan nilai default
            val namaBarang = barang?.name ?: "Nama Tidak Diketahui"
            val kategori = barang?.category ?: "Kategori Tidak Ada"
            val hargaBeli = barang?.initialPrice ?: 0
            val hargaJual = barang?.salePrice ?: 0

            // Menghitung total harga berdasarkan jumlah
            val totalHarga = hargaJual * quantity

            // Mengembalikan Triple yang berisi nama barang, kategori & jumlah, serta harga total, beli, dan jual
            Triple(namaBarang, Pair(kategori, quantity), Triple(totalHarga, hargaBeli, hargaJual))
        }
    }
    // Fungsi untuk membersihkan semua barang yang dipilih
    fun clearSelectedItems() {
        selectedItems.clear() // Menghapus semua barang yang dipilih
        itemQuantities.clear() // Menghapus semua data jumlah
        notifyDataSetChanged() // Memberitahukan RecyclerView untuk me-refresh tampilan
    }

}
