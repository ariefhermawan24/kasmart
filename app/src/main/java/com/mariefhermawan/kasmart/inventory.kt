package com.mariefhermawan.kasmart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class inventory : Fragment() {
    // Firebase Database Reference
    private val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
    private val inventoryRef = database.getReference("barang") // Reference to "barang" node

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private val itemList = mutableListOf<Barang>()
    private lateinit var emptyMessage: TextView
    private lateinit var toko: String
    private lateinit var searchBar: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)
        // Define color and drawable map for item types
        val typeColors = mapOf(
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

        val typeDrawables = mapOf(
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

        // Initialize empty message TextView
        emptyMessage = view.findViewById(R.id.empty_inventory_message)
        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.rc_product)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        itemAdapter = ItemAdapter(itemList)
        recyclerView.adapter = itemAdapter


        searchBar = view.findViewById(R.id.search_bar)

        // Get toko name from SharedPreferences
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        toko = sharedPreferences.getString("NAMA_TOKO", "Nama Toko Tidak Tersedia") ?: ""

        // Panggil fetchItems() hanya jika toko memiliki nilai
        if (toko.isNotEmpty()) {
            fetchItems()
        } else {
            emptyMessage.visibility = View.VISIBLE
            emptyMessage.text = "Toko tidak ditemukan, silakan cek pengaturan akun."
        }

        val btnFilter: Button = view.findViewById(R.id.filter)
        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        itemAdapter.onItemClick = { item ->
            val dialogView = inflater.inflate(R.layout.dialog_item_card, null)

            val builder = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)

            // Binding data item ke dalam layout dialog
            val categoryTitle: TextView = dialogView.findViewById(R.id.category_title)
            val itemName: TextView = dialogView.findViewById(R.id.item_name)
            val itemId: TextView = dialogView.findViewById(R.id.item_id)
            val buyPrice: TextView = dialogView.findViewById(R.id.buy_price)
            val itemPrice: TextView = dialogView.findViewById(R.id.item_price)
            val itemStock: TextView = dialogView.findViewById(R.id.item_stock)
            val itemImage: ImageView = dialogView.findViewById(R.id.image)
            val btnEdit: Button = dialogView.findViewById(R.id.btn_edit)
            val btnHapus: Button = dialogView.findViewById(R.id.btn_hapus)

            // Display item data (handling nullable fields)
            categoryTitle.text = item.category ?: "No Category"
            itemName.text = item.name ?: "Unnamed Item"
            itemId.text = "ID: ${item.id ?: "Unknown"}"
            buyPrice.text = "Rp ${item.initialPrice ?: 0}"
            itemPrice.text = "Rp ${item.salePrice ?: 0}"
            itemStock.text = if (item.stock == null || item.stock == 0) {
                "Stok Habis"  // If stock is null or 0, show "Stok Habis"
            } else {
                String.format("%02d", item.stock ?: 0)  // Otherwise show formatted stock number
            }

            // Determine if night mode is active
            val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            // Change text color based on conditions and night mode
            if ((item.salePrice ?: 0) < (item.initialPrice ?: 0)) {
                itemPrice.setTextColor(Color.RED) // Sale price is less than initial price
            } else {
                itemPrice.setTextColor(if (isNightMode) Color.WHITE else Color.BLACK) // White for night mode, black for day mode
            }

            // Update text color based on stock value and night mode
            val stockValue = item.stock ?: 0 // Default to 0 if null
            if (stockValue <= 5) {
                itemStock.setTextColor(Color.RED) // Set text color to red for low stock
            } else {
                itemStock.setTextColor(if (isNightMode) Color.WHITE else Color.BLACK) // White for night mode, black for day mode
            }

            // Set color and image based on item category
            val typeColor = typeColors[item.category?.toLowerCase() ?: ""] ?: "#000000"
            categoryTitle.setBackgroundColor(Color.parseColor(typeColor))

            val drawableRes = typeDrawables[item.category?.toLowerCase() ?: ""]
            if (drawableRes != null) {
                Glide.with(dialogView.context)
                    .load(drawableRes)
                    .placeholder(R.drawable.box)
                    .into(itemImage)
            } else {
                itemImage.setImageResource(R.drawable.box)
            }

            // Show main dialog
            val dialog = builder.create()
            dialog.show()

            // Edit button logic
            btnEdit.setOnClickListener {
                val editDialogView = inflater.inflate(R.layout.dialog_edit_item, null)
                val editBuilder = AlertDialog.Builder(requireContext())
                    .setView(editDialogView)
                    .setCancelable(true)

                val editItemName: EditText = editDialogView.findViewById(R.id.edit_item_name)
                val editInitialPrice: EditText = editDialogView.findViewById(R.id.edit_initial_price)
                val editSalePrice: EditText = editDialogView.findViewById(R.id.edit_sale_price)
                val editStock: EditText = editDialogView.findViewById(R.id.edit_stock)
                val btnReset: Button = editDialogView.findViewById(R.id.reset)
                val btnSave: Button = editDialogView.findViewById(R.id.save)

                // Set initial values in edit fields
                editItemName.setText(item.name ?: "")
                editInitialPrice.setText(item.initialPrice?.toString() ?: "0")
                editSalePrice.setText(item.salePrice?.toString() ?: "0")
                editStock.setText(item.stock?.toString() ?: "Stok Habis")

                val editDialog = editBuilder.create()
                editDialog.show()
                // Save button logic
                btnSave.setOnClickListener {
                    val updatedName = editItemName.text.toString()
                    val updatedInitialPrice = editInitialPrice.text.toString().toIntOrNull() ?: item.initialPrice ?: 0
                    val updatedSalePrice = editSalePrice.text.toString().toIntOrNull() ?: item.salePrice ?: 0
                    val updatedStock = editStock.text.toString().toIntOrNull() ?: item.stock ?: 0

                    // Accessing the correct Firebase path and updating data
                    val itemRef = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/").getReference("barang/${item.id}")
                    itemRef.updateChildren(
                        mapOf(
                            "name" to updatedName,
                            "initialPrice" to updatedInitialPrice,
                            "salePrice" to updatedSalePrice,
                            "stock" to updatedStock
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data Berhasil Di Edit", Toast.LENGTH_SHORT).show()
                        editDialog.dismiss() // Close main dialog after saving
                        dialog.dismiss() // Close edit dialog
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Pengeditan Gagal", Toast.LENGTH_SHORT).show()
                    }
                }

                // Reset button logic
                btnReset.setOnClickListener {
                    editItemName.setText(item.name ?: "")
                    editInitialPrice.setText(item.initialPrice?.toString() ?: "0")
                    editSalePrice.setText(item.salePrice?.toString() ?: "0")
                    editStock.setText(item.stock?.toString() ?: "0")
                }


            }

            // Delete button logic
            btnHapus.setOnClickListener {
                // Create confirmation alert dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi Penghapusan")
                    .setMessage("Apakah Anda yakin ingin menghapus data ini?")
                    .setPositiveButton("Ya") { dialogInterface, _ ->
                        // Get reference to item in Firebase
                        val itemRef =
                            FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
                                .getReference("barang/${item.id}")

                        // Check if the item id is valid before attempting to delete
                        if (!item.id.isNullOrEmpty()) {
                            itemRef.removeValue().addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Item berhasil dihapus",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss() // Close main dialog after deletion
                            }.addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Gagal menghapus item",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "ID item tidak valid",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        dialogInterface.dismiss() // Dismiss confirmation dialog
                    }
                    .setNegativeButton("Tidak") { dialogInterface, _ ->
                        dialogInterface.dismiss() // Dismiss confirmation dialog without action
                    }
                    .show()
            }
        }

        itemAdapter.itemLongClickListener = { item ->
            // Menampilkan dialog untuk menambah stok
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_stock, null)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Tambah Stok")

            // Ambil EditText dari layout dialog
            val input = dialogView.findViewById<EditText>(R.id.inputStock) // Pastikan ID sesuai dengan XML
            input.hint = "Tambah Stok ${item.name}"

            builder.setView(dialogView)

            // Set aksi ketika tombol 'OK' diklik
            builder.setPositiveButton("OK") { dialog, _ ->
                val stokBaru = input.text.toString().toIntOrNull()
                if (stokBaru != null) {
                    // Menambahkan stok baru ke stok lama
                    val stokLama = item.stock ?: 0
                    val updatedStock = stokLama + stokBaru

                    if (item.id != null) {
                        // Update stok barang di database (Firebase)
                        val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
                            .getReference("barang")
                            .child(item.id)

                        database.child("stock").setValue(updatedStock).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Stok berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Gagal menambah stok", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "ID barang tidak valid", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Masukkan jumlah stok yang valid", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            // Set aksi ketika tombol 'Cancel' diklik
            builder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }

            // Menampilkan dialog
            builder.show()
        }


        // Initialize Search Functionality
        SearchFunctionality()

        // Fetch items from Firebase and update RecyclerView
        fetchItems()

        // Button for adding inventory item
        val btnAddInventory: Button = view.findViewById(R.id.btn_add_inventory)
        btnAddInventory.setOnClickListener {
            showAddItemDialog(requireContext())
        }

        return view
    }

    private fun SearchFunctionality() {

        // Optional: Real-time search as the user types
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchItems(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun searchItems(query: String) {
        if (query.isEmpty()) {
            itemAdapter.updateData(itemList)
            emptyMessage.visibility = if (itemList.isEmpty()) View.VISIBLE else View.GONE
            emptyMessage.text = "Inventory masih kosong, silahkan tambahkan barang terlebih dahulu" // Isi teks untuk keadaan data kosong
            return
        }

        val filteredList = itemList.filter { item ->
            val nameMatch = item.name?.contains(query, ignoreCase = true) == true
            val priceMatch = item.salePrice.toString().contains(query)

            nameMatch || priceMatch
        }

        itemAdapter.updateData(filteredList)
        emptyMessage.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        // Ganti teks ketika hasil pencarian tidak ada
        emptyMessage.text = if (filteredList.isEmpty()) "Tidak ada barang yang sesuai dengan pencarian" else ""
    }

    private fun fetchItems() {
        inventoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                itemList.clear()
                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Barang::class.java)
                    if (item?.toko == toko) {  // Only add items from the same store
                        itemList.add(item)
                    }
                }
                // Mengurutkan itemList
                itemList.sortBy { it.category?.toUpperCase(Locale.getDefault()) }
                itemAdapter.notifyDataSetChanged()

                // Show or hide the empty inventory message based on item count
                if (itemList.isEmpty()) {
                    emptyMessage.visibility = View.VISIBLE
                } else {
                    emptyMessage.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf("Filter Kategori", "Urutkan Nama (A-Z)", "Harga Tertinggi", "Harga Terendah" , "Stok Tertinggi", "Stok Terendah")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pilih Filter")
        builder.setItems(filterOptions) { _, which ->
            when (which) {
                0 -> filterByCategory()
                1 -> sortByAlphabet()
                2 -> sortByPriceDescending()
                3 -> sortByPriceAscending()
                4 -> sortByStockDescending()
                5 -> sortByStockAscending()
            }
        }
        builder.show()
    }

    private fun updateRecyclerView(newList: List<Barang>) {
        itemAdapter = ItemAdapter(newList)
        recyclerView.adapter = itemAdapter
        itemAdapter.notifyDataSetChanged()
    }

    private fun showAddItemDialog(context: Context) {
        // Inflate custom dialog layout
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_add_item, null)

        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = builder.create()

        // Handle form inputs
        val edtItemName: EditText = dialogView.findViewById(R.id.edtItemName)
        val spinnerCategory: Spinner = dialogView.findViewById(R.id.spinnerCategory)
        val edtInitialPrice: EditText = dialogView.findViewById(R.id.edtInitialPrice)
        val edtSalePrice: EditText = dialogView.findViewById(R.id.edtSalePrice)
        val edtStock: EditText = dialogView.findViewById(R.id.edtStock)
        val btnSave: Button = dialogView.findViewById(R.id.btnSave)

        val categoriesWithEmoji = listOf(
            "🍫 Snack",
            "🥤 Minuman",
            "🍦 Es Krim",
            "🍅 Bumbu Dapur",
            "🍚 Sembako",
            "💧 Galon",
            "🔥 Gas",
            "🚬 Rokok",
            "💊 Obat-obatan"
        )

        val categoriesWithoutEmoji = listOf(
            "Snack",
            "Minuman",
            "Es Krim",
            "Bumbu Dapur",
            "Sembako",
            "Galon",
            "Gas",
            "Rokok",
            "Obat-obatan"
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categoriesWithEmoji)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter



        btnSave.setOnClickListener {
            val itemName = edtItemName.text.toString().trim()
            val selectedCategoryWithEmoji = spinnerCategory.selectedItem.toString()
            val selectedCategory = categoriesWithoutEmoji[categoriesWithEmoji.indexOf(selectedCategoryWithEmoji)]
            val initialPrice = edtInitialPrice.text.toString().toIntOrNull() ?: 0
            val salePrice = edtSalePrice.text.toString().toIntOrNull() ?: 0
            val stock = edtStock.text.toString().toIntOrNull() ?: 0

            if (itemName.isEmpty() || selectedCategory.isEmpty() || initialPrice == 0 || salePrice == 0 || stock == 0) {
                Toast.makeText(context, "Silakan lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            inventoryRef.orderByChild("name").equalTo(itemName).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var itemExists = false

                    // Iterate through the snapshot and check if the item already exists in the same store
                    for (data in snapshot.children) {
                        val existingItem = data.getValue(Barang::class.java)
                        if (existingItem?.toko == toko) {  // Check if the store is the same
                            itemExists = true
                            break
                        }
                    }

                    if (itemExists) {
                        Toast.makeText(context, "Barang sudah ada di inventaris toko ini", Toast.LENGTH_SHORT).show()
                    } else {
                        val itemId = inventoryRef.push().key ?: return
                        val newItem = Barang(itemId, itemName, selectedCategory, initialPrice, salePrice, stock, toko)

                        inventoryRef.child(itemId).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(context, "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Gagal menambahkan barang", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Terjadi kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun filterByCategory() {
        val categories = listOf("Snack", "Minuman", "Es Krim", "Bumbu Dapur", "Sembako", "Galon", "Gas", "Rokok", "Obat-obatan")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pilih Kategori")
        builder.setItems(categories.toTypedArray()) { _, which ->
            val selectedCategory = categories[which]
            val filteredList = itemList.filter { it.category == selectedCategory }
            updateRecyclerView(filteredList)
        }
        builder.show()
    }

    private fun sortByAlphabet() {
        val sortedList = itemList.sortedBy { it.name }
        updateRecyclerView(sortedList)
    }

    private fun sortByPriceDescending() {
        val sortedList = itemList.sortedByDescending { it.salePrice }
        updateRecyclerView(sortedList)
    }

    private fun sortByPriceAscending() {
        val sortedList = itemList.sortedBy { it.salePrice }
        updateRecyclerView(sortedList)
    }

    private fun sortByStockDescending() {
        val sortedList = itemList.sortedByDescending { it.stock }
        updateRecyclerView(sortedList)
    }

    private fun sortByStockAscending() {
        val sortedList = itemList.sortedBy { it.stock }
        updateRecyclerView(sortedList)
    }


    // Data class for the inventory item
    data class Barang(
        val id: String? = null,
        val name: String? = null,
        val category: String? = null,
        val initialPrice: Int? = null,
        val salePrice: Int? = null,
        val stock: Int? = null,
        val toko: String? = null,
        var isChecked: Boolean = false // Menyimpan status checkbox
    )
}
