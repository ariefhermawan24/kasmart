package com.mariefhermawan.kasmart

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class splashscreen : AppCompatActivity() {

    private var noInternetDialog: AlertDialog? = null
    private val handler = Handler() // Initialize the Handler
    private var isMainActivityStarted = false // Flag to check if MainActivity is already started

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splashscreen)

        // Menambahkan insets untuk layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Konfigurasi VideoView
        val videoView: VideoView = findViewById(R.id.videoView)
        val videoPath = "android.resource://${packageName}/${R.raw.splashscreen}"
        val videoUri = Uri.parse(videoPath)

        videoView.setVideoURI(videoUri)

        // Scaling manual untuk menyesuaikan ukuran layar
        videoView.setOnPreparedListener { mediaPlayer ->
            val videoWidth = mediaPlayer.videoWidth
            val videoHeight = mediaPlayer.videoHeight
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            val layoutParams = videoView.layoutParams
            if (videoWidth > videoHeight) {
                layoutParams.width = screenWidth
                layoutParams.height = (videoHeight * screenWidth / videoWidth)
            } else {
                layoutParams.width = (videoWidth * screenHeight / videoHeight)
                layoutParams.height = screenHeight
            }
            videoView.layoutParams = layoutParams
        }

        // Cek koneksi internet saat video selesai
        videoView.setOnCompletionListener {
            if (isInternetAvailable()) {
                // Navigasi ke MainActivity jika koneksi internet tersedia
                if (!isMainActivityStarted) { // Ensure MainActivity is only started once
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("result_code", 200)
                    // Add flags to clear the back stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    isMainActivityStarted = true // Set the flag to prevent multiple starts
                    finish() // Finish the SplashScreen activity immediately
                }
            } else {
                // Tampilkan alert jika tidak ada koneksi internet
                showNoInternetAlert()
            }
        }

        // Memulai video
        videoView.start()
    }

    // Fungsi untuk memeriksa koneksi internet
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Menampilkan AlertDialog yang tidak bisa hilang sampai ada koneksi internet
    private fun showNoInternetAlert() {
        if (noInternetDialog == null) {
            // Membuat AlertDialog tanpa tombol
            noInternetDialog = AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Aplikasi memerlukan internet untuk berjalan dengan sempurna.\n\nMenunggu koneksi internet...")
                .setCancelable(false) // Set agar dialog tidak bisa ditutup
                .create()

            noInternetDialog?.show()

            // Periksa koneksi internet secara terus menerus
            checkInternetAndProceed()
        }
    }

    // Fungsi untuk memeriksa koneksi dan melanjutkan ketika internet tersedia
    private fun checkInternetAndProceed() {
        val internetCheckRunnable = object : Runnable {
            override fun run() {
                if (isInternetAvailable()) {
                    // Pastikan activity masih dalam keadaan valid sebelum dismiss dialog
                    if (!isFinishing && !isDestroyed) {
                        noInternetDialog?.dismiss()
                    }

                    // Navigasi ke MainActivity setelah koneksi tersedia
                    if (!isMainActivityStarted) { // Ensure MainActivity is only started once
                        val intent = Intent(this@splashscreen, MainActivity::class.java)
                        intent.putExtra("result_code", 200)
                        // Add flags to clear the back stack
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        isMainActivityStarted = true // Set the flag to prevent multiple starts
                        finish() // Finish the SplashScreen activity immediately
                    }
                } else {
                    // Jika koneksi belum tersedia, coba lagi setelah 2 detik
                    noInternetDialog?.setMessage("Aplikasi memerlukan internet untuk berjalan dengan sempurna\n\nMenunggu koneksi internet...")
                    handler.postDelayed(this, 2000) // Memanggil pengecekan lagi setelah 2 detik
                }
            }
        }

        // Memulai pengecekan internet dengan Handler
        handler.post(internetCheckRunnable)
    }
}
