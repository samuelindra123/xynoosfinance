# XynoosFinance — Smart Personal & Business Financial Platform

**Dibangun dan Dikembangkan oleh Xynoos Technology**

---

## Tentang Platform

**XynoosFinance** adalah platform manajemen keuangan pribadi dan bisnis modern berbasis Android yang dirancang dan dibangun oleh **Xynoos Technology**. Platform ini menggabungkan pencatatan keuangan multi-rekening presisi tinggi, pemantauan nilai tukar mata uang *real-time* (IDR/USD), grafik pasar bergaya terminal finansial profesional, asisten kecerdasan finansial interaktif, serta sistem pelaporan terintegrasi dalam satu ekosistem aplikasi yang cepat, aman, dan adaptif di segala ukuran layar.

---

## Fitur Utama

### 1. Layar Hitung Nominal Saldo Real-Time (`XynoosCurrencyNominalInput`)
- **Digital Balance Counter**: Menampilkan angka nominal saldo besar dengan pemisah ribuan otomatis (`Rp 15.250.000`) saat pengguna mengetik.
- **Indikator Skala & Digit**: Penanda skala otomatis (`RIBUAN`, `JUTAAN`, `MILIARAN`, `TRILIUNAN`) dan penghitung jumlah digit untuk mencegah kesalahan input nol.
- **Rincian Satuan & Ejaan Terbilang**: Menampilkan rincian nilai satuan (contoh: `15 Juta + 250 Ribu`) serta kalimat **Terbilang Resmi Bahasa Indonesia** secara langsung.
- **Kalkulator Nominal Cepat & Simulasi Saldo Rekening**: Dilengkapi tombol pengali cepat (`+000`, `+000.000`), penambahan nominal instan (`+50 Rb` s/d `+10 Jt`), serta pratinjau saldo awal dan saldo akhir rekening sebelum transaksi disimpan.

### 2. Terminal Pasar & Grafik Saham / Valuta Asing Real-Time (USD/IDR)
- **Live USD Rate Per Menit**: Pembaruan kurs Dolar Amerika Serikat (USD/IDR) secara langsung setiap 1 menit dengan indikator arah pergerakan pasar (*Bullish* / *Bearish*).
- **Grafik Candlestick & Area Chart Profesional**: Visualisasi arus kas dan pergerakan kurs USD/IDR dengan grafik *candlestick* OHLC (*Open, High, Low, Close*) interaktif.
- **Konversi Multi-Mata Uang Otomatis**: Seluruh total kekayaan bersih (*Net Worth*), saldo rekening, dan target tabungan dapat dilihat dalam ekuivalen **Real USD** secara langsung.

### 3. Target Tabungan Terkoneksi Otomatis dengan Total Saldo
- **Auto-Sync Total Saldo**: Target tabungan dapat dihubungkan langsung ke **Total Saldo Kekayaan Bersih** tanpa perlu penambahan manual berulang, dilengkapi perhitungan persentase progres dan sisa kebutuhan dana otomatis.
- **Smart Budgeting**: Pemantauan batas anggaran pengeluaran per kategori dengan peringatan dini saat mendekati ambang batas.

### 4. Asisten Keuangan Interaktif & Pencatat Pintar
- **Mode Tanya Jawab & Konsultasi Finansial**: Diskusi interaktif seputar strategi menabung, analisis arus kas, alokasi investasi, dan evaluasi kesehatan finansial berdasarkan data aktual pengguna.
- **Mode Pencatatan Otomatis**: Mengubah kalimat transaksi sehari-hari menjadi draf transaksi terstruktur lengkap dengan kategori, nominal, dan waktu detail.
- **Desain Input Ergonomis**: Tata letak adaptif yang otomatis mengikuti pergerakan *keyboard* (`imePadding` & *auto-scroll*) sehingga teks yang diketik selalu terlihat jelas.

### 5. Ekspor Kartu Status Laporan Keuangan & Integrasi WhatsApp Gratis
- **Generator Gambar Laporan Keuangan HD**: Mengekspor ringkasan laporan keuangan, rasio tabungan, nilai ekuivalen USD, dan grafik tren menjadi kartu gambar beresolusi tinggi siap bagikan (*shareable image card*).
- **Notifikasi & Laporan WhatsApp (`XynoosFinance`)**: Pengiriman rincian transaksi lengkap (tanggal, bulan, tahun, jam:menit) dan laporan status keuangan berlabel resmi **XynoosFinance** langsung ke nomor WhatsApp pengguna tanpa biaya berlangganan.

### 6. Pencatatan Waktu Presisi Tinggi & Manajemen Bisnis
- **Timestamp Lengkap (Tanggal, Bulan, Tahun, Jam:Menit)**: Setiap transaksi wajib mencatat detail waktu lengkap hingga tahun dan jam transaksi.
- **Manajemen Kas Bisnis & Invoice**: Pemisahan pembukuan kas operasional bisnis, pelacakan status tagihan (*Invoice*), serta jadwal transaksi berulang (*Recurring*).

---

## Arsitektur & Teknologi (Xynoos Technology Stack)

Platform Android ini dibangun menggunakan standar rekayasa perangkat lunak modern oleh **Xynoos Technology**:

