package com.mariefhermawan.kasmart

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class hutang : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var hutangAdapter: HutangAdapter
    private val hutangList = mutableListOf<dashboard.Hutang>()
    private var toko: String = ""
    private lateinit var emptyMessage: TextView
    private lateinit var searchBar: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hutang, container, false)
        emptyMessage = view.findViewById(R.id.empty_dept_message)
        searchBar = view.findViewById(R.id.search_bar)
        // Inisialisasi RecyclerView
        recyclerView = view.findViewById(R.id.rc_debt_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        hutangAdapter = HutangAdapter(hutangList)
        recyclerView.adapter = hutangAdapter

        hutangAdapter.onItemClick = { hutang ->
            val context = context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_hutang_previews, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            // Bind views
            val penghutangText = dialogView.findViewById<TextView>(R.id.penghutang)
            val statusText = dialogView.findViewById<TextView>(R.id.status_title)
            val tanggalText = dialogView.findViewById<TextView>(R.id.tanggal_hutang)
            val totalHutangText = dialogView.findViewById<TextView>(R.id.total_hutang)
            val tableBarang = dialogView.findViewById<TableLayout>(R.id.list_barang_dihutang)
            val image = dialogView.findViewById<ImageView>(R.id.image)
            val hapus = dialogView.findViewById<Button>(R.id.btn_hapus)

            // Set data to views
            penghutangText.text = hutang.penghutang ?: "Tidak diketahui"
            statusText.text = "Status: ${hutang.status ?: "Tidak diketahui"}"
            tanggalText.text = "Tanggal: ${hutang.tanggal_hutang ?: "Tidak diketahui"}"
            totalHutangText.text = "Total: ${hutang.total_hutang ?: 0}"
            // Memastikan status tidak null
            val status = hutang.status?.lowercase()

// Pilih warna berdasarkan status
            val statusColor = when (status) {
                "lunas" -> "#0CA5EA" // Biru
                else -> "#EE463A"    // Merah
            }

// Mengatur warna latar belakang teks
            try {
                statusText.setBackgroundColor(Color.parseColor(statusColor))
            } catch (e: IllegalArgumentException) {
                // Log atau gunakan warna default jika parsing gagal
                statusText.setBackgroundColor(Color.GRAY)
            }

// Mengatur warna pada drawable jika tidak null
            image?.drawable?.let { drawable ->
                val wrappedDrawable = DrawableCompat.wrap(drawable)
                try {
                    DrawableCompat.setTint(wrappedDrawable, Color.parseColor(statusColor))
                    image.setImageDrawable(wrappedDrawable) // Set drawable kembali ke ImageView
                } catch (e: IllegalArgumentException) {
                    // Log atau gunakan warna default jika parsing gagal
                    DrawableCompat.setTint(wrappedDrawable, Color.GRAY)
                    image.setImageDrawable(wrappedDrawable)
                }
            }

            // Mengatur warna background tint pada Button
            hapus?.let { btn ->
                try {
                    btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(statusColor))
                } catch (e: IllegalArgumentException) {
                    // Log atau gunakan warna default jika parsing gagal
                    btn.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                }
            }


            // Populate table with barang_dihutang data
            if (!hutang.barang_dihutang.isNullOrEmpty()) {
                hutang.barang_dihutang.entries.forEachIndexed { index, entry ->
                    val barang = entry.value as? dashboard.BarangHutang // Pastikan 'BarangHutang' adalah model data Anda
                    val row = TableRow(context).apply {
                        layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(8, 8, 8, 8) // Padding sesuai contoh XML
                    }

                    // No column
                    val no = TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                        text = "${index + 1}"
                        gravity = Gravity.CENTER
                        setPadding(4, 4, 4, 4)
                    }

                    // Nama Barang column
                    val namaBarang = TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3f)
                        text = barang?.nama_barang ?: "Tidak diketahui"
                        gravity = Gravity.CENTER
                        setPadding(4, 4, 4, 4)
                    }

                    // Category column
                    val category = TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                        text = barang?.category ?: "Tidak diketahui"
                        gravity = Gravity.CENTER
                        setPadding(4, 4, 4, 4)
                    }

                    // Jumlah column
                    val jumlah = TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                        text = barang?.jumlah?.toString() ?: "0"
                        gravity = Gravity.CENTER
                        setPadding(4, 4, 4, 4)
                    }

                    // Total Harga column
                    val total = TextView(context).apply {
                        layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                        text = "Rp ${barang?.total_harga ?: "0"}"
                        gravity = Gravity.CENTER
                        setPadding(4, 4, 4, 4)
                        textSize = 12f
                    }

                    // Add columns to row
                    row.addView(no)
                    row.addView(namaBarang)
                    row.addView(category)
                    row.addView(jumlah)
                    row.addView(total)

                    // Add row to table
                    tableBarang.addView(row)
                }
            } else {
                // Jika tidak ada barang dihutang, tambahkan pesan kosong
                val row = TableRow(context).apply {
                    layoutParams = TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(8, 8, 8, 8)
                }
                val emptyMessage = TextView(context).apply {
                    text = "Tidak ada barang dihutang"
                    gravity = Gravity.CENTER
                    setPadding(16, 16, 16, 16)
                }
                row.addView(emptyMessage)
                tableBarang.addView(row)
            }


            // Handle delete button
            hapus.setOnClickListener {
                // Menampilkan dialog konfirmasi
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Apakah Anda yakin ingin menghapus hutang ${hutang.penghutang}?")
                    .setPositiveButton("Ya") { _, _ ->
                        // Mendapatkan referensi ke Firebase Realtime Database
                        val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
                        val hutangRef = database.getReference("hutang/${hutang.id}") // Gunakan id unik dari hutang

                        // Menghapus data dari Firebase
                        hutangRef.removeValue()
                            .addOnSuccessListener {
                                // Tampilkan toast jika berhasil menghapus
                                Toast.makeText(
                                    requireContext(),
                                    "Hutang ${hutang.penghutang} berhasil dihapus",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss() // Menutup dialog setelah penghapusan berhasil
                            }
                            .addOnFailureListener { e ->
                                // Tampilkan toast jika gagal
                                Toast.makeText(
                                    requireContext(),
                                    "Gagal menghapus data: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Tidak", null) // Menutup dialog jika memilih Tidak
                    .show()
            }

            // Show dialog
            dialog.show()
        }

        hutangAdapter.itemLongClickListener = { hutang ->
            if (hutang.status?.lowercase() == "lunas") {
                // Jika sudah lunas, tampilkan toast
                Toast.makeText(
                    requireContext(),
                    "Hutang ${hutang.penghutang} Sudah Lunas",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Tampilkan dialog konfirmasi untuk mengubah status ke lunas
                AlertDialog.Builder(requireContext())
                    .setTitle("Konfirmasi")
                    .setMessage("Apakah Anda ingin mengubah status hutang ${hutang.penghutang} menjadi lunas?")
                    .setPositiveButton("Ya") { _, _ ->
                        // Update status ke Realtime Database
                        val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
                        val hutangRef = database.getReference("hutang/${hutang.id}") // Ganti "hutang" dengan nama node Anda
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                        val currentDay = SimpleDateFormat("EEEE, dd", Locale.getDefault()).format(Date())

                        hutangRef.child("status").setValue("lunas")
                            .addOnSuccessListener {
                                // Perbarui data income
                                val incomeRef = database.getReference("income/${hutang.toko}/${currentYear}/${currentMonth}/${currentDay}")

                                val barangHutang = hutang.barang_dihutang ?: mapOf()

                                var totalHargaHarian = 0
                                var labaBersihHarian = 0

                                for ((idBarang, barang) in barangHutang) {
                                    val hargaSatuan = barang.harga_satuan ?: 0
                                    val hargaBeli = barang.harga_beli ?: 0
                                    val jumlah = barang.jumlah ?: 0

                                    val labaBersih = (hargaSatuan - hargaBeli) * jumlah
                                    val totalHarga = hargaSatuan * jumlah

                                    totalHargaHarian += totalHarga
                                    labaBersihHarian += labaBersih

                                    // Tambahkan barang ke database harian
                                    incomeRef.child(idBarang).addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.exists()) {
                                                // Update data lama
                                                snapshot.ref.child("jumlah").setValue(ServerValue.increment(jumlah.toDouble()))
                                                snapshot.ref.child("laba_bersih").setValue(ServerValue.increment(labaBersih.toDouble()))
                                                snapshot.ref.child("total_harga").setValue(ServerValue.increment(totalHarga.toDouble()))
                                            } else {
                                                // Tambahkan data baru
                                                snapshot.ref.setValue(
                                                    mapOf(
                                                        "category" to barang.category,
                                                        "id_barang" to barang.id,
                                                        "nama_barang" to barang.nama_barang,
                                                        "jumlah" to jumlah,
                                                        "harga_beli" to hargaBeli,
                                                        "harga_satuan" to hargaSatuan,
                                                        "laba_bersih" to labaBersih,
                                                        "total_harga" to totalHarga
                                                    )
                                                )
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(
                                                requireContext(),
                                                "Gagal memperbarui barang: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                }

                                // Perbarui total harga harian dan laba bersih harian
                                incomeRef.child("total_harga_harian").addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val existingTotal = snapshot.getValue(Int::class.java) ?: 0
                                        snapshot.ref.setValue(existingTotal + totalHargaHarian)
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })

                                incomeRef.child("laba_bersih_harian").addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val existingLaba = snapshot.getValue(Int::class.java) ?: 0
                                        snapshot.ref.setValue(existingLaba + labaBersihHarian)
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })

                                // Perbarui total bulanan dan tahunan
                                val monthlyRef = database.getReference("income/${hutang.toko}/${currentYear}/${currentMonth}")
                                monthlyRef.child("total_harga_bulanan").setValue(ServerValue.increment(totalHargaHarian.toDouble()))
                                monthlyRef.child("laba_bersih_bulanan").setValue(ServerValue.increment(labaBersihHarian.toDouble()))

                                val yearlyRef = database.getReference("income/${hutang.toko}/${currentYear}")
                                yearlyRef.child("total_harga_tahunan").setValue(ServerValue.increment(totalHargaHarian.toDouble()))
                                yearlyRef.child("laba_bersih_tahunan").setValue(ServerValue.increment(labaBersihHarian.toDouble()))

                                // Tampilkan toast jika berhasil
                                Toast.makeText(
                                    requireContext(),
                                    "Status hutang ${hutang.penghutang} berhasil diubah menjadi lunas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                // Tampilkan toast jika gagal
                                Toast.makeText(
                                    requireContext(),
                                    "Gagal mengubah status: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Tidak", null) // Tutup dialog tanpa aksi
                    .show()
            }
        }

        // Get toko name from SharedPreferences
        val sharedPreferences: SharedPreferences =
            requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        toko = sharedPreferences.getString("NAMA_TOKO", "Nama Toko Tidak Tersedia") ?: ""

        // Load data from Firebase
        fetchItems()

        // Initialize Search Functionality
        SearchFunctionality()
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
            hutangAdapter.updateData(hutangList)
            emptyMessage.visibility = if (hutangList.isEmpty()) View.VISIBLE else View.GONE
            emptyMessage.text = "anda sekarang belung memiliki penghutang :)" // Isi teks untuk keadaan data kosong
            return
        }

        val filteredList = hutangList.filter { hutang ->
            val nameMatch = hutang.penghutang?.contains(query, ignoreCase = true) == true
            val totalMatch = hutang.total_hutang.toString().contains(query)
            val statusMatch = hutang.status?.contains(query, ignoreCase = true) == true

            nameMatch || totalMatch || statusMatch
        }

        hutangAdapter.updateData(filteredList)
        emptyMessage.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        // Ganti teks ketika hasil pencarian tidak ada
        emptyMessage.text = if (filteredList.isEmpty()) "Tidak ada hutang yang sesuai dengan pencarian" else ""
    }

    private fun fetchItems() {
        val databaseReference = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
            .getReference("hutang")

        // Menggunakan ValueEventListener untuk pembaruan otomatis
        databaseReference.orderByChild("toko").equalTo(toko).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                hutangList.clear() // Hapus data lama
                for (data in snapshot.children) {
                    val hutang = data.getValue(dashboard.Hutang::class.java)
                    if (hutang != null) {
                        hutangList.add(hutang)
                    }
                }

                // Mengurutkan data berdasarkan status
                hutangList.sortWith(Comparator { hutang1, hutang2 ->
                    // Urutkan berdasarkan status (lunas di atas pending)
                    when {
                        hutang1.status == "lunas" && hutang2.status != "lunas" -> -1
                        hutang1.status != "lunas" && hutang2.status == "lunas" -> 1
                        else -> 0
                    }
                })
                // Show or hide the empty inventory message based on item count
                if (hutangList.isEmpty()) {
                    emptyMessage.visibility = View.VISIBLE
                } else {
                    emptyMessage.visibility = View.GONE
                }
                hutangAdapter.notifyDataSetChanged() // Perbarui RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
