package com.mariefhermawan.kasmart

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class login : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private var alertDialog: AlertDialog? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback  // Menyimpan referensi callback
    private var isNetworkCallbackRegistered = false  // Flag untuk menandai status callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
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

        // Inisialisasi Firebase Database
        database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/").reference

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        // UI Adjustments for Edge-to-Edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil komponen dari layout
        val btnSignup = findViewById<Button>(R.id.btn_signup)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val seePasswordButton = findViewById<Button>(R.id.see_password)

        btnSignup.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                database.child("users").get()
                    .addOnSuccessListener { dataSnapshot ->
                        var emailExists = false
                        var passwordCorrect = false

                        for (userSnapshot in dataSnapshot.children) {
                            val dbEmail = userSnapshot.child("email").getValue(String::class.java)
                            val dbPassword = userSnapshot.child("password").getValue(String::class.java)
                            val storeName = userSnapshot.child("namaToko").getValue(String::class.java)
                            val usernames = userSnapshot.child("pemilik").getValue(String::class.java)
                            // Ambil unique key dari user yang login
                            val userUniqueKey = userSnapshot.key

                            if (dbEmail == email) {
                                emailExists = true
                                if (dbPassword == password) {
                                    passwordCorrect = true
                                    // Simpan informasi user ke SharedPreferences
                                    sharedPreferences.edit().apply {
                                        putString("USER_KEY", userUniqueKey) // Simpan unique key
                                        putString("USERNAME", usernames)
                                        putString("NAMA_TOKO", storeName)
                                        putString("EMAIL", email)
                                        putString("PASSWORD", password)
                                        apply()
                                    }

                                    // Login berhasil, buka MainActivity
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                    break
                                }
                            }
                        }

                        when {
                            !emailExists -> {
                                Toast.makeText(this, "Email tidak terdaftar. Silakan daftar akun baru.", Toast.LENGTH_LONG).show()
                            }
                            !passwordCorrect -> {
                                Toast.makeText(this, "Password salah. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengakses data pengguna.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Mohon isi email dan password.", Toast.LENGTH_SHORT).show()
            }
        }


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