- **Bahasa Pemrograman**: 100% Kotlin dengan *Kotlin Coroutines* & *StateFlow* untuk pemrosesan asinkron yang reaktif.
- **Antarmuka Pengguna (UI)**: **Jetpack Compose** dengan sistem desain **Material Design 3 (M3)** yang dikustomisasi menggunakan identitas visual *Obsidian & Emerald Financial Theme* khas Xynoos Technology.
- **Desain Responsif & Adaptif**: Mendukung penuh berbagai ukuran layar (*Compact*, *Medium*, *Expanded*) mulai dari ponsel vertikal, perangkat layar lipat (*foldable*), hingga tablet dan lanskap menggunakan *Window Size Classes* dan `BoxWithConstraints`.
- **Infrastruktur Cloud & Penyimpanan Lokal**:
  - **Firebase Firestore & Authentication**: Sinkronisasi cloud *real-time* lintas perangkat dengan aturan keamanan berlapis (*zero-trust security rules*) dan Google Sign-In melalui Jetpack `CredentialManager`.
  - **Room Database & DataStore**: Penyimpanan lokal terstruktur dan pengelolaan preferensi instan untuk keandalan *offline-first*.
- **Visualisasi Data**: Mesin grafik kustom berbasis *Jetpack Compose Canvas* untuk merender grafik *Candlestick* pasar saham, *Sparkline* valuta asing, dan *Donut Chart* alokasi anggaran secara presisi.

---

## Struktur Direktori Proyek

```text
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── data/          # Repositori data, Room DAO, model entitas, dan sinkronisasi Firestore
│   │   │   ├── domain/        # Kalkulator finansial, IdrFormatter, Terbilang, & logika analisis
│   │   │   ├── ui/
│   │   │   │   ├── components/# Komponen khusus Xynoos (XynoosCurrencyNominalInput, Grafik Saham, Logo)
│   │   │   │   ├── screens/   # Layar Dashboard, Transaksi, Target, Pasar USD Live, Bisnis, & Laporan
│   │   │   │   ├── theme/     # Sistem warna, tipografi, dan tema XynoosFinance
│   │   │   │   └── util/      # Generator kartu gambar laporan HD & integrasi pengiriman WhatsApp
│   │   │   └── MainActivity.kt
│   │   └── res/               # Sumber daya string, ikon adaptif, dan konfigurasi XML
├── firestore.rules            # Aturan keamanan database cloud XynoosFinance
└── build.gradle.kts           # Konfigurasi build Gradle (Kotlin DSL)
```

---

## Unduh & Distribusi APK (GitHub Releases)

Platform **XynoosFinance** oleh **Xynoos Technology** menyediakan dua cara mudah untuk mendapatkan dan menyebarluaskan file instalasi Android (**APK**) ke publik melalui **GitHub Releases**:

### Cara 1: Unduh APK Langsung & Upload ke GitHub Releases (Paling Cepat & Direkomendasikan)
1. **Unduh APK dari Editor**:
   - Klik menu **Settings / Export (ikon di pojok kanan atas layar)** lalu pilih **Download APK** (atau **Generate APK / AAB**).
2. **Push Kode ke GitHub**:
   - Klik tombol **GitHub (Push to GitHub)** di pojok kanan atas untuk mengunggah seluruh kode sumber dan `README.md` ini ke repositori GitHub Anda.
3. **Terbitkan di GitHub Releases Agar Bisa Disebarluaskan**:
   - Buka repositori GitHub Anda di browser, lalu klik menu **Releases** (di kolom sebelah kanan) → klik **Create a new release** / **Draft a new release**.
   - Ketik tag versi (misalnya `v1.0.0`) dan judul rilis **`XynoosFinance v1.0.0 — Official Android APK by Xynoos Technology`**.
   - Seret (*drag & drop*) file **APK** yang sudah Anda unduh ke kotak **Attach binaries by dropping them here or selecting them**.
   - Klik **Publish release**. Sekarang tautan rilis APK siap disebarluaskan ke siapa saja!

### Cara 2: Otomatisasi Build APK via GitHub Actions (`scripts/release-apk.yml`)
- File konfigurasi CI/CD otomatis telah disediakan di **`scripts/release-apk.yml`** (ditempatkan di folder `scripts/` agar proses *Push to GitHub* tidak tertolak oleh pembatasan izin *OAuth `workflows` scope* dari GitHub App).
- Jika Anda ingin mengaktifkan pembuatan APK otomatis di server GitHub setiap kali ada pembaruan:
  1. Setelah melakukan *Push to GitHub*, buka file **`scripts/release-apk.yml`** di web GitHub Anda.
  2. Klik ikon pensil (**Edit this file**), lalu ubah nama jalur file menjadi **`.github/workflows/release-apk.yml`** dan klik **Commit changes**.
  3. GitHub Actions akan otomatis membangun dan menerbitkan **`XynoosFinance-Release.apk`** ke halaman **Releases**.

---

## Identitas Pengembang

- **Pengembang Platform**: **Xynoos Technology**
- **Produk**: **XynoosFinance** — *Smart Personal & Business Finance Manager*
- **Platform Target**: Android (Ponsel, Foldable, & Tablet)

---
*© Xynoos Technology. Seluruh hak cipta dilindungi undang-undang.*
