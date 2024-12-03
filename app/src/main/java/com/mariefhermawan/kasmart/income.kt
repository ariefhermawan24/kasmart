package com.mariefhermawan.kasmart

import android.app.DatePickerDialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class income : Fragment() {

    private lateinit var btnPickDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvDay: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvYear: TextView
    private lateinit var bulanLaba: TextView
    private lateinit var tahunLabe: TextView

    private lateinit var incomeHarian: TextView
    private lateinit var incomeBulanan: TextView
    private lateinit var incomeTahunan: TextView
    private lateinit var labaBersihBulanan: TextView
    private lateinit var labaBersihTahunan: TextView
    private lateinit var tableLayout: TableLayout
    private lateinit var tableList: TableLayout
    private lateinit var labaBersihHarian: TextView


    private lateinit var database: DatabaseReference
    private lateinit var toko: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income, container, false)

        btnPickDate = view.findViewById(R.id.btn_pick_date)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        tvDay = view.findViewById(R.id.tv_day)
        tvMonth = view.findViewById(R.id.tv_month)
        tvYear = view.findViewById(R.id.tv_year)
        incomeHarian = view.findViewById(R.id.incomeharian)
        incomeBulanan = view.findViewById(R.id.incomebulanan)
        incomeTahunan = view.findViewById(R.id.incomeTahunan)
        tableLayout = view.findViewById(R.id.laba_bersih) // Initialize tableLayout
        labaBersihHarian = tableLayout.findViewById(R.id.total_clear_income) // Now it can find the TextView
        tableList = view.findViewById(R.id.list_total_harian)
        labaBersihBulanan = view.findViewById(R.id.clear_income_bulanan)
        labaBersihTahunan = view.findViewById(R.id.clear_income_Tahunan)
        bulanLaba = view.findViewById(R.id.tv_laba_bulan)
        tahunLabe = view.findViewById(R.id.tv_laba_tahun)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", MODE_PRIVATE)
        toko = sharedPreferences.getString("NAMA_TOKO", "Nama Toko Tidak Tersedia") ?: ""

        database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
            .getReference("income").child(toko)

        setInitialDate()

        btnPickDate.setOnClickListener { showDatePickerDialog() }

        return view
    }

    private fun setInitialDate() {
        val calendar = Calendar.getInstance()
        val todayDate = calendar.time

        val dayFormat = SimpleDateFormat("EEEE, dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM", Locale("id", "ID"))
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        tvDay.text = dayFormat.format(todayDate)
        tvMonth.text = monthFormat.format(todayDate)
        tvYear.text = yearFormat.format(todayDate)
        bulanLaba.text = "Laba Bersih Bulan "+monthFormat.format(todayDate)
        tahunLabe.text = "Laba Bersih Tahun "+yearFormat.format(todayDate)

        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(todayDate)
        tvSelectedDate.text = "Tanggal dipilih: $formattedDate"

        fetchData(tvYear.text.toString(), tvMonth.text.toString(), tvDay.text.toString())
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                updateDateViews(selectedCalendar)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun updateDateViews(calendar: Calendar) {
        val dayFormat = SimpleDateFormat("EEEE, dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM", Locale("id", "ID"))
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

        tvDay.text = dayFormat.format(calendar.time)
        tvMonth.text = monthFormat.format(calendar.time)
        tvYear.text = yearFormat.format(calendar.time)
        bulanLaba.text = "Laba Bersih Bulan "+monthFormat.format(calendar.time)
        tahunLabe.text = "Laba Bersih Tahun "+yearFormat.format(calendar.time)
        tvSelectedDate.text = "Tanggal dipilih: ${fullDateFormat.format(calendar.time)}"

        fetchData(tvYear.text.toString(), tvMonth.text.toString(), tvDay.text.toString())
    }

    private fun fetchData(year: String, month: String, day: String) {
        val dateKey = "$day" // Sesuaikan dengan struktur Firebase Anda

        // Ambil data laba bersih harian dari Firebase
        database.child(year).child(month).child(dateKey).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener // Pastikan fragment masih aktif
                val dailyIncome = snapshot.child("total_harga_harian").getValue(Int::class.java) ?: 0
                incomeHarian.text = "Rp.${dailyIncome}"

                val labaBersihharian = snapshot.child("laba_bersih_harian").getValue(Int::class.java) ?: 0
                labaBersihHarian.text = "Rp.${labaBersihharian}" // Tampilkan laba bersih harian

                // Memproses data barang untuk ditampilkan di tabel
                val items = snapshot.children.filter { it.key != "total_harga_harian" && it.key != "laba_bersih_harian" }
                populateTable(items)

                // Ambil data bulanan
                database.child(year).child(month).get()
                    .addOnSuccessListener { monthSnapshot ->
                        if (!isAdded) return@addOnSuccessListener // Pastikan fragment masih aktif
                        val monthlyIncome = monthSnapshot.child("total_harga_bulanan").getValue(Int::class.java) ?: 0
                        val labaBersihBulananValue = monthSnapshot.child("laba_bersih_bulanan").getValue(Int::class.java) ?: 0
                        incomeBulanan.text = "Rp.${monthlyIncome}"
                        labaBersihBulanan.text = "Rp.${labaBersihBulananValue}" // Correct assignment

                        // Ambil data tahunan
                        database.child(year).get()
                            .addOnSuccessListener { yearSnapshot ->
                                if (!isAdded) return@addOnSuccessListener // Pastikan fragment masih aktif
                                val yearlyIncome = yearSnapshot.child("total_harga_tahunan").getValue(Int::class.java) ?: 0
                                val labaBersihTahunanValue = yearSnapshot.child("laba_bersih_tahunan").getValue(Int::class.java) ?: 0
                                incomeTahunan.text = "Rp.${yearlyIncome}"
                                labaBersihTahunan.text = "Rp.${labaBersihTahunanValue}" // Correct assignment
                            }
                            .addOnFailureListener {
                                labaBersihTahunan.text = "Rp.000"
                                incomeTahunan.text = "Rp.000"
                            }
                    }
                    .addOnFailureListener {
                        labaBersihBulanan.text ="Rp.000"
                        incomeBulanan.text = "Rp.000"
                    }
            }
            .addOnFailureListener {
                // Menangani kegagalan dan menampilkan default
                incomeHarian.text = "Rp.000"
                incomeBulanan.text = "Rp.000"
                incomeTahunan.text = "Rp.000"
                labaBersihHarian.text = "Rp.000"
                labaBersihBulanan.text = "Rp.000"
                labaBersihTahunan.text = "Rp.000"
            }
    }


    private fun populateTable(items: List<DataSnapshot>) {
        if (!isAdded) return // Pastikan fragment masih terhubung ke konteks

        // Hapus baris sebelumnya (kecuali header)
        tableList.removeViews(1, tableList.childCount - 1)

        if (items.isEmpty()) {
            // Tampilkan pesan jika tidak ada transaksi
            val emptyRow = TableRow(requireContext()).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val emptyMessageTextView = TextView(requireContext()).apply {
                text = "Belum ada transaksi pada hari ini"
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                ).apply {
                    span = 5 // Mengatur TextView agar mencakup semua kolom
                }
            }

            emptyRow.addView(emptyMessageTextView)
            tableList.addView(emptyRow)
            return
        }

        // Urutkan data berdasarkan jumlah secara menurun
        val sortedItems = items.sortedByDescending { it.child("jumlah").getValue(Int::class.java) ?: 0 }

        var index = 1
        for (item in sortedItems) {
            val namaBarang = item.child("nama_barang").getValue(String::class.java) ?: "-"
            val jumlah = item.child("jumlah").getValue(Int::class.java) ?: 0
            val hargaBeli = item.child("harga_beli").getValue(Int::class.java) ?: 0
            val hargaSatuan = item.child("harga_satuan").getValue(Int::class.java) ?: 0

            // Membuat baris baru
            val tableRow = TableRow(requireContext()).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Menambahkan kolom ke dalam baris
            val noTextView = createTextView(index.toString(), true)
            val barangTextView = createTextView(namaBarang, false)
            val jumlahTextView = createTextView(jumlah.toString(), true)
            val hargaBeliTextView = createTextView("Rp.${hargaBeli}", true)
            val hargaJualTextView = createTextView("Rp.${hargaSatuan}", true)

            // Menambahkan TextView ke TableRow
            tableRow.addView(noTextView)
            tableRow.addView(barangTextView)
            tableRow.addView(jumlahTextView)
            tableRow.addView(hargaBeliTextView)
            tableRow.addView(hargaJualTextView)

            // Menambahkan TableRow ke TableLayout
            tableList.addView(tableRow)
            index++
        }
    }

    // Fungsi pembantu untuk membuat TextView dengan gaya tertentu
    private fun createTextView(text: String, isCentered: Boolean): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            this.setPadding(8, 8, 8, 8)
            this.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            this.gravity = if (isCentered) android.view.Gravity.CENTER else android.view.Gravity.START
        }
    }

}
