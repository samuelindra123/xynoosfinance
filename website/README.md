# XynoosFinance website

Website resmi dan panduan aplikasi Android. Menggunakan Next.js App Router, React, Tailwind CSS, dan React Icons.

## Pengembangan

Gunakan Node.js 22 atau lebih baru.

```bash
npm ci
npm run dev
```

Buka `http://localhost:3000`. Jalankan `npm run lint`, `npm run typecheck`, dan `npm run build` sebelum mengirim perubahan.

Halaman rilis dan unduhan mengambil daftar rilis publik dari GitHub API pada browser. Jika GitHub tidak dapat diakses atau membatasi permintaan, tersedia tautan ke halaman GitHub Releases. Tombol unduh langsung hanya ditampilkan jika rilis terbaru menyediakan aset APK bernama `*-Release.apk`. Data rilis bergantung pada akses jaringan dan publikasi di repositori.

Website dapat dijalankan dengan `npm run build` lalu `npm start` atau di-deploy ke penyedia yang mendukung Next.js. Tentukan `website` sebagai root directory pada pengaturan deployment.
