package com.mariefhermawan.kasmart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var namaTokoTextView: TextView
    private lateinit var incomeTeks: TextView
    private var updateNamaTokoReceiver: BroadcastReceiver? = null
    private lateinit var databaseReference: DatabaseReference

    // Variabel untuk menyimpan tombol
    private lateinit var dashboardButton: CardView
    private lateinit var inventoryButton: CardView
    private lateinit var debtButton: CardView
    private lateinit var incomeButton: CardView
    private lateinit var accountButton: CardView
    private lateinit var cardHeader: CardView
    private var alertDialog: AlertDialog? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback  // Menyimpan referensi callback
    private var isNetworkCallbackRegistered = false  // Flag untuk menandai status callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Cek status koneksi saat aplikasi dimulai
        checkInternetConnection()

        // Setup listener untuk perubahan status koneksi
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                runOnUiThread {
                    // Jika koneksi tersedia, dismiss dialog jika ada
                    alertDialog?.dismiss()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                runOnUiThread {
                    // Jika koneksi hilang, tampilkan dialog peringatan
                    if (alertDialog == null || !alertDialog!!.isShowing) {
                        showNoInternetDialog() // Pastikan memanggil fungsi yang benar
                    }
                }
            }
        }

        // Daftarkan callback untuk status koneksi
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        isNetworkCallbackRegistered = true  // Tandai bahwa callback telah terdaftar

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val storeName = sharedPreferences.getString("NAMA_TOKO", null)

        if (storeName == null) {
            startActivity(Intent(this, login::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        dashboardButton = findViewById(R.id.dashboard_button)
        inventoryButton = findViewById(R.id.inventory_button)
        debtButton = findViewById(R.id.debt_button)
        incomeButton = findViewById(R.id.income_button)
        accountButton = findViewById(R.id.account_button)
        incomeTeks = findViewById(R.id.income)
        cardHeader = findViewById(R.id.header)
        // Inisialisasi Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/").getReference("income")

        val dateTextView = findViewById<TextView>(R.id.date)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        dateTextView.text = dateFormat.format(calendar.time)

        namaTokoTextView = findViewById(R.id.nama_toko)
        namaTokoTextView.text = storeName
        // Panggil fungsi untuk membaca data dari database
        fetchDailyIncome(storeName)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        if (savedInstanceState == null) {
            loadFragment(dashboard())  // Memastikan Home ditampilkan saat pertama kali masuk
            highlightButton(dashboardButton)
        }

        // Menambahkan listener pada setiap button
        dashboardButton.setOnClickListener {
            loadFragment(dashboard())
            highlightButton(dashboardButton)
            saveSelectedButton(R.id.dashboard_button)
        }
        inventoryButton.setOnClickListener {
            loadFragment(inventory())
            highlightButton(inventoryButton)
            saveSelectedButton(R.id.inventory_button)
        }
        debtButton.setOnClickListener {
            loadFragment(hutang())
            highlightButton(debtButton)
            saveSelectedButton(R.id.debt_button)
        }
        incomeButton.setOnClickListener {
            loadFragment(income())
            highlightButton(incomeButton)
            saveSelectedButton(R.id.income_button)
        }
        accountButton.setOnClickListener {
            loadFragment(akun())
            highlightButton(accountButton)
            saveSelectedButton(R.id.account_button)
        }

        // Daftar receiver untuk memperbarui nama toko
        updateNamaTokoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val storeName = intent.getStringExtra("NAMA_TOKO")
                storeName?.let {
                    namaTokoTextView.text = it
                    // Panggil fungsi untuk membaca data dari database
                    fetchDailyIncome(it)
                }
            }
        }

        // Registrasi receiver
        registerReceiver(
            updateNamaTokoReceiver,
            IntentFilter("com.mariefhermawan.kasmart.ACTION_UPDATE_NAMA_TOKO"),
            RECEIVER_NOT_EXPORTED
        )



        // Panggil fungsi untuk menyorot tombol yang dipilih terakhir kali
        highlightLastSelectedButton()
    }

    private fun fetchDailyIncome(storeName: String) {
        // Mendapatkan tanggal hari ini dalam format sesuai struktur database
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = SimpleDateFormat("MMMM", Locale("id", "ID")).format(calendar.time)
        val day = SimpleDateFormat("EEEE, dd", Locale("id", "ID")).format(calendar.time)

        // Referensi langsung ke "Minggu, 24"
        val dailyIncomeRef = databaseReference
            .child(storeName) // Nama toko
            .child(year) // Tahun
            .child(month) // Bulan
            .child(day) // Hari (contoh: Minggu, 24)

        // Dengarkan perubahan data di path ini
        dailyIncomeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Ambil nilai dari total_harga_harian langsung
                    val totalIncome = snapshot.child("total_harga_harian").getValue(Double::class.java) ?: 0.0
                    // Tampilkan data di TextView
                    incomeTeks.text = "Rp${String.format("%,.0f", totalIncome)}"
                } else {
                    // Jika tidak ada data untuk hari ini
                    incomeTeks.text = "Rp0"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Menangani error saat membaca data
                incomeTeks.text = "Error!"
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Panggil fungsi untuk menyorot tombol yang dipilih terakhir kali saat aplikasi dibuka kembali
        highlightLastSelectedButton()

        // Pastikan aplikasi selalu kembali ke Home saat di-restart
        if (isTaskRoot) {  // Mengecek apakah aktivitas ini adalah root activity (aplikasi dimulai ulang)
            loadFragment(dashboard())  // Memastikan fragment home selalu muncul saat aplikasi kembali
            highlightButton(dashboardButton)  // Menyorot tombol Home
        }
    }

    private fun highlightLastSelectedButton() {
        val sharedPreferences = getSharedPreferences("KASmartPrefs", MODE_PRIVATE)
        val selectedButtonId = sharedPreferences.getInt("selectedButtonId", R.id.dashboard_button) // Default ke dashboardButton jika belum ada

        val selectedButton = findViewById<CardView>(selectedButtonId)

        // Add a null check to prevent NullPointerException
        if (selectedButton != null) {
            highlightButton(selectedButton)  // Panggil fungsi highlightButton untuk menyorot tombol
        } else {
            // Handle the case where the button is not found (e.g., default to the dashboard button)
            highlightButton(dashboardButton)
        }
    }

    private fun highlightButton(selectedButton: CardView) {
        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val selectedColor = Color.parseColor("#226CFF")
        val defaultTextColor = if (isNightMode) Color.WHITE else Color.BLACK

        // Reset semua tombol
        val buttonToTextMap = mapOf(
            dashboardButton to R.id.dashboard_text,
            inventoryButton to R.id.inventory_text,
            debtButton to R.id.debt_text,
            incomeButton to R.id.income_text,
            accountButton to R.id.account_text
        )

        val buttonToIconMap = mapOf(
            dashboardButton to R.id.dashboard_icon,
            inventoryButton to R.id.inventory_icon,
            debtButton to R.id.debt_icon,
            incomeButton to R.id.income_icon,
            accountButton to R.id.account_icon
        )

        buttonToTextMap.forEach { (button, textId) ->
            // Reset warna teks
            button.findViewById<TextView>(textId).setTextColor(defaultTextColor)

            // Reset warna ikon (mengubah kembali ke warna default)
            val iconImageView = button.findViewById<ImageView>(buttonToIconMap[button] ?: return@forEach)
            iconImageView.setColorFilter(defaultTextColor) // Set warna default untuk ikon
        }

        // Ganti warna ikon dan teks untuk tombol yang terpilih
        val activeTextId = buttonToTextMap[selectedButton]
        val activeIconImageView = selectedButton.findViewById<ImageView>(buttonToIconMap[selectedButton] ?: return)

        if (activeTextId != null && activeIconImageView != null) {
            // Set warna teks untuk tombol aktif
            selectedButton.findViewById<TextView>(activeTextId).setTextColor(selectedColor)

            // Set warna ikon untuk tombol aktif
            activeIconImageView.setColorFilter(selectedColor)  // Ganti warna ikon
        }
    }

    private fun saveSelectedButton(buttonId: Int) {
        val sharedPreferences = getSharedPreferences("KASmartPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("selectedButtonId", buttonId)
        editor.apply()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkInternetConnection() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            showNoInternetDialog() // Tampilkan dialog jika tidak ada koneksi
        }
    }

    private fun showNoInternetDialog() {
        // Tampilkan alert dialog jika koneksi tidak ada
        if (alertDialog == null || !alertDialog!!.isShowing) {
            alertDialog = AlertDialog.Builder(this)
                .setTitle("No Internet")
                .setMessage("You are currently offline. Please check your internet connection.")
                .setCancelable(false)  // Tidak dapat ditutup tanpa koneksi
                .show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister callback saat activity dihancurkan
        if (isNetworkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)  // Menggunakan objek yang sudah terdaftar
                isNetworkCallbackRegistered = false  // Tandai callback telah di-unregister
            } catch (e: IllegalArgumentException) {
                e.printStackTrace() // Handle exception jika callback sudah tidak terdaftar
            }
        }
        try {
            if (updateNamaTokoReceiver != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                unregisterReceiver(updateNamaTokoReceiver)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

}


