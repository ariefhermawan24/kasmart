package com.mariefhermawan.kasmart

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.util.Patterns
import androidx.appcompat.app.AlertDialog

class signup : AppCompatActivity() {
    private var isPasswordVisible = false
    private var isProcessing = false // Flag untuk memeriksa apakah sedang memproses
    private var alertDialog: AlertDialog? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback  // Menyimpan referensi callback
    private var isNetworkCallbackRegistered = false  // Flag untuk menandai status callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referensi ke komponen layout
        val passwordEditText = findViewById<EditText>(R.id.password)
        val seePasswordButton = findViewById<Button>(R.id.see_password)
        val btnCreateAccount = findViewById<Button>(R.id.btn_create)
        val pemilikEditText = findViewById<EditText>(R.id.pemilik)
        val namatokoEditText = findViewById<EditText>(R.id.namatoko)
        val emailEditText = findViewById<EditText>(R.id.email)

        // Toggle visibility dan ikon pada password ketika tombol ditekan
        seePasswordButton.setOnClickListener {
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                seePasswordButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.lihat_password, 0)
            } else {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                seePasswordButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.sembunyi_password, 0)
            }
            isPasswordVisible = !isPasswordVisible
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        // Fungsi untuk membuat akun di Realtime Database
        btnCreateAccount.setOnClickListener {
            if (isProcessing) return@setOnClickListener // Mencegah eksekusi ganda

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val pemilik = pemilikEditText.text.toString().trim()
            val namaToko = namatokoEditText.text.toString().trim()

            // Validasi input
            if (email.isEmpty() || password.isEmpty() || pemilik.isEmpty() || namaToko.isEmpty()) {
                Toast.makeText(this, "Silakan lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi format email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Masukkan email dengan format yang benar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi input "Pemilik" agar hanya mengandung huruf
            if (!pemilik.matches(Regex("^[a-zA-Z\\s]+$"))) {
                Toast.makeText(this, "Nama pemilik hanya boleh mengandung huruf", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Set tombol tidak aktif dan aktifkan flag
            btnCreateAccount.isEnabled = false
            isProcessing = true

            val database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/")
            val userRef = database.getReference("users")

            // Cek apakah email atau nama toko sudah ada
            userRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(this@signup, "Email sudah terdaftar. Gunakan email lain.", Toast.LENGTH_SHORT).show()
                        resetProcessingState(btnCreateAccount) // Reset state tombol
                    } else {
                        userRef.orderByChild("namaToko").equalTo(namaToko).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    Toast.makeText(this@signup, "Nama toko sudah digunakan. Gunakan nama toko lain.", Toast.LENGTH_SHORT).show()
                                    resetProcessingState(btnCreateAccount) // Reset state tombol
                                } else {
                                    val newUserRef = userRef.push()
                                    val user = mapOf(
                                        "pemilik" to pemilik,
                                        "namaToko" to namaToko,
                                        "email" to email,
                                        "password" to password
                                    )

                                    newUserRef.setValue(user)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@signup, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this@signup, "Gagal menyimpan data pengguna", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnCompleteListener {
                                            resetProcessingState(btnCreateAccount) // Reset state tombol
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@signup, "Terjadi kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                                resetProcessingState(btnCreateAccount) // Reset state tombol
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@signup, "Terjadi kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                    resetProcessingState(btnCreateAccount) // Reset state tombol
                }
            })
        }
    }

    private fun resetProcessingState(button: Button) {
        button.isEnabled = true
        isProcessing = false
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
    }
}

