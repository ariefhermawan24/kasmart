package com.mariefhermawan.kasmart

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.mariefhermawan.kasmart.inventory.Barang
import java.text.SimpleDateFormat
import java.util.*

class dashboard : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var barangAdapter: barangAdapter
    private val barangList = mutableListOf<Barang>()
    private lateinit var buttonHutang: Button
    private lateinit var buttonSucces: Button
    private lateinit var filteredBarangList: MutableList<Barang> // To store filtered data
    private lateinit var emptyMessage: TextView

    // Firebase Database Reference
    private val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
    private val inventoryRef = database.getReference("barang") // Reference to "barang" node
    private val hutangRef = database.getReference("hutang") // Reference to "hutang" node

    // Toko name from SharedPreferences
    private lateinit var toko: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.produk_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val totalText: TextView = view.findViewById(R.id.totalText)
        barangAdapter = barangAdapter(barangList) { total ->
            totalText.text = "Rp$total"
        }
        recyclerView.adapter = barangAdapter
        // Initialize empty message TextView
        emptyMessage = view.findViewById(R.id.empty_inventory_message)

        // Button for hutang

        buttonHutang = view.findViewById(R.id.buttonHutang)
        buttonHutang.setOnClickListener { showHutangDialog() }
        buttonSucces = view.findViewById(R.id.buttonPembayaranSelesai)
        buttonSucces.setOnClickListener {
            // Menonaktifkan tombol untuk mencegah spam
            buttonSucces.isEnabled = false
            saveIncomeToFirebase()
        }

        // Get toko name from SharedPreferences
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        toko = sharedPreferences.getString("NAMA_TOKO", "Nama Toko Tidak Tersedia") ?: ""

        // Fetch data from Firebase
        fetchDataFromFirebase()

        return view
    }

    private fun fetchDataFromFirebase() {
        inventoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                barangList.clear()
                for (dataSnapshot in snapshot.children) {
                    val barang = dataSnapshot.getValue(Barang::class.java)
                    if (barang != null && barang.toko == toko && barang.stock != 0) {
                        barangList.add(barang)
                    }
                }
                // Mengurutkan barangList berdasarkan nama barang (A-Z)
                barangList.sortBy { it.name?.toLowerCase(Locale.getDefault()) }
                filteredBarangList = ArrayList(barangList) // Copy data to filtered list
                barangAdapter.notifyDataSetChanged()
                // Show or hide the empty inventory message based on item count
                if (barangList.isEmpty()) {
                    emptyMessage.visibility = View.VISIBLE
                } else {
                    emptyMessage.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data: ${error.message}")
                Toast.makeText(requireContext(), "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showHutangDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_hutang, null)
        val editTextPenghutang: EditText = dialogView.findViewById(R.id.edit_text_penghutang)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Input Hutang")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val namaPenghutang = editTextPenghutang.text.toString().trim()
                if (namaPenghutang.isNotEmpty()) {
                    saveHutangToFirebase(namaPenghutang)
                } else {
                    Toast.makeText(requireContext(), "Nama penghutang harus diisi!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create()
        alertDialog.show()
    }

    private fun saveHutangToFirebase(penghutang: String) {
        // Validasi nama penghutang hanya boleh huruf
        if (!penghutang.matches("^[a-zA-Z]+$".toRegex())) {
            Toast.makeText(requireContext(), "Nama penghutang hanya boleh terdiri dari huruf!", Toast.LENGTH_SHORT).show()
            return
        }

        hutangRef.orderByChild("penghutang").equalTo(penghutang).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val penghutangSamaDiToko = snapshot.children.any {
                        it.child("toko").value.toString() == toko
                    }

                    if (penghutangSamaDiToko) {
                        // Jika penghutang sudah ada di toko yang sama, tampilkan Toast
                        Toast.makeText(requireContext(), "Nama penghutang sudah terdaftar untuk toko ini, silakan gunakan nama lain.", Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                // Nama penghutang valid, lanjutkan dengan proses validasi barang
                val barangDihutang = barangAdapter.getSelectedItems() // Get selected items
                if (barangDihutang.isEmpty()) {
                    // Pesan toast jika tidak ada barang yang dipilih
                    Toast.makeText(requireContext(), "Silakan pilih barang terlebih dahulu dari daftar yang ada.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Kumpulkan barang yang jumlahnya melebihi stok
                val barangMelebihiStok = mutableListOf<String>()
                for ((barangId, data) in barangDihutang) {
                    val barang = barangList.find { it.id == barangId }
                    if (barang != null && data.second.second > (barang.stock ?: 0)) {
                        barangMelebihiStok.add("${data.first} (stok tersedia: ${barang.stock ?: 0})")
                    }
                }

                // Jika ada barang yang melebihi stok, tampilkan pesan error dan batalkan proses
                if (barangMelebihiStok.isNotEmpty()) {
                    val pesanError = "Barang berikut melebihi stok yang tersedia:\n" +
                            barangMelebihiStok.joinToString("\n") { it }
                    Toast.makeText(requireContext(), pesanError, Toast.LENGTH_LONG).show()
                    return
                }

                // Hitung total hutang
                val totalHutang = barangDihutang.entries.sumOf { it.value.third.first } // Mengambil harga total dari Triple

                // Mendapatkan tanggal saat ini
                val tanggalHutang = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) // Current date

                // Menyusun data hutang
                val hutangData = mapOf(
                    "tanggal_hutang" to tanggalHutang,
                    "penghutang" to penghutang,
                    "status" to "pending",
                    "toko" to toko,
                    "barang_dihutang" to barangDihutang.mapKeys { it.key }.mapValues { (barangId, data) ->
                        val barang = barangList.find { it.id == barangId }
                        if (barang != null) {
                            mapOf(
                                "id_barang" to barangId,
                                "nama_barang" to data.first,
                                "category" to data.second.first,
                                "jumlah" to data.second.second,
                                "harga_satuan" to data.third.third,
                                "harga_beli" to data.third.second,
                                "total_harga" to data.third.first,
                            )
                        } else {
                            mapOf(
                                "id_barang" to "id tidak ditemukan",
                                "nama_barang" to "nama barang tidak diketahui",
                                "category" to "tidak ada category",
                                "jumlah" to 0,
                                "harga_satuan" to 0,
                                "harga_beli" to 0,
                                "total_harga" to 0
                            )
                        }
                    },
                    "total_hutang" to totalHutang
                )

                // Menyimpan data hutang ke Firebase dan mendapatkan ID dari push()
                val newHutangRef = hutangRef.push()
                val hutangId = newHutangRef.key // Mendapatkan ID yang dihasilkan oleh push()

                // Menambahkan ID hutang ke dalam data
                val hutangDataWithId = hutangData + ("id" to hutangId)

                // Menyimpan data hutang dengan ID ke Firebase
                newHutangRef.setValue(hutangDataWithId)
                    .addOnSuccessListener {
                        // Setelah data hutang disimpan, mengurangi stock barang yang dipilih
                        for ((barangId, data) in barangDihutang) {
                            val barang = barangList.find { it.id == barangId }
                            if (barang != null) {
                                val updatedStock = (barang.stock ?: 0) - data.second.second
                                inventoryRef.child(barangId).child("stock").setValue(updatedStock)
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Gagal mengurangi stock: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        barangAdapter.clearSelectedItems()
                        Toast.makeText(requireContext(), "Hutang berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal menyimpan hutang: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memproses permintaan: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun saveIncomeToFirebase() {
        val dateFormat = SimpleDateFormat("EEEE, dd", Locale("id", "ID")) // Format nama hari, tanggal
        val monthFormat = SimpleDateFormat("MMMM", Locale("id", "ID"))   // Format bulan
        val yearFormat = SimpleDateFormat("yyyy", Locale("id", "ID"))    // Format tahun

        val currentDate = dateFormat.format(Date())
        val currentMonth = monthFormat.format(Date())
        val currentYear = yearFormat.format(Date())

        val selectedItems = barangAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada barang yang dipilih untuk transaksi.", Toast.LENGTH_SHORT).show()
            // Mengaktifkan kembali tombol jika tidak ada barang yang dipilih
            buttonSucces.isEnabled = true
            return
        }

        // Kumpulkan barang yang jumlahnya melebihi stok
        val barangMelebihiStok = mutableListOf<String>()
        for ((barangId, data) in selectedItems) {
            val barang = barangList.find { it.id == barangId }
            if (barang != null && data.second.second > (barang.stock ?: 0)) {
                barangMelebihiStok.add("${data.first} (stok tersedia: ${barang.stock ?: 0})")
            }
        }

        // Jika ada barang yang melebihi stok, tampilkan pesan error dan batalkan proses
        if (barangMelebihiStok.isNotEmpty()) {
            val pesanError = "Barang berikut melebihi stok yang tersedia:\n" +
                    barangMelebihiStok.joinToString("\n") { it }
            Toast.makeText(requireContext(), pesanError, Toast.LENGTH_LONG).show()
            // Mengaktifkan kembali tombol jika tidak ada barang yang dipilih
            buttonSucces.isEnabled = true
            return
        }

        // Referensi Firebase untuk hari, bulan, dan tahun
        val incomeRef = database.getReference("income/$toko/$currentYear/$currentMonth/$currentDate")
        val yearlyRef = database.getReference("income/$toko/$currentYear")

        incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val incomeData = snapshot.value as? Map<String, Any> ?: emptyMap()
                var totalHargaHarian = incomeData["total_harga_harian"] as? Long ?: 0L
                var labaBersihHarian = incomeData["laba_bersih_harian"] as? Long ?: 0L

                selectedItems.forEach { (barangId, data) ->
                    val existingItem = incomeData.entries.find { entry ->
                        val item = entry.value as? Map<String, Any>
                        item?.get("id_barang") == barangId
                    }

                    if (existingItem != null) {
                        val key = existingItem.key
                        val oldData = existingItem.value as? Map<String, Any> ?: emptyMap()
                        val jumlahLama = (oldData["jumlah"] as? Long ?: 0L).toInt()
                        val totalHargaLama = (oldData["total_harga"] as? Long ?: 0L)
                        val labaBersihLama = (oldData["laba_bersih"] as? Long ?: 0L)

                        val jumlahBaru = jumlahLama + data.second.second
                        val totalHargaBaru = totalHargaLama + data.third.first
                        val labaBersihBaru = labaBersihLama + (data.third.first - (data.third.second * data.second.second))

                        incomeRef.child(barangId).updateChildren(
                            mapOf(
                                "jumlah" to jumlahBaru,
                                "total_harga" to totalHargaBaru,
                                "laba_bersih" to labaBersihBaru
                            )
                        )

                        totalHargaHarian += data.third.first
                        labaBersihHarian += (data.third.first - (data.third.second * data.second.second))

                        barangAdapter.clearSelectedItems()
                    } else {
                        incomeRef.child(barangId).setValue(
                            mapOf(
                                "id_barang" to barangId,
                                "nama_barang" to data.first,
                                "category" to data.second.first,
                                "jumlah" to data.second.second,
                                "harga_satuan" to data.third.third,
                                "harga_beli" to data.third.second,
                                "total_harga" to data.third.first,
                                "laba_bersih" to (data.third.first - (data.third.second * data.second.second))
                            )
                        )

                        totalHargaHarian += data.third.first
                        labaBersihHarian += (data.third.first - (data.third.second * data.second.second))
                    }
                }

                incomeRef.child("total_harga_harian").setValue(totalHargaHarian)
                incomeRef.child("laba_bersih_harian").setValue(labaBersihHarian)

                yearlyRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(yearSnapshot: DataSnapshot) {
                        var totalHargaBulanan = 0L
                        var totalHargaTahunan = 0L
                        var labaBersihBulanan = 0L
                        var labaBersihTahunan = 0L

                        yearSnapshot.children.forEach { monthSnapshot ->
                            var totalBulan = 0L
                            var labaBersihBulan = 0L

                            monthSnapshot.children.forEach { daySnapshot ->
                                if (daySnapshot.key != "total_harga_bulanan" && daySnapshot.key != "laba_bersih_bulanan") {
                                    totalBulan += daySnapshot.child("total_harga_harian").value as? Long ?: 0L
                                    labaBersihBulan += daySnapshot.child("laba_bersih_harian").value as? Long ?: 0L
                                }
                            }

                            if (monthSnapshot.key == currentMonth) {
                                totalHargaBulanan = totalBulan
                                labaBersihBulanan = labaBersihBulan
                            }

                            totalHargaTahunan += totalBulan
                            labaBersihTahunan += labaBersihBulan
                        }

                        yearlyRef.child("$currentMonth/total_harga_bulanan").setValue(totalHargaBulanan)
                        yearlyRef.child("$currentMonth/laba_bersih_bulanan").setValue(labaBersihBulanan)

                        yearlyRef.child("total_harga_tahunan").setValue(totalHargaTahunan)
                        yearlyRef.child("laba_bersih_tahunan").setValue(labaBersihTahunan)
                            .addOnSuccessListener {
                                for ((barangId, data) in selectedItems) {
                                    val barang = barangList.find { it.id == barangId }
                                    if (barang != null) {
                                        val updatedStock = (barang.stock ?: 0) - data.second.second
                                        inventoryRef.child(barangId).child("stock").setValue(updatedStock)
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context?.applicationContext, "Gagal mengurangi stock: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                context?.let {
                                    Toast.makeText(it.applicationContext, "Data pendapatan berhasil diperbarui!", Toast.LENGTH_SHORT).show()

                                    // Mengaktifkan kembali tombol jika tidak ada barang yang dipilih
                                    buttonSucces.isEnabled = true
                                }
                                barangAdapter.clearSelectedItems()
                            }
                            .addOnFailureListener { e ->
                                context?.let {
                                    Toast.makeText(it.applicationContext, "Gagal menyimpan data pendapatan: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        context?.let {
                            Toast.makeText(it.applicationContext, "Gagal memuat data tahunan: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                context?.let {
                    Toast.makeText(it.applicationContext, "Gagal memuat data harian: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    // Data class for the debt item
    data class Hutang(
        val id: String? = null,                  // ID unik untuk setiap hutang
        val tanggal_hutang: String? = null,      // Tanggal ketika hutang dibuat
        val penghutang: String? = null,         // Nama penghutang
        val status: String = "pending",         // Status hutang (default: "pending")
        val toko: String? = null,               // Nama toko terkait hutang
        val barang_dihutang: Map<String, BarangHutang>? = null,// Daftar barang yang dihutang
        val total_hutang: Int?= null
    )

    // Data class for the items in debt
    data class BarangHutang(
        val id: String? = null,                 // ID unik barang
        val nama_barang: String? = null,         // Nama barang
        val category: String? = null,           // Kategori barang
        val jumlah: Int? = null,                // Jumlah barang dihutang
        val harga_satuan: Int? = null,
        val harga_beli: Int? = null,
        val total_harga: Int? = null             // Total harga barang
    )
}
