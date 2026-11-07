# Form Inspeksi Kendaraan

Antarmuka React + TypeScript untuk formulir inspeksi kendaraan. Rute `/inspections/new` membuat draft, sedangkan `/inspections/:id` memuat dan mengubah draft atau inspeksi berstatus RECHECK. Status SUBMITTED dan APPROVED ditampilkan sebagai baca-saja.

## Menjalankan

1. Jalankan backend dari root repository: `docker compose up --build -d`.
2. Masuk ke `frontend`, jalankan `npm install` lalu `npm run dev`.
3. Buka alamat yang ditampilkan Vite, lalu kunjungi `/inspections/new`.

Vite meneruskan permintaan `/api` ke `http://localhost:8080`. Jika backend memakai alamat lain, buat `frontend/.env.local` dengan `VITE_API_URL=http://localhost:PORT` (lihat `.env.example`), lalu jalankan ulang `npm run dev`. Saat frontend dipasang, siapkan reverse proxy `/api` ke backend pada origin yang sama. Proxy Vite hanya berlaku saat pengembangan.

## Perilaku formulir

- Kendaraan diambil dari E1; titik inspeksi dibaca langsung dari `../checklist.json`.
- Simpan draft mengirim E2 untuk data baru atau E3 dengan `version` terkini untuk data yang sudah tersimpan. Titik yang belum diisi tidak dikirim saat menyimpan draft.
- Simpan dan kirim memvalidasi 14 titik, menyimpan data terlebih dahulu, lalu memanggil E6 dengan `to: SUBMITTED` dan `actor` dari nama pemeriksa. Jika transisi gagal, draft yang telah disimpan tetap dapat dibuka di URL inspeksinya.
- Tanpa kunci, titik ENGINE otomatis menjadi NOT_INSPECTABLE dengan alasan yang bisa diubah. Rusak berat wajib punya minimal satu nama file foto; aplikasi tidak mengunggah file.
- Konflik versi (HTTP 409) ditampilkan tanpa menimpa perubahan yang ada di server. Muat ulang halaman untuk mengambil versi terbaru sebelum mencoba lagi.
- Antarmuka menyediakan keadaan memuat, kosong, gagal, dan pemberitahuan hasil aksi. Tema terang atau gelap mengikuti pengaturan perangkat.

Perintah verifikasi: `npm run build`, `npm run lint`, dan `npm run test`. Pengujian browser menggunakan Chrome atau Edge lokal melalui `playwright-core` dan API tiruan, sehingga tidak mengubah data backend. Jika browser tidak terdeteksi otomatis, atur variabel lingkungan `CHROME_PATH` ke lokasi berkas executable Chrome atau Chromium.
