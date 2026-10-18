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

## Identitas Pengembang

- **Pengembang Platform**: **Xynoos Technology**
- **Produk**: **XynoosFinance** — *Smart Personal & Business Finance Manager*
- **Platform Target**: Android (Ponsel, Foldable, & Tablet)

---
*© Xynoos Technology. Seluruh hak cipta dilindungi undang-undang.*
