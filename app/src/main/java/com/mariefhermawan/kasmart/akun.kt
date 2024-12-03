package com.mariefhermawan.kasmart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.SharedPreferences
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast

class akun : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var namaToko: String
    private lateinit var receiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://kasmart-projek-c3076-default-rtdb.firebaseio.com/").reference
        sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_akun, container, false)

        // Ambil data dari SharedPreferences
        val username = sharedPreferences.getString("USERNAME", "Username tidak tersedia")
        namaToko = sharedPreferences.getString("NAMA_TOKO", "Nama toko tidak tersedia") ?: "Nama toko tidak tersedia"
        val emailUser = sharedPreferences.getString("EMAIL", "Email tidak tersedia")

        // Set data ke TextView
        val usernameTextView: TextView = view.findViewById(R.id.user_account)
        val namaTokoTextView: TextView = view.findViewById(R.id.nama_toko)
        val emailTextView: TextView = view.findViewById(R.id.email_account)
        val inventoryCount: TextView = view.findViewById(R.id.count_inventory)
        val HutangCount: TextView = view.findViewById(R.id.count_dept)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.mariefhermawan.kasmart.ACTION_UPDATE_NAMA_TOKO") {
                    val updatedNamaToko = intent.getStringExtra("NAMA_TOKO")
                    updatedNamaToko?.let {
                        fetchInventoryCount(it, inventoryCount)
                    }
                }
            }
        }

        // Register receiver
        requireContext().registerReceiver(receiver, IntentFilter("com.mariefhermawan.kasmart.ACTION_UPDATE_NAMA_TOKO"),
            Context.RECEIVER_NOT_EXPORTED)

        usernameTextView.text = username
        namaTokoTextView.text = namaToko
        emailTextView.text = emailUser

        // Ambil jumlah barang dari Firebase untuk nama toko yang sesuai
        fetchInventoryCount(namaToko, inventoryCount)
        fetchHutangCount(namaToko, HutangCount)

        // Tombol pengaturan akun
        val accountSettingsButton: Button = view.findViewById(R.id.account_settings)
        accountSettingsButton.setOnClickListener {
            showAccountSettingsDialog()
        }

        // Tombol logout
        val logoutButton: Button = view.findViewById(R.id.logout)
        logoutButton.setOnClickListener {
            // Membuat alert dialog untuk konfirmasi logout
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Konfirmasi Logout")
            builder.setMessage("Apakah Anda yakin ingin keluar?")
            builder.setPositiveButton("Ya") { dialog, which ->
                // Hapus data login dari SharedPreferences
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()

                // Logout dari Firebase
                auth.signOut()

                // Arahkan ke LoginActivity setelah logout
                val intent = Intent(activity, login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            builder.setNegativeButton("Tidak") { dialog, which ->
                // Menutup dialog jika user memilih 'Tidak'
                dialog.dismiss()
            }

            // Menampilkan alert dialog
            builder.show()
        }

        return view
    }

    private fun fetchInventoryCount(namaToko: String, inventoryCount: TextView) {
        // Referensi ke tabel data barang dalam database Firebase
        val inventoryRef = database.child("barang")

        // Query untuk mendapatkan data yang sesuai dengan nama toko
        inventoryRef.orderByChild("toko").equalTo(namaToko)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Hitung jumlah data yang sesuai
                    val totalInventory = dataSnapshot.childrenCount.toInt()
                    // Update TextView dengan jumlah total data yang sesuai
                    inventoryCount.text = totalInventory.toString()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Tangani jika terjadi error dalam pengambilan data
                    inventoryCount.text = "Error: ${databaseError.message}"
                }
            })
    }

    private fun fetchHutangCount(namaToko: String, HutangCount: TextView) {
        // Referensi ke tabel data hutang dalam database Firebase
        val HutangRef = database.child("hutang")

        // Query untuk mendapatkan data yang sesuai dengan nama toko dan status "Pending"
        HutangRef.orderByChild("toko")
            .equalTo(namaToko)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Hitung jumlah data dengan status "Pending"
                    var pendingCount = 0
                    for (hutangSnapshot in dataSnapshot.children) {
                        val status = hutangSnapshot.child("status").getValue(String::class.java)
                        if (status != null && status.equals("Pending", ignoreCase = true)) {
                            pendingCount++
                        }
                    }
                    // Update TextView dengan jumlah hutang yang statusnya "Pending"
                    HutangCount.text = pendingCount.toString()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Tangani jika terjadi error dalam pengambilan data
                    HutangCount.text = "Error: ${databaseError.message}"
                }
            })
    }

    private fun showAccountSettingsDialog() {
        // Daftar opsi pengaturan akun
        val settingsOptions = arrayOf("Edit Profil", "Ubah Kata Sandi", "Hapus Akun")

        // Inisialisasi AlertDialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Pengaturan Akun")
        builder.setItems(settingsOptions) { _, which ->
            when (which) {
                0 -> UserSettingsDialog()   // Edit Profil
                1 -> ChangePasswordDialog()       // Ubah Kata Sandi
                2 -> confirmDeleteAccount() // Hapus Akun
            }
        }
        builder.show() // Menampilkan dialog
    }

    private fun UserSettingsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_usersettings, null)
        builder.setView(dialogView)

        // Menampilkan dialog
        val dialog = builder.create()
        dialog.show()

        // Ambil referensi dari komponen-komponen di dialog
        val editUsername = dialogView.findViewById<EditText>(R.id.editUsername)
        val edittokoname = dialogView.findViewById<EditText>(R.id.edittokoname)
        val btnSave = dialogView.findViewById<Button>(R.id.logout)
        val btnReset = dialogView.findViewById<Button>(R.id.account_settings)

        // Ambil nilai dari SharedPreferences
        val username = sharedPreferences.getString("USERNAME", "Username tidak tersedia")
        val namaToko = sharedPreferences.getString("NAMA_TOKO", "Nama toko tidak tersedia") ?: "Nama toko tidak tersedia"

        // Set teks default pada EditText sesuai dengan nilai di SharedPreferences
        editUsername.setText(username)
        edittokoname.setText(namaToko)

        // Set listener untuk tombol Simpan
        btnSave.setOnClickListener {
            val newUsername = editUsername.text.toString().trim()
            val newTokoName = edittokoname.text.toString().trim()

            if (newUsername.isNotEmpty() && newTokoName.isNotEmpty()) {
                // Mengubah nama pengguna dan nama toko di Firebase
                updateUserSettings(newUsername, newTokoName)

                // Notify AkunFragment to update UI with latest data
                (activity as? akun)?.updateUI()
                // Tutup dialog setelah perubahan
                dialog.dismiss()

            } else {
                Toast.makeText(requireContext(), "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
            }
        }

        // Set listener untuk tombol Reset
        btnReset.setOnClickListener {
            // Reset form
            editUsername.text.clear()
            edittokoname.text.clear()
        }
    }

    private fun updateUI() {
        // Fetch the updated username and toko name from SharedPreferences
        val updatedUsername = sharedPreferences.getString("USERNAME", "Username tidak tersedia")
        val updatedTokoName = sharedPreferences.getString("NAMA_TOKO", "Nama toko tidak tersedia") ?: "Nama toko tidak tersedia"

        // Update the TextViews in the fragment with the new values
        val usernameTextView: TextView? = view?.findViewById(R.id.user_account)
        val namaTokoTextView: TextView? = view?.findViewById(R.id.nama_toko)

        // Set the updated values to the TextViews
        usernameTextView?.text = updatedUsername
        namaTokoTextView?.text = updatedTokoName
    }


    private fun updateUserSettings(newUsername: String, newTokoName: String) {
        val userkey = sharedPreferences.getString("USER_KEY", "User key tidak tersedia")
        val oldTokoName = sharedPreferences.getString("NAMA_TOKO", "Nama toko tidak tersedia") ?: "Nama toko tidak tersedia"

        userkey?.let { key ->
            // Referensi ke database pengguna
            val userRef = database.child("users").child(key)

            // Update nama pengguna dan nama toko
            val userUpdates = mapOf(
                "pemilik" to newUsername,
                "namaToko" to newTokoName
            )

            userRef.updateChildren(userUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update SharedPreferences with the new data
                    updateSharedPreferences(newUsername, newTokoName)
                    // Notify UI to update
                    updateUI(newUsername, newTokoName)

                    // Jika nama toko benar-benar diubah
                    if (oldTokoName != newTokoName) {
                        updateItemsByToko(oldTokoName, newTokoName)
                        updateHutangByToko(oldTokoName, newTokoName)
                        updateIncomeByToko(oldTokoName, newTokoName) { isSuccess ->
                            if (isSuccess) {
                                val intent = Intent("com.mariefhermawan.kasmart.ACTION_UPDATE_NAMA_TOKO")
                                intent.putExtra("NAMA_TOKO", newTokoName)
                                requireContext().sendBroadcast(intent)
                                Toast.makeText(requireContext(), "Pengaturan akun berhasil diubah.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Gagal memperbarui data terkait nama toko.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Pengaturan akun berhasil diubah.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal mengubah pengaturan akun.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateIncomeByToko(oldTokoName: String, newTokoName: String, callback: (Boolean) -> Unit) {
        // Referensi ke data income
        val incomeRef = database.child("income")

        // Cari income yang memiliki toko yang sama dengan oldTokoName
        incomeRef.child(oldTokoName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Jika ada data income yang ditemukan untuk toko yang sesuai
                if (dataSnapshot.exists()) {
                    // Ambil data yang ada pada oldTokoName
                    val incomeData = dataSnapshot.value as Map<String, Any>

                    // Buat referensi untuk menyimpan data dengan newTokoName
                    val newTokoRef = incomeRef.child(newTokoName)

                    // Simpan data dengan newTokoName
                    newTokoRef.setValue(incomeData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Hapus oldTokoName setelah data berhasil disalin
                            incomeRef.child(oldTokoName).removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    callback(true) // Berhasil
                                } else {
                                    // Gagal menghapus oldTokoName
                                    Toast.makeText(requireContext(), "Gagal menghapus data lama toko.", Toast.LENGTH_SHORT).show()
                                    callback(false) // Error saat mengambil data
                                }
                            }
                        } else {
                            // Gagal menyimpan data baru dengan newTokoName
                            Toast.makeText(requireContext(), "Gagal memperbarui data income.", Toast.LENGTH_SHORT).show()
                            callback(false) // Error saat mengambil data
                        }
                    }
                } else {
                    // Tidak ada data income untuk oldTokoName
                    Toast.makeText(requireContext(), "Tidak ditemukan income untuk toko yang lama.", Toast.LENGTH_SHORT).show()
                    callback(true) // Berhasil
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Tangani jika terjadi error saat mengambil data income
                Toast.makeText(requireContext(), "Gagal mengambil data income.", Toast.LENGTH_SHORT).show()
                callback(false) // Error saat mengambil data
            }
        })
    }

    private fun updateItemsByToko(oldTokoName: String, newTokoName: String) {
        val barangRef = database.child("barang")

        barangRef.orderByChild("toko").equalTo(oldTokoName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val barangUpdates = mapOf("toko" to newTokoName)

                    snapshot.ref.updateChildren(barangUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Update barang berhasil
                        } else {
                            Toast.makeText(requireContext(), "Gagal memperbarui nama toko di barang.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data barang.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateHutangByToko(oldTokoName: String, newTokoName: String) {
        val hutangRef = database.child("hutang")

        hutangRef.orderByChild("toko").equalTo(oldTokoName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val hutangUpdates = mapOf("toko" to newTokoName)

                    snapshot.ref.updateChildren(hutangUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                        } else {
                            Toast.makeText(requireContext(), "Gagal memperbarui nama toko di hutang.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data hutang.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSharedPreferences(newUsername: String, newTokoName: String) {
        val editor = sharedPreferences.edit()
        editor.putString("USERNAME", newUsername)
        editor.putString("NAMA_TOKO", newTokoName)
        editor.apply()  // Simpan perubahan
    }

    private fun updateUI(newUsername: String, newTokoName: String) {
        // Update the fragment UI with new username and toko name
        val usernameTextView: TextView? = view?.findViewById(R.id.user_account)
        val tokoNameTextView: TextView? = view?.findViewById(R.id.nama_toko)

        usernameTextView?.text = newUsername
        tokoNameTextView?.text = newTokoName

        // Update inventory count
        val inventoryCountTextView: TextView? = view?.findViewById(R.id.count_inventory)
        fetchInventoryCount(newTokoName, inventoryCountTextView!!)
    }

    private fun ChangePasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Ambil referensi dari komponen-komponen di dialog
        val passwordLamaEditText = dialogView.findViewById<EditText>(R.id.password_lama)
        val passwordBaruEditText = dialogView.findViewById<EditText>(R.id.password_baru)
        val verifikasiCheckBox = dialogView.findViewById<CheckBox>(R.id.verifikasi)
        val btnSave = dialogView.findViewById<Button>(R.id.logout)
        val btnReset = dialogView.findViewById<Button>(R.id.account_settings)

        // Set listener untuk tombol Simpan
        btnSave.setOnClickListener {
            val passwordLama = passwordLamaEditText.text.toString().trim()
            val passwordBaru = passwordBaruEditText.text.toString().trim()

            // Periksa apakah semua kolom diisi dan checkbox dicentang
            if (passwordLama.isNotEmpty() && passwordBaru.isNotEmpty() && verifikasiCheckBox.isChecked) {
                // Mengubah kata sandi
                changePassword(passwordLama, passwordBaru)
                dialog.dismiss() // Tutup dialog setelah perubahan
            } else {
                Toast.makeText(requireContext(), "Semua kolom harus diisi dan setujui perubahan", Toast.LENGTH_SHORT).show()
            }
        }

        // Set listener untuk tombol Reset
        btnReset.setOnClickListener {
            // Reset form
            passwordLamaEditText.text.clear()
            passwordBaruEditText.text.clear()
            verifikasiCheckBox.isChecked = false
        }
    }

    private fun changePassword(passwordLama: String, passwordBaru: String) {
        val userkey = sharedPreferences.getString("USER_KEY", "User key tidak tersedia")

        // Periksa apakah userkey valid
        userkey?.let { key ->
            val userRef = database.child("users").child(key)

            // Pertama, periksa kata sandi lama dengan mengacu pada data di Firebase
            userRef.child("password").get().addOnSuccessListener { snapshot ->
                val storedPassword = snapshot.value.toString()

                // Verifikasi jika kata sandi lama cocok
                if (storedPassword == passwordLama) {
                    // Jika cocok, update kata sandi baru
                    userRef.child("password").setValue(passwordBaru).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Kata sandi berhasil diubah.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Gagal mengubah kata sandi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Kata sandi lama tidak valid.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengambil data kata sandi.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus Akun")
            .setMessage("Apakah Anda yakin ingin menghapus akun ini?")
            .setPositiveButton("Hapus") { _, _ ->
                // Logika penghapusan akun dari Firebase atau SharedPreferences di sini
                deleteAccountFromFirebase()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAccountFromFirebase() {
        namaToko = sharedPreferences.getString("NAMA_TOKO", "Nama toko tidak tersedia") ?: "Nama toko tidak tersedia"
        val userkey = sharedPreferences.getString("USER_KEY", "User key tidak tersedia")
        userkey?.let { key ->
            database.child("users").child(key).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    deleteIncomeByToko(namaToko)
                    deleteHutangByToko(namaToko)
                    deleteItemsByToko(namaToko)
                    Toast.makeText(requireContext(), "Akun berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    // Hapus data login dari SharedPreferences
                    val editor = sharedPreferences.edit()
                    editor.clear()
                    editor.apply()
                    auth.signOut()
                    val intent = Intent(activity, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "Gagal menghapus akun.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteItemsByToko(namaToko: String) {
        // Referensi ke data barang yang memiliki toko yang sama dengan namaToko
        val barangRef = database.child("barang")

        // Cari barang yang memiliki toko yang sama dengan namaToko
        barangRef.orderByChild("toko").equalTo(namaToko).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Jika ada barang yang ditemukan, hapus data barang tersebut
                for (snapshot in dataSnapshot.children) {
                    snapshot.ref.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                        } else {
                            // Gagal menghapus barang
                            Toast.makeText(requireContext(), "Gagal menghapus barang terkait.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Tangani jika terjadi error saat mengambil data barang
                Toast.makeText(requireContext(), "Gagal mengambil data barang.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteIncomeByToko(namaToko: String) {
        val incomeRef = database.child("income")

        // Cari income yang memiliki toko yang sama dengan namaToko
        incomeRef.child(namaToko).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Jika ditemukan, hapus data income terkait
                    incomeRef.child(namaToko).removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus data income.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Jika tidak ada data untuk toko tersebut
                    Toast.makeText(requireContext(), "Tidak ditemukan data income untuk toko ini.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data income.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteHutangByToko(namaToko: String) {
        val hutangRef = database.child("hutang")

        // Cari hutang yang memiliki toko yang sama dengan namaToko
        hutangRef.orderByChild("toko").equalTo(namaToko).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    snapshot.ref.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                        } else {
                            // Gagal menghapus data hutang
                            Toast.makeText(requireContext(), "Gagal menghapus data hutang.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (!dataSnapshot.exists()) {
                    // Tidak ada data hutang yang sesuai dengan toko tersebut
                    Toast.makeText(requireContext(), "Tidak ditemukan data hutang untuk toko ini.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal mengambil data hutang.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister receiver
        requireContext().unregisterReceiver(receiver)
    }

}
