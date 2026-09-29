package com.example.hangman

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    companion object {
        // tahap permainan -> menentukan overlay mana yang tampil
        private const val TAHAP_MULAI = 0
        private const val TAHAP_LEVEL = 1
        private const val TAHAP_MAIN = 2
        private const val TAHAP_SELESAI = 3

        private const val POIN_MENANG = 10

        // kunci untuk onSaveInstanceState (latihan 6)
        private const val KEY_KATA = "kata"
        private const val KEY_BENAR = "benar"
        private const val KEY_SALAH = "salah"
        private const val KEY_MAKS = "maks"
        private const val KEY_SKOR = "skor"
        private const val KEY_TAHAP = "tahap"
        private const val KEY_MENANG = "menang"
    }

    // ==== data permainan ====
    private lateinit var kataRahasia: String
    private val hurufBenar = mutableSetOf<Char>()
    private val hurufSalah = mutableSetOf<Char>()
    private var maksNyawa = 6
    private var skor = 0
    private var tahap = TAHAP_MULAI
    private var menang = false

    // ==== view ====
    private lateinit var tvNyawa: TextView
    private lateinit var tvSkor: TextView
    private lateinit var tvKata: TextView
    private lateinit var tvPetunjuk: TextView
    private lateinit var tvRiwayat: TextView
    private lateinit var etTebak: EditText
    private lateinit var gridHuruf: GridLayout
    private lateinit var overlayHasil: LinearLayout
    private lateinit var overlayMulai: LinearLayout
    private lateinit var overlayLevel: LinearLayout
    private lateinit var tvHasil: TextView
    private lateinit var tvKataAkhir: TextView
    private lateinit var tvSkorHasil: TextView

    private lateinit var bagianTubuh: List<ImageView>
    private val tombolHuruf = mutableMapOf<Char, Button>()

    // =====================================================
    // SIKLUS HIDUP
    // =====================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ikatView()
        buatTombolHuruf()
        pasangListener()

        if (savedInstanceState == null) {
            siapkanRonde()                       // pertama kali dibuka
        } else {
            pulihkanState(savedInstanceState)    // setelah layar diputar
        }
        tampilkanTahap()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_KATA, kataRahasia)
        outState.putString(KEY_BENAR, hurufBenar.joinToString(""))
        outState.putString(KEY_SALAH, hurufSalah.joinToString(""))
        outState.putInt(KEY_MAKS, maksNyawa)
        outState.putInt(KEY_SKOR, skor)
        outState.putInt(KEY_TAHAP, tahap)
        outState.putBoolean(KEY_MENANG, menang)
    }

    private fun pulihkanState(s: Bundle) {
        kataRahasia = s.getString(KEY_KATA)
            ?: resources.getStringArray(R.array.daftar_kata).random()
        hurufBenar.clear()
        hurufBenar.addAll((s.getString(KEY_BENAR) ?: "").toList())
        hurufSalah.clear()
        hurufSalah.addAll((s.getString(KEY_SALAH) ?: "").toList())
        maksNyawa = s.getInt(KEY_MAKS, 6)
        skor = s.getInt(KEY_SKOR, 0)
        tahap = s.getInt(KEY_TAHAP, TAHAP_MULAI)
        menang = s.getBoolean(KEY_MENANG, false)

        if (tahap == TAHAP_SELESAI) isiOverlayHasil()
        gambarPapan()
    }

    // =====================================================
    // PERSIAPAN VIEW
    // =====================================================
    private fun ikatView() {
        tvNyawa = findViewById(R.id.tvNyawa)
        tvSkor = findViewById(R.id.tvSkor)
        tvKata = findViewById(R.id.tvKata)
        tvPetunjuk = findViewById(R.id.tvPetunjuk)
        tvRiwayat = findViewById(R.id.tvRiwayat)
        etTebak = findViewById(R.id.etTebak)
        gridHuruf = findViewById(R.id.gridHuruf)
        overlayHasil = findViewById(R.id.overlayHasil)
        overlayMulai = findViewById(R.id.overlayMulai)
        overlayLevel = findViewById(R.id.overlayLevel)
        tvHasil = findViewById(R.id.tvHasil)
        tvKataAkhir = findViewById(R.id.tvKataAkhir)
        tvSkorHasil = findViewById(R.id.tvSkorHasil)

        // urutan list = urutan kemunculan bagian tubuh
        bagianTubuh = listOf(
            findViewById(R.id.imgKepala),
            findViewById(R.id.imgBadan),
            findViewById(R.id.imgTanganKiri),
            findViewById(R.id.imgTanganKanan),
            findViewById(R.id.imgKakiKiri),
            findViewById(R.id.imgKakiKanan)
        )
    }

    /** Latihan 1: membuat 26 tombol huruf A-Z di dalam GridLayout. */
    private fun buatTombolHuruf() {
        val density = resources.displayMetrics.density
        val tinggi = (40 * density).toInt()
        val jarak = (2 * density).toInt()

        for (huruf in 'A'..'Z') {
            val tombol = MaterialButton(this).apply {
                text = huruf.toString()
                textSize = 13f
                setPadding(0, 0, 0, 0)
                insetTop = 0
                insetBottom = 0
                minWidth = 0
                minimumWidth = 0
                setOnClickListener { etTebak.setText(huruf.toString()) }
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)   // tiap kolom sama lebar
            ).apply {
                width = 0
                height = tinggi
                setMargins(jarak, jarak, jarak, jarak)
            }
            gridHuruf.addView(tombol, lp)
            tombolHuruf[huruf] = tombol
        }
    }

    private fun pasangListener() {
        findViewById<Button>(R.id.btnTebak).setOnClickListener { prosesTebakan() }
        etTebak.setOnEditorActionListener { _, _, _ -> prosesTebakan(); true }

        findViewById<Button>(R.id.btnMulai).setOnClickListener { keTahapLevel() }
        findViewById<Button>(R.id.btnGantiLevel).setOnClickListener { keTahapLevel() }
        findViewById<Button>(R.id.btnMainLagi).setOnClickListener {
            tahap = TAHAP_MAIN
            siapkanRonde()
            tampilkanTahap()
        }

        findViewById<Button>(R.id.btnMudah).setOnClickListener { pilihLevel(8) }
        findViewById<Button>(R.id.btnSedang).setOnClickListener { pilihLevel(6) }
        findViewById<Button>(R.id.btnSulit).setOnClickListener { pilihLevel(4) }
    }

    // =====================================================
    // ALUR PERMAINAN
    // =====================================================
    private fun keTahapLevel() {
        tahap = TAHAP_LEVEL
        tampilkanTahap()
    }

    /** Latihan 5: nyawa ditentukan oleh level yang dipilih. */
    private fun pilihLevel(nyawa: Int) {
        maksNyawa = nyawa
        tahap = TAHAP_MAIN
        siapkanRonde()
        tampilkanTahap()
    }

    /** Mengambil kata acak dan mengosongkan tebakan. */
    private fun siapkanRonde() {
        val daftarKata = resources.getStringArray(R.array.daftar_kata)
        kataRahasia = daftarKata.random()
        hurufBenar.clear()
        hurufSalah.clear()
        etTebak.text.clear()
        gambarPapan()
    }

    /** Menampilkan overlay sesuai tahap. Hanya mengubah visibility (inti FrameLayout). */
    private fun tampilkanTahap() {
        overlayMulai.visibility = if (tahap == TAHAP_MULAI) View.VISIBLE else View.GONE
        overlayLevel.visibility = if (tahap == TAHAP_LEVEL) View.VISIBLE else View.GONE
        overlayHasil.visibility = if (tahap == TAHAP_SELESAI) View.VISIBLE else View.GONE
        aturInput()
    }

    /** Input & tombol huruf hanya aktif saat tahap MAIN. */
    private fun aturInput() {
        val aktif = tahap == TAHAP_MAIN
        etTebak.isEnabled = aktif
        tombolHuruf.forEach { (huruf, tombol) ->
            tombol.isEnabled = aktif && huruf !in hurufBenar && huruf !in hurufSalah
        }
    }

    // =====================================================
    // GAMBAR PAPAN
    // =====================================================
    private fun gambarPapan() {
        perbaruiGantungan(hurufSalah.size)

        tvKata.text = kataRahasia
            .map { if (hurufBenar.contains(it)) it else '_' }
            .joinToString(" ")

        tvNyawa.text = "Nyawa: ${maksNyawa - hurufSalah.size}"
        tvSkor.text = "Skor: $skor"

        tvRiwayat.text = if (hurufSalah.isEmpty()) {
            "Salah: -"
        } else {
            "Salah: ${hurufSalah.joinToString(" ")}"
        }

        // Latihan 3: petunjuk sesuai kata (indeks array sama)
        val indeks = resources.getStringArray(R.array.daftar_kata).indexOf(kataRahasia)
        val petunjuk = resources.getStringArray(R.array.daftar_petunjuk)
        tvPetunjuk.text = if (indeks in petunjuk.indices) "Petunjuk: ${petunjuk[indeks]}" else ""

        aturInput()
    }

    /**
     * Menampilkan bagian tubuh sesuai jumlah kesalahan.
     * Jumlah bagian dihitung proporsional supaya semua level (4/6/8 nyawa)
     * tetap menampilkan 6 bagian tubuh lengkap saat nyawa habis.
     * Latihan 2: bagian yang baru muncul dianimasikan alpha 0 -> 1.
     */
    private fun perbaruiGantungan(kesalahan: Int) {
        val jumlah = bagianTubuh.size
        val tampil = (kesalahan * jumlah + maksNyawa - 1) / maksNyawa

        bagianTubuh.forEachIndexed { urutan, gambar ->
            if (urutan < tampil) {
                if (gambar.visibility != View.VISIBLE) {
                    gambar.alpha = 0f
                    gambar.visibility = View.VISIBLE
                    gambar.animate().alpha(1f).setDuration(400).start()
                }
            } else {
                gambar.animate().cancel()
                gambar.alpha = 1f
                gambar.visibility = View.INVISIBLE   // INVISIBLE agar ruang tetap
            }
        }
    }

    // =====================================================
    // TEBAKAN
    // =====================================================
    /** Dari EditText + tombol Tebak. */
    private fun prosesTebakan() {
        val input = etTebak.text.toString().trim().uppercase()

        if (input.length != 1 || !input[0].isLetter()) {
            Toast.makeText(this, "Masukkan satu huruf", Toast.LENGTH_SHORT).show()
            etTebak.text.clear()
            return
        }
        tebakHuruf(input[0])
    }

    /** Dipakai bersama oleh EditText dan tombol A-Z. */
    private fun tebakHuruf(huruf: Char) {
        if (tahap != TAHAP_MAIN) return

        if (hurufBenar.contains(huruf) || hurufSalah.contains(huruf)) {
            Toast.makeText(this, "Huruf $huruf sudah ditebak", Toast.LENGTH_SHORT).show()
            etTebak.text.clear()
            return
        }

        if (kataRahasia.contains(huruf)) hurufBenar.add(huruf) else hurufSalah.add(huruf)

        etTebak.text.clear()
        gambarPapan()
        periksaAkhirPermainan()
    }

    private fun periksaAkhirPermainan() {
        val semuaTerbuka = kataRahasia.all { hurufBenar.contains(it) }
        when {
            semuaTerbuka -> selesaiPermainan(true)                    // cek menang dulu
            hurufSalah.size >= maksNyawa -> selesaiPermainan(false)
        }
    }

    // =====================================================
    // OVERLAY HASIL
    // =====================================================
    private fun selesaiPermainan(hasilMenang: Boolean) {
        menang = hasilMenang
        if (menang) skor += POIN_MENANG          // latihan 4
        tahap = TAHAP_SELESAI

        gambarPapan()                            // perbarui skor di baris judul
        isiOverlayHasil()
        tampilkanTahap()

        // animasi memudar: alpha 0 -> tampil -> animate ke 1
        overlayHasil.alpha = 0f
        overlayHasil.animate().alpha(1f).setDuration(350).start()
        overlayHasil.requestFocus()
    }

    /** Hanya mengisi teks; dipisah agar bisa dipakai lagi saat state dipulihkan. */
    private fun isiOverlayHasil() {
        tvHasil.text = if (menang) getString(R.string.menang) else getString(R.string.kalah)
        tvHasil.setTextColor(if (menang) 0xFF3DDC84.toInt() else 0xFFF87171.toInt())
        tvKataAkhir.text = "Kata: $kataRahasia"

        val tambahan = if (menang) POIN_MENANG else 0
        tvSkorHasil.text = "+$tambahan poin  •  Skor: $skor"
    }
}